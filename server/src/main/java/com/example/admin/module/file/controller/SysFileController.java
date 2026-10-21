package com.example.admin.module.file.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.file.dto.SysFileQuery;
import com.example.admin.module.file.dto.SysFileVO;
import com.example.admin.module.file.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/system/file")
@RequiredArgsConstructor
public class SysFileController {

    private final SysFileService sysFileService;

    @Operation(summary = "上传文件")
    @PreAuthorize("@ps.has('system:file:add')")
    @Log(title = "文件管理", businessType = BusinessType.INSERT, saveRequestData = false)
    @PostMapping("/upload")
    public R<SysFileVO> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(sysFileService.upload(file));
    }

    @Operation(summary = "分页查询文件")
    @PreAuthorize("@ps.has('system:file:list')")
    @GetMapping("/list")
    public R<PageResult<SysFileVO>> list(@Valid SysFileQuery query) {
        return R.ok(sysFileService.page(query));
    }

    @Operation(summary = "删除文件", description = "逻辑删除 + 物理删除磁盘文件")
    @PreAuthorize("@ps.has('system:file:remove')")
    @Log(title = "文件管理", businessType = BusinessType.DELETE, saveRequestData = false)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        sysFileService.delete(request.getIds());
        return R.ok();
    }

    @Operation(summary = "下载文件")
    @PreAuthorize("@ps.has('system:file:query')")
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam Long id) {
        SysFileVO fileInfo = sysFileService.getById(id);
        Resource resource = sysFileService.loadResource(id);
        String encodedName = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
