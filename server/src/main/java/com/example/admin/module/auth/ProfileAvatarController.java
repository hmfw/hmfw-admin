package com.example.admin.module.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "个人头像资源")
@RestController
@RequestMapping("/api/profile-avatars")
@RequiredArgsConstructor
public class ProfileAvatarController {

    private final ProfileAvatarService profileAvatarService;

    @Operation(summary = "读取头像资源", description = "使用不可预测资源键公开读取规范化 PNG 头像")
    @GetMapping("/{key}")
    public ResponseEntity<Resource> avatar(@PathVariable String key) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(profileAvatarService.load(key));
    }
}
