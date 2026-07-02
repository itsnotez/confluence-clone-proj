package com.company.wiki.space.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.space.dto.SpaceDto;
import com.company.wiki.space.service.SpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    private Long getCurrentUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    @GetMapping
    public ApiResponse<List<SpaceDto.Response>> getSpaces(
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        return ApiResponse.ok(spaceService.findAll(userId));
    }

    @GetMapping("/{spaceKey}")
    public ApiResponse<SpaceDto.Response> getSpace(
            @PathVariable String spaceKey,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        return ApiResponse.ok(spaceService.findByKey(spaceKey, userId));
    }

    @PostMapping
    public ApiResponse<SpaceDto.Response> createSpace(
            @Valid @RequestBody SpaceDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        return ApiResponse.ok(spaceService.create(req, userId));
    }

    @PutMapping("/{spaceKey}")
    public ApiResponse<SpaceDto.Response> updateSpace(
            @PathVariable String spaceKey,
            @Valid @RequestBody SpaceDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        return ApiResponse.ok(spaceService.update(spaceKey, req, userId));
    }

    @DeleteMapping("/{spaceKey}")
    public ResponseEntity<Void> deleteSpace(
            @PathVariable String spaceKey,
            @AuthenticationPrincipal UserDetails principal) {
        spaceService.delete(spaceKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{spaceKey}/favorite")
    public ApiResponse<Void> addFavorite(
            @PathVariable String spaceKey,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        spaceService.toggleFavorite(spaceKey, userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{spaceKey}/favorite")
    public ApiResponse<Void> removeFavorite(
            @PathVariable String spaceKey,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        spaceService.toggleFavorite(spaceKey, userId);
        return ApiResponse.ok(null);
    }
}
