package com.example.admin.module.auth;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.user.entity.SysUser;
import com.example.admin.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProfileAvatarService {

    private static final String AVATAR_URL_PREFIX = "/api/profile-avatars/";
    static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    static final int MAX_DIMENSION = 4096;
    static final int AVATAR_SIZE = 512;
    private static final Pattern RESOURCE_KEY = Pattern.compile("[0-9a-f]{32}\\.png");

    private final SysUserMapper userMapper;
    private final Path avatarDirectory;

    public ProfileAvatarService(SysUserMapper userMapper,
                                @Value("${admin.upload.path}") String uploadPath) {
        this.userMapper = userMapper;
        this.avatarDirectory = Path.of(uploadPath).toAbsolutePath().normalize().resolve("avatars");
    }

    @Transactional
    public String upload(Long userId, MultipartFile file) {
        BufferedImage source = readAndValidate(file);
        SysUser user = requireUser(userId);
        String newKey = UUID.randomUUID().toString().replace("-", "") + ".png";
        Path newFile = storeNormalized(source, newKey);
        String oldKey = user.getAvatar();
        try {
            UpdateWrapper<SysUser> update = avatarUpdate(userId, oldKey)
                    .set("avatar", newKey);
            if (userMapper.update(null, update) != 1) {
                throw avatarChanged();
            }
            user.setAvatar(newKey);
            arrangeCleanupAfterUpdate(oldKey, newKey);
            return newKey;
        } catch (RuntimeException e) {
            deleteQuietly(newFile, "回滚新头像失败");
            throw e;
        }
    }

    @Transactional
    public void remove(Long userId) {
        SysUser user = requireUser(userId);
        String oldKey = user.getAvatar();
        if (oldKey == null || oldKey.isBlank()) {
            return;
        }
        int updated = userMapper.update(null, avatarUpdate(userId, oldKey)
                .set("avatar", null));
        if (updated != 1) {
            throw avatarChanged();
        }
        user.setAvatar(null);
        deleteAfterCommit(oldKey);
    }

    public Resource load(String key) {
        Path file = resolveKey(key);
        if (!Files.isRegularFile(file)) {
            throw new BizException(ResultCode.NOT_FOUND, "头像资源不存在", HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(file);
    }

    public String urlFor(String key) {
        return key == null || key.isBlank() ? null : AVATAR_URL_PREFIX + key;
    }

    private BufferedImage readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择头像图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST, "头像图片不能超过 5 MiB");
        }

        try (InputStream stream = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(stream)) {
            if (imageInput == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
                    throw invalidImage();
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new BizException(ResultCode.BAD_REQUEST, "头像图片宽高不能超过 4096 像素");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw invalidImage();
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            throw invalidImage();
        }
    }

    private Path storeNormalized(BufferedImage source, String key) {
        try {
            Files.createDirectories(avatarDirectory);
            Path destination = resolveKey(key);
            Path temporary = Files.createTempFile(avatarDirectory, ".avatar-", ".tmp");
            try {
                BufferedImage normalized = normalize(source);
                if (!ImageIO.write(normalized, "png", temporary.toFile())) {
                    throw new IOException("PNG writer unavailable");
                }
                moveAtomically(temporary, destination);
                return destination;
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            log.error("头像文件保存失败", e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "头像文件保存失败");
        }
    }

    private BufferedImage normalize(BufferedImage source) {
        double scale = Math.max((double) AVATAR_SIZE / source.getWidth(),
                (double) AVATAR_SIZE / source.getHeight());
        int width = (int) Math.ceil(source.getWidth() * scale);
        int height = (int) Math.ceil(source.getHeight() * scale);
        int x = (AVATAR_SIZE - width) / 2;
        int y = (AVATAR_SIZE - height) / 2;

        BufferedImage result = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, x, y, width, height, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination);
        }
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return user;
    }

    private UpdateWrapper<SysUser> avatarUpdate(Long userId, String expectedKey) {
        UpdateWrapper<SysUser> update = new UpdateWrapper<SysUser>().eq("id", userId);
        return expectedKey == null ? update.isNull("avatar") : update.eq("avatar", expectedKey);
    }

    private BizException avatarChanged() {
        return new BizException(ResultCode.BAD_REQUEST, "头像状态已发生变化，请重试");
    }

    private void arrangeCleanupAfterUpdate(String oldKey, String newKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteKeyQuietly(oldKey, "旧头像删除失败");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteKeyQuietly(oldKey, "旧头像删除失败");
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteKeyQuietly(newKey, "回滚新头像失败");
                }
            }
        });
    }

    private void deleteAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteKeyQuietly(key, "头像删除失败");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteKeyQuietly(key, "头像删除失败");
            }
        });
    }

    private Path resolveKey(String key) {
        if (key == null || !RESOURCE_KEY.matcher(key).matches()) {
            throw new BizException(ResultCode.BAD_REQUEST, "头像资源键不合法", HttpStatus.BAD_REQUEST);
        }
        Path resolved = avatarDirectory.resolve(key).normalize();
        if (!resolved.startsWith(avatarDirectory)) {
            throw new BizException(ResultCode.BAD_REQUEST, "头像资源键不合法", HttpStatus.BAD_REQUEST);
        }
        return resolved;
    }

    private void deleteKeyQuietly(String key, String message) {
        if (key == null || !RESOURCE_KEY.matcher(key).matches()) {
            return;
        }
        deleteQuietly(avatarDirectory.resolve(key).normalize(), message);
    }

    private void deleteQuietly(Path file, String message) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("{}: {}", message, file, e);
        }
    }

    private BizException invalidImage() {
        return new BizException(ResultCode.BAD_REQUEST, "头像必须是有效的 JPEG 或 PNG 图片");
    }
}
