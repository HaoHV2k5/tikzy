package com.tikzy.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tikzy.common.config.CloudinaryProperties;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryImageStorageService implements ImageStorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    @Override
    public StoredImage upload(byte[] content, String publicId) {
        requireConfigured();
        if (content == null || content.length == 0 || !StringUtils.hasText(publicId)) {
            throw new AppException(ErrorCode.INVALID_EVENT_IMAGE);
        }
        String qualifiedPublicId = qualify(publicId);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    content,
                    ObjectUtils.asMap(
                            "public_id", qualifiedPublicId,
                            "overwrite", true,
                            "invalidate", true,
                            "resource_type", "image"));
            String url = firstText(result.get("secure_url"), result.get("url"));
            if (!StringUtils.hasText(url)) {
                throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
            String storedPublicId = result.get("public_id") != null
                    ? result.get("public_id").toString()
                    : qualifiedPublicId;
            return new StoredImage(url, storedPublicId);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Cloudinary upload failed for publicId={}", qualifiedPublicId, exception);
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }
        requireConfigured();
        String qualifiedPublicId = qualify(publicId);
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(
                    qualifiedPublicId,
                    ObjectUtils.asMap(
                            "invalidate", true,
                            "resource_type", "image"));
            Object status = result == null ? null : result.get("result");
            if (status != null
                    && !"ok".equals(status.toString())
                    && !"not found".equals(status.toString())) {
                log.error("Cloudinary destroy returned {} for publicId={}", status, qualifiedPublicId);
                throw new AppException(ErrorCode.IMAGE_DELETE_FAILED);
            }
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Cloudinary delete failed for publicId={}", qualifiedPublicId, exception);
            throw new AppException(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new AppException(ErrorCode.IMAGE_STORAGE_NOT_CONFIGURED);
        }
    }

    private String qualify(String publicId) {
        String folder = properties.resolvedFolder();
        String trimmed = publicId.trim();
        if (trimmed.startsWith(folder + "/")) {
            return trimmed;
        }
        return folder + "/" + trimmed;
    }

    private String firstText(Object first, Object second) {
        if (first != null && StringUtils.hasText(first.toString())) {
            return first.toString();
        }
        return second == null ? null : second.toString();
    }
}
