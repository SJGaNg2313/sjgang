package com.ai4.auth.service;

import com.ai4.auth.captcha.service.CaptchaService;
import com.ai4.auth.dto.LoginRequest;
import com.ai4.auth.dto.LoginResponse;
import com.ai4.auth.jwt.JwtService;
import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.user.model.User;
import com.ai4.user.repository.UserRepository;
import com.ai4.user.security.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录认证业务
 * 1.根据用户名查询数据库用户
 * 2.校验用户输入的密码是否正确
 * 3.登录成功后返回登录结果
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CaptchaService captchaService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CaptchaService captchaService
    ){
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.captchaService = captchaService;
    }

    /**
     * 查询用户、校验密码并生成登录 Token。
     */
    public LoginResponse login(LoginRequest request){
        captchaService.verify(request.captcha_id(), request.captcha_code());
        User user = userRepository.findByUsername(request.user_name())
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_USERNAME_OR_PASSWORD));

        if(user.status() != 0){
            throw new BizException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }

        if(!passwordEncoder.matches(request.password(), user.password())){
            throw new BizException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }

        String token = jwtService.generateToken(user.userId(), user.userName());

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.expireSeconds()
        );
    }
}
