package com.example.admin.module.dept.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class SysDeptTreeVO {

    private Long id;
    private Long parentId;
    private String deptName;
    private String leader;
    private String phone;
    private String email;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;

    private List<SysDeptTreeVO> children = new ArrayList<>();

    /**
     * 扁平列表组装成树。父节点被过滤掉的节点会作为根返回，不丢失。
     */
    public static List<SysDeptTreeVO> build(List<SysDeptTreeVO> flat) {
        Map<Long, SysDeptTreeVO> byId = flat.stream()
                .collect(Collectors.toMap(SysDeptTreeVO::getId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        List<SysDeptTreeVO> roots = new ArrayList<>();
        for (SysDeptTreeVO node : byId.values()) {
            Long parentId = node.getParentId();
            SysDeptTreeVO parent = parentId == null ? null : byId.get(parentId);
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
