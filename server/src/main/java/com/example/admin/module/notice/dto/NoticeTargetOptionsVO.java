package com.example.admin.module.notice.dto;

import com.example.admin.common.api.OptionVO;
import lombok.Data;

import java.util.List;

@Data
public class NoticeTargetOptionsVO {
    private List<OptionVO> roles;
    private List<OptionVO> departments;
    private List<OptionVO> users;
}
