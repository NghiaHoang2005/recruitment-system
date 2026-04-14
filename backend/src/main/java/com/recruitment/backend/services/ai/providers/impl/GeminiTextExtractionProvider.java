package com.recruitment.backend.services.ai.providers.impl;

import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.AiUsage;
import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;
import com.recruitment.backend.services.ai.providers.TextExtractionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.function.Supplier;

@Component
@Slf4j
@ConditionalOnProperty(
    name = "ai.extraction.active-provider",
    havingValue = "gemini",
    matchIfMissing = false
)
public class GeminiTextExtractionProvider implements TextExtractionProvider {

    private final AiProperties aiProperties;
    private final RetryStrategy retryStrategy;
    private final ChatClient chatClient;

    public GeminiTextExtractionProvider(
            AiProperties aiProperties,
            RetryStrategy retryStrategy,
            ChatClient.Builder chatClientBuilder
    ) {
        this.aiProperties = aiProperties;
        this.retryStrategy = retryStrategy;
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public StructuredExtractionResult extractStructured(StructuredExtractionRequest request) {
        long startTime = System.currentTimeMillis();
        
        String primaryModel = request.getModel();
        if (primaryModel == null || primaryModel.isBlank()) {
            primaryModel = aiProperties.getExtraction().getModel();
        }
        
        String fallbackModel = aiProperties.getExtraction().getFallbackModel();
        final String primary = primaryModel;
        final String fallback = fallbackModel;

        try {
            log.info("🔄 Starting CV extraction with retry strategy. Primary: {}, Fallback: {}", primary, fallback);
            log.debug("Retry config: {}", retryStrategy.getRetryConfig());

            // Primary operation: Call with primary model
            Supplier<StructuredExtractionResult> primaryOperation = () -> 
                callGeminiApi(primary, request, startTime);

            // Fallback operation: Call with fallback model
            Supplier<StructuredExtractionResult> fallbackOperation = () -> {
                log.info("Attempting extraction with fallback model: {}", fallback);
                return callGeminiApi(fallback, request, startTime);
            };
            
            // Execute with retry logic
            return retryStrategy.executeWithRetry(
                "CV Extraction (" + primary + ")",
                primaryOperation,
                fallbackOperation
            );
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Gemini extraction completely failed after {}ms: {}", latency, e.getMessage(), e);
            throw new IllegalStateException("Gemini text extraction failed: " + e.getMessage(), e);
        }
    }

    private StructuredExtractionResult callGeminiApi(
            String model, 
            StructuredExtractionRequest request,
            long startTime) {
        try {
            log.debug("Calling Gemini via Spring AI with model: {}", model);

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(model)
                    .temperature(request.getTemperature() != null ? request.getTemperature() : 0.1)
                    .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 2000)
                    .build();

            ChatResponse chatResponse = chatClient.prompt()
                    .options(options)
                    .user(request.getPrompt())
                    .call()
                    .chatResponse();

            String extractedJson = chatResponse != null
                    && chatResponse.getResult() != null
                    && chatResponse.getResult().getOutput() != null
                    ? chatResponse.getResult().getOutput().getText()
                    : "";

            long latency = System.currentTimeMillis() - startTime;
            log.info("Gemini AI extracted JSON: {}", extractedJson);

            Usage usage = chatResponse != null && chatResponse.getMetadata() != null
                    ? chatResponse.getMetadata().getUsage()
                    : null;
            int inputTokens = usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
            int outputTokens = usage != null && usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

            log.info("✓ Gemini extraction successful. model={}, inputTokens={}, outputTokens={}, latencyMs={}",
                    model, inputTokens, outputTokens, latency);

            return StructuredExtractionResult.builder()
                    .json(extractedJson)
                    .modelName(model)
                    .modelVersion("gemini-" + model)
                    .provider(providerName())
                    .usage(AiUsage.builder()
                            .inputTokens(inputTokens)
                            .outputTokens(outputTokens)
                            .latencyMs(latency)
                            .build())
                    .build();
                    
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Gemini API call failed after {}ms with model {}: {}", latency, model, e.getMessage());
            throw e;
        }
    }
}
