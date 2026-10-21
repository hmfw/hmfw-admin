package com.example.admin.module.monitor.loginlog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.PageResult;
import com.example.admin.module.monitor.loginlog.dto.SysLoginLogQuery;
import com.example.admin.module.monitor.loginlog.entity.SysLoginLog;
import com.example.admin.module.monitor.loginlog.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysLoginLogService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SysLoginLogMapper sysLoginLogMapper;

    /** 记录一条登录日志；login_time 固定北京时间，和实体字段填充策略保持一致 */
    public void record(String username, String ip, boolean success, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setIp(ip);
        log.setStatus(success ? 0 : 1);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now(ZONE));
        sysLoginLogMapper.insert(log);
    }

    public PageResult<SysLoginLog> page(SysLoginLogQuery query) {
        Page<SysLoginLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysLoginLog> wrapper = Wrappers.<SysLoginLog>lambdaQuery()
                .like(StringUtils.hasText(query.getUsername()), SysLoginLog::getUsername, query.getUsername())
                .eq(query.getStatus() != null, SysLoginLog::getStatus, query.getStatus())
                .orderByDesc(SysLoginLog::getId);
        sysLoginLogMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    public void delete(List<Long> ids) {
        sysLoginLogMapper.deleteByIds(ids);
    }

    /** 清空全部登录日志 */
    public void clean() {
        sysLoginLogMapper.delete(Wrappers.emptyWrapper());
    }
}
