package com.example.admin.module.dict.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.OptionVO;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.dict.dto.DictTypeSaveRequest;
import com.example.admin.module.dict.dto.SysDictTypeQuery;
import com.example.admin.module.dict.dto.SysDictTypeRequest;
import com.example.admin.module.dict.entity.SysDictType;
import com.example.admin.module.dict.service.SysDictTypeService;
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

@Tag(name = "字典类型")
@RestController
@RequestMapping("/api/system/dict/type")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final SysDictTypeService sysDictTypeService;

    @Operation(summary = "分页查询字典类型")
    @PreAuthorize("@ps.has('system:dict:list')")
    @GetMapping("/list")
    public R<PageResult<SysDictType>> page(@Valid SysDictTypeQuery query) {
        return R.ok(sysDictTypeService.page(query));
    }

    @Operation(summary = "字典类型字典", description = "启用类型的名称/标识，供下拉选择")
    @PreAuthorize("@ps.has('system:dict:list')")
    @GetMapping("/options")
    public R<List<OptionVO>> options() {
        return R.ok(sysDictTypeService.listOptions());
    }

    @Operation(summary = "保存字典（类型+字典项，原子）",
            description = "一个事务存类型与其全部字典项；id 为空为新增，否则修改。字典项按最终态增/改/删")
    @PreAuthorize("@ps.hasAny('system:dict:add', 'system:dict:edit')")
    @Log(title = "字典", businessType = BusinessType.UPDATE)
    @PostMapping("/save")
    public R<Long> save(@Valid @RequestBody DictTypeSaveRequest request) {
        return R.ok(sysDictTypeService.save(request));
    }

    @Operation(summary = "字典类型详情")
    @PreAuthorize("@ps.has('system:dict:query')")
    @GetMapping("/info")
    public R<SysDictType> detail(@RequestParam Long id) {
        return R.ok(sysDictTypeService.getDetail(id));
    }

    @Operation(summary = "新增字典类型")
    @PreAuthorize("@ps.has('system:dict:add')")
    @Log(title = "字典类型", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysDictTypeRequest request) {
        return R.ok(sysDictTypeService.create(request));
    }

    @Operation(summary = "修改字典类型", description = "改标识会级联更新字典项的 dictType")
    @PreAuthorize("@ps.has('system:dict:edit')")
    @Log(title = "字典类型", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysDictTypeRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        sysDictTypeService.update(request.getId(), request);
        return R.ok();
    }

    @Operation(summary = "删除字典类型", description = "连同该类型下的字典项一并删除")
    @PreAuthorize("@ps.has('system:dict:remove')")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        request.getIds().forEach(sysDictTypeService::delete);
        return R.ok();
    }
}
