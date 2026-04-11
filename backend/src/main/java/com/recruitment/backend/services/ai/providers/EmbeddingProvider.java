package com.recruitment.backend.services.ai.providers;

import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;

public interface EmbeddingProvider {
    String providerName();

    EmbeddingResult embed(EmbeddingRequest request);
}
