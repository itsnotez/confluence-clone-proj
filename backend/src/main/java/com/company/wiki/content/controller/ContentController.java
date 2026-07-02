package com.company.wiki.content.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.content.dto.ContentDto;
import com.company.wiki.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    private Long getUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    private String getRole(UserDetails principal) {
        return principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    @GetMapping("/spaces/{spaceKey}/contents")
    public ApiResponse<List<ContentDto.TreeNode>> getContentTree(
            @PathVariable String spaceKey,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getUserId(principal);
        String role = getRole(principal);
        return ApiResponse.ok(contentService.getContentTree(spaceKey, userId, role));
    }

    @PostMapping("/spaces/{spaceKey}/contents")
    public ApiResponse<ContentDto.Response> createContent(
            @PathVariable String spaceKey,
            @Valid @RequestBody ContentDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getUserId(principal);
        String role = getRole(principal);
        return ApiResponse.ok(contentService.createContent(spaceKey, req, userId, role));
    }

    @GetMapping("/contents/{id}")
    public ApiResponse<ContentDto.Response> getContent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getUserId(principal);
        String role = getRole(principal);
        return ApiResponse.ok(contentService.getContent(id, userId, role));
    }

    @PutMapping("/contents/{id}")
    public ApiResponse<ContentDto.Response> updateContent(
            @PathVariable Long id,
            @RequestBody ContentDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getUserId(principal);
        String role = getRole(principal);
        return ApiResponse.ok(contentService.updateContent(id, req, userId, role));
    }

    @PostMapping("/contents/{id}/publish")
    public ApiResponse<ContentDto.Response> publishContent(
            @PathVariable Long id,
            @Valid @RequestBody ContentDto.PublishRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getUserId(principal);
        String role = getRole(principal);
        return ApiResponse.ok(contentService.publishContent(id, req, userId, role));
    }

    @DeleteMapping("/contents/{id}")
    public ResponseEntity<Void> deleteContent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getUserId(principal);
        String role = getRole(principal);
        contentService.deleteContent(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/contents/{id}/versions")
    public ApiResponse<List<ContentDto.VersionResponse>> getVersions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getUserId(principal);
        String role = getRole(principal);
        return ApiResponse.ok(contentService.getVersions(id, userId, role));
    }
}
