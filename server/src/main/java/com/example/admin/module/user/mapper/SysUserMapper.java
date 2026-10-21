package com.example.admin.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.module.user.dto.UserRoleNameRow;
import com.example.admin.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 查询用户已分配的角色 id；SQL 见同目录 SysUserMapper.xml */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 批量查询用户当前有效的角色名称，用于列表回填。 */
    List<UserRoleNameRow> selectRoleNamesByUserIds(@Param("userIds") List<Long> userIds);

    /** 删除用户与角色的关联；SQL 见同目录 SysUserMapper.xml */
    int deleteUserRoles(@Param("userId") Long userId);

    /** roleIds 不可为空，由调用方保证；SQL 见同目录 SysUserMapper.xml */
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
