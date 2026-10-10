package com.ai4.externalcrm.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/** CRM 班级结束回调中的学生考勤数据。 */
public record CrmClassEndStudentRequest(
        String phone,
        long attendanceduration,
        int isattendance,
        int islate,
        int isleaveearly,
        String entrytime,
        String logouttime
) {

    /** 缺勤学生没有进入和退出时间时，省略这两个字段。 */
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phone", phone);
        payload.put("attendanceduration", attendanceduration);
        payload.put("isattendance", isattendance);
        payload.put("islate", islate);
        payload.put("isleaveearly", isleaveearly);
        if (entrytime != null) {
            payload.put("entrytime", entrytime);
        }
        if (logouttime != null) {
            payload.put("logouttime", logouttime);
        }
        return payload;
    }
}
