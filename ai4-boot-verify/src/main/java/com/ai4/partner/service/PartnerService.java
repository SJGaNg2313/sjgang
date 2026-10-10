package com.ai4.partner.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.response.PageResponse;
import com.ai4.file.service.OssStorageService;
import com.ai4.partner.dto.*;
import com.ai4.partner.model.Partner;
import com.ai4.partner.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/** 合伙人的分页查询、详情、新增、修改和逻辑删除业务 */
@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final OssStorageService ossStorageService;

    public PartnerService(PartnerRepository partnerRepository, OssStorageService ossStorageService) {
        this.partnerRepository = partnerRepository;
        this.ossStorageService = ossStorageService;
    }

    /** 分页查询未删除的合伙人基础信息。 */
    @Transactional(readOnly = true)
    public PageResponse<PartnerListItemResponse> list(PartnerPageQuery query) {
        int pageNum = query.normalizedPageNum();
        int pageSize = query.normalizedPageSize();
        List<Partner> partners = partnerRepository.findPage(query.offset(), pageSize);
        List<PartnerListItemResponse> records = partners.stream()
                .map(partner -> new PartnerListItemResponse(
                        partner.id(), partner.partnerName(),
                        partner.title(), partner.declaration(),
                        partner.themeColor(), partner.large()
                ))
                .toList();
        return new PageResponse<>(
                records, partnerRepository.count(), pageNum, pageSize
        );
    }

    /** 查询合伙人完整信息，用于修改表单回填 */
    @Transactional(readOnly = true)
    public PartnerDetailResponse get(String partnerId) {
        Partner partner = findPartner(partnerId);
        return toDetailResponse(partner);
    }

    /** 在同一事务中新增合伙人 */
    @Transactional
    public PartnerDetailResponse create(CreatePartnerRequest request) {
        String partnerName = request.partnerName().trim();
        // 当前允许不同用户创建同名合伙人；如后续恢复名称唯一，再启用此校验。
        // validateNameAvailable(partnerName, null);
        String partnerId = UUID.randomUUID().toString();
        Partner partner = buildPartner(
                partnerId, partnerName, request.title(),
                request.declaration(), request.openingMessage(), request.themeColor(),
                request.large(), request.medium(), request.small(),
                request.fullBody(), request.avatar()
        );
        if (partnerRepository.save(partner) == 0) {
            throw new IllegalStateException("Failed to create partner");
        }
//        try {
//            if (partnerRepository.save(partner) == 0) {
//                throw new IllegalStateException("Failed to create partner");
//            }
//        } catch (DuplicateKeyException exception) {
//            throw new BizException(ErrorCode.PARTNER_NAME_ALREADY_EXISTS);
//        }
        return get(partnerId);
    }

    /** 在同一事务中完整修改合伙人 */
    @Transactional
    public PartnerDetailResponse update(String partnerId, UpdatePartnerRequest request) {
        Partner existing = findPartner(partnerId);
        String partnerName = request.partnerName().trim();
        // 当前允许不同用户使用相同的合伙人名称。
        // validateNameAvailable(partnerName, partnerId);
        Partner partner = buildPartner(
                partnerId, partnerName, request.title(),
                request.declaration(), request.openingMessage(), request.themeColor(),
                keepExistingIfBlank(request.large(), existing.large()),
                keepExistingIfBlank(request.medium(), existing.medium()),
                keepExistingIfBlank(request.small(), existing.small()),
                keepExistingIfBlank(request.fullBody(), existing.fullBody()),
                keepExistingIfBlank(request.avatar(), existing.avatar())
        );
        if (partnerRepository.update(partner, request.version()) == 0) {
            if (partnerRepository.findById(partnerId).isEmpty()) {
                throw new BizException(ErrorCode.PARTNER_NOT_FOUND);
            }
            throw new BizException(ErrorCode.PARTNER_CONCURRENT_MODIFICATION);
        }
//        try {
//            if (partnerRepository.update(partner, request.version()) == 0) {
//                if (partnerRepository.findById(partnerId).isEmpty()) {
//                    throw new BizException(ErrorCode.PARTNER_NOT_FOUND);
//                }
//                throw new BizException(ErrorCode.PARTNER_CONCURRENT_MODIFICATION);
//            }
//        } catch (DuplicateKeyException exception) {
//            throw new BizException(ErrorCode.PARTNER_NAME_ALREADY_EXISTS);
//        }
        return get(partnerId);
    }

    /** 归档未绑定项目的合伙人，不删除历史业务数据 */
    @Transactional
    public void delete(String partnerId) {
        if (partnerRepository.softDelete(partnerId) == 0) {
            if (partnerRepository.findById(partnerId).isEmpty()) {
                throw new BizException(ErrorCode.PARTNER_NOT_FOUND);
            }
            throw new BizException(ErrorCode.PARTNER_IN_USE);
        }
    }

    private Partner findPartner(String partnerId) {
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BizException(ErrorCode.PARTNER_NOT_FOUND));
    }

