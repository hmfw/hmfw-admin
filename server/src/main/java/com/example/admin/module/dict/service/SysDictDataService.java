package com.example.admin.module.dict.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.dict.dto.SysDictDataQuery;
import com.example.admin.module.dict.dto.SysDictDataRequest;
import com.example.admin.module.dict.entity.SysDictData;
import com.example.admin.module.dict.mapper.SysDictDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDictDataService {

    private final SysDictDataMapper sysDictDataMapper;

    public PageResult<SysDictData> page(SysDictDataQuery query) {
        Page<SysDictData> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDictData> wrapper = Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, query.getDictType())
                .like(StringUtils.hasText(query.getDictLabel()), SysDictData::getDictLabel, query.getDictLabel())
                .eq(query.getStatus() != null, SysDictData::getStatus, query.getStatus())
                .orderByAsc(SysDictData::getSort);
        sysDictDataMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    /** 某字典类型下的启用项，供前端按 type 取用（下拉、状态标签等） */
    public List<SysDictData> listByType(String dictType) {
        return sysDictDataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort));
    }

    public SysDictData getDetail(Long id) {
        return requireData(id);
    }

    public Long create(SysDictDataRequest request) {
        SysDictData data = new SysDictData();
        applyRequest(data, request);
        sysDictDataMapper.insert(data);
        return data.getId();
    }

    public void update(Long id, SysDictDataRequest request) {
        SysDictData data = requireData(id);
        applyRequest(data, request);
        sysDictDataMapper.updateById(data);
    }

    public void delete(List<Long> ids) {
        sysDictDataMapper.deleteByIds(ids);
    }

    private void applyRequest(SysDictData data, SysDictDataRequest request) {
        data.setDictType(request.getDictType());
        data.setDictLabel(request.getDictLabel());
        data.setDictValue(request.getDictValue());
        data.setSort(request.getSort() == null ? 0 : request.getSort());
        data.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        data.setRemark(request.getRemark());
    }

    private SysDictData requireData(Long id) {
        SysDictData data = sysDictDataMapper.selectById(id);
        if (data == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return data;
    }
}
