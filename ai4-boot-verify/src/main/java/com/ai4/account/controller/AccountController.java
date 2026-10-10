package com.ai4.account.controller;

import com.ai4.account.dto.AccountDetailResponse;
import com.ai4.account.dto.AccountListItemResponse;
import com.ai4.account.dto.AccountListQuery;
import com.ai4.account.dto.CreateAccountRequest;
import com.ai4.account.dto.CreateAccountResponse;
import com.ai4.account.dto.ResetAccountPasswordRequest;
import com.ai4.common.response.PageResponse;
import com.ai4.account.service.AccountService;
import com.ai4.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务账户管理接口。
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 新增学员账户。
     */
    @PostMapping
    public ApiResponse<CreateAccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        return ApiResponse.success(accountService.createAccount(request));
    }

    /**
     * 按条件分页查询学员账户。
     */
    @GetMapping
    public ApiResponse<PageResponse<AccountListItemResponse>> listAccounts(
            @Valid @ModelAttribute AccountListQuery query
    ) {
        return ApiResponse.success(accountService.listAccounts(query));
    }

    /**
     * 查询学员基础信息和最近一节直播课。
     */
    @GetMapping("/{userId}")
    public ApiResponse<AccountDetailResponse> getAccountDetail(
            @PathVariable String userId
    ) {
        return ApiResponse.success(accountService.getAccountDetail(userId));
    }

    /**
     * 后台重置指定服务账户的密码。
     */
    @PutMapping("/{userId}/password")
    public ApiResponse<Void> resetPassword(
            @PathVariable String userId,
            @Valid @RequestBody ResetAccountPasswordRequest request
    ) {
        accountService.resetPassword(userId, request.newPassword());
        return ApiResponse.success(null);
    }
}
