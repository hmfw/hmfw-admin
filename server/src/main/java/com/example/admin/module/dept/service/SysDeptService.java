package com.example.admin.module.dept.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.admin.common.api.OptionVO;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.dept.dto.SysDeptRequest;
import com.example.admin.module.dept.dto.SysDeptTreeVO;
import com.example.admin.module.dept.entity.SysDept;
import com.example.admin.module.dept.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper sysDeptMapper;

    /** 部门树，可按名称/状态过滤 */
    public List<SysDeptTreeVO> tree(String deptName, Integer status) {
        LambdaQueryWrapper<SysDept> wrapper = Wrappers.<SysDept>lambdaQuery()
                .like(StringUtils.hasText(deptName), SysDept::getDeptName, deptName)
                .eq(status != null, SysDept::getStatus, status)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getSort);
        return SysDeptTreeVO.build(sysDeptMapper.selectList(wrapper).stream()
                .map(this::toTreeVO)
                .toList());
    }

    public SysDept getDetail(Long id) {
        return requireDept(id);
    }

    @Transactional
    public Long create(SysDeptRequest request) {
        SysDept dept = new SysDept();
        applyRequest(dept, request);
        dept.setAncestors(computeAncestors(request.getParentId()));
        sysDeptMapper.insert(dept);
        return dept.getId();
    }

    @Transactional
    public void update(Long id, SysDeptRequest request) {
        SysDept dept = requireDept(id);
        Long newParentId = request.getParentId() == null ? 0L : request.getParentId();
        requireNoCycle(id, newParentId);

        String oldAncestors = dept.getAncestors();
        applyRequest(dept, request);
        String newAncestors = computeAncestors(newParentId);
        dept.setAncestors(newAncestors);
        sysDeptMapper.updateById(dept);

        // 父级变化时，同步刷新所有子孙的 ancestors（否则数据范围按 ancestors 过滤会错）
        if (!newAncestors.equals(oldAncestors)) {
            refreshChildrenAncestors(id, oldAncestors + "," + id, newAncestors + "," + id);
        }
    }

    @Transactional
    public void delete(Long id) {
        requireDept(id);
        if (sysDeptMapper.countChildren(id) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "存在子部门，无法删除");
        }
        if (sysDeptMapper.countUsers(id) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "部门下存在用户，无法删除");
        }
        sysDeptMapper.deleteById(id);
    }

    /** 部门字典：id/名称，供下拉或穿梭用 */
    public List<OptionVO> listOptions() {
        return sysDeptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                        .eq(SysDept::getStatus, 1)
                        .orderByAsc(SysDept::getParentId)
                        .orderByAsc(SysDept::getSort))
                .stream()
                .map(d -> OptionVO.of(d.getDeptName(), String.valueOf(d.getId())))
                .toList();
    }

    /** 某部门及其所有子孙的 id（供数据范围"本部门及以下"） */
    public List<Long> listSelfAndDescendantIds(Long deptId) {
        List<SysDept> all = sysDeptMapper.selectList(null);
        return all.stream()
                .filter(d -> d.getId().equals(deptId) || inSubtree(d, deptId))
                .map(SysDept::getId)
                .toList();
    }

    private boolean inSubtree(SysDept dept, Long rootId) {
        if (dept.getAncestors() == null) {
            return false;
        }
        for (String a : dept.getAncestors().split(",")) {
            if (a.equals(String.valueOf(rootId))) {
                return true;
            }
        }
        return false;
    }

    private void refreshChildrenAncestors(Long parentId, String oldPrefix, String newPrefix) {
        List<SysDept> children = sysDeptMapper.selectList(
                Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, parentId));
        for (SysDept child : children) {
            String childAncestors = child.getAncestors() == null ? ""
                    : child.getAncestors().replaceFirst("^" + java.util.regex.Pattern.quote(oldPrefix), newPrefix);
            child.setAncestors(childAncestors);
            sysDeptMapper.updateById(child);
            refreshChildrenAncestors(child.getId(), oldPrefix + "," + child.getId(), newPrefix + "," + child.getId());
        }
    }

    private String computeAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = sysDeptMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "父部门不存在");
        }
        if (parent.getStatus() != null && parent.getStatus() == 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "父部门已停用，不允许新增");
        }
        return parent.getAncestors() + "," + parentId;
    }

    private void requireNoCycle(Long id, Long newParentId) {
        if (newParentId == null || newParentId == 0L) {
            return;
        }
        if (newParentId.equals(id)) {
            throw new BizException(ResultCode.BAD_REQUEST, "父部门不能是自己");
        }
        Long cursor = newParentId;
        int guard = 0;
        while (cursor != null && cursor != 0L && guard++ < 100) {
            if (cursor.equals(id)) {
                throw new BizException(ResultCode.BAD_REQUEST, "父部门不能是自己的子部门");
            }
            SysDept parent = sysDeptMapper.selectById(cursor);
            cursor = parent == null ? null : parent.getParentId();
        }
    }

    private void applyRequest(SysDept dept, SysDeptRequest request) {
        dept.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        dept.setDeptName(request.getDeptName());
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setEmail(request.getEmail());
        dept.setSort(request.getSort() == null ? 0 : request.getSort());
        dept.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private SysDept requireDept(Long id) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return dept;
    }

    private SysDeptTreeVO toTreeVO(SysDept dept) {
        SysDeptTreeVO vo = new SysDeptTreeVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setDeptName(dept.getDeptName());
        vo.setLeader(dept.getLeader());
        vo.setPhone(dept.getPhone());
        vo.setEmail(dept.getEmail());
        vo.setSort(dept.getSort());
        vo.setStatus(dept.getStatus());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }
}
