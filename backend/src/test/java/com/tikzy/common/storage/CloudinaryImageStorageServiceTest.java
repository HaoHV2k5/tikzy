package com.tikzy.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tikzy.common.config.CloudinaryProperties;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    private CloudinaryImageStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new CloudinaryImageStorageService(
                cloudinary,
                new CloudinaryProperties("demo", "key", "secret", "tikzy"));
    }

    @Test
    void upload_prefixesFolderAndReturnsSecureUrl() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/tikzy/events/id/banner.jpg",
                "public_id", "tikzy/events/id/banner"));

        StoredImage stored = storageService.upload(new byte[] {1, 2, 3}, "events/id/banner");

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/v1/tikzy/events/id/banner.jpg",
                stored.url());
        assertEquals("tikzy/events/id/banner", stored.publicId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), options.capture());
        assertEquals("tikzy/events/id/banner", options.getValue().get("public_id"));
        assertEquals(true, options.getValue().get("overwrite"));
    }

    @Test
    void upload_withoutConfiguration_throws() throws Exception {
        CloudinaryImageStorageService unconfigured = new CloudinaryImageStorageService(
                cloudinary,
                new CloudinaryProperties("", "", "", "tikzy"));

        AppException exception = assertThrows(
                AppException.class,
                () -> unconfigured.upload(new byte[] {1}, "events/id/banner"));

        assertEquals(ErrorCode.IMAGE_STORAGE_NOT_CONFIGURED, exception.getErrorCode());
        verify(uploader, never()).upload(any(byte[].class), anyMap());
    }

    @Test
    void upload_cloudinaryFailure_throwsSafeException() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("timeout"));

        AppException exception = assertThrows(
                AppException.class,
                () -> storageService.upload(new byte[] {1, 2, 3}, "events/id/banner"));

        assertEquals(ErrorCode.IMAGE_UPLOAD_FAILED, exception.getErrorCode());
    }

    @Test
    void delete_ignoresNotFound() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("tikzy/events/id/banner"), anyMap()))
                .thenReturn(Map.of("result", "not found"));

        storageService.delete("events/id/banner");

        verify(uploader).destroy(eq("tikzy/events/id/banner"), anyMap());
    }

    @Test
    void delete_unexpectedResult_throws() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("tikzy/events/id/banner"), anyMap()))
                .thenReturn(Map.of("result", "error"));

        AppException exception = assertThrows(
                AppException.class,
                () -> storageService.delete("events/id/banner"));

        assertEquals(ErrorCode.IMAGE_DELETE_FAILED, exception.getErrorCode());
    }
}
