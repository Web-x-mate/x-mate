// src/main/java/xmate/com/storage/LocalFileStorageService.java
package xmate.com.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    /** Thư mục thực tế trên máy để lưu file, ví dụ: ./uploads */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** prefix URL public để browser truy cập, ví dụ: /uploads/ */
    @Value("${app.upload.public-prefix:/uploads/}")
    private String publicPrefix;

    @Override
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new RuntimeException("Empty file");
        try {
            Files.createDirectories(Path.of(uploadDir));
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "");
            String finalName = (ext != null && !ext.isBlank()) ? filename + "." + ext : filename;
            Path target = Path.of(uploadDir, finalName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return publicPrefix + finalName; // ví dụ /uploads/xxx.png
        } catch (IOException e) {
            log.error("Save file error", e);
            throw new RuntimeException("Cannot save file", e);
        }
    }

    @Override
    public void deleteByUrl(String url) {
        if (url == null) return;
        try {
            // url dạng /uploads/xxx => map về file thực tế
            if (!url.contains(publicPrefix)) return;
            String name = url.substring(url.lastIndexOf('/') + 1);
            Path p = Path.of(uploadDir, name);
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Delete file failed: {}", url);
        }
    }
}
