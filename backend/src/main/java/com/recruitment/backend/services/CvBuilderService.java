package com.recruitment.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recruitment.backend.domain.dtos.CvBuilder.AddCustomSectionRequest;
import com.recruitment.backend.domain.dtos.CvBuilder.CreateDraftFromTemplateRequest;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderDraftResponse;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderDraftPageResponse;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderTemplateResponse;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderValidationResult;
import com.recruitment.backend.domain.dtos.CvBuilder.ReorderCvBuilderSectionsRequest;
import com.recruitment.backend.domain.dtos.CvBuilder.UpdateDraftTemplateRequest;
import com.recruitment.backend.domain.dtos.CvBuilder.UpdateCvBuilderDraftRequest;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvStatus;
import com.recruitment.backend.domain.entities.CvBuilder.CvBuilderDraft;
import com.recruitment.backend.domain.entities.CvBuilder.CvBuilderDraftStatus;
import com.recruitment.backend.domain.entities.CvBuilder.CvBuilderTemplate;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.CvBuilderDraftRepository;
import com.recruitment.backend.repositories.CvBuilderTemplateRepository;
import com.recruitment.backend.repositories.CvRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class CvBuilderService {
    private static final String DEFAULT_SLOT = "main";

    private final CvBuilderTemplateRepository templateRepository;
    private final CvBuilderDraftRepository draftRepository;
    private final CandidateRepository candidateRepository;
    private final CvRepository cvRepository;
    private final ObjectMapper objectMapper;
    private final CvBuilderValidationService cvBuilderValidationService;

    @Transactional(readOnly = true)
    public List<CvBuilderTemplateResponse> getActiveTemplates() {
        return templateRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Transactional
    public CvBuilderDraftResponse createDraftFromTemplate(UUID currentUserId, CreateDraftFromTemplateRequest request) {
        if (request == null || request.getTemplateId() == null) {
            throw new AppException(ErrorCode.CV_BUILDER_TEMPLATE_REQUIRED);
        }

        Candidate candidate = candidateRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));

        CvBuilderTemplate template = templateRepository.findByIdAndIsActiveTrue(request.getTemplateId())
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_TEMPLATE_NOT_FOUND));

        String title = (request.getTitle() == null || request.getTitle().isBlank())
                ? "My CV Draft"
                : request.getTitle().trim();

        String initialContent = buildInitialContent(request.getProfileSeed(), template);

        CvBuilderDraft draft = CvBuilderDraft.builder()
                .candidate(candidate)
                .template(template)
                .title(title)
                .contentJson(initialContent)
                .status(CvBuilderDraftStatus.DRAFT)
                .build();

        CvBuilderDraft saved = draftRepository.save(draft);
        return toDraftResponse(saved);
    }

    @Transactional(readOnly = true)
    public CvBuilderDraftResponse getDraft(UUID currentUserId, UUID draftId) {
        CvBuilderDraft draft = draftRepository.findByIdAndCandidateUserId(draftId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_DRAFT_NOT_FOUND));

        return toDraftResponse(draft);
    }

    @Transactional(readOnly = true)
    public CvBuilderDraftPageResponse getDrafts(UUID currentUserId, String cursor, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, safeLimit);
        List<CvBuilderDraft> drafts;

        if (cursor == null || cursor.isBlank()) {
            drafts = draftRepository.findFirstDrafts(currentUserId, pageable);
        } else {
            CursorPayload payload = decodeCursor(cursor);
            drafts = draftRepository.findNextDrafts(currentUserId, payload.updatedAt, payload.draftId, pageable);
        }

        List<CvBuilderDraftResponse> items = drafts.stream()
                .map(this::toDraftResponse)
                .toList();

        String nextCursor = null;
        if (drafts.size() == safeLimit) {
            CvBuilderDraft last = drafts.get(drafts.size() - 1);
            nextCursor = encodeCursor(last.getUpdatedAt(), last.getId());
        }

        return CvBuilderDraftPageResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .build();
    }

    @Transactional
    public CvBuilderDraftResponse updateDraft(UUID currentUserId, UUID draftId, UpdateCvBuilderDraftRequest request) {
        CvBuilderDraft draft = draftRepository.findByIdAndCandidateUserId(draftId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_DRAFT_NOT_FOUND));

        if (request == null || request.getContentJson() == null || request.getContentJson().isBlank()) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
        }

        if (request.getVersion() != null && !request.getVersion().equals(draft.getVersion())) {
            throw new AppException(ErrorCode.CV_BUILDER_VERSION_CONFLICT);
        }

        String normalizedContent = normalizeAndValidateContent(request.getContentJson(), draft.getTemplate());

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            draft.setTitle(request.getTitle().trim());
        }
        draft.setContentJson(normalizedContent);

        CvBuilderDraft saved = draftRepository.save(draft);
        return toDraftResponse(saved);
    }

    @Transactional
    public CvBuilderDraftResponse updateDraftTemplate(UUID currentUserId, UUID draftId, UpdateDraftTemplateRequest request) {
        if (request == null || request.getTemplateId() == null) {
            throw new AppException(ErrorCode.CV_BUILDER_TEMPLATE_REQUIRED);
        }

        CvBuilderDraft draft = draftRepository.findByIdAndCandidateUserId(draftId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_DRAFT_NOT_FOUND));

        CvBuilderTemplate template = templateRepository.findByIdAndIsActiveTrue(request.getTemplateId())
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_TEMPLATE_NOT_FOUND));

        draft.setTemplate(template);

        String normalizedContent = normalizeAndValidateContent(draft.getContentJson(), template);
        draft.setContentJson(normalizedContent);

        CvBuilderDraft saved = draftRepository.save(draft);
        return toDraftResponse(saved);
    }

    @Transactional
    public CvBuilderDraftResponse addCustomSection(UUID currentUserId, UUID draftId, AddCustomSectionRequest request) {
        CvBuilderDraft draft = draftRepository.findByIdAndCandidateUserId(draftId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_DRAFT_NOT_FOUND));

        if (request == null || request.getSectionTitle() == null || request.getSectionTitle().isBlank()) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_SECTION);
        }

        ObjectNode root = readContentAsObjectNode(draft.getContentJson());
        ArrayNode sections = ensureSectionsArray(root);

        ObjectNode newSection = objectMapper.createObjectNode();
        newSection.put("sectionId", UUID.randomUUID().toString());
        String sectionType = normalizeSectionType(request.getSectionType());
        newSection.put("type", sectionType);
        newSection.put("title", request.getSectionTitle().trim());
        newSection.set("data", parseSectionData(request.getDataJson()));
        newSection.put("visible", true);
        newSection.put("slot", resolveDefaultSlot(draft.getTemplate(), sectionType));

        int insertIndex = resolveInsertIndex(request.getInsertAt(), sections.size());
        sections.insert(insertIndex, newSection);
        refreshSectionOrder(sections, draft.getTemplate());

        draft.setContentJson(writeNode(root));
        CvBuilderDraft saved = draftRepository.save(draft);
        return toDraftResponse(saved);
    }

    @Transactional
    public CvBuilderDraftResponse reorderSections(UUID currentUserId, UUID draftId, ReorderCvBuilderSectionsRequest request) {
        CvBuilderDraft draft = draftRepository.findByIdAndCandidateUserId(draftId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_DRAFT_NOT_FOUND));

        if (request == null || request.getSectionIds() == null || request.getSectionIds().isEmpty()) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_SECTION_ORDER);
        }

        ObjectNode root = readContentAsObjectNode(draft.getContentJson());
        ArrayNode sections = ensureSectionsArray(root);
        refreshSectionOrder(sections, draft.getTemplate());
        validateReorderRequest(request.getSectionIds(), sections);

        Map<String, ObjectNode> sectionById = mapSectionsById(sections);
        ArrayNode reorderedSections = objectMapper.createArrayNode();
        for (String sectionId : request.getSectionIds()) {
            reorderedSections.add(sectionById.get(sectionId));
        }

        refreshSectionOrder(reorderedSections, draft.getTemplate());
        root.set("sections", reorderedSections);

        draft.setContentJson(writeNode(root));
        CvBuilderDraft saved = draftRepository.save(draft);
        return toDraftResponse(saved);
    }

    @Transactional
    public CvBuilderDraftResponse deleteSection(UUID currentUserId, UUID draftId, String sectionId) {
        CvBuilderDraft draft = draftRepository.findByIdAndCandidateUserId(draftId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_DRAFT_NOT_FOUND));

        if (sectionId == null || sectionId.isBlank()) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_SECTION);
        }

        ObjectNode root = readContentAsObjectNode(draft.getContentJson());
        ArrayNode sections = ensureSectionsArray(root);

        int sectionIndex = findSectionIndexById(sections, sectionId.trim());
        if (sectionIndex < 0) {
            throw new AppException(ErrorCode.CV_BUILDER_SECTION_NOT_FOUND);
        }

        sections.remove(sectionIndex);
        refreshSectionOrder(sections, draft.getTemplate());

        draft.setContentJson(writeNode(root));
        CvBuilderDraft saved = draftRepository.save(draft);
        return toDraftResponse(saved);
    }

    @Transactional
    public CvBuilderDraftResponse publishDraft(UUID currentUserId, UUID draftId) {
        CvBuilderDraft draft = draftRepository.findByIdAndCandidateUserId(draftId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_BUILDER_DRAFT_NOT_FOUND));

        cvBuilderValidationService.validateStrict(draft.getContentJson(), draft.getTemplate());
        draft.setStatus(CvBuilderDraftStatus.PUBLISHED);

        CvBuilderDraft saved = draftRepository.save(draft);
        createCvRecordForDraft(saved);
        return toDraftResponse(saved);
    }

    private void createCvRecordForDraft(CvBuilderDraft draft) {
        String title = (draft.getTitle() == null || draft.getTitle().isBlank())
                ? "CV Builder"
                : draft.getTitle().trim();

        Cv cv = new Cv();
        cv.setCandidate(draft.getCandidate());
        cv.setCvName(title);
        cv.setFileUrl(null);
        cv.setIsDefault(false);
        cv.setAiStatus(CvStatus.COMPLETED);
        cv.setRawText(null);
        cv.setParsedData(draft.getContentJson());

        cvRepository.save(cv);
    }

    private String buildInitialContent(String profileSeed, CvBuilderTemplate template) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("sections", objectMapper.createArrayNode());
        root.set("blocks", buildDefaultBlocks(template));
        root.set("meta", buildMetaNode(template));
        if (profileSeed != null && !profileSeed.isBlank()) {
            root.put("profileSeed", profileSeed.trim());
        }

        return writeNode(root);
    }

    private ArrayNode buildDefaultBlocks(CvBuilderTemplate template) {
        ArrayNode blocks = objectMapper.createArrayNode();
        JsonNode layoutSchema = readTemplateLayoutSchema(template);

        blocks.add(createSectionBlock(
                "profile",
                "Thong tin ca nhan",
                false,
                true,
                new String[][]{
                        {"fullName", "Ho va ten", "text"},
                        {"position", "Vi tri ung tuyen", "text"},
                        {"phone", "So dien thoai", "phone"},
                        {"email", "Email", "email"},
                        {"location", "Dia chi", "text"},
                        {"website", "Website", "url"}
                },
                layoutSchema
        ));

        JsonNode allowedTypesNode = layoutSchema.path("allowedSectionTypes");
        if (allowedTypesNode.isArray()) {
            for (JsonNode typeNode : allowedTypesNode) {
                if (!typeNode.isTextual()) {
                    continue;
                }
                String type = typeNode.asText("custom").trim().toLowerCase();
                if (type.equals("custom")) {
                    continue;
                }
                blocks.add(createSectionBlock(
                        type,
                        capitalize(type),
                        isRepeatable(type),
                        false,
                        resolveDefaultFields(type),
                        layoutSchema
                ));
            }
        }

        return blocks;
    }

    private ObjectNode createSectionBlock(
            String sectionType,
            String title,
            boolean repeatable,
            boolean locked,
            String[][] fields,
            JsonNode layoutSchema
    ) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", UUID.randomUUID().toString());
        block.put("type", "section");
        block.put("slot", resolveDefaultSlotFromLayout(layoutSchema, sectionType));
        block.put("visible", true);
        block.put("locked", locked);

        ObjectNode props = objectMapper.createObjectNode();
        props.put("sectionType", sectionType);
        props.put("title", title);
        props.put("repeatable", repeatable);
        ArrayNode fieldDefs = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("id", UUID.randomUUID().toString());
        ObjectNode itemFields = objectMapper.createObjectNode();

        for (String[] field : fields) {
            ObjectNode fieldDef = objectMapper.createObjectNode();
            fieldDef.put("key", field[0]);
            fieldDef.put("label", field[1]);
            fieldDef.put("type", field[2]);
            fieldDefs.add(fieldDef);
            itemFields.put(field[0], "");
        }

        ArrayNode items = objectMapper.createArrayNode();
        item.set("fields", itemFields);
        items.add(item);

        props.set("fields", fieldDefs);
        props.set("items", items);
        block.set("props", props);
        return block;
    }

    private String resolveDefaultSlotFromLayout(JsonNode layoutSchema, String sectionType) {
        JsonNode defaultSlotByType = layoutSchema.path("layoutRules").path("defaultSlotByType");
        if (defaultSlotByType.isObject()) {
            JsonNode mapped = defaultSlotByType.path(sectionType);
            if (!mapped.isMissingNode() && !mapped.isNull() && !mapped.asText().isBlank()) {
                return mapped.asText();
            }
        }
        return DEFAULT_SLOT;
    }

    private boolean isRepeatable(String sectionType) {
        return switch (sectionType) {
            case "experience", "education", "project", "certification" -> true;
            default -> false;
        };
    }

    private String[][] resolveDefaultFields(String sectionType) {
        return switch (sectionType) {
            case "profile" -> new String[][]{
                    {"fullName", "Ho va ten", "text"},
                    {"position", "Vi tri ung tuyen", "text"},
                    {"phone", "So dien thoai", "phone"},
                    {"email", "Email", "email"},
                    {"location", "Dia chi", "text"},
                    {"website", "Website", "url"}
            };
            case "summary" -> new String[][]{{"content", "Noi dung", "textarea"}};
            case "experience" -> new String[][]{
                    {"company", "Ten cong ty", "text"},
                    {"role", "Vi tri", "text"},
                    {"startDate", "Bat dau", "month"},
                    {"endDate", "Ket thuc", "month"},
                    {"summary", "Mo ta", "textarea"}
            };
            case "education" -> new String[][]{
                    {"school", "Ten truong", "text"},
                    {"degree", "Nganh hoc", "text"},
                    {"startDate", "Bat dau", "month"},
                    {"endDate", "Ket thuc", "month"},
                    {"description", "Mo ta", "textarea"}
            };
            case "skills" -> new String[][]{{"skills", "Ky nang", "textarea"}};
            case "project" -> new String[][]{
                    {"name", "Ten du an", "text"},
                    {"role", "Vai tro", "text"},
                    {"description", "Mo ta", "textarea"},
                    {"technologies", "Cong nghe", "text"},
                    {"url", "Lien ket", "url"}
            };
            case "certification" -> new String[][]{
                    {"name", "Ten chung chi", "text"},
                    {"issuer", "Don vi cap", "text"},
                    {"issueDate", "Ngay cap", "month"}
            };
            default -> new String[][]{{"content", "Noi dung", "textarea"}};
        };
    }

    private String normalizeAndValidateContent(String contentJson, CvBuilderTemplate template) {
        ObjectNode root = readContentAsObjectNode(contentJson);
        ensureMetaNode(root, template);

        JsonNode blocksNode = root.path("blocks");
        if (blocksNode.isArray()) {
            return writeNode(root);
        }

        ArrayNode sections = ensureSectionsArray(root);
        refreshSectionOrder(sections, template);
        return writeNode(root);
    }

    private ObjectNode buildMetaNode(CvBuilderTemplate template) {
        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("schemaVersion", resolveSchemaVersion(template));
        meta.put("templateCode", template.getCode());
        meta.put("templateVersion", resolveTemplateVersion(template));
        return meta;
    }

    private void ensureMetaNode(ObjectNode root, CvBuilderTemplate template) {
        JsonNode metaNode = root.path("meta");
        ObjectNode meta;
        if (!metaNode.isObject()) {
            meta = objectMapper.createObjectNode();
            root.set("meta", meta);
        } else {
            meta = (ObjectNode) metaNode;
        }

        meta.put("schemaVersion", resolveSchemaVersion(template));
        meta.put("templateCode", template.getCode());
        meta.put("templateVersion", resolveTemplateVersion(template));
    }

    private ObjectNode readContentAsObjectNode(String contentJson) {
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            if (node == null || !node.isObject()) {
                throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
            }
            return (ObjectNode) node;
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
        }
    }

    private ArrayNode ensureSectionsArray(ObjectNode root) {
        JsonNode sectionsNode = root.path("sections");
        if (sectionsNode.isMissingNode() || sectionsNode.isNull()) {
            ArrayNode sections = objectMapper.createArrayNode();
            root.set("sections", sections);
            return sections;
        }

        if (!sectionsNode.isArray()) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
        }

        return (ArrayNode) sectionsNode;
    }

    private JsonNode parseSectionData(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(dataJson);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_SECTION);
        }
    }

    private int resolveInsertIndex(Integer insertAt, int maxSize) {
        if (insertAt == null) {
            return maxSize;
        }

        if (insertAt < 0 || insertAt > maxSize) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_SECTION_ORDER);
        }

        return insertAt;
    }

    private String normalizeSectionType(String sectionType) {
        if (sectionType == null || sectionType.isBlank()) {
            return "custom";
        }

        return sectionType.trim().toLowerCase();
    }

    private void refreshSectionOrder(ArrayNode sections, CvBuilderTemplate template) {
        for (int i = 0; i < sections.size(); i++) {
            JsonNode sectionNode = sections.get(i);
            if (!sectionNode.isObject()) {
                throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
            }

            ObjectNode section = (ObjectNode) sectionNode;
            String sectionType = normalizeSectionType(section.path("type").asText(null));
            section.put("type", sectionType);
            if (!section.hasNonNull("sectionId") || section.path("sectionId").asText().isBlank()) {
                section.put("sectionId", UUID.randomUUID().toString());
            }
            if (!section.hasNonNull("title") || section.path("title").asText().isBlank()) {
                section.put("title", sectionType.equals("custom") ? "Custom Section" : capitalize(sectionType));
            }
            if (!section.has("data") || section.path("data").isNull()) {
                section.set("data", objectMapper.createObjectNode());
            }
            if (!section.has("visible") || section.path("visible").isNull()) {
                section.put("visible", true);
            }
            if (!section.hasNonNull("slot") || section.path("slot").asText().isBlank()) {
                section.put("slot", resolveDefaultSlot(template, sectionType));
            }
            section.put("order", i);
        }
    }

    private String resolveDefaultSlot(CvBuilderTemplate template, String sectionType) {
        JsonNode layoutSchema = readTemplateLayoutSchema(template);
        JsonNode defaultSlotByType = layoutSchema.path("layoutRules").path("defaultSlotByType");
        if (defaultSlotByType.isObject()) {
            String normalizedType = normalizeSectionType(sectionType);
            JsonNode mapped = defaultSlotByType.path(normalizedType);
            if (!mapped.isMissingNode() && !mapped.isNull() && !mapped.asText().isBlank()) {
                return mapped.asText();
            }
            JsonNode custom = defaultSlotByType.path("custom");
            if (!custom.isMissingNode() && !custom.isNull() && !custom.asText().isBlank()) {
                return custom.asText();
            }
        }
        return DEFAULT_SLOT;
    }

    private int resolveSchemaVersion(CvBuilderTemplate template) {
        JsonNode layoutSchema = readTemplateLayoutSchema(template);
        return layoutSchema.path("schemaVersion").asInt(1);
    }

    private int resolveTemplateVersion(CvBuilderTemplate template) {
        JsonNode layoutSchema = readTemplateLayoutSchema(template);
        return layoutSchema.path("templateVersion").asInt(1);
    }

    private JsonNode readTemplateLayoutSchema(CvBuilderTemplate template) {
        String layoutSchema = template.getLayoutSchema();
        if (layoutSchema == null || layoutSchema.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            JsonNode parsed = objectMapper.readTree(layoutSchema);
            return parsed != null && parsed.isObject() ? parsed : objectMapper.createObjectNode();
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Custom Section";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void validateReorderRequest(List<String> requestedSectionIds, ArrayNode sections) {
        if (requestedSectionIds.size() != sections.size()) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_SECTION_ORDER);
        }

        Set<String> requestedSet = new HashSet<>();
        for (String sectionId : requestedSectionIds) {
            if (sectionId == null || sectionId.isBlank() || !requestedSet.add(sectionId)) {
                throw new AppException(ErrorCode.CV_BUILDER_INVALID_SECTION_ORDER);
            }
        }

        Set<String> existingSet = new HashSet<>();
        for (JsonNode sectionNode : sections) {
            if (!sectionNode.isObject()) {
                throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
            }

            String sectionId = sectionNode.path("sectionId").asText();
            if (sectionId.isBlank()) {
                throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
            }
            existingSet.add(sectionId);
        }

        if (!existingSet.equals(requestedSet)) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_SECTION_ORDER);
        }
    }

    private Map<String, ObjectNode> mapSectionsById(ArrayNode sections) {
        Map<String, ObjectNode> sectionById = new HashMap<>();
        for (JsonNode sectionNode : sections) {
            ObjectNode section = (ObjectNode) sectionNode;
            sectionById.put(section.path("sectionId").asText(), section);
        }
        return sectionById;
    }

    private int findSectionIndexById(ArrayNode sections, String sectionId) {
        for (int i = 0; i < sections.size(); i++) {
            JsonNode sectionNode = sections.get(i);
            if (!sectionNode.isObject()) {
                throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
            }

            if (sectionId.equals(sectionNode.path("sectionId").asText())) {
                return i;
            }
        }
        return -1;
    }

    private String writeNode(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_CONTENT);
        }
    }

    private String encodeCursor(LocalDateTime updatedAt, UUID draftId) {
        String payload = updatedAt.toString() + "|" + draftId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private CursorPayload decodeCursor(String cursor) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            LocalDateTime updatedAt = LocalDateTime.parse(parts[0]);
            UUID draftId = UUID.fromString(parts[1]);
            return new CursorPayload(updatedAt, draftId);
        } catch (Exception e) {
            throw new AppException(ErrorCode.CV_BUILDER_INVALID_CURSOR);
        }
    }

    private record CursorPayload(LocalDateTime updatedAt, UUID draftId) {}

    private CvBuilderTemplateResponse toTemplateResponse(CvBuilderTemplate template) {
        return CvBuilderTemplateResponse.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .description(template.getDescription())
                .previewImageUrl(template.getPreviewImageUrl())
                .displayOrder(template.getDisplayOrder())
                .layoutSchema(template.getLayoutSchema())
                .build();
    }

    private CvBuilderDraftResponse toDraftResponse(CvBuilderDraft draft) {
        CvBuilderValidationResult validationResult = cvBuilderValidationService.validateSoft(
                draft.getContentJson(),
                draft.getTemplate()
        );

        return CvBuilderDraftResponse.builder()
                .id(draft.getId())
                .templateId(draft.getTemplate().getId())
                .templateCode(draft.getTemplate().getCode())
                .templateName(draft.getTemplate().getName())
                .sourceCvId(draft.getSourceCv() != null ? draft.getSourceCv().getId() : null)
                .title(draft.getTitle())
                .contentJson(draft.getContentJson())
                .status(draft.getStatus())
                .validationStatus(validationResult.getStatus())
                .validationIssues(validationResult.getIssues())
                .validatedAt(validationResult.getValidatedAt())
                .version(draft.getVersion())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }
}
