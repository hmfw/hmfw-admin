package com.example.admin.security.datascope;

import com.example.admin.module.dept.service.SysDeptService;
import com.example.admin.module.role.entity.SysRole;
import com.example.admin.module.role.mapper.SysRoleMapper;
import com.example.admin.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据范围计算：根据当前用户所有角色的 data_scope，推导其可见的部门集合。
 *
 * <p>数据范围取值：1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人。
 * 多角色取并集；任一角色为"全部"即不受限。超级管理员恒为"全部"。
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final SysRoleMapper sysRoleMapper;
    private final SysDeptService sysDeptService;

    public DeptDataScope compute(LoginUser loginUser) {
        if (loginUser.isSuperAdmin()) {
            return DeptDataScope.unrestricted();
        }

        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(loginUser.getUserId());
        Set<Long> deptIds = new HashSet<>();
        boolean selfOnly = false;

        for (SysRole role : roles) {
            Integer scope = role.getDataScope() == null ? 1 : role.getDataScope();
            switch (scope) {
                case 1 -> {
                    return DeptDataScope.unrestricted();
                }
                case 2 -> deptIds.addAll(sysRoleMapper.selectDeptIdsByRoleId(role.getId()));
                case 3 -> {
                    if (loginUser.getDeptId() != null) {
                        deptIds.add(loginUser.getDeptId());
                    }
                }
                case 4 -> {
                    if (loginUser.getDeptId() != null) {
                        deptIds.addAll(sysDeptService.listSelfAndDescendantIds(loginUser.getDeptId()));
                    }
                }
                case 5 -> selfOnly = true;
                default -> {
                    // 未知范围当作最严格：仅本人
                    selfOnly = true;
                }
            }
        }
        return new DeptDataScope(false, deptIds, selfOnly);
    }

    /**
     * 数据范围结果。
     *
     * @param all      不受限（全部数据）
     * @param deptIds  可见部门 id 集合
     * @param selfOnly 是否包含"仅本人"
     */
    public record DeptDataScope(boolean all, Set<Long> deptIds, boolean selfOnly) {

        static DeptDataScope unrestricted() {
            return new DeptDataScope(true, Set.of(), false);
        }
    }
}
