package com.ai4.role.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.role.dto.CreatedRoleRequest;
import com.ai4.role.dto.UpdateRoleRequest;
import com.ai4.role.model.Role;
import com.ai4.role.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository){
        this.roleRepository = roleRepository;
    }

    /**
     * 查询全部角色。
     */
    public List<Role> list() {
        return roleRepository.findAll();
    }

    /**
     * 根据角色 ID 查询角色详情。
     */
    public Role get(long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(ErrorCode.ROLE_NOT_FOUND));
    }

    /**
     * 创建角色
     */
    @Transactional
    public Role create(CreatedRoleRequest request){
        String roleCode = request.roleCode().trim();

        if(roleRepository.existsByRoleCode(roleCode)){
            throw new BizException(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        }

        int status = request.status() == null ? 0 : request.status();
        validateStatus(status);

        Role role = new Role(
                null,
                request.roleName().trim(),
                roleCode,
                status,
                request.roleSort() == null ? 0 : request.roleSort(),
                null,
                null
        );

        long roleId = roleRepository.save(role);
        return get(roleId);
    }

    /**
     * 校验角色状态只能是 正常或禁用。
     */
    private void validateStatus(int status) {
        if (status != 0 && status != 1) {
            throw new BizException(ErrorCode.INVALID_ROLE_STATUS);
        }
    }

    /**
     * 修改角色名称和显示顺序。
     */
    @Transactional
    public Role update(long roleId, UpdateRoleRequest request) {
        Role existingRole = get(roleId);

        Role updatedRole = new Role(
                existingRole.roleId(),
                request.roleName().trim(),
                existingRole.roleCode(),
                existingRole.status(),
                request.roleSort() == null
                        ? existingRole.roleSort()
                        : request.roleSort(),
                existingRole.createdAt(),
                existingRole.updatedAt()
        );

        if (roleRepository.update(updatedRole) == 0) {
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }

        return get(roleId);
    }

    /**
     * 修改角色状态。
     */
    @Transactional
    public Role updateStatus(long roleId, int status) {
        Role existingRole = get(roleId);
        validateStatus(status);

        if ("super_admin".equals(existingRole.roleCode()) && status == 1) {
            throw new BizException(ErrorCode.SUPER_ADMIN_PROTECTED);
        }

        if (roleRepository.updateStatus(roleId, status) == 0) {
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }

        return get(roleId);
    }

    /**
     * 删除角色及其菜单授权关系。
     */
    @Transactional
    public void delete(long roleId) {
        Role existingRole = get(roleId);

        if ("super_admin".equals(existingRole.roleCode())) {
            throw new BizException(ErrorCode.SUPER_ADMIN_PROTECTED);
        }

        if (roleRepository.countUsersByRoleId(roleId) > 0) {
            throw new BizException(ErrorCode.ROLE_HAS_USERS);
        }

        roleRepository.deleteMenuRelationsByRoleId(roleId);

        if (roleRepository.deleteById(roleId) == 0) {
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }
    }
}
