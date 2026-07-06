package com.company.wiki.label.service;

import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.content.entity.Content;
import com.company.wiki.content.repository.ContentRepository;
import com.company.wiki.label.dto.LabelDto;
import com.company.wiki.label.entity.ContentLabel;
import com.company.wiki.label.entity.ContentLabelId;
import com.company.wiki.label.entity.Label;
import com.company.wiki.label.repository.ContentLabelRepository;
import com.company.wiki.label.repository.LabelRepository;
import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.user.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;
    private final ContentLabelRepository contentLabelRepository;
    private final ContentRepository contentRepository;
    private final PermissionService permissionService;
    private final GroupMemberRepository groupMemberRepository;

    private List<Long> getUserGroupIds(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(gm -> gm.getId().getGroupId())
                .collect(Collectors.toList());
    }

    public List<LabelDto.Response> getLabels(Long contentId, Long userId, String role) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canRead(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        List<Long> labelIds = contentLabelRepository.findByIdContentId(contentId).stream()
                .map(cl -> cl.getId().labelId())
                .collect(Collectors.toList());
        return labelRepository.findAllById(labelIds).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public LabelDto.Response addLabel(Long contentId, Long labelId, Long userId, String role) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canWrite(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LABEL_NOT_FOUND));
        ContentLabel cl = ContentLabel.builder()
                .id(new ContentLabelId(contentId, labelId))
                .build();
        contentLabelRepository.save(cl);
        return toResponse(label);
    }

    public void removeLabel(Long contentId, Long labelId, Long userId, String role) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canWrite(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        contentLabelRepository.deleteByIdContentIdAndIdLabelId(contentId, labelId);
    }

    public LabelDto.Response createLabel(LabelDto.CreateRequest req, Long userId, String role) {
        if (req.getSpaceId() != null &&
                !permissionService.canWrite(req.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        Label label = Label.builder()
                .spaceId(req.getSpaceId())
                .name(req.getName())
                .color(req.getColor())
                .build();
        label = labelRepository.save(label);
        return toResponse(label);
    }

    @Transactional(readOnly = true)
    public List<LabelDto.Response> listBySpace(Long spaceId, Long userId, String role) {
        if (!permissionService.canRead(spaceId, userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return labelRepository.findBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private LabelDto.Response toResponse(Label label) {
        return LabelDto.Response.builder()
                .id(label.getId())
                .name(label.getName())
                .color(label.getColor())
                .spaceId(label.getSpaceId())
                .build();
    }
}
