package com.example.admin.module.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.module.dept.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /** 统计某部门的直接子部门数（含逻辑删除过滤）；SQL 见同目录 SysDeptMapper.xml */
    long countChildren(@Param("parentId") Long parentId);

    /** 统计挂在某部门下的在用用户数（含逻辑删除过滤）；SQL 见同目录 SysDeptMapper.xml */
    long countUsers(@Param("deptId") Long deptId);
}
