package com.company.wiki.search.service;

import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.search.dto.SearchDto;
import com.company.wiki.search.repository.SearchRepository;
import com.company.wiki.user.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    private final PermissionService permissionService;
    private final GroupMemberRepository groupMemberRepository;

    private List<Long> getUserGroupIds(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(gm -> gm.getId().getGroupId())
                .collect(Collectors.toList());
    }

    public List<SearchDto.Response> search(String q, Long userId, String userRole) {
        if (q == null || q.isBlank()) return List.of();

        List<Object[]> rows = searchRepository.searchContents(q);
        List<Long> groupIds = getUserGroupIds(userId);

        // UNION ALL 결과에서 id 기준 중복 제거 (첫 항목 유지 — rank 높은 순)
        Map<Long, SearchDto.Response> seen = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            if (seen.containsKey(id)) continue;

            Long spaceId = ((Number) row[2]).longValue();
            if (!permissionService.canRead(spaceId, userId, userRole, groupIds)) continue;

            SearchDto.Response resp = SearchDto.Response.builder()
                    .id(id)
                    .title((String) row[1])
                    .spaceId(spaceId)
                    .spaceKey((String) row[3])
                    .status((String) row[4])
                    .updatedAt(row[5] != null ? ((Timestamp) row[5]).toLocalDateTime() : null)
                    .rank(row[6] != null ? ((Number) row[6]).doubleValue() : 0.0)
                    .build();
            seen.put(id, resp);
        }

        return new ArrayList<>(seen.values());
    }
}
