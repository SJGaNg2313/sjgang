package com.ai4.account.repository;

import com.ai4.account.dto.AccountListQuery;
import com.ai4.account.dto.AccountListItemResponse;
import com.ai4.account.model.AccountCourseDetailData;

import java.util.List;
import java.util.Optional;

/**
 * 服务账户的数据访问边界。
 */
public interface AccountRepository {

    Optional<AccountCourseDetailData> findDetailByUserId(String userId);

    long count(AccountListQuery query);

    List<AccountListItemResponse> findPage(AccountListQuery query, int offset, int limit);
}
