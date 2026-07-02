package com.company.wiki.user.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.user.dto.UserDto;
import com.company.wiki.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserDto.Response> getMe(@AuthenticationPrincipal UserDetails principal) {
        Long userId = Long.parseLong(principal.getUsername());
        return ApiResponse.ok(userService.findMe(userId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<List<UserDto.Response>> getUsers() {
        return ApiResponse.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN') or #id == T(Long).parseLong(authentication.name)")
    public ApiResponse<UserDto.Response> getUser(@PathVariable Long id) {
        return ApiResponse.ok(userService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<UserDto.Response> createUser(@Valid @RequestBody UserDto.CreateRequest req) {
        return ApiResponse.ok(userService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<UserDto.Response> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.UpdateRequest req) {
        return ApiResponse.ok(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deactivate(id);
        return ApiResponse.ok(null);
    }
}
