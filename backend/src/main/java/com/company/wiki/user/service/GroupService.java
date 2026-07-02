package com.company.wiki.user.service;

import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.user.dto.GroupDto;
import com.company.wiki.user.dto.UserDto;
import com.company.wiki.user.entity.Group;
import com.company.wiki.user.entity.GroupMember;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.GroupMemberRepository;
import com.company.wiki.user.repository.GroupRepository;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public List<GroupDto.Response> findAll() {
        return groupRepository.findAll().stream()
                .map(GroupDto.Response::from)
                .collect(Collectors.toList());
    }

    public GroupDto.Response findById(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        return GroupDto.Response.from(group);
    }

    @Transactional
    public GroupDto.Response create(GroupDto.CreateRequest req) {
        if (groupRepository.existsByName(req.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SPACE_KEY);
        }

        Group group = Group.builder()
                .name(req.name())
                .description(req.description())
                .build();

        Group saved = groupRepository.save(group);
        return GroupDto.Response.from(saved);
    }

    @Transactional
    public GroupDto.Response update(Long id, GroupDto.CreateRequest req) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));

        if (req.name() != null && !req.name().equals(group.getName())) {
            if (groupRepository.existsByName(req.name())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SPACE_KEY);
            }
            group.setName(req.name());
        }
        if (req.description() != null) {
            group.setDescription(req.description());
        }

        return GroupDto.Response.from(group);
    }

    @Transactional
    public void delete(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        groupRepository.delete(group);
    }

    @Transactional
    public void addMember(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        GroupMember.GroupMemberId memberId = new GroupMember.GroupMemberId(groupId, userId);
        if (!groupMemberRepository.existsById(memberId)) {
            GroupMember member = new GroupMember(group, user);
            groupMemberRepository.save(member);
        }
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        groupMemberRepository.deleteByIdGroupIdAndIdUserId(groupId, userId);
    }

    public List<UserDto.Response> getMembers(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(gm -> UserDto.Response.from(gm.getUser()))
                .collect(Collectors.toList());
    }
}
