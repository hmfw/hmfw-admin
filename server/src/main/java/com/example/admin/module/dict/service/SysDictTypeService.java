package com.example.admin.module.dict.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.OptionVO;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.dict.dto.DictTypeSaveRequest;
import com.example.admin.module.dict.dto.SysDictTypeQuery;
import com.example.admin.module.dict.dto.SysDictTypeRequest;
import com.example.admin.module.dict.entity.SysDictData;
import com.example.admin.module.dict.entity.SysDictType;
import com.example.admin.module.dict.mapper.SysDictDataMapper;
import com.example.admin.module.dict.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysDictTypeService {

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictDataMapper sysDictDataMapper;

    public PageResult<SysDictType> page(SysDictTypeQuery query) {
        Page<SysDictType> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDictType> wrapper = Wrappers.<SysDictType>lambdaQuery()
                .like(StringUtils.hasText(query.getDictName()), SysDictType::getDictName, query.getDictName())
                .like(StringUtils.hasText(query.getDictType()), SysDictType::getDictType, query.getDictType())
                .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus())
                .orderByDesc(SysDictType::getId);
        sysDictTypeMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    /** 启用字典类型的字典项（label=名称，value=type 字符串），供下拉选择 */
    public List<OptionVO> listOptions() {
        return sysDictTypeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                        .eq(SysDictType::getStatus, 1)
                        .orderByAsc(SysDictType::getId))
                .stream()
                .map(t -> OptionVO.of(t.getDictName(), t.getDictType()))
                .toList();
    }

    public SysDictType getDetail(Long id) {
        return requireType(id);
    }

    /**
     * 一体化保存：类型（新增/修改）+ 其字典项（增/改/删）在同一事务内完成。
     * 字典项以请求携带的列表为最终态——有 id 的更新，无 id 的新增，原有但本次未出现的删除。
     *
     * @return 字典类型 id
     */
    @Transactional
    public Long save(DictTypeSaveRequest request) {
        SysDictTypeRequest typeRequest = new SysDictTypeRequest();
        typeRequest.setId(request.getId());
        typeRequest.setDictName(request.getDictName());
        typeRequest.setDictType(request.getDictType());
        typeRequest.setStatus(request.getStatus());
        typeRequest.setRemark(request.getRemark());

        Long typeId;
        if (request.getId() == null) {
            typeId = create(typeRequest);
        } else {
            typeId = request.getId();
            update(typeId, typeRequest); // 含唯一校验与改标识时的字典项级联改名
        }

        reconcileData(request.getDictType(), request.getDataList());
        return typeId;
    }

    /** 把某类型下的字典项对齐到目标列表：有 id 的更新、无 id 的新增、原有但不在列表的删除 */
    private void reconcileData(String dictType, List<DictTypeSaveRequest.Item> items) {
        List<SysDictData> existing = sysDictDataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType));

        Set<Long> keepIds = new HashSet<>();
        for (DictTypeSaveRequest.Item item : items) {
            if (item.getId() != null) {
                SysDictData data = sysDictDataMapper.selectById(item.getId());
                if (data == null || !dictType.equals(data.getDictType())) {
                    throw new BizException(ResultCode.DATA_NOT_FOUND);
                }
                applyItem(data, item, dictType);
                sysDictDataMapper.updateById(data);
                keepIds.add(data.getId());
            } else {
                SysDictData data = new SysDictData();
                applyItem(data, item, dictType);
                sysDictDataMapper.insert(data);
                keepIds.add(data.getId());
            }
        }

        List<Long> toDelete = existing.stream()
                .map(SysDictData::getId)
                .filter(id -> !keepIds.contains(id))
                .toList();
        if (!toDelete.isEmpty()) {
            sysDictDataMapper.deleteByIds(toDelete);
        }
    }

    private void applyItem(SysDictData data, DictTypeSaveRequest.Item item, String dictType) {
        data.setDictType(dictType);
        data.setDictLabel(item.getDictLabel());
        data.setDictValue(item.getDictValue());
        data.setSort(item.getSort() == null ? 0 : item.getSort());
        data.setStatus(item.getStatus() == null ? 1 : item.getStatus());
        data.setRemark(item.getRemark());
    }

    public Long create(SysDictTypeRequest request) {
        requireTypeAvailable(request.getDictType(), null);
        SysDictType type = new SysDictType();
        type.setDictName(request.getDictName());
        type.setDictType(request.getDictType());
        type.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        type.setRemark(request.getRemark());
        sysDictTypeMapper.insert(type);
        return type.getId();
    }

    @Transactional
    public void update(Long id, SysDictTypeRequest request) {
        SysDictType type = requireType(id);
        requireTypeAvailable(request.getDictType(), id);

        String oldType = type.getDictType();
        type.setDictName(request.getDictName());
        type.setDictType(request.getDictType());
        if (request.getStatus() != null) {
            type.setStatus(request.getStatus());
        }
        type.setRemark(request.getRemark());
        sysDictTypeMapper.updateById(type);

        // 类型标识改名时，级联改字典项的 dict_type，避免数据对不上
        if (!oldType.equals(request.getDictType())) {
            SysDictData patch = new SysDictData();
            patch.setDictType(request.getDictType());
            sysDictDataMapper.update(patch, Wrappers.<SysDictData>lambdaUpdate()
                    .eq(SysDictData::getDictType, oldType));
        }
    }

    @Transactional
    public void delete(Long id) {
        SysDictType type = requireType(id);
        // 连同该类型下的字典项一并逻辑删除
        sysDictDataMapper.delete(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, type.getDictType()));
        sysDictTypeMapper.deleteById(id);
    }

    private SysDictType requireType(Long id) {
        SysDictType type = sysDictTypeMapper.selectById(id);
        if (type == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return type;
    }

    private void requireTypeAvailable(String dictType, Long excludeId) {
        Long count = sysDictTypeMapper.selectCount(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getDictType, dictType)
                .ne(excludeId != null, SysDictType::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "字典类型已存在");
        }
    }
}
