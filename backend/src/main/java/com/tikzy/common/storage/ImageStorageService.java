package com.tikzy.common.storage;

public interface ImageStorageService {

    StoredImage upload(byte[] content, String publicId);

    void delete(String publicId);
}
