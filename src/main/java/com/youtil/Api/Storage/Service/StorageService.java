package com.youtil.Api.Storage.Service;


import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.youtil.Api.Storage.Dto.StorageResponseDTO.ImageUploadResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import com.youtil.Exception.StorageException.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final Storage storage;
    @Value("${spring.cloud.bucket}")
    private String bucketName;

    public ImageUploadResponse imageUploadService(Long userId, MultipartFile file,
            String storageName) {
        validateImageFile(file);
        String fileName = generateFileName(userId, file);

        try {
            BlobInfo blobInfo = storage.create(
                    BlobInfo.newBuilder(bucketName, fileName)
                            .setContentType(file.getContentType())
                            .build(),
                    file.getInputStream()
            );
            String imageUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName,
                    fileName);
            return ImageUploadResponse.builder().imageUrl(imageUrl).build();

        } catch (IOException e) {
            throw new StorageException.ImageUploadException();
        }
    }

    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new StorageException.NotImageException();
        }
    }

    private String generateFileName(Long userId, MultipartFile file) {
        String extension = Objects.requireNonNull(file.getOriginalFilename())
                .substring(file.getOriginalFilename().lastIndexOf("."));
        return "user/" + userId + "/" + UUID.randomUUID() + extension;
    }
}

