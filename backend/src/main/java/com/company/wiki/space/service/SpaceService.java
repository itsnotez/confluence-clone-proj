package com.company.wiki.space.service;

import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.space.dto.SpaceDto;
import com.company.wiki.space.entity.Space;
import com.company.wiki.space.entity.SpaceFavorite;
import com.company.wiki.space.entity.SpaceFavoriteId;
import com.company.wiki.space.repository.SpaceFavoriteRepository;
import com.company.wiki.space.repository.SpaceRepository;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceFavoriteRepository spaceFavoriteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SpaceDto.Response> findAll(Long currentUserId) {
        return spaceRepository.findByStatusAndDeletedAtIsNull("ACTIVE")
                .stream()
                .map(space -> {
                    boolean favorited = spaceFavoriteRepository.existsByIdSpaceIdAndIdUserId(
                            space.getId(), currentUserId);
                    return SpaceDto.Response.from(space, favorited);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpaceDto.Response findByKey(String spaceKey, Long currentUserId) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
        boolean favorited = spaceFavoriteRepository.existsByIdSpaceIdAndIdUserId(
                space.getId(), currentUserId);
        return SpaceDto.Response.from(space, favorited);
    }

    public SpaceDto.Response create(SpaceDto.CreateRequest req, Long createdById) {
        if (spaceRepository.existsBySpaceKey(req.getSpaceKey())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SPACE_KEY);
        }

        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Space space = Space.builder()
                .spaceKey(req.getSpaceKey())
                .name(req.getName())
                .description(req.getDescription())
                .type(req.getType() != null ? req.getType() : "PRIVATE")
                .iconEmoji(req.getIconEmoji())
                .createdBy(creator)
                .build();

        Space saved = spaceRepository.save(space);
        return SpaceDto.Response.from(saved, false);
    }

    public SpaceDto.Response update(String spaceKey, SpaceDto.UpdateRequest req, Long currentUserId) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        if ("ARCHIVED".equals(space.getStatus())) {
            throw new BusinessException(ErrorCode.ARCHIVED_SPACE);
        }

        if (req.getName() != null) {
            space.setName(req.getName());
        }
        if (req.getDescription() != null) {
            space.setDescription(req.getDescription());
        }
        if (req.getType() != null) {
            space.setType(req.getType());
        }
        if (req.getIconEmoji() != null) {
            space.setIconEmoji(req.getIconEmoji());
        }

        Space saved = spaceRepository.save(space);
        boolean favorited = spaceFavoriteRepository.existsByIdSpaceIdAndIdUserId(
                saved.getId(), currentUserId);
        return SpaceDto.Response.from(saved, favorited);
    }

    public void delete(String spaceKey) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        space.setDeletedAt(LocalDateTime.now());
        space.setStatus("DELETED");
        spaceRepository.save(space);
    }

    public void toggleFavorite(String spaceKey, Long userId) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        Long spaceId = space.getId();

        if (spaceFavoriteRepository.existsByIdSpaceIdAndIdUserId(spaceId, userId)) {
            spaceFavoriteRepository.deleteBySpaceIdAndUserId(spaceId, userId);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            SpaceFavorite favorite = SpaceFavorite.builder()
                    .id(new SpaceFavoriteId(spaceId, userId))
                    .space(space)
                    .user(user)
                    .build();
            spaceFavoriteRepository.save(favorite);
        }
    }
}
