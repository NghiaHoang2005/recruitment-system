package com.recruitment.backend.services.ai.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
@Slf4j
public class JsonCleanerService {

    private static final Pattern XML_TAG_PATTERN = Pattern.compile("</?[a-zA-Z][^>]*>");
    private static final Pattern THINKING_TAG_PATTERN = Pattern.compile("<thinking>.*?</thinking>", Pattern.DOTALL);
    private static final Pattern THOUGHT_PREFIX_PATTERN = Pattern.compile("^[\"']?\\w+[\"']?>\\s*");

    public String cleanJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return rawResponse;
        }

        String cleaned = rawResponse.trim();
        boolean changed = false;

        // Remove markdown code fences
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            log.debug("Removed opening markdown fence");
            changed = true;
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.replaceFirst("\\s*```$", "");
            log.debug("Removed closing markdown fence");
            changed = true;
        }

        // Remove XML/thinking tags (e.g., <thinking>...</thinking>)
        if (cleaned.contains("<thinking>") || cleaned.contains("</thinking>")) {
            cleaned = THINKING_TAG_PATTERN.matcher(cleaned).replaceAll("");
            log.debug("Removed thinking tags");
            changed = true;
        }

        // Remove any other XML-like tags (e.g., <thought>, </thought>)
        if (XML_TAG_PATTERN.matcher(cleaned).find()) {
            cleaned = XML_TAG_PATTERN.matcher(cleaned).replaceAll("");
            log.debug("Removed XML tags");
            changed = true;
        }

        // Remove prefix patterns like thought">, mind">, idea">
        String trimmedCleaned = cleaned.trim();
        if (THOUGHT_PREFIX_PATTERN.matcher(trimmedCleaned).find()) {
            cleaned = THOUGHT_PREFIX_PATTERN.matcher(trimmedCleaned).replaceFirst("");
            log.debug("Removed thought/mind prefix");
            changed = true;
        }

        cleaned = cleaned.trim();

        if (changed) {
            log.info("Cleaned JSON response (removed markdown/XML wrappers)");
            log.debug("Cleaned result starts with: {}", 
                cleaned.substring(0, Math.min(50, cleaned.length())));
        }

        // Check for incomplete JSON and attempt completion
        if (isIncompleteJson(cleaned)) {
            log.warn("JSON appears incomplete, attempting auto-completion");
            cleaned = attemptJsonCompletion(cleaned);
        }

        return cleaned;
    }

    public boolean isIncompleteJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        
        String trimmed = json.trim();
        
        // Count opening and closing braces
        int openBraces = countCharacter(trimmed, '{');
        int closeBraces = countCharacter(trimmed, '}');
        
        if (openBraces != closeBraces) {
            log.warn("Brace mismatch: {} open, {} close", openBraces, closeBraces);
            return true;
        }
        
        // Check if ends with incomplete token (e.g., unclosed string or separator)
        if (endsWithIncompleteToken(trimmed)) {
            log.warn("JSON ends with incomplete token: {}", trimmed.substring(Math.max(0, trimmed.length() - 20)));
            return true;
        }
        
        return false;
    }

    private boolean endsWithIncompleteToken(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // Remove trailing whitespace for check
        String trimmed = str.replaceAll("\\s+$", "");
        
        // If ends with comma, colon, opening bracket/brace, or quote, it's incomplete
        return trimmed.endsWith(",") || 
               trimmed.endsWith(":") || 
               trimmed.endsWith("[") || 
               trimmed.endsWith("{") ||
               (trimmed.endsWith("\"") && !isCompleteString(trimmed));
    }

    public String attemptJsonCompletion(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        
        String completed = json.trim();
        
        // Count braces (accounting for strings)
        int openBraces = countCharacter(completed, '{');
        int closeBraces = countCharacter(completed, '}');
        int openBrackets = countCharacter(completed, '[');
        int closeBrackets = countCharacter(completed, ']');
        
        int bracesDiff = openBraces - closeBraces;
        int bracketsDiff = openBrackets - closeBrackets;
        
        // Add missing closing braces
        for (int i = 0; i < bracesDiff; i++) {
            completed += "}";
        }
        
        // Add missing closing brackets
        for (int i = 0; i < bracketsDiff; i++) {
            completed += "]";
        }
        
        if (bracesDiff > 0 || bracketsDiff > 0) {
            log.info("Completed JSON: added {} braces and {} brackets", bracesDiff, bracketsDiff);
        }
        
        return completed;
    }

    private boolean isCompleteString(String str) {
        // Count unescaped quotes
        int quoteCount = 0;
        boolean escaped = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                quoteCount++;
            }
        }
        
        // If even number of quotes, they're all complete
        return quoteCount % 2 == 0;
    }

    private int countCharacter(String str, char ch) {
        int count = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : str.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString && c == ch) {
                count++;
            }
        }
        
        return count;
    }

    public boolean isWrappedInMarkdown(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        String trimmed = json.trim();
        return trimmed.startsWith("```") && trimmed.endsWith("```");
    }

    public boolean isWrappedInXml(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        String trimmed = json.trim();
        return trimmed.contains("<") && trimmed.contains(">");
    }

    public String validateAndClean(String rawResponse) {
        if (isWrappedInMarkdown(rawResponse) || isWrappedInXml(rawResponse)) {
            log.warn("JSON wrapped in markdown/XML, cleaning...");
            return cleanJson(rawResponse);
        }
        return rawResponse;
    }
}
