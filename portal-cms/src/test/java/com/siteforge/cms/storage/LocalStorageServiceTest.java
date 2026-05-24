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
    void store_filenameWithoutExtension_usesBinExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "noext", "image/jpeg", "jpg-bytes".getBytes());

        StorageResult result = storageService.store(file);

        assertThat(result.filePath()).endsWith(".bin");
    }
}
