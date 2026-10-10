package com.ai4.externalcrm.repository;

import com.ai4.externalcrm.model.CrmTeacherData;
import com.ai4.externalcrm.model.CrmTeacherIdentity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 使用 JdbcTemplate 实现 CRM 教师数据同步。
 */
@Repository
public class JdbcCrmTeacherRepository implements CrmTeacherRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCrmTeacherRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CrmTeacherIdentity> findByCrmTeacherId(Integer crmTeacherId) {
        return jdbcTemplate.query(
                "SELECT id, user_id FROM live_teach WHERE crm_teacher_id = ? AND del_flag = 1 LIMIT 1",
                (resultSet, rowNumber) -> new CrmTeacherIdentity(
                        resultSet.getString("id"),
                        resultSet.getObject("user_id", Long.class)
                ),
                crmTeacherId
        ).stream().findFirst();
    }

    @Override
    public Optional<CrmTeacherIdentity> findByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id FROM live_teach WHERE user_id = ? AND del_flag = 1 LIMIT 1",
                (resultSet, rowNumber) -> new CrmTeacherIdentity(
                        resultSet.getString("id"),
                        resultSet.getObject("user_id", Long.class)
                ),
                userId
        ).stream().findFirst();
    }

    @Override
    public boolean usernameExists(String username, Integer excludedCrmTeacherId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM live_teach WHERE username = ? "
                        + "AND (? IS NULL OR crm_teacher_id <> ?)",
                Integer.class,
                username,
                excludedCrmTeacherId,
                excludedCrmTeacherId
        );
        return count != null && count > 0;
    }

    @Override
    public void save(CrmTeacherData teacher) {
        jdbcTemplate.update(
                """
                INSERT INTO live_teach(
                    id, user_id, username, password, nickname, del_flag, crm_teacher_id, bz, avatar
                ) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)
                """,
                teacher.id(), teacher.userId(), teacher.username(), teacher.password(), teacher.nickname(),
                teacher.crmTeacherId(), teacher.bz(), teacher.avatar()
        );
    }

    @Override
    public int update(CrmTeacherData teacher) {
        return jdbcTemplate.update(
                """
                UPDATE live_teach
                SET username = ?,
                    password = COALESCE(?, password),
                    nickname = ?,
                    bz = ?,
                    avatar = ?
                WHERE crm_teacher_id = ?
                  AND del_flag = 1
                """,
                teacher.username(), teacher.password(), teacher.nickname(), teacher.bz(),
                teacher.avatar(), teacher.crmTeacherId()
        );
    }
}
