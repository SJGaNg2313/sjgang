package com.ai4.partner.repository;

import com.ai4.partner.model.Partner;

import java.util.List;
import java.util.Optional;

/** 合伙人数据访问接口 */
public interface PartnerRepository {
    long count();
    List<Partner> findPage(int offset, int limit);
    Optional<Partner> findById(String partnerId);
    boolean existsByName(String partnerName, String excludePartnerId);
    int save(Partner partner);
    int update(Partner partner, int expectedVersion);
    int softDelete(String partnerId);
}
