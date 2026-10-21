package com.example.admin.module.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.module.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 查询用户拥有的权限标识；SQL 见同目录 SysMenuMapper.xml */
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 查询用户可见的目录与菜单；SQL 见同目录 SysMenuMapper.xml */
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /** 根据角色ID查询菜单ID列表；SQL 见同目录 SysMenuMapper.xml */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 删除角色与菜单的关联；SQL 见同目录 SysMenuMapper.xml */
    int deleteRoleMenus(@Param("roleId") Long roleId);

    /** menuIds 不可为空，由调用方保证；SQL 见同目录 SysMenuMapper.xml */
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /** 该菜单是否已被角色引用；SQL 见同目录 SysMenuMapper.xml */
    long countRoleByMenuId(@Param("menuId") Long menuId);

    /** 该菜单是否有子节点；SQL 见同目录 SysMenuMapper.xml */
    long countChildren(@Param("menuId") Long menuId);
}
