package com.company.wiki.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDto {

    private long activeUsers;
    private long totalSpaces;
    private long totalContents;
    private long storageUsedBytes;
    private long mailAccountsOk;
    private long mailAccountsFailed;
}
