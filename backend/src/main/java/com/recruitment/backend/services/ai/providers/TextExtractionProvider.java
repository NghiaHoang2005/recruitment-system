package com.recruitment.backend.services.ai.providers;

import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;

public interface TextExtractionProvider {
    String providerName();

    StructuredExtractionResult extractStructured(StructuredExtractionRequest request);
}
