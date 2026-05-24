package com.siteforge.cms.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp"
    );

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Override
    public StorageResult store(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File must not be empty");

        // 防止路徑穿越攻擊：RFC 7578 不保證 originalFilename 是純檔名
        String cleanedPath = StringUtils.cleanPath(
            file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        // cleanPath 正規化後仍含 ".." 或 "\" 代表有路徑穿越意圖
        if (cleanedPath.contains("..") || cleanedPath.contains("\\"))
            throw new IllegalArgumentException("Invalid filename: " + cleanedPath);
        String baseName = cleanedPath.contains("/")
            ? cleanedPath.substring(cleanedPath.lastIndexOf('/') + 1)
            : cleanedPath;
        // baseName 不應含路徑分隔符（防禦性驗證）
        if (baseName.contains("/") || baseName.contains("\\"))
            throw new IllegalArgumentException("Invalid filename: " + baseName);

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType))
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        String ext = extractExtension(baseName);
        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new IllegalArgumentException("Unsupported file extension: " + ext);

        LocalDate today = LocalDate.now();
        String subPath = today.getYear() + "/"
            + String.format("%02d", today.getMonthValue()) + "/"
            + UUID.randomUUID() + "." + ext;

        Path target = Paths.get(uploadDir).resolve(subPath);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + e.getMessage(), e);
        }

        return new StorageResult(
            baseName,
            "/uploads/" + subPath,
            mimeType,
            file.getSize()
        );
    }

    private String extractExtension(String filename) {
        // 無副檔名無法進行白名單比對，直接拒絕而非回傳模糊的 "bin"
        if (filename == null || !filename.contains("."))
            throw new IllegalArgumentException("File extension is missing");
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
