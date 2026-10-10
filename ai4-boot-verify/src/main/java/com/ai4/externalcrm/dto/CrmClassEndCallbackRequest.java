package com.ai4.externalcrm.dto;

import java.util.List;

/** CRM 班级直播课结束回调请求。 */
public record CrmClassEndCallbackRequest(
        int liveclassid,
        String classdate,
        String liveurl,
        List<CrmClassEndStudentRequest> list
) {
}
