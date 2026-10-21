package com.example.admin.module.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.module.role.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 查询用户拥有的角色标识；SQL 见同目录 SysRoleMapper.xml */
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /** 统计该角色已分配给多少用户，用于删除前的占用检查；SQL 见同目录 SysRoleMapper.xml */
    long countUserByRoleId(@Param("roleId") Long roleId);

    /** 查询用户拥有的在用角色（含 data_scope），用于计算数据范围；SQL 见同目录 SysRoleMapper.xml */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /** 自定义数据范围：某角色勾选的部门 id；SQL 见同目录 SysRoleMapper.xml */
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /** 删除角色与部门的关联；SQL 见同目录 SysRoleMapper.xml */
    int deleteRoleDepts(@Param("roleId") Long roleId);

    /** deptIds 不可为空，由调用方保证；SQL 见同目录 SysRoleMapper.xml */
    int insertRoleDepts(@Param("roleId") Long roleId, @Param("deptIds") List<Long> deptIds);
}
