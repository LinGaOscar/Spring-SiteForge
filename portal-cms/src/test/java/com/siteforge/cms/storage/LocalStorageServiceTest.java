package com.siteforge.cms.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService();
        ReflectionTestUtils.setField(storageService, "uploadDir", tempDir.toString());
    }

    @Test
    void store_validPng_savesFileAndReturnsCorrectResult() {
        byte[] content = "fake-image-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "logo.png", "image/png", content);

        StorageResult result = storageService.store(file);

        assertThat(result.originalFilename()).isEqualTo("logo.png");
        assertThat(result.filePath()).startsWith("/uploads/");
        assertThat(result.filePath()).endsWith(".png");
        assertThat(result.mimeType()).isEqualTo("image/png");
        assertThat(result.size()).isEqualTo(content.length);
        // 驗證物理檔案確實寫入磁碟
        String relativePath = result.filePath().substring("/uploads/".length());
        assertThat(tempDir.resolve(relativePath)).exists();
    }

    @Test
    void store_emptyFile_throwsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storageService.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be empty");
    }

    @Test
    void store_unsupportedMimeType_throwsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported file type");
    }

    @Test
    void store_unsupportedExtension_throwsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "script.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported file type");
    }

    @Test
    void store_filenameWithoutExtension_throwsIllegalArgument() {
        // 無副檔名時 extractExtension 應直接拋出例外，訊息包含 "missing"
        MockMultipartFile file = new MockMultipartFile(
            "file", "noext", "image/jpeg", "jpg-bytes".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("missing");
    }

    @Test
    void store_traversalFilename_throwsIllegalArgument() {
        // 路徑穿越攻擊：../../etc/passwd.png 應被清理後偵測並拒絕
        MockMultipartFile file = new MockMultipartFile(
            "file", "../../etc/passwd.png", "image/png", "fake".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid filename");
    }

    @Test
    void store_mimeExtensionMismatch_isAccepted() {
        // 當前設計僅進行白名單檢查，不交叉驗證 MIME 與副檔名
        // 此測試錨定現有行為，未來若要強化可明確修改
        MockMultipartFile file = new MockMultipartFile(
            "file", "image.png", "image/jpeg", "fake-jpeg-bytes".getBytes());

        StorageResult result = storageService.store(file);

        assertThat(result.mimeType()).isEqualTo("image/jpeg");
        assertThat(result.filePath()).endsWith(".png");
    }
}
