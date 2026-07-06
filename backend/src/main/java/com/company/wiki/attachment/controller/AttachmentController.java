package com.company.wiki.attachment.controller;

import com.company.wiki.attachment.dto.AttachmentDto;
import com.company.wiki.attachment.service.AttachmentService;
import com.company.wiki.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    private Long getUserId(UserDetails p) {
        return Long.parseLong(p.getUsername());
    }

    private String getRole(UserDetails p) {
        return p.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    @PostMapping(value = "/contents/{contentId}/attachments",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AttachmentDto.Response> upload(
            @PathVariable Long contentId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(
                attachmentService.upload(contentId, file, getUserId(principal), getRole(principal)));
    }

    @GetMapping("/contents/{contentId}/attachments")
    public ApiResponse<List<AttachmentDto.Response>> list(
            @PathVariable Long contentId,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(
                attachmentService.list(contentId, getUserId(principal), getRole(principal)));
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return attachmentService.download(id, getUserId(principal), getRole(principal));
    }

    @DeleteMapping("/attachments/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        attachmentService.delete(id, getUserId(principal), getRole(principal));
        return ResponseEntity.noContent().build();
    }
}
