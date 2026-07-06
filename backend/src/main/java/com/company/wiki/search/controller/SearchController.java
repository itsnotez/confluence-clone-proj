package com.company.wiki.search.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.search.dto.SearchDto;
import com.company.wiki.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    private Long getUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    private String getRole(UserDetails principal) {
        return principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    @GetMapping("/search")
    public ApiResponse<List<SearchDto.Response>> search(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(searchService.search(q, getUserId(principal), getRole(principal)));
    }
}
