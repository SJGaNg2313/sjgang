package com.ai4.account.service;

import com.ai4.account.client.ClientUserClient;
import com.ai4.account.client.dto.InternalCreateUserRequest;
import com.ai4.account.client.dto.InternalCreateUserResponse;
import com.ai4.account.client.dto.InternalResetPasswordRequest;
import com.ai4.account.dto.AccountDetailResponse;
import com.ai4.account.dto.AccountListItemResponse;
import com.ai4.account.dto.AccountListQuery;
import com.ai4.account.dto.CreateAccountRequest;
import com.ai4.account.dto.CreateAccountResponse;
import com.ai4.account.model.AccountCourseDetailData;
import com.ai4.account.repository.AccountRepository;
import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.response.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 负责服务账户的创建、查询和密码重置规则。
 */
@Service
public class AccountService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final AccountRepository accountRepository;
    private final ClientUserClient clientUserClient;

    public AccountService(
            AccountRepository accountRepository,
            ClientUserClient clientUserClient
    ) {
        this.accountRepository = accountRepository;
        this.clientUserClient = clientUserClient;
    }

    public CreateAccountResponse createAccount(CreateAccountRequest request) {
        InternalCreateUserResponse user = clientUserClient.createUser(
                new InternalCreateUserRequest(
                        request.phone().trim(),
                        request.password(),
                        request.nick()
                )
        );

        return new CreateAccountResponse(
                user.userId(),
                user.account(),
                user.nick(),
                1
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountListItemResponse> listAccounts(AccountListQuery query) {
        int pageNum = query.normalizedPageNum();
        int pageSize = query.normalizedPageSize();
        int offset = (pageNum - 1) * pageSize;

        long total = accountRepository.count(query);
        List<AccountListItemResponse> records = accountRepository
                .findPage(query, offset, pageSize);

        return new PageResponse<>(records, total, pageNum, pageSize);
    }

    /** 查询学员基础信息和最近一节直播课。 */
    @Transactional(readOnly = true)
    public AccountDetailResponse getAccountDetail(String userId) {
        String normalizedUserId = userId == null ? "" : userId.trim();
        AccountCourseDetailData detail = accountRepository.findDetailByUserId(normalizedUserId)
                .orElseThrow(() -> new BizException(ErrorCode.ACCOUNT_NOT_FOUND));

        return new AccountDetailResponse(
                detail.userId(),
                detail.phone(),
                detail.className(),
                detail.courseId(),
                detail.projectName(),
                detail.teacherName(),
                toLocalDateTime(detail.courseTime()),
                calculateActualDurationSeconds(detail),
                detail.status(),
                detail.attended()
        );
    }

    public void resetPassword(String userId, String newPassword) {
        clientUserClient.resetPassword(new InternalResetPasswordRequest(userId, newPassword));
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return Instant.ofEpochMilli(epochMillis)
                .atZone(BUSINESS_ZONE)
                .toLocalDateTime();
    }

    private Long calculateActualDurationSeconds(AccountCourseDetailData detail) {
        if (detail.courseId() == null) {
            return null;
        }
        Long startTime = detail.startTime();
        if (startTime == null || startTime <= 0) {
            return 0L;
        }

        Long endTime = detail.endTime();
        long effectiveEndTime;
        if (endTime != null && endTime >= startTime) {
            effectiveEndTime = endTime;
        } else if (detail.status() != null
                && (detail.status() == 1 || detail.status() == 2)) {
            effectiveEndTime = System.currentTimeMillis();
        } else {
            return 0L;
        }
        return Math.max(0, effectiveEndTime - startTime) / 1000;
    }

}
