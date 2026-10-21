package com.example.admin.module.user.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.user.dto.AssignRolesRequest;
import com.example.admin.module.user.dto.ResetPasswordRequest;
import com.example.admin.module.user.dto.SysUserCreateRequest;
import com.example.admin.module.user.dto.SysUserQuery;
import com.example.admin.module.user.dto.SysUserUpdateRequest;
import com.example.admin.module.user.dto.SysUserVO;
import com.example.admin.module.user.dto.UpdateStatusRequest;
import com.example.admin.module.user.dto.UserImportResult;
import com.example.admin.module.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询用户")
    @PreAuthorize("@ps.has('system:user:list')")
    @GetMapping("/list")
    public R<PageResult<SysUserVO>> page(@Valid SysUserQuery query) {
        return R.ok(sysUserService.page(query));
    }

    @Operation(summary = "查询用户详情", description = "含已分配的角色 id")
    @PreAuthorize("@ps.has('system:user:query')")
    @GetMapping("/info")
    public R<SysUserVO> detail(@RequestParam Long id) {
        return R.ok(sysUserService.getDetail(id));
    }

    @Operation(summary = "新增用户")
    @PreAuthorize("@ps.has('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysUserCreateRequest request) {
        return R.ok(sysUserService.create(request));
    }

    @Operation(summary = "修改用户", description = "不改账号名与密码")
    @PreAuthorize("@ps.has('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysUserUpdateRequest request) {
        sysUserService.update(request.getId(), request);
        return R.ok();
    }

    @Operation(summary = "删除用户", description = "逻辑删除；内置管理员与当前登录账号不可删")
    @PreAuthorize("@ps.has('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        request.getIds().forEach(sysUserService::delete);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PreAuthorize("@ps.has('system:user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/resetPwd")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        sysUserService.resetPassword(request.getId(), request.getPassword());
        return R.ok();
    }

    @Operation(summary = "启用/停用")
    @PreAuthorize("@ps.has('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/changeStatus")
    public R<Void> updateStatus(@Valid @RequestBody UpdateStatusRequest request) {
        sysUserService.updateStatus(request.getId(), request.getStatus());
        return R.ok();
    }

    @Operation(summary = "分配角色", description = "传空数组表示清空该用户的角色")
    @PreAuthorize("@ps.has('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/assignRoles")
    public R<Void> assignRoles(@Valid @RequestBody AssignRolesRequest request) {
        sysUserService.assignRoles(request.getId(), request.getRoleIds());
        return R.ok();
    }

    @Operation(summary = "导入用户", description = "Excel 批量导入，默认密码 123456")
    @PreAuthorize("@ps.has('system:user:import')")
    @Log(title = "用户管理", businessType = BusinessType.IMPORT, saveRequestData = false)
    @PostMapping("/import")
    public R<UserImportResult> importUsers(@RequestParam("file") MultipartFile file) {
        return R.ok(sysUserService.importUsers(file));
    }

    @Operation(summary = "导出用户", description = "按当前查询条件导出 Excel")
    @PreAuthorize("@ps.has('system:user:export')")
    @Log(title = "用户管理", businessType = BusinessType.EXPORT, saveRequestData = false)
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(@Valid SysUserQuery query) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sysUserService.exportUsers(query, out);
        byte[] bytes = out.toByteArray();
        String filename = URLEncoder.encode("用户列表.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Operation(summary = "下载导入模板")
    @PreAuthorize("@ps.has('system:user:export')")
    @GetMapping("/importTemplate")
    public ResponseEntity<byte[]> importTemplate() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sysUserService.buildImportTemplate(out);
        byte[] bytes = out.toByteArray();
        String filename = URLEncoder.encode("用户导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
