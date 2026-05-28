package com.recruitment.backend.utils;

public final class VectorSearchUtil {
    private VectorSearchUtil() {
    }

    public static String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(vector[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    public static Double distanceToSimilarity(Double distance) {
        if (distance == null) {
            return null;
        }
        return Math.max(0.0, Math.min(1.0, 1.0 - distance));
    }
}
