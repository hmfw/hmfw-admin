package com.example.admin.module.auth;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.user.entity.SysUser;
import com.example.admin.module.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileAvatarServiceTest {

    @TempDir
    Path uploadRoot;

    private SysUserMapper userMapper;
    private ProfileAvatarService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        service = new ProfileAvatarService(userMapper, uploadRoot.toString());
    }

    @Test
    void rejectsFilesLargerThanFiveMebibytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.upload(1L, file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("5 MiB");
    }

    @Test
    void rejectsContentThatOnlyClaimsToBePng() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> service.upload(1L, file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("JPEG 或 PNG");
    }

    @Test
    void rejectsImagesWhosePixelDimensionsAreTooLarge() throws Exception {
        MockMultipartFile file = imageFile("wide.png", "png", 4097, 1);

        assertThatThrownBy(() -> service.upload(1L, file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("4096");
    }

    @Test
    void normalizesAcceptedJpegToSquarePngAndUpdatesUser() throws Exception {
        SysUser user = user(1L, null);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.update(isNull(), any())).thenReturn(1);

        String key = service.upload(1L, imageFile("portrait.jpg", "jpeg", 320, 640));

        assertThat(key).matches("[0-9a-f]{32}\\.png");
        Path stored = uploadRoot.resolve("avatars").resolve(key);
        assertThat(stored).exists();
        BufferedImage normalized = ImageIO.read(stored.toFile());
        assertThat(normalized.getWidth()).isEqualTo(512);
        assertThat(normalized.getHeight()).isEqualTo(512);
        assertThat(Files.readAllBytes(stored)).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
        assertThat(user.getAvatar()).isEqualTo(key);
    }

    @Test
    void successfulReplacementDeletesPreviousAvatar() throws Exception {
        String oldKey = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png";
        Path oldFile = createStoredAvatar(oldKey);
        SysUser user = user(1L, oldKey);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.update(isNull(), any())).thenReturn(1);

        String newKey = service.upload(1L, imageFile("new.png", "png", 512, 512));

        assertThat(newKey).isNotEqualTo(oldKey);
        assertThat(oldFile).doesNotExist();
        assertThat(uploadRoot.resolve("avatars").resolve(newKey)).exists();
    }

    @Test
    void databaseFailureRemovesNewFileAndPreservesPreviousAvatar() throws Exception {
        String oldKey = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png";
        Path oldFile = createStoredAvatar(oldKey);
        SysUser user = user(1L, oldKey);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.update(isNull(), any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.upload(1L, imageFile("new.png", "png", 512, 512)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");

        assertThat(oldFile).exists();
        assertThat(user.getAvatar()).isEqualTo(oldKey);
        try (var files = Files.list(uploadRoot.resolve("avatars"))) {
            assertThat(files.toList()).containsExactly(oldFile);
        }
    }

    @Test
    void missingUserDoesNotLeaveAnOrphanedAvatar() throws Exception {
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.upload(99L, imageFile("new.png", "png", 512, 512)))
                .isInstanceOf(BizException.class);

        Path avatarDirectory = uploadRoot.resolve("avatars");
        if (Files.exists(avatarDirectory)) {
            try (var files = Files.list(avatarDirectory)) {
                assertThat(files).isEmpty();
            }
        }
    }

    @Test
    void concurrentReplacementLosingCompareAndSetRemovesItsNewFile() throws Exception {
        String oldKey = "dddddddddddddddddddddddddddddddd.png";
        Path oldFile = createStoredAvatar(oldKey);
        SysUser user = user(1L, oldKey);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.upload(1L, imageFile("new.png", "png", 512, 512)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已发生变化");

        assertThat(oldFile).exists();
        try (var files = Files.list(uploadRoot.resolve("avatars"))) {
            assertThat(files.toList()).containsExactly(oldFile);
        }
    }

    @Test
    void removeClearsAssociationAndDeletesPreviousAvatar() throws Exception {
        String oldKey = "cccccccccccccccccccccccccccccccc.png";
        Path oldFile = createStoredAvatar(oldKey);
        SysUser user = user(1L, oldKey);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.update(isNull(), any())).thenReturn(1);

        service.remove(1L);

        assertThat(user.getAvatar()).isNull();
        assertThat(oldFile).doesNotExist();
        verify(userMapper).update(isNull(), any());
    }

    @Test
    void concurrentChangePreventsRemovalOfStaleAvatarFile() throws Exception {
        String oldKey = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee.png";
        Path oldFile = createStoredAvatar(oldKey);
        SysUser user = user(1L, oldKey);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.remove(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已发生变化");

        assertThat(oldFile).exists();
        assertThat(user.getAvatar()).isEqualTo(oldKey);
    }

    @Test
    void genericEntityUpdatesNeverWriteAvatarColumn() throws Exception {
        TableField field = SysUser.class.getDeclaredField("avatar").getAnnotation(TableField.class);

        assertThat(field).isNotNull();
        assertThat(field.updateStrategy()).isEqualTo(FieldStrategy.NEVER);
    }

    @Test
    void resourceKeysCannotEscapeAvatarDirectory() {
        assertThatThrownBy(() -> service.load("../secret.png"))
                .isInstanceOf(BizException.class);
    }

    private MockMultipartFile imageFile(String filename, String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height,
                "jpeg".equals(format) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(28, 112, 180));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        String contentType = "jpeg".equals(format) ? "image/jpeg" : "image/png";
        return new MockMultipartFile("file", filename, contentType, output.toByteArray());
    }

    private Path createStoredAvatar(String key) throws Exception {
        Path directory = Files.createDirectories(uploadRoot.resolve("avatars"));
        Path file = directory.resolve(key);
        Files.write(file, imageFile("old.png", "png", 10, 10).getBytes());
        return file;
    }

    private SysUser user(Long id, String avatar) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setAvatar(avatar);
        return user;
    }
}
