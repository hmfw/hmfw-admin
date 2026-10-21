package com.example.admin.module.dict.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.dict.dto.SysDictDataQuery;
import com.example.admin.module.dict.dto.SysDictDataRequest;
import com.example.admin.module.dict.entity.SysDictData;
import com.example.admin.module.dict.service.SysDictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "字典数据")
@RestController
@RequestMapping("/api/system/dict/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final SysDictDataService sysDictDataService;

    @Operation(summary = "分页查询字典数据")
    @PreAuthorize("@ps.has('system:dict:list')")
    @GetMapping("/list")
    public R<PageResult<SysDictData>> page(@Valid SysDictDataQuery query) {
        return R.ok(sysDictDataService.page(query));
    }

    @Operation(summary = "按类型取字典项", description = "登录用户可用，供前端渲染下拉/标签")
    @GetMapping("/type/{dictType}")
    public R<List<SysDictData>> listByType(@org.springframework.web.bind.annotation.PathVariable String dictType) {
        return R.ok(sysDictDataService.listByType(dictType));
    }

    @Operation(summary = "字典数据详情")
    @PreAuthorize("@ps.has('system:dict:query')")
    @GetMapping("/info")
    public R<SysDictData> detail(@RequestParam Long id) {
        return R.ok(sysDictDataService.getDetail(id));
    }

    @Operation(summary = "新增字典数据")
    @PreAuthorize("@ps.has('system:dict:add')")
    @Log(title = "字典数据", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysDictDataRequest request) {
        return R.ok(sysDictDataService.create(request));
    }

    @Operation(summary = "修改字典数据")
    @PreAuthorize("@ps.has('system:dict:edit')")
    @Log(title = "字典数据", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysDictDataRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        sysDictDataService.update(request.getId(), request);
        return R.ok();
    }

    @Operation(summary = "删除字典数据", description = "按 id 批量删除")
    @PreAuthorize("@ps.has('system:dict:remove')")
    @Log(title = "字典数据", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        sysDictDataService.delete(request.getIds());
        return R.ok();
    }
}
