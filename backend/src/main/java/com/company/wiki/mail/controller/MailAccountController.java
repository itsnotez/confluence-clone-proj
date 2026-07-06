package com.company.wiki.mail.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.mail.dto.MailAccountDto;
import com.company.wiki.mail.service.MailAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces/{spaceKey}/mail-accounts")
@RequiredArgsConstructor
public class MailAccountController {

    private final MailAccountService mailAccountService;

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
    public ResponseEntity<ApiResponse<List<MailAccountDto.Response>>> getMailAccounts(
            @PathVariable String spaceKey) {
        List<MailAccountDto.Response> accounts = mailAccountService.findBySpace(spaceKey);
        return ResponseEntity.ok(ApiResponse.ok(accounts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MailAccountDto.Response>> createMailAccount(
            @PathVariable String spaceKey,
            @Valid @RequestBody MailAccountDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails currentUser) {
        MailAccountDto.Response response = mailAccountService.create(
                spaceKey, req,
                getUserId(currentUser),
                getRole(currentUser),
                getGroupIds());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMailAccount(
            @PathVariable String spaceKey,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        mailAccountService.delete(
                spaceKey, id,
                getUserId(currentUser),
                getRole(currentUser),
                getGroupIds());
        return ResponseEntity.noContent().build();
    }
}
