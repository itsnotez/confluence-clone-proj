package com.company.wiki.comment.controller;

import com.company.wiki.comment.dto.CommentDto;
import com.company.wiki.comment.service.CommentService;
import com.company.wiki.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private Long getUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    private String getRole(UserDetails principal) {
        return principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    @GetMapping("/contents/{contentId}/comments")
    public ApiResponse<List<CommentDto.CommentNode>> getComments(
            @PathVariable Long contentId,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(commentService.getComments(contentId, getUserId(principal), getRole(principal)));
    }

    @PostMapping("/contents/{contentId}/comments")
    public ApiResponse<CommentDto.CommentNode> createComment(
            @PathVariable Long contentId,
            @RequestBody CommentDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(commentService.createComment(contentId, req, getUserId(principal), getRole(principal)));
    }

    @PutMapping("/comments/{commentId}")
    public ApiResponse<CommentDto.CommentNode> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(commentService.updateComment(commentId, req, getUserId(principal), getRole(principal)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails principal) {
        commentService.deleteComment(commentId, getUserId(principal), getRole(principal));
        return ResponseEntity.noContent().build();
    }
}
