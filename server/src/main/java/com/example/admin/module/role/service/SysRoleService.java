package com.example.admin.module.role.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.OptionVO;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.constant.SecurityConstants;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.menu.mapper.SysMenuMapper;
import com.example.admin.module.role.dto.SysRoleQuery;
import com.example.admin.module.role.dto.SysRoleRequest;
import com.example.admin.module.role.dto.SysRoleVO;
import com.example.admin.module.role.entity.SysRole;
import com.example.admin.module.role.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    /** 内置超级管理员角色，不允许删除，也不允许改 roleKey */
    private static final Long BUILTIN_ADMIN_ROLE_ID = 1L;

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    public PageResult<SysRoleVO> page(SysRoleQuery query) {
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.<SysRole>lambdaQuery()
                .like(StringUtils.hasText(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId);

        sysRoleMapper.selectPage(page, wrapper);
        return PageResult.of(page, this::toVO);
    }

    /** 启用角色的字典项（label=角色名，value=角色 id 字符串），供前端下拉选择使用 */
    public List<OptionVO> listOptions() {
        return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getSort))
                .stream()
                .map(this::toOptionVO)
                .toList();
    }

    public SysRoleVO getDetail(Long id) {
        SysRoleVO vo = toVO(requireRole(id));
        vo.setMenuIds(sysMenuMapper.selectMenuIdsByRoleId(id));
        vo.setDeptIds(sysRoleMapper.selectDeptIdsByRoleId(id));
        return vo;
    }

    @Transactional
    public Long create(SysRoleRequest request) {
        requireRoleKeyAvailable(request.getRoleKey(), null);

        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setSort(request.getSort() == null ? 0 : request.getSort());
        role.setDataScope(request.getDataScope() == null ? 1 : request.getDataScope());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setRemark(request.getRemark());
        sysRoleMapper.insert(role);

        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            sysMenuMapper.insertRoleMenus(role.getId(), request.getMenuIds());
        }
        replaceDepts(role.getId(), role.getDataScope(), request.getDeptIds());
        return role.getId();
    }

    @Transactional
    public void update(Long id, SysRoleRequest request) {
        SysRole role = requireRole(id);

        if (BUILTIN_ADMIN_ROLE_ID.equals(id) && !SecurityConstants.ADMIN_ROLE_KEY.equals(request.getRoleKey())) {
            throw BizException.forbidden(ResultCode.FORBIDDEN, "内置管理员角色的标识不允许修改");
        }
        requireRoleKeyAvailable(request.getRoleKey(), id);

        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        if (request.getSort() != null) {
            role.setSort(request.getSort());
        }
        if (request.getDataScope() != null) {
            role.setDataScope(request.getDataScope());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        role.setRemark(request.getRemark());
        sysRoleMapper.updateById(role);

        if (request.getMenuIds() != null) {
            replaceMenus(id, request.getMenuIds());
        }
        // 数据范围变化或传了自定义部门时，刷新 sys_role_dept
        if (request.getDataScope() != null || request.getDeptIds() != null) {
            replaceDepts(id, role.getDataScope(), request.getDeptIds());
        }
    }

    @Transactional
    public void delete(Long id) {
        requireRole(id);

        if (BUILTIN_ADMIN_ROLE_ID.equals(id)) {
            throw BizException.forbidden(ResultCode.FORBIDDEN, "内置管理员角色不允许删除");
        }
        if (sysRoleMapper.countUserByRoleId(id) > 0) {
            throw new BizException(ResultCode.ROLE_IN_USE);
        }

        sysMenuMapper.deleteRoleMenus(id);
        sysRoleMapper.deleteRoleDepts(id);
        sysRoleMapper.deleteById(id);   // 逻辑删除
    }

    @Transactional
    public void assignMenus(Long id, List<Long> menuIds) {
        requireRole(id);
        replaceMenus(id, menuIds);
    }

    private void replaceMenus(Long roleId, List<Long> menuIds) {
        sysMenuMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            sysMenuMapper.insertRoleMenus(roleId, menuIds);
        }
    }

    /** 刷新角色自定义数据范围的部门：非自定义(2)一律清空 */
    private void replaceDepts(Long roleId, Integer dataScope, List<Long> deptIds) {
        sysRoleMapper.deleteRoleDepts(roleId);
        if (dataScope != null && dataScope == 2 && deptIds != null && !deptIds.isEmpty()) {
            sysRoleMapper.insertRoleDepts(roleId, deptIds);
        }
    }

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return role;
    }

    private void requireRoleKeyAvailable(String roleKey, Long excludeId) {
        Long count = sysRoleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.ROLE_KEY_EXISTS);
        }
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleKey(role.getRoleKey());
        vo.setSort(role.getSort());
        vo.setDataScope(role.getDataScope());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }

    private OptionVO toOptionVO(SysRole role) {
        return OptionVO.of(role.getRoleName(), String.valueOf(role.getId()));
    }
}
