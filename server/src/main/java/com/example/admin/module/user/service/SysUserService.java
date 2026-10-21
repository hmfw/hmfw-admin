package com.example.admin.module.user.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.dept.entity.SysDept;
import com.example.admin.module.dept.mapper.SysDeptMapper;
import com.example.admin.module.dept.service.SysDeptService;
import com.example.admin.module.role.entity.SysRole;
import com.example.admin.module.role.mapper.SysRoleMapper;
import com.example.admin.module.user.dto.SysUserCreateRequest;
import com.example.admin.module.user.dto.SysUserQuery;
import com.example.admin.module.user.dto.SysUserUpdateRequest;
import com.example.admin.module.user.dto.SysUserVO;
import com.example.admin.module.user.dto.UserExportRow;
import com.example.admin.module.user.dto.UserImportError;
import com.example.admin.module.user.dto.UserImportResult;
import com.example.admin.module.user.dto.UserImportRow;
import com.example.admin.module.user.dto.UserRoleNameRow;
import com.example.admin.module.user.entity.SysUser;
import com.example.admin.module.user.mapper.SysUserMapper;
import com.example.admin.module.user.support.DropdownValidationHandler;
import com.example.admin.security.SecurityUtils;
import com.example.admin.security.datascope.DataScopeService;
import com.example.admin.security.datascope.DataScopeService.DeptDataScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    /** 内置超级管理员，不允许删除或停用 */
    private static final Long BUILTIN_ADMIN_ID = 1L;

    /** 导入用户时使用的默认初始密码 */
    private static final String DEFAULT_INIT_PASSWORD = "123456";

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysDeptMapper sysDeptMapper;
    private final SysDeptService sysDeptService;
    private final SysRoleMapper sysRoleMapper;
    private final DataScopeService dataScopeService;

    public PageResult<SysUserVO> page(SysUserQuery query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());

        // 按部门过滤（含子部门）
        if (query.getDeptId() != null) {
            wrapper.in(SysUser::getDeptId, sysDeptService.listSelfAndDescendantIds(query.getDeptId()));
        }

        // 数据范围：非"全部"时按可见部门/本人收窄
        applyDataScope(wrapper);

        wrapper.orderByDesc(SysUser::getId);
        sysUserMapper.selectPage(page, wrapper);
        PageResult<SysUserVO> result = PageResult.of(page, this::toVO);
        fillDeptNames(result.getRecords());
        fillRoleNames(result.getRecords());
        return result;
    }

    /** 把当前用户的数据范围追加到查询条件 */
    private void applyDataScope(LambdaQueryWrapper<SysUser> wrapper) {
        DeptDataScope scope = dataScopeService.compute(SecurityUtils.getLoginUser());
        if (scope.all()) {
            return;
        }
        Long uid = SecurityUtils.getUserId();
        wrapper.and(w -> {
            boolean added = false;
            if (!scope.deptIds().isEmpty()) {
                w.in(SysUser::getDeptId, scope.deptIds());
                added = true;
            }
            if (scope.selfOnly()) {
                if (added) {
                    w.or();
                }
                w.eq(SysUser::getId, uid);
                added = true;
            }
            if (!added) {
                // 既无可见部门也非仅本人 → 看不到任何数据
                w.eq(SysUser::getId, -1L);
            }
        });
    }

    public SysUserVO getDetail(Long id) {
        SysUserVO vo = toVO(requireUser(id));
        vo.setRoleIds(sysUserMapper.selectRoleIdsByUserId(id));
        if (vo.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(vo.getDeptId());
            vo.setDeptName(dept == null ? null : dept.getDeptName());
        }
        return vo;
    }

    /** 批量回填部门名称，避免列表 N+1 */
    private void fillDeptNames(List<SysUserVO> records) {
        List<Long> deptIds = records.stream()
                .map(SysUserVO::getDeptId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameById = sysDeptMapper.selectByIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        records.forEach(r -> r.setDeptName(nameById.get(r.getDeptId())));
    }

    /** 按当前页用户批量回填有效角色名称，避免逐用户查询。 */
    private void fillRoleNames(List<SysUserVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(SysUserVO::getId).toList();
        Map<Long, List<String>> namesByUserId = sysUserMapper.selectRoleNamesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(
                        UserRoleNameRow::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(UserRoleNameRow::getRoleName, Collectors.toList())));
        records.forEach(r -> r.setRoleNames(namesByUserId.getOrDefault(r.getId(), List.of())));
    }

    @Transactional
    public Long create(SysUserCreateRequest request) {
        requireUsernameAvailable(request.getUsername());

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDeptId(request.getDeptId());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setRemark(request.getRemark());
        sysUserMapper.insert(user);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            sysUserMapper.insertUserRoles(user.getId(), request.getRoleIds());
        }
        return user.getId();
    }

    @Transactional
    public void update(Long id, SysUserUpdateRequest request) {
        SysUser user = requireUser(id);

        if (BUILTIN_ADMIN_ID.equals(id) && request.getStatus() != null && request.getStatus() == 0) {
            throw BizException.forbidden(ResultCode.FORBIDDEN, "内置管理员账号不允许停用");
        }
        if (BUILTIN_ADMIN_ID.equals(id) && request.getRoleIds() != null) {
            throw BizException.forbidden(ResultCode.FORBIDDEN, "内置管理员账号不允许修改角色");
        }

        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDeptId(request.getDeptId());
        user.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        sysUserMapper.updateById(user);

        // roleIds 为 null 表示不改角色；为空列表表示清空角色
        if (request.getRoleIds() != null) {
            replaceRoles(id, request.getRoleIds());
        }
    }

    @Transactional
    public void delete(Long id) {
        requireUser(id);

        if (BUILTIN_ADMIN_ID.equals(id)) {
            throw BizException.forbidden(ResultCode.FORBIDDEN, "内置管理员账号不允许删除");
        }
        if (id.equals(SecurityUtils.getUserId())) {
            throw BizException.forbidden(ResultCode.FORBIDDEN, "不允许删除当前登录账号");
        }

        sysUserMapper.deleteUserRoles(id);
        sysUserMapper.deleteById(id);   // 逻辑删除，deleted 置 1
    }

    public void resetPassword(Long id, String rawPassword) {
        SysUser user = requireUser(id);
        user.setPassword(passwordEncoder.encode(rawPassword));
        sysUserMapper.updateById(user);
    }

    /** 当前登录用户修改个人资料：仅昵称/邮箱/手机号 */
    public void updateProfile(Long userId, String nickname, String email, String phone) {
        SysUser user = requireUser(userId);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        sysUserMapper.updateById(user);
    }

    /** 当前登录用户修改自己的密码：先校验原密码 */
    public void changePassword(Long userId, String oldRaw, String newRaw) {
        SysUser user = requireUser(userId);
        if (!passwordEncoder.matches(oldRaw, user.getPassword())) {
            throw new BizException(ResultCode.OLD_PASSWORD_WRONG);
        }
        user.setPassword(passwordEncoder.encode(newRaw));
        sysUserMapper.updateById(user);
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = requireUser(id);
        if (BUILTIN_ADMIN_ID.equals(id) && status == 0) {
            throw BizException.forbidden(ResultCode.FORBIDDEN, "内置管理员账号不允许停用");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        if (BUILTIN_ADMIN_ID.equals(id)) {
            throw BizException.forbidden(ResultCode.FORBIDDEN, "内置管理员账号不允许修改角色");
        }
        replaceRoles(id, roleIds);
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        sysUserMapper.deleteUserRoles(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            sysUserMapper.insertUserRoles(userId, roleIds);
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return user;
    }

    private void requireUsernameAvailable(String username) {
        Long count = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
    }

    private SysUserVO toVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setDeptId(user.getDeptId());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    // ==================== Excel 导入导出 ====================

    /**
     * 导入用户：读取 Excel，逐行校验+创建，返回成功/失败统计。
     */
    public UserImportResult importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        List<UserImportRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(UserImportRow.class).sheet().doReadSync();
        } catch (IOException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件格式错误，请使用正确的 Excel 文件");
        }

        if (rows == null || rows.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件中没有数据");
        }

        // 预构建名称→ID 映射，避免逐行查询
        Map<String, Long> deptNameToId = sysDeptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getDeptName, SysDept::getId, (a, b) -> a));
        Map<String, Long> roleNameToId = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getRoleName, SysRole::getId, (a, b) -> a));

        UserImportResult result = new UserImportResult();
        result.setSuccessCount(0);
        result.setFailCount(0);
        result.setErrors(new ArrayList<>());

        for (int i = 0; i < rows.size(); i++) {
            UserImportRow row = rows.get(i);
            int rowNum = i + 2; // 第 1 行是表头，数据从 Excel 第 2 行开始
            try {
                if (!StringUtils.hasText(row.getUsername())) {
                    throw new BizException(ResultCode.BAD_REQUEST, "用户名不能为空");
                }

                // 用户名查重
                Long count = sysUserMapper.selectCount(
                        Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, row.getUsername()));
                if (count != null && count > 0) {
                    throw new BizException(ResultCode.USERNAME_EXISTS, "用户名已存在");
                }

                // 构建创建请求
                SysUserCreateRequest createRequest = new SysUserCreateRequest();
                createRequest.setUsername(row.getUsername());
                createRequest.setNickname(row.getNickname());
                createRequest.setPhone(row.getPhone());
                createRequest.setEmail(row.getEmail());
                createRequest.setPassword(DEFAULT_INIT_PASSWORD);

                // 解析部门
                if (StringUtils.hasText(row.getDeptName())) {
                    Long deptId = deptNameToId.get(row.getDeptName());
                    if (deptId == null) {
                        throw new BizException(ResultCode.BAD_REQUEST, "部门「" + row.getDeptName() + "」不存在");
                    }
                    createRequest.setDeptId(deptId);
                }

                // 解析状态
                if (StringUtils.hasText(row.getStatus())) {
                    createRequest.setStatus("停用".equals(row.getStatus()) ? 0 : 1);
                } else {
                    createRequest.setStatus(1);
                }

                // 解析角色
                if (StringUtils.hasText(row.getRoleName())) {
                    Long roleId = roleNameToId.get(row.getRoleName());
                    if (roleId == null) {
                        throw new BizException(ResultCode.BAD_REQUEST, "角色「" + row.getRoleName() + "」不存在");
                    }
                    createRequest.setRoleIds(List.of(roleId));
                }

                create(createRequest);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.setFailCount(result.getFailCount() + 1);
                String msg = e.getMessage() == null ? "未知错误" : e.getMessage();
                result.getErrors().add(new UserImportError(rowNum, row.getUsername(), msg));
                log.warn("导入用户失败: row={}, msg={}", rowNum, msg);
            }
        }

        return result;
    }

    /**
     * 导出用户：按查询条件查全量，写入 Excel。
     */
    public void exportUsers(SysUserQuery query, OutputStream out) {
        // 查询全量（不分页），应用数据范围
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());

        if (query.getDeptId() != null) {
            wrapper.in(SysUser::getDeptId, sysDeptService.listSelfAndDescendantIds(query.getDeptId()));
        }
        applyDataScope(wrapper);
        wrapper.orderByDesc(SysUser::getId);

        List<SysUser> users = sysUserMapper.selectList(wrapper);

        // 批量回填部门名称
        List<Long> deptIds = users.stream()
                .map(SysUser::getDeptId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Map.of() :
                sysDeptMapper.selectByIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));

        List<UserExportRow> exportRows = users.stream().map(u -> {
            UserExportRow row = new UserExportRow();
            row.setUsername(u.getUsername());
            row.setNickname(u.getNickname());
            row.setDeptName(u.getDeptId() == null ? null : deptNameMap.get(u.getDeptId()));
            row.setPhone(u.getPhone());
            row.setEmail(u.getEmail());
            row.setStatus(u.getStatus() == 1 ? "正常" : "停用");
            row.setCreateTime(u.getCreateTime() == null ? "" : u.getCreateTime().format(DATE_TIME_FORMAT));
            return row;
        }).toList();

        EasyExcel.write(out, UserExportRow.class).sheet("用户列表").doWrite(exportRows);
    }

    /**
     * 生成导入模板：header + 一行示例数据，部门/状态/角色列带下拉验证。
     */
    public void buildImportTemplate(OutputStream out) {
        UserImportRow example = new UserImportRow();
        example.setUsername("zhangsan");
        example.setNickname("张三");
        example.setDeptName("研发部");
        example.setPhone("13800000003");
        example.setEmail("z*******n@example.com");
        example.setStatus("正常");
        example.setRoleName("只读用户");

        // 查询所有启用的部门和角色名，构建下拉选项
        List<SysDept> depts = sysDeptMapper.selectList(Wrappers.lambdaQuery(SysDept.class)
                .eq(SysDept::getStatus, 1));
        List<String> deptNames = depts.stream().map(SysDept::getDeptName).distinct().toList();

        List<SysRole> roles = sysRoleMapper.selectList(Wrappers.lambdaQuery(SysRole.class)
                .eq(SysRole::getStatus, 1));
        List<String> roleNames = roles.stream().map(SysRole::getRoleName).toList();

        // 列索引：部门=2, 状态=5, 角色=6（对应 UserImportRow 字段顺序）
        Map<Integer, List<String>> dropdowns = new LinkedHashMap<>();
        dropdowns.put(2, deptNames);
        dropdowns.put(5, List.of("正常", "停用"));
        dropdowns.put(6, roleNames);

        EasyExcel.write(out, UserImportRow.class)
                .sheet("用户导入模板")
                .registerWriteHandler(new DropdownValidationHandler(dropdowns, 100))
                .doWrite(List.of(example));
    }
}
