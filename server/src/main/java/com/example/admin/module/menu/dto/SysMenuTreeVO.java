package com.example.admin.module.menu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class SysMenuTreeVO {

    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;

    private List<SysMenuTreeVO> children = new ArrayList<>();

    /**
     * 由扁平列表组装成树。
     *
     * <p>若某个节点的父节点不在列表里（典型场景：按名称过滤后父节点被筛掉），
     * 该节点会被当作根节点返回，而不是凭空消失。
     */
    public static List<SysMenuTreeVO> build(List<SysMenuTreeVO> flat) {
        Map<Long, SysMenuTreeVO> byId = flat.stream()
                .collect(Collectors.toMap(SysMenuTreeVO::getId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        List<SysMenuTreeVO> roots = new ArrayList<>();
        for (SysMenuTreeVO node : byId.values()) {
            Long parentId = node.getParentId();
            SysMenuTreeVO parent = parentId == null ? null : byId.get(parentId);
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
