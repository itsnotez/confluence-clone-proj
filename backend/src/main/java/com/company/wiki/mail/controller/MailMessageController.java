package com.company.wiki.mail.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.mail.dto.MailAttachmentDto;
import com.company.wiki.mail.dto.MailMessageDto;
import com.company.wiki.mail.entity.MailMessageAttachment;
import com.company.wiki.mail.service.MailMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces/{spaceKey}/mail-accounts/{accountId}/messages")
@RequiredArgsConstructor
public class MailMessageController {

    private final MailMessageService mailMessageService;

    private Long getUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    private String getRole(UserDetails principal) {
        return principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    private List<Long> getGroupIds() {
        return List.of();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MailMessageDto.Response>>> getMessages(
            @PathVariable String spaceKey,
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails currentUser) {
        List<MailMessageDto.Response> messages = mailMessageService.findByAccount(
                spaceKey, accountId,
                getUserId(currentUser),
                getRole(currentUser),
                getGroupIds());
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    @GetMapping("/{msgId}/attachments")
    public ResponseEntity<ApiResponse<List<MailAttachmentDto.Meta>>> getAttachments(
            @PathVariable String spaceKey,
            @PathVariable Long accountId,
            @PathVariable Long msgId,
            @AuthenticationPrincipal UserDetails currentUser) {
        List<MailAttachmentDto.Meta> metas = mailMessageService.getAttachmentMeta(
                spaceKey, accountId, msgId,
                getUserId(currentUser),
                getRole(currentUser),
                getGroupIds());
        return ResponseEntity.ok(ApiResponse.ok(metas));
    }

    @GetMapping("/{msgId}/attachments/{attachId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable String spaceKey,
            @PathVariable Long accountId,
            @PathVariable Long msgId,
            @PathVariable Long attachId,
            @AuthenticationPrincipal UserDetails currentUser) {
        MailMessageAttachment attachment = mailMessageService.downloadAttachment(
                spaceKey, accountId, msgId, attachId,
                getUserId(currentUser),
                getRole(currentUser),
                getGroupIds());
        String encodedName;
        try {
            encodedName = java.net.URLEncoder.encode(attachment.getFileName(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            encodedName = "attachment";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(attachment.getFileData());
    }

    @PostMapping("/{msgId}/convert")
    public ResponseEntity<ApiResponse<MailMessageDto.ConvertResponse>> convertToPage(
            @PathVariable String spaceKey,
            @PathVariable Long accountId,
            @PathVariable Long msgId,
            @AuthenticationPrincipal UserDetails currentUser) {
        MailMessageDto.ConvertResponse response = mailMessageService.convertToPage(
                spaceKey, accountId, msgId,
                getUserId(currentUser),
                getRole(currentUser),
                getGroupIds());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
