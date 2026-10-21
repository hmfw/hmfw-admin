package com.example.admin.module.menu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.menu.dto.SysMenuRequest;
import com.example.admin.module.menu.dto.SysMenuTreeVO;
import com.example.admin.module.menu.dto.SysMenuVO;
import com.example.admin.module.menu.entity.SysMenu;
import com.example.admin.module.menu.mapper.SysMenuMapper;
import com.example.admin.security.LoginUser;
import com.example.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    /** 菜单类型：目录 / 菜单 / 按钮 */
    private static final String TYPE_DIR = "M";
    private static final String TYPE_MENU = "C";

    private final SysMenuMapper sysMenuMapper;

    /** 菜单树，可按名称/状态过滤（过滤后仍返回树形结构） */
    public List<SysMenuTreeVO> tree(String menuName, Integer status) {
        LambdaQueryWrapper<SysMenu> wrapper = Wrappers.<SysMenu>lambdaQuery()
                .like(StringUtils.hasText(menuName), SysMenu::getMenuName, menuName)
                .eq(status != null, SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort);

        return SysMenuTreeVO.build(sysMenuMapper.selectList(wrapper).stream()
                .map(this::toTreeVO)
                .toList());
    }

    public SysMenuVO getDetail(Long id) {
        return toVO(requireMenu(id));
    }

    public Long create(SysMenuRequest request) {
        requireParentExists(request.getParentId());

        SysMenu menu = new SysMenu();
        applyRequest(menu, request);
        sysMenuMapper.insert(menu);
        return menu.getId();
    }

    public void update(Long id, SysMenuRequest request) {
        SysMenu menu = requireMenu(id);
        requireParentExists(request.getParentId());
        requireNoCycle(id, request.getParentId());

        applyRequest(menu, request);
        sysMenuMapper.updateById(menu);
    }

    public void delete(Long id) {
        requireMenu(id);

        if (sysMenuMapper.countChildren(id) > 0) {
            throw new BizException(ResultCode.MENU_HAS_CHILDREN);
        }
        if (sysMenuMapper.countRoleByMenuId(id) > 0) {
            throw new BizException(ResultCode.MENU_IN_USE);
        }
        sysMenuMapper.deleteById(id);
    }

    /** 当前登录用户可见的目录与菜单，供前端生成动态路由 */
    public List<SysMenuTreeVO> menusOfCurrentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();

        // 超级管理员看全量，其余按角色关联取
        List<SysMenu> menus = loginUser.isSuperAdmin()
                ? sysMenuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                        .in(SysMenu::getMenuType, TYPE_DIR, TYPE_MENU)
                        .eq(SysMenu::getStatus, 1)
                        .eq(SysMenu::getVisible, 1)
                        .orderByAsc(SysMenu::getParentId)
                        .orderByAsc(SysMenu::getSort))
                : sysMenuMapper.selectMenusByUserId(loginUser.getUserId());

        return SysMenuTreeVO.build(menus.stream().map(this::toTreeVO).toList());
    }

    private void applyRequest(SysMenu menu, SysMenuRequest request) {
        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setMenuType(request.getMenuType());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setSort(request.getSort() == null ? 0 : request.getSort());
        menu.setVisible(request.getVisible() == null ? 1 : request.getVisible());
        menu.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private void requireParentExists(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;   // 0 表示根节点
        }
        if (sysMenuMapper.selectById(parentId) == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "父菜单不存在");
        }
    }

    /** 防止把自己挂到自己的子孙下面，形成环 */
    private void requireNoCycle(Long id, Long newParentId) {
        if (newParentId == null || newParentId == 0L) {
            return;
        }
        if (newParentId.equals(id)) {
            throw new BizException(ResultCode.BAD_REQUEST, "父菜单不能是自己");
        }
        Long cursor = newParentId;
        int guard = 0;
        while (cursor != null && cursor != 0L && guard++ < 100) {
            if (cursor.equals(id)) {
                throw new BizException(ResultCode.BAD_REQUEST, "父菜单不能是自己的子节点");
            }
            SysMenu parent = sysMenuMapper.selectById(cursor);
            cursor = parent == null ? null : parent.getParentId();
        }
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return menu;
    }

    private SysMenuVO toVO(SysMenu menu) {
        SysMenuVO vo = new SysMenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPerms(menu.getPerms());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setCreateTime(menu.getCreateTime());
        return vo;
    }

    private SysMenuTreeVO toTreeVO(SysMenu menu) {
        SysMenuTreeVO vo = new SysMenuTreeVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPerms(menu.getPerms());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        return vo;
    }
}
