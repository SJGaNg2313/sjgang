package com.ai4.user.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.role.model.Role;
import com.ai4.role.repository.RoleRepository;
import com.ai4.user.model.User;
import com.ai4.user.repository.UserRepository;
import com.ai4.user.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户角色授权业务。
 */
@Service
public class UserRoleService {

    private static final String SUPER_ADMIN_ROLE_CODE = "super_admin";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public UserRoleService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * 查询用户当前关联的角色 ID。
     */
    public List<Long> findRoleIds(long userId) {
        getUser(userId);
        return userRoleRepository.findRoleIdsByUserId(userId);
    }

    /**
     * 替换用户角色，并禁止分配已禁用角色。
     */
    @Transactional
    public List<Long> replaceRoles(long userId, List<Long> requestedRoleIds) {
        User user = getUser(userId);
        Set<Long> roleIds = new LinkedHashSet<>(requestedRoleIds);
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
        }

        protectLastSuperAdmin(user, roleIds, roleMap);

        userRoleRepository.deleteByUserId(userId);
        userRoleRepository.batchInsert(userId, roleIds);

        return roleIds.stream().sorted().toList();
    }

    /**
     * 禁止移除系统最后一个启用超级管理员用户的超级管理员角色。
     */
    private void protectLastSuperAdmin(
            User user,
            Set<Long> requestedRoleIds,
            Map<Long, Role> roleMap
    ) {
        List<Long> existingRoleIds = userRoleRepository.findRoleIdsByUserId(user.userId());

        boolean currentlySuperAdmin = existingRoleIds.stream()
                .map(roleMap::get)
                .anyMatch(role -> role != null
                        && SUPER_ADMIN_ROLE_CODE.equals(role.roleCode()));

        boolean remainsSuperAdmin = requestedRoleIds.stream()
                .map(roleMap::get)
                .anyMatch(role -> role != null
                        && SUPER_ADMIN_ROLE_CODE.equals(role.roleCode()));

        if (user.status() == 0
                && currentlySuperAdmin
                && !remainsSuperAdmin
                && userRoleRepository.countActiveUsersByRoleCode(SUPER_ADMIN_ROLE_CODE) <= 1) {
            throw new BizException(ErrorCode.LAST_SUPER_ADMIN_REQUIRED);
        }
    }

    /**
     * 查询后台用户，不存在时抛出业务异常。
     */
    private User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }
}
