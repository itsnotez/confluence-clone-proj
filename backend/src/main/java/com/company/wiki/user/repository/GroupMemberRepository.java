package com.company.wiki.user.repository;

import com.company.wiki.user.entity.GroupMember;
import com.company.wiki.user.entity.GroupMember.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    List<GroupMember> findByGroupId(Long groupId);
    List<GroupMember> findByUserId(Long userId);
    void deleteByIdGroupIdAndIdUserId(Long groupId, Long userId);
}