//    private void validateNameAvailable(String partnerName, String excludePartnerId) {
//        if (partnerRepository.existsByName(partnerName, excludePartnerId)) {
//            throw new BizException(ErrorCode.PARTNER_NAME_ALREADY_EXISTS);
//        }
//    }

    private Partner buildPartner(
            String partnerId, String partnerName, String title, String declaration,
            String openingMessage, String themeColor, String large, String medium,
            String small, String fullBody, String avatar
    ) {
        return new Partner(
                partnerId, partnerName.trim(), title.trim(), declaration.trim(),
                openingMessage.trim(), themeColor.trim(), trimToNull(large),
                trimToNull(medium), trimToNull(small), trimToNull(fullBody),
                trimToNull(avatar), 0, 1, null, null
        );
    }

    private PartnerDetailResponse toDetailResponse(Partner partner) {
        return new PartnerDetailResponse(
                partner.id(), partner.partnerName(), partner.version(), partner.title(),
                partner.declaration(), partner.openingMessage(), partner.themeColor(),
                partner.large(), partner.medium(), partner.small(),
                partner.fullBody(), partner.avatar()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String keepExistingIfBlank(String requested, String existing) {
        return requested == null || requested.isBlank() ? existing : requested;
    }

    /**
     * 校验并上传合伙人图片
     */
    public PartnerImageUploadResponse uploadImages(
            MultipartFile large,
            MultipartFile medium,
            MultipartFile small,
            MultipartFile fullBody,
            MultipartFile avatar
    ){
        if (!hasFile(large)
                && !hasFile(medium)
                && !hasFile(small)
                && !hasFile(fullBody)
                && !hasFile(avatar)) {
            throw new BizException("PARTNER_IMAGE_EMPTY", "至少上传一张合伙人图片");
        }

        return new PartnerImageUploadResponse(
                uploadImageIfPresent(large, "partner/large"),
                uploadImageIfPresent(medium, "partner/medium"),
                uploadImageIfPresent(small, "partner/small"),
                uploadImageIfPresent(fullBody, "partner/full-body"),
                uploadImageIfPresent(avatar, "partner/avatar")
        );
    }

    private String uploadImageIfPresent(MultipartFile file, String directory) {
        if (!hasFile(file)) {
            return null;
        }
        validateImage(file);
        return ossStorageService.upload(file, directory).url();
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    /**
     * 校验图片
     */
    private void validateImage(MultipartFile file){
        if(file.getSize() >5L * 1024 * 1024){
            throw new BizException("PARTNER_IMAGE_TOO_LARGE","单张图片不能大于5M");
        }

        String filename = file.getOriginalFilename();
        int dotIndex = filename == null ? -1 : filename.lastIndexOf('.');

        String extension = dotIndex < 0
                ? ""
                : filename.substring(dotIndex + 1).toLowerCase();

        if (!Set.of("png", "jpg", "jpeg").contains(extension)) {
            throw new BizException(
                    "PARTNER_IMAGE_FORMAT_INVALID",
                    "图片格式只支持png、jpg、jpeg"
            );
        }
    }
}
