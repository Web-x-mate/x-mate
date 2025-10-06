
package xmate.com.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /** @return public URL để gán vào ProductMedia.url */
    String save(MultipartFile file);
    void deleteByUrl(String url);
}
