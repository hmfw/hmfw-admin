package com.example.admin.module.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.OptionVO;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.notice.dto.SysNoticeQuery;
import com.example.admin.module.notice.dto.SysNoticeRequest;
import com.example.admin.module.notice.dto.NoticeTargetOptionsVO;
import com.example.admin.module.notice.entity.SysNotice;
import com.example.admin.module.notice.entity.SysNoticeRead;
import com.example.admin.module.notice.entity.SysNoticeTarget;
import com.example.admin.module.notice.mapper.SysNoticeMapper;
import com.example.admin.module.notice.mapper.SysNoticeReadMapper;
import com.example.admin.module.notice.mapper.SysNoticeTargetMapper;
import com.example.admin.security.LoginUser;
import com.example.admin.security.SecurityUtils;
import com.example.admin.module.role.service.SysRoleService;
import com.example.admin.module.dept.service.SysDeptService;
import com.example.admin.module.user.entity.SysUser;
import com.example.admin.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysNoticeService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SysNoticeMapper sysNoticeMapper;
    private final SysNoticeTargetMapper targetMapper;
    private final SysNoticeReadMapper readMapper;
    private final SysRoleService roleService;
    private final SysDeptService deptService;
    private final SysUserMapper userMapper;

    public PageResult<SysNotice> page(SysNoticeQuery query) {
        Page<SysNotice> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysNotice> wrapper = Wrappers.<SysNotice>lambdaQuery()
                .like(StringUtils.hasText(query.getNoticeTitle()), SysNotice::getNoticeTitle, query.getNoticeTitle())
                .eq(query.getNoticeType() != null, SysNotice::getNoticeType, query.getNoticeType())
                .eq(query.getStatus() != null, SysNotice::getStatus, query.getStatus())
                .orderByDesc(SysNotice::getCreateTime)
                .orderByDesc(SysNotice::getId);
        sysNoticeMapper.selectPage(page, wrapper);
        page.getRecords().forEach(this::fillTargets);
        return PageResult.of(page);
    }

    public SysNotice getDetail(Long id) {
        SysNotice notice = requireNotice(id);
        fillTargets(notice);
        return notice;
    }

    public NoticeTargetOptionsVO targetOptions() {
        NoticeTargetOptionsVO vo = new NoticeTargetOptionsVO();
        vo.setRoles(roleService.listOptions());
        vo.setDepartments(deptService.listOptions());
        vo.setUsers(userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getStatus, 1)
                        .orderByAsc(SysUser::getUsername))
                .stream()
                .map(user -> OptionVO.of(
                        (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                                + " (" + user.getUsername() + ")",
                        String.valueOf(user.getId())))
                .toList());
        return vo;
    }

    @Transactional
    public Long create(SysNoticeRequest request) {
        validateAudience(request);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysNotice notice = new SysNotice();
        apply(notice, request);
        notice.setCreatorId(loginUser.getUserId());
        notice.setCreatorName(StringUtils.hasText(loginUser.getNickname())
                ? loginUser.getNickname() : loginUser.getUsername());
        if (notice.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now(ZONE));
        }
        sysNoticeMapper.insert(notice);
        saveTargets(notice.getId(), request);
        return notice.getId();
    }

    @Transactional
    public void update(Long id, SysNoticeRequest request) {
        validateAudience(request);
        SysNotice notice = requireNotice(id);
        Integer previousStatus = notice.getStatus();
        apply(notice, request);
        if (notice.getStatus() == 1 && (previousStatus == null || previousStatus == 0)) {
            notice.setPublishTime(LocalDateTime.now(ZONE));
        } else if (notice.getStatus() == 0) {
            notice.setPublishTime(null);
        }
        sysNoticeMapper.updateById(notice);
        targetMapper.delete(Wrappers.<SysNoticeTarget>lambdaQuery()
                .eq(SysNoticeTarget::getNoticeId, id));
        readMapper.delete(Wrappers.<SysNoticeRead>lambdaQuery()
                .eq(SysNoticeRead::getNoticeId, id));
        saveTargets(id, request);
    }

    @Transactional
    public void delete(List<Long> ids) {
        targetMapper.delete(Wrappers.<SysNoticeTarget>lambdaQuery()
                .in(SysNoticeTarget::getNoticeId, ids));
        readMapper.delete(Wrappers.<SysNoticeRead>lambdaQuery()
                .in(SysNoticeRead::getNoticeId, ids));
        sysNoticeMapper.deleteByIds(ids);
    }

    private void apply(SysNotice notice, SysNoticeRequest request) {
        notice.setNoticeTitle(request.getNoticeTitle().trim());
        notice.setNoticeType(request.getNoticeType());
        notice.setNoticeContent(request.getNoticeContent());
        notice.setStatus(request.getStatus());
        notice.setRemark(request.getRemark());
    }

    private SysNotice requireNotice(Long id) {
        SysNotice notice = sysNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return notice;
    }

    private void validateAudience(SysNoticeRequest request) {
        String type = request.getAudienceType().toUpperCase();
        if (!Set.of("ALL", "ROLE", "DEPT", "USER").contains(type)) {
            throw new IllegalArgumentException("接收范围不正确");
        }
        request.setAudienceType(type);
        if (!"ALL".equals(type) && (request.getTargetIds() == null || request.getTargetIds().isEmpty())) {
            throw new IllegalArgumentException("请选择接收对象");
        }
    }

    private void saveTargets(Long noticeId, SysNoticeRequest request) {
        List<Long> targetIds = "ALL".equals(request.getAudienceType())
                ? List.of(0L) : request.getTargetIds().stream().distinct().toList();
        for (Long targetId : targetIds) {
            SysNoticeTarget target = new SysNoticeTarget();
            target.setNoticeId(noticeId);
            target.setTargetType(request.getAudienceType());
            target.setTargetId(targetId);
            targetMapper.insert(target);
        }
    }

    private void fillTargets(SysNotice notice) {
        List<SysNoticeTarget> targets = targetMapper.selectList(Wrappers.<SysNoticeTarget>lambdaQuery()
                .eq(SysNoticeTarget::getNoticeId, notice.getId()));
        if (targets.isEmpty()) {
            notice.setAudienceType("ALL");
            notice.setTargetIds(List.of());
            return;
        }
        notice.setAudienceType(targets.getFirst().getTargetType());
        notice.setTargetIds(targets.stream()
                .filter(target -> target.getTargetId() != 0)
                .map(SysNoticeTarget::getTargetId)
                .toList());
    }
}
