package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.LocationDTO;
import com.recruitment.backend.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationRepository locationRepository;

    @GetMapping
    public ApiResponse<List<LocationDTO>> getLocations() {
        return ApiResponse.success(locationRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(location -> LocationDTO.builder()
                        .code(location.getCode())
                        .name(location.getName())
                        .build())
                .toList());
    }
}
