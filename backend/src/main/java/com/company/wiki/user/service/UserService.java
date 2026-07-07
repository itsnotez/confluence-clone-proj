package com.company.wiki.user.service;

import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.user.dto.UserDto;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto.Response> findAll() {
        return userRepository.findAll().stream()
                .map(UserDto.Response::from)
                .collect(Collectors.toList());
    }

    public UserDto.Response findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserDto.Response.from(user);
    }

    @Transactional
    public UserDto.Response create(UserDto.CreateRequest req) {
        if (userRepository.existsByLoginId(req.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .loginId(req.loginId())
                .name(req.name())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role() != null ? req.role() : "MEMBER")
                .status("ACTIVE")
                .build();

        User saved = userRepository.save(user);
        return UserDto.Response.from(saved);
    }

    @Transactional
    public UserDto.Response update(Long id, UserDto.UpdateRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (req.name() != null) {
            user.setName(req.name());
        }
        if (req.email() != null) {
            if (!req.email().equals(user.getEmail()) && userRepository.existsByEmail(req.email())) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            user.setEmail(req.email());
        }
        if (req.role() != null) {
            user.setRole(req.role());
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }

        return UserDto.Response.from(user);
    }

    @Transactional
    public void deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setStatus("INACTIVE");
    }

    public UserDto.Response findMe(Long userId) {
        return findById(userId);
    }
}
