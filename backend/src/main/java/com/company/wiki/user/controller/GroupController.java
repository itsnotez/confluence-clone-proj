package com.company.wiki.user.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.user.dto.GroupDto;
import com.company.wiki.user.dto.UserDto;
import com.company.wiki.user.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ApiResponse<List<GroupDto.Response>> getGroups() {
        return ApiResponse.ok(groupService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupDto.Response> getGroup(@PathVariable Long id) {
        return ApiResponse.ok(groupService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<GroupDto.Response> createGroup(@Valid @RequestBody GroupDto.CreateRequest req) {
        return ApiResponse.ok(groupService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<GroupDto.Response> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody GroupDto.CreateRequest req) {
        return ApiResponse.ok(groupService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        groupService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<Void> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        groupService.addMember(id, userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId) {
        groupService.removeMember(id, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<UserDto.Response>> getMembers(@PathVariable Long id) {
        return ApiResponse.ok(groupService.getMembers(id));
    }
}
