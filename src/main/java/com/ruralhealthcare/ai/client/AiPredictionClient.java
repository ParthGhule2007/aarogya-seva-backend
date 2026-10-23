package com.ruralhealthcare.ai.client;

import com.ruralhealthcare.ai.dto.AiPredictionRequest;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;

/**
 * Replaceable client contract for clinical AI model prediction.
 * Decouples domain logic from specific external communication mechanism (REST, gRPC, Mock).
 */
public interface AiPredictionClient {

    /**
     * Executes AI disease screening, risk assessment, and clinical service prediction.
     *
     * @param request clinical request payload containing case symptoms and non-identifying patient demographics
     * @return AI prediction response
     */
    AiPredictionResponse predict(AiPredictionRequest request);
}
