package com.example.admin.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class AvatarVO {

    private String avatarUrl;
}
