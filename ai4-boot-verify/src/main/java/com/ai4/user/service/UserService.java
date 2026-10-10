package com.ai4.user.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.user.dto.CreateUserRequest;
import com.ai4.user.dto.CreateUserResponse;
import com.ai4.user.dto.UserListItemResponse;
import com.ai4.user.model.User;
import com.ai4.user.repository.UserRepository;
import com.ai4.user.security.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 负责用户创建相关的业务规则。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 校验用户名、编码密码并保存新用户。
     */
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.user_name())) {
            throw new BizException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = userRepository.save(
                request.user_name(),
                encodedPassword
        );

        // 响应中不返回密码字段。
        return new CreateUserResponse(
                user.userId(),
                user.userName()
        );
    }

    /**
     * 查询所有后台用户，不返回密码
     */
    public List<UserListItemResponse> listUsers(){
        return userRepository.findAll()
                .stream()
                .map(user -> new UserListItemResponse(
                        user.userId(),
                        user.userName(),
                        user.status()
                ))
                .toList();
    }

    /**
     * 修改后台用户的状态
     */
    public void updateStatus(Long userId,Integer status){
        int updatedRows = userRepository.updateStatus(userId, status);

        if(updatedRows == 0){
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 重置后台用户密码
     */
    @Transactional
    public void resetUserPassword(Long userId,String password){
        String encodePassword = passwordEncoder.encode(password);

        int updateRows = userRepository.updatePassword(userId, encodePassword);

        if(updateRows == 0){
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        userRepository.updateLinkedTeacherPassword(userId, encodePassword);
    }
}
