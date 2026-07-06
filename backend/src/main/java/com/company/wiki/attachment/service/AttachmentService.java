package com.company.wiki.attachment.service;

import com.company.wiki.attachment.dto.AttachmentDto;
import com.company.wiki.attachment.entity.Attachment;
import com.company.wiki.attachment.repository.AttachmentRepository;
import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.common.service.StorageService;
import com.company.wiki.content.entity.Content;
import com.company.wiki.content.repository.ContentRepository;
import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.user.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ContentRepository contentRepository;
    private final StorageService storageService;
    private final PermissionService permissionService;
    private final GroupMemberRepository groupMemberRepository;

    private List<Long> getUserGroupIds(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(gm -> gm.getId().getGroupId())
                .collect(Collectors.toList());
    }

    private AttachmentDto.Response toResponse(Attachment a) {
        return AttachmentDto.Response.builder()
                .id(a.getId())
                .contentId(a.getContentId())
                .fileName(a.getFileName())
                .mimeType(a.getMimeType())
                .sizeBytes(a.getSizeBytes())
                .version(a.getVersion())
                .uploadedBy(a.getUploadedBy())
                .createdAt(a.getCreatedAt())
                .build();
    }

    public AttachmentDto.Response upload(Long contentId, MultipartFile file, Long userId, String role) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canWrite(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        String key = "contents/" + contentId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            storageService.upload(key, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        Attachment attachment = Attachment.builder()
                .contentId(contentId)
                .fileName(file.getOriginalFilename())
                .storagePath(key)
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .uploadedBy(userId)
                .build();
        return toResponse(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto.Response> list(Long contentId, Long userId, String role) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canRead(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return attachmentRepository.findByContentId(contentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(Long id, Long userId, String role) {
        Attachment att = attachmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));
        Content content = contentRepository.findByIdAndDeletedAtIsNull(att.getContentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canRead(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        try (ResponseInputStream<GetObjectResponse> in = storageService.download(att.getStoragePath())) {
            byte[] bytes = in.readAllBytes();
            String encodedName = UriUtils.encode(att.getFileName(), StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName);
            MediaType mediaType = att.getMimeType() != null
                    ? MediaType.parseMediaType(att.getMimeType())
                    : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().headers(headers).contentType(mediaType).body(bytes);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void delete(Long id, Long userId, String role) {
        Attachment att = attachmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));
        Content content = contentRepository.findByIdAndDeletedAtIsNull(att.getContentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canWrite(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        storageService.delete(att.getStoragePath());
        attachmentRepository.delete(att);
    }
}
