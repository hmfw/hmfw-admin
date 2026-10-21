package com.example.admin.module.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.file.dto.SysFileQuery;
import com.example.admin.module.file.dto.SysFileVO;
import com.example.admin.module.file.entity.SysFile;
import com.example.admin.module.file.mapper.SysFileMapper;
import com.example.admin.security.LoginUser;
import com.example.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SysFileMapper sysFileMapper;

    @Value("${admin.upload.path}")
    private String uploadPath;

    /**
     * 上传文件：按日期分目录存盘，生成 uuid 文件名，落库。
     */
    public SysFileVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件名不能为空");
        }

        // 提取扩展名
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalName.substring(dotIndex).toLowerCase();
        }

        // 生成存储名：uuid + ext
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 日期分目录
        String dateDir = LocalDate.now().format(DATE_DIR);
        Path dirPath = Paths.get(uploadPath, dateDir);

        try {
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(storedName);
            file.transferTo(filePath.toAbsolutePath().toFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "文件保存失败: " + e.getMessage());
        }

        // 相对路径（相对于 upload.path）
        String relativePath = dateDir + "/" + storedName;

        // 落库
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysFile sysFile = new SysFile();
        sysFile.setOriginalName(originalName);
        sysFile.setStoredName(storedName);
        sysFile.setUrl("/upload/" + relativePath);
        sysFile.setFilePath(relativePath);
        sysFile.setSize(file.getSize());
        sysFile.setMimeType(file.getContentType());
        sysFile.setUploaderId(loginUser.getUserId());
        sysFile.setUploaderName(loginUser.getNickname());
        sysFile.setUploadTime(LocalDateTime.now());
        sysFileMapper.insert(sysFile);

        return toVO(sysFile);
    }

    public PageResult<SysFileVO> page(SysFileQuery query) {
        Page<SysFile> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysFile> wrapper = Wrappers.<SysFile>lambdaQuery()
                .like(StringUtils.hasText(query.getOriginalName()), SysFile::getOriginalName, query.getOriginalName());
        if (StringUtils.hasText(query.getMimeType())) {
            if (query.getMimeType().endsWith("/")) {
                wrapper.likeRight(SysFile::getMimeType, query.getMimeType());
            } else {
                wrapper.eq(SysFile::getMimeType, query.getMimeType());
            }
        }
        wrapper.orderByDesc(SysFile::getUploadTime);
        sysFileMapper.selectPage(page, wrapper);
        return PageResult.of(page, this::toVO);
    }

    /**
     * 删除文件：逻辑删除 + 物理删除磁盘文件。
     */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysFile file = sysFileMapper.selectById(id);
            if (file == null) {
                continue;
            }
            // 逻辑删除
            sysFileMapper.deleteById(id);
            // 物理删除磁盘文件
            try {
                Path fullPath = Paths.get(uploadPath, file.getFilePath());
                Files.deleteIfExists(fullPath);
            } catch (IOException e) {
                log.warn("文件磁盘删除失败: {}", file.getFilePath(), e);
            }
        }
    }

    /**
     * 加载文件资源，供下载用。
     */
    public Resource loadResource(Long id) {
        SysFile file = sysFileMapper.selectById(id);
        if (file == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        Path fullPath = Paths.get(uploadPath, file.getFilePath());
        if (!Files.exists(fullPath)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "文件不存在");
        }
        return new FileSystemResource(fullPath.toFile());
    }

    /**
     * 获取文件信息（VO），供下载时设置 Content-Disposition。
     */
    public SysFileVO getById(Long id) {
        SysFile file = sysFileMapper.selectById(id);
        if (file == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return toVO(file);
    }

    private SysFileVO toVO(SysFile file) {
        SysFileVO vo = new SysFileVO();
        vo.setId(file.getId());
        vo.setOriginalName(file.getOriginalName());
        vo.setUrl(file.getUrl());
        vo.setSize(file.getSize());
        vo.setMimeType(file.getMimeType());
        vo.setUploaderName(file.getUploaderName());
        vo.setUploadTime(file.getUploadTime());
        return vo;
    }
}
