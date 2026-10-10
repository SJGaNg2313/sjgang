package com.ai4.partner.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.common.response.PageResponse;
import com.ai4.partner.dto.*;
import com.ai4.partner.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 合伙人管理接口 */
@RestController
@RequestMapping("/partners")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    /** 分页查询未删除的合伙人 */
    @GetMapping
    public ApiResponse<PageResponse<PartnerListItemResponse>> list(
            @Valid @ModelAttribute PartnerPageQuery query
    ) {
        return ApiResponse.success(partnerService.list(query));
    }

    /** 查询合伙人完整详情，用于修改表单回填 */
    @GetMapping("/{partnerId}")
    public ApiResponse<PartnerDetailResponse> get(@PathVariable String partnerId) {
        return ApiResponse.success(partnerService.get(partnerId));
    }

    /** 新增合伙人 */
    @PostMapping
    public ApiResponse<PartnerDetailResponse> create(
            @Valid @RequestBody CreatePartnerRequest request
    ) {
        return ApiResponse.success(partnerService.create(request));
    }

    /** 完整修改合伙人 */
    @PutMapping("/{partnerId}")
    public ApiResponse<PartnerDetailResponse> update(
            @PathVariable String partnerId,
            @Valid @RequestBody UpdatePartnerRequest request
    ) {
        return ApiResponse.success(partnerService.update(partnerId, request));
    }

    /** 归档未绑定项目的合伙人 */
    @DeleteMapping("/{partnerId}")
    public ApiResponse<Void> delete(@PathVariable String partnerId) {
        partnerService.delete(partnerId);
        return ApiResponse.success(null);
    }

    /**
     * 上传合伙人图片
     * @param large
     * @param medium
     * @param small
     * @param fullBody
     * @param avatar
     * @return
     */
    @PostMapping("/images/upload")
    public ApiResponse<PartnerImageUploadResponse> uploadImages(
            @RequestParam(value = "large", required = false) MultipartFile large,
            @RequestParam(value = "medium", required = false) MultipartFile medium,
            @RequestParam(value = "small", required = false) MultipartFile small,
            @RequestParam(value = "fullBody", required = false) MultipartFile fullBody,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        return ApiResponse.success(
                partnerService.uploadImages(
                        large, medium, small, fullBody, avatar
                )
        );
    }
}
