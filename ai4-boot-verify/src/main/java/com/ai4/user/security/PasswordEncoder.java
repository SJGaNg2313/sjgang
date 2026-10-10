package com.ai4.user.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码处理组件。
 *
 * 作用：
 * 1. 创建用户时，把前端传入的明文密码转换成 BCrypt 哈希后再保存。
 * 2. 登录时，把用户输入的明文密码和数据库中的 BCrypt 哈希进行匹配。
 *
 * 注意：
 * BCrypt 是单向哈希，不是加密解密。
 * 数据库里的密码不能被“解密”回明文，只能通过 matches 判断是否匹配。
 */
@Component
public class PasswordEncoder {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    /**
     * 把明文密码转换成 BCrypt 哈希。
     *
     * 同一个明文密码每次生成的结果通常都不同，因为 BCrypt 会自动加入随机盐。
     */
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }


    /**
     * 判断用户输入的明文密码，是否能匹配数据库中保存的 BCrypt 哈希。
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
