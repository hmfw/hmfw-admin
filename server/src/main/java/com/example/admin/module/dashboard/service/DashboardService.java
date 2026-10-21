package com.example.admin.module.dashboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.admin.module.dashboard.dto.DashboardSummaryVO;
import com.example.admin.module.file.entity.SysFile;
import com.example.admin.module.file.mapper.SysFileMapper;
import com.example.admin.module.monitor.loginlog.entity.SysLoginLog;
import com.example.admin.module.monitor.loginlog.mapper.SysLoginLogMapper;
import com.example.admin.module.monitor.online.service.SysOnlineSessionService;
import com.example.admin.module.monitor.operlog.entity.SysOperLog;
import com.example.admin.module.monitor.operlog.mapper.SysOperLogMapper;
import com.example.admin.module.notice.dto.MessageQuery;
import com.example.admin.module.notice.service.MessageService;
import com.example.admin.module.user.entity.SysUser;
import com.example.admin.module.user.mapper.SysUserMapper;
import com.example.admin.security.LoginUser;
import com.example.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SysUserMapper userMapper;
    private final SysFileMapper fileMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysOperLogMapper operLogMapper;
    private final SysOnlineSessionService onlineSessionService;
    private final MessageService messageService;

    public DashboardSummaryVO summary() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime todayStart = today.atStartOfDay();
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setTotalUsers(userMapper.selectCount(Wrappers.lambdaQuery()));
        vo.setEnabledUsers(userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getStatus, 1)));
        vo.setTodayLogins(loginLogMapper.selectCount(Wrappers.<SysLoginLog>lambdaQuery()
                .eq(SysLoginLog::getStatus, 0)
                .ge(SysLoginLog::getLoginTime, todayStart)));
        vo.setTodayFailures(loginLogMapper.selectCount(Wrappers.<SysLoginLog>lambdaQuery()
                .eq(SysLoginLog::getStatus, 1)
                .ge(SysLoginLog::getLoginTime, todayStart)));
        vo.setOnlineSessions(onlineSessionService.countOnlineUsers());
        List<SysFile> files = fileMapper.selectList(Wrappers.lambdaQuery());
        vo.setFileCount(files.size());
        vo.setFileSize(files.stream().map(SysFile::getSize).filter(size -> size != null)
                .mapToLong(Long::longValue).sum());
        vo.setUnreadMessages(messageService.unreadCount());
        vo.setLoginTrend(loginTrend(today));
        vo.setRecentActivities(recentActivities());

        MessageQuery messageQuery = new MessageQuery();
        messageQuery.setPageSize(5);
        vo.setRecentMessages(messageService.page(messageQuery).getRecords());
        return vo;
    }

    private List<DashboardSummaryVO.LoginTrend> loginTrend(LocalDate today) {
        LocalDate firstDay = today.minusDays(6);
        List<SysLoginLog> logs = loginLogMapper.selectList(Wrappers.<SysLoginLog>lambdaQuery()
                .ge(SysLoginLog::getLoginTime, firstDay.atStartOfDay())
                .le(SysLoginLog::getLoginTime, today.atTime(LocalTime.MAX))
                .orderByAsc(SysLoginLog::getLoginTime));
        Map<LocalDate, DashboardSummaryVO.LoginTrend> days = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            DashboardSummaryVO.LoginTrend point = new DashboardSummaryVO.LoginTrend();
            point.setDate(firstDay.plusDays(i));
            days.put(point.getDate(), point);
        }
        for (SysLoginLog log : logs) {
            DashboardSummaryVO.LoginTrend point = days.get(log.getLoginTime().toLocalDate());
            if (point == null) continue;
            if (log.getStatus() == 0) point.setSuccess(point.getSuccess() + 1);
            else point.setFailure(point.getFailure() + 1);
        }
        return new ArrayList<>(days.values());
    }

    private List<DashboardSummaryVO.Activity> recentActivities() {
        LoginUser current = SecurityUtils.getLoginUser();
        var wrapper = Wrappers.<SysOperLog>lambdaQuery();
        if (!current.isSuperAdmin()) wrapper.eq(SysOperLog::getOperName, current.getUsername());
        wrapper.orderByDesc(SysOperLog::getOperTime).last("LIMIT 6");
        return operLogMapper.selectList(wrapper).stream().map(log -> {
            DashboardSummaryVO.Activity item = new DashboardSummaryVO.Activity();
            item.setTitle(log.getTitle());
            item.setBusinessType(log.getBusinessType());
            item.setOperator(log.getOperName());
            item.setStatus(log.getStatus());
            item.setTime(log.getOperTime());
            return item;
        }).toList();
    }
}
