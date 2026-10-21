package com.example.admin.module.monitor.operlog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.PageResult;
import com.example.admin.module.monitor.operlog.dto.SysOperLogQuery;
import com.example.admin.module.monitor.operlog.entity.SysOperLog;
import com.example.admin.module.monitor.operlog.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysOperLogService {

    private final SysOperLogMapper sysOperLogMapper;

    /** 由切面调用：日志自身出错不应影响业务，调用方已 try/catch */
    public void save(SysOperLog log) {
        sysOperLogMapper.insert(log);
    }

    public PageResult<SysOperLog> page(SysOperLogQuery query) {
        Page<SysOperLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysOperLog> wrapper = Wrappers.<SysOperLog>lambdaQuery()
                .like(StringUtils.hasText(query.getTitle()), SysOperLog::getTitle, query.getTitle())
                .like(StringUtils.hasText(query.getOperName()), SysOperLog::getOperName, query.getOperName())
                .eq(query.getStatus() != null, SysOperLog::getStatus, query.getStatus())
                .orderByDesc(SysOperLog::getId);
        sysOperLogMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    public void delete(List<Long> ids) {
        sysOperLogMapper.deleteByIds(ids);
    }

    /** 清空全部操作日志 */
    public void clean() {
        sysOperLogMapper.delete(Wrappers.emptyWrapper());
    }
}
