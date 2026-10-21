package com.example.admin.module.dashboard.dto;

import com.example.admin.module.notice.dto.MessageVO;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DashboardSummaryVO {
    private long totalUsers;
    private long enabledUsers;
    private long todayLogins;
    private long todayFailures;
    private long onlineSessions;
    private long fileCount;
    private long fileSize;
    private long unreadMessages;
    private List<LoginTrend> loginTrend;
    private List<Activity> recentActivities;
    private List<MessageVO> recentMessages;

    @Data
    public static class LoginTrend {
        private LocalDate date;
        private long success;
        private long failure;
    }

    @Data
    public static class Activity {
        private String title;
        private String businessType;
        private String operator;
        private Integer status;
        private LocalDateTime time;
    }
}
