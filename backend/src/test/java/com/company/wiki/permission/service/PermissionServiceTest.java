package com.company.wiki.permission.service;

import com.company.wiki.permission.entity.SpacePermission;
import com.company.wiki.permission.repository.ContentPermissionRepository;
import com.company.wiki.permission.repository.SpacePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private SpacePermissionRepository spacePermissionRepository;

    @Mock
    private ContentPermissionRepository contentPermissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private static final Long SPACE_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long GROUP_ID = 20L;
    private static final List<Long> EMPTY_GROUPS = Collections.emptyList();
    private static final List<Long> ONE_GROUP = List.of(GROUP_ID);

    @BeforeEach
    void setUp() {
        // Mockito가 기본으로 Optional.empty()를 반환하도록 설정 (lenient 처리)
    }

    /**
     * 테스트 1: SITE_ADMIN은 canRead가 항상 true
     */
    @Test
    @DisplayName("canRead_siteAdmin_alwaysTrue: SITE_ADMIN 역할이면 권한 조회 없이 항상 true")
    void canRead_siteAdmin_alwaysTrue() {
        // Repository 호출 없이 true 반환되어야 함
        boolean result = permissionService.canRead(SPACE_ID, USER_ID, "SITE_ADMIN", EMPTY_GROUPS);

        assertThat(result).isTrue();
        verifyNoInteractions(spacePermissionRepository);
    }

    /**
     * 테스트 2: USER 타입 READ 권한 → canRead true
     */
    @Test
    @DisplayName("canRead_personalRead_true: 개인 READ 권한 존재 시 canRead true")
    void canRead_personalRead_true() {
        SpacePermission readPerm = SpacePermission.builder()
                .id(1L)
                .spaceId(SPACE_ID)
                .subjectType("USER")
                .subjectId(USER_ID)
                .permissionLevel("READ")
                .build();

        when(spacePermissionRepository.findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "USER", USER_ID))
                .thenReturn(Optional.of(readPerm));

        boolean result = permissionService.canRead(SPACE_ID, USER_ID, "MEMBER", EMPTY_GROUPS);

        assertThat(result).isTrue();
    }

    /**
     * 테스트 3: GROUP 타입 WRITE 권한 → canRead true (WRITE >= READ)
     */
    @Test
    @DisplayName("canRead_groupWrite_true: 그룹 WRITE 권한 존재 시 canRead true")
    void canRead_groupWrite_true() {
        SpacePermission writePerm = SpacePermission.builder()
                .id(2L)
                .spaceId(SPACE_ID)
                .subjectType("GROUP")
                .subjectId(GROUP_ID)
                .permissionLevel("WRITE")
                .build();

        // 개인 권한 없음
        when(spacePermissionRepository.findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "USER", USER_ID))
                .thenReturn(Optional.empty());
        // 그룹 권한 있음
        when(spacePermissionRepository.findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "GROUP", GROUP_ID))
                .thenReturn(Optional.of(writePerm));

        boolean result = permissionService.canRead(SPACE_ID, USER_ID, "MEMBER", ONE_GROUP);

        assertThat(result).isTrue();
    }

    /**
     * 테스트 4: 아무 권한도 없으면 canRead false
     */
    @Test
    @DisplayName("canRead_noPermission_false: 권한 없으면 canRead false")
    void canRead_noPermission_false() {
        // 개인 권한 없음
        when(spacePermissionRepository.findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "USER", USER_ID))
                .thenReturn(Optional.empty());
        // 전체(ALL) 권한 없음
        when(spacePermissionRepository.findBySpaceIdAndSubjectTypeAndSubjectIdIsNull(SPACE_ID, "ALL"))
                .thenReturn(Optional.empty());

        boolean result = permissionService.canRead(SPACE_ID, USER_ID, "MEMBER", EMPTY_GROUPS);

        assertThat(result).isFalse();
    }

    /**
     * 테스트 5: 개인 NONE + 그룹 READ → canRead false (개인 권한이 그룹 권한보다 우선)
     * 핵심 우선순위 검증 테스트
     */
    @Test
    @DisplayName("canRead_personalNone_overridesGroupRead: 개인 NONE이 그룹 READ를 오버라이드")
    void canRead_personalNone_overridesGroupRead() {
        // 개인 NONE 권한 — 그룹 READ보다 우선순위가 높음
        SpacePermission nonePerm = SpacePermission.builder()
                .id(3L)
                .spaceId(SPACE_ID)
                .subjectType("USER")
                .subjectId(USER_ID)
                .permissionLevel("NONE")
                .build();

        // 그룹 READ 권한 (개인 NONE에 의해 무시되어야 함)
        SpacePermission groupReadPerm = SpacePermission.builder()
                .id(4L)
                .spaceId(SPACE_ID)
                .subjectType("GROUP")
                .subjectId(GROUP_ID)
                .permissionLevel("READ")
                .build();

        // 개인 NONE 권한이 먼저 반환됨 → 그룹 권한을 조회하지 않고 NONE 반환
        when(spacePermissionRepository.findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "USER", USER_ID))
                .thenReturn(Optional.of(nonePerm));

        // 그룹 권한은 조회되지 않아야 함 (개인 권한이 이미 결정됨)
        // 단, lenient 설정으로 혹시 호출되더라도 문제없도록 설정
        lenient().when(spacePermissionRepository.findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "GROUP", GROUP_ID))
                .thenReturn(Optional.of(groupReadPerm));

        boolean result = permissionService.canRead(SPACE_ID, USER_ID, "MEMBER", ONE_GROUP);

        // 개인 NONE이 우선 → canRead false
        assertThat(result).isFalse();

        // 개인 권한이 있으면 그룹 권한 조회 없이 반환되어야 함
        verify(spacePermissionRepository, times(1))
                .findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "USER", USER_ID);
        verify(spacePermissionRepository, never())
                .findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "GROUP", GROUP_ID);
    }

    /**
     * 테스트 6: READ 권한만 있으면 canWrite false
     */
    @Test
    @DisplayName("canWrite_readOnly_false: READ 권한만 있으면 canWrite false")
    void canWrite_readOnly_false() {
        SpacePermission readPerm = SpacePermission.builder()
                .id(5L)
                .spaceId(SPACE_ID)
                .subjectType("USER")
                .subjectId(USER_ID)
                .permissionLevel("READ")
                .build();

        when(spacePermissionRepository.findBySpaceIdAndSubjectTypeAndSubjectId(SPACE_ID, "USER", USER_ID))
                .thenReturn(Optional.of(readPerm));

        boolean result = permissionService.canWrite(SPACE_ID, USER_ID, "MEMBER", EMPTY_GROUPS);

        assertThat(result).isFalse();
    }
}
