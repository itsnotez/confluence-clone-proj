package com.company.wiki.permission.controller;

import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.permission.dto.PermissionDto;
import com.company.wiki.permission.entity.SpacePermission;
import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.space.entity.Space;
import com.company.wiki.space.repository.SpaceRepository;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Space 권한 관리 API
 * GET    /spaces/{spaceKey}/permissions  — 권한 목록 조회
 * POST   /spaces/{spaceKey}/permissions  — 권한 부여
 * DELETE /spaces/{spaceKey}/permissions  — 권한 삭제
 */
@RestController
@RequestMapping("/spaces/{spaceKey}/permissions")
@RequiredArgsConstructor
public class SpacePermissionController {

    private final PermissionService permissionService;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;

    private Long getCurrentUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    private Space getSpaceOrThrow(String spaceKey) {
        return spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.SPACE_NOT_FOUND.getMessage()));
    }

    private User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));
    }

    /**
     * GET /spaces/{spaceKey}/permissions
     * Space 관리자 또는 SITE_ADMIN만 조회 가능
     */
    @GetMapping
    public ApiResponse<List<PermissionDto.Response>> getPermissions(
            @PathVariable String spaceKey,
            @AuthenticationPrincipal UserDetails principal) {

        Long userId = getCurrentUserId(principal);
        User user = getCurrentUser(userId);
        Space space = getSpaceOrThrow(spaceKey);

        // Space 관리자 이상만 권한 목록 조회 가능
        if (!permissionService.isSpaceAdmin(space.getId(), userId, user.getRole(), Collections.emptyList())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
        }

        List<PermissionDto.Response> result = permissionService
                .findSpacePermissions(space.getId())
                .stream()
                .map(PermissionDto.Response::from)
                .collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    /**
     * POST /spaces/{spaceKey}/permissions
     * Space 관리자 또는 SITE_ADMIN만 권한 부여 가능
     */
    @PostMapping
    public ApiResponse<PermissionDto.Response> grantPermission(
            @PathVariable String spaceKey,
            @Valid @RequestBody PermissionDto.GrantRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        Long userId = getCurrentUserId(principal);
        User user = getCurrentUser(userId);
        Space space = getSpaceOrThrow(spaceKey);

        // Space 관리자 이상만 권한 부여 가능
        if (!permissionService.isSpaceAdmin(space.getId(), userId, user.getRole(), Collections.emptyList())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
        }

        SpacePermission granted = permissionService.grantSpacePermission(
                space.getId(),
                request.getSubjectType(),
                request.getSubjectId(),
                request.getPermissionLevel()
        );

        return ApiResponse.ok(PermissionDto.Response.from(granted));
    }

    /**
     * DELETE /spaces/{spaceKey}/permissions?subjectType=USER&subjectId=1
     * Space 관리자 또는 SITE_ADMIN만 권한 삭제 가능
     */
    @DeleteMapping
    public ResponseEntity<Void> revokePermission(
            @PathVariable String spaceKey,
            @RequestParam String subjectType,
            @RequestParam(required = false) Long subjectId,
            @AuthenticationPrincipal UserDetails principal) {

        Long userId = getCurrentUserId(principal);
        User user = getCurrentUser(userId);
        Space space = getSpaceOrThrow(spaceKey);

        // Space 관리자 이상만 권한 삭제 가능
        if (!permissionService.isSpaceAdmin(space.getId(), userId, user.getRole(), Collections.emptyList())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
        }

        permissionService.revokeSpacePermission(space.getId(), subjectType, subjectId);

        return ResponseEntity.noContent().build();
    }
}
