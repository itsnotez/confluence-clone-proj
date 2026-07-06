package com.company.wiki.label.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.label.dto.LabelDto;
import com.company.wiki.label.service.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    private Long getUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    private String getRole(UserDetails principal) {
        return principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    @GetMapping("/contents/{contentId}/labels")
    public ApiResponse<List<LabelDto.Response>> getLabels(
            @PathVariable Long contentId,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(labelService.getLabels(contentId, getUserId(principal), getRole(principal)));
    }

    @PostMapping("/contents/{contentId}/labels")
    public ApiResponse<LabelDto.Response> addLabel(
            @PathVariable Long contentId,
            @RequestBody LabelDto.AddRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(labelService.addLabel(contentId, req.getLabelId(), getUserId(principal), getRole(principal)));
    }

    @DeleteMapping("/contents/{contentId}/labels/{labelId}")
    public ResponseEntity<Void> removeLabel(
            @PathVariable Long contentId,
            @PathVariable Long labelId,
            @AuthenticationPrincipal UserDetails principal) {
        labelService.removeLabel(contentId, labelId, getUserId(principal), getRole(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/labels")
    public ApiResponse<List<LabelDto.Response>> listBySpace(
            @RequestParam Long spaceId,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(labelService.listBySpace(spaceId, getUserId(principal), getRole(principal)));
    }

    @PostMapping("/labels")
    public ApiResponse<LabelDto.Response> createLabel(
            @RequestBody LabelDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(labelService.createLabel(req, getUserId(principal), getRole(principal)));
    }
}
