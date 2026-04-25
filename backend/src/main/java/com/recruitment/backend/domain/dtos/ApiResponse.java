package com.recruitment.backend.domain.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Builder.Default
    private int code = 1000; // Success code
    @Builder.Default
    private String message = "Success";
    private T result;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .result(data)
                .build();
    }
}
