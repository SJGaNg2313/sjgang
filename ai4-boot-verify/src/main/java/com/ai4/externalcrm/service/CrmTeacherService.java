package com.ai4.externalcrm.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.id.SnowflakeIdGenerator;
import com.ai4.externalcrm.dto.CreateCrmTeacherRequest;
import com.ai4.externalcrm.dto.UpdateCrmTeacherRequest;
import com.ai4.externalcrm.model.CrmTeacherData;
import com.ai4.externalcrm.model.CrmTeacherIdentity;
import com.ai4.externalcrm.repository.CrmTeacherRepository;
import com.ai4.role.model.Role;
import com.ai4.role.repository.RoleRepository;
import com.ai4.user.model.User;
import com.ai4.user.repository.UserRepository;
import com.ai4.user.repository.UserRoleRepository;
import com.ai4.user.security.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 根据 CRM 推送的数据新增或修改本地教师。
 */
@Service
public class CrmTeacherService {
    private static final String SUPER_ADMIN_ROLE_CODE = "super_admin";

    private final CrmTeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;

    public CrmTeacherService(
            CrmTeacherRepository teacherRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SnowflakeIdGenerator snowflakeIdGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void create(CreateCrmTeacherRequest request) {
        String username = normalizeUsername(request.username());
        if (teacherRepository.findByCrmTeacherId(request.crmTeacherId()).isPresent()) {
            throw new BizException(ErrorCode.CRM_TEACHER_ALREADY_EXISTS);
        }
        validateUsernameAvailable(username, null);
        List<Long> roleIds = validateRoles(request.roleIds());
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = userRepository.save(username, encodedPassword);
        } else {
            if (teacherRepository.findByUserId(user.userId()).isPresent()) {
                throw new BizException(ErrorCode.CRM_TEACHER_ACCOUNT_ALREADY_BOUND);
            }
            ensureNotSuperAdmin(user);
            if (userRepository.updateCredentials(user.userId(), username, encodedPassword) == 0) {
                throw new BizException(ErrorCode.USER_NOT_FOUND);
            }
        }

        teacherRepository.save(new CrmTeacherData(
                snowflakeIdGenerator.nextId(), user.userId(), username,
                encodedPassword, request.nickname().trim(),
                request.crmTeacherId(), request.bz(), request.avatar()
        ));
        userRoleRepository.deleteByUserId(user.userId());
        userRoleRepository.batchInsert(user.userId(), roleIds);
    }

    @Transactional
    public void update(UpdateCrmTeacherRequest request) {
        CrmTeacherIdentity teacher = teacherRepository.findByCrmTeacherId(request.crmTeacherId())
                .orElseThrow(() -> new BizException(ErrorCode.CRM_TEACHER_NOT_FOUND));
        String username = normalizeUsername(request.username());
        validateUsernameAvailable(username, request.crmTeacherId());
        validateBackendUsernameAvailable(username, teacher.userId());
        String password = request.password() == null || request.password().isBlank()
                ? null : passwordEncoder.encode(request.password());

        int userUpdated = userRepository.updateCredentials(teacher.userId(), username, password);
        if (userUpdated == 0) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        int teacherUpdated = teacherRepository.update(new CrmTeacherData(
                teacher.id(), teacher.userId(), username, password, request.nickname().trim(),
                request.crmTeacherId(), request.bz(), request.avatar()
        ));
        if (teacherUpdated == 0) {
            throw new BizException(ErrorCode.CRM_TEACHER_NOT_FOUND);
        }
    }

    private String normalizeUsername(String username) {
        String normalized = username.trim();
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new BizException(ErrorCode.CRM_TEACHER_USERNAME_INVALID);
        }
        return normalized;
    }

    private void validateUsernameAvailable(String username, Integer excludedCrmTeacherId) {
        if (teacherRepository.usernameExists(username, excludedCrmTeacherId)) {
            throw new BizException(ErrorCode.CRM_TEACHER_USERNAME_ALREADY_EXISTS);
        }
    }

    private void validateBackendUsernameAvailable(String username, Long currentUserId) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (currentUserId == null || !currentUserId.equals(user.userId())) {
                throw new BizException(ErrorCode.CRM_TEACHER_USERNAME_ALREADY_EXISTS);
            }
        });
    }

    private void ensureNotSuperAdmin(User user) {
        Map<Long, Role> roleMap = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::roleId, Function.identity()));
        boolean superAdmin = userRoleRepository.findRoleIdsByUserId(user.userId()).stream()
                .map(roleMap::get)
                .anyMatch(role -> role != null && SUPER_ADMIN_ROLE_CODE.equals(role.roleCode()));
        if (superAdmin) {
            throw new BizException(ErrorCode.SUPER_ADMIN_PROTECTED);
        }
    }

    private List<Long> validateRoles(List<Long> requestedRoleIds) {
        LinkedHashSet<Long> roleIds = new LinkedHashSet<>(requestedRoleIds);
        if (roleIds.size() != requestedRoleIds.size()) {
            throw new BizException(ErrorCode.CRM_TEACHER_ROLE_DUPLICATED);
        }

        Map<Long, Role> roleMap = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::roleId, Function.identity()));
        for (Long roleId : roleIds) {
            Role role = roleMap.get(roleId);
            if (role == null) {
                throw new BizException(ErrorCode.ROLE_NOT_FOUND);
            }
            if (role.status() != 0) {
                throw new BizException(ErrorCode.ROLE_DISABLED);
            }
            if (SUPER_ADMIN_ROLE_CODE.equals(role.roleCode())) {
                throw new BizException(ErrorCode.SUPER_ADMIN_PROTECTED);
            }
        }
        return List.copyOf(roleIds);
    }
}
