package com.ai4.user.repository;

import com.ai4.user.model.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口。
 *
 * 作用：
 * 1. 定义用户数据访问层必须提供哪些能力。
 * 2. 让 Service 只依赖这个稳定接口，不直接依赖具体实现。
 * 3. 后续把内存实现换成 MySQL / MyBatis 实现时，尽量不改业务层代码。
 */
public interface UserRepository{

    /**
     *根据用户ID查询用户
     */
    Optional<User> findById(Long userId);

    /**
     * 根据用户名判断用户是否已经存在。
     */
    boolean existsByUsername(String userName);

    /**
     * 根据用户名查询用户。
     *
     * Optional<User> 表示：
     * - 查到了：里面有一个 User
     * - 没查到：里面是空的
     */
    Optional<User> findByUsername(String userName);

    /**
     * 保存用户，并返回保存后的用户对象。
     */
    User save(String userName,String password);

    /**
     * 查询后台全部用户
     */
    List<User> findAll();

    /**
     * 修改后台用户的状态
     * @return SQL影响的记录数
     */
    int updateStatus(Long userId,Integer status);

    /**
     * 重置后台用户密码
     */
    int updatePassword(Long userId, String encodedPassword);

    /**
     * 修改后台用户的用户名，并在新密码不为空时同步修改密码。
     */
    int updateCredentials(Long userId, String userName, String encodedPassword);

    /**
     * 同步修改与后台用户关联的直播教师密码。
     */
    int updateLinkedTeacherPassword(Long userId, String encodedPassword);
}
