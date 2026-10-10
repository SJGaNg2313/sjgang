package com.ai4.externalcrm.controller;

import com.ai4.account.dto.CreateAccountRequest;
import com.ai4.account.dto.CreateAccountResponse;
import com.ai4.account.service.AccountService;
import com.ai4.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CRM 学员同步接口。 */
@RestController
@RequestMapping("/external/aiUser")
public class ExternalCrmStudentController {
    private final AccountService accountService;

    public ExternalCrmStudentController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** CRM 新建学员账户。 */
    @PostMapping("/add")
    public ApiResponse<CreateAccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        return ApiResponse.success(accountService.createAccount(request));
    }
}
