package com.youtil.Api.Storage.Service;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.youtil.Api.Storage.Dto.StorageResponseDTO.ImageUploadResponse;
import com.youtil.Exception.StorageException.StorageException;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
public class StorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg");

    private final String bucketName;
    private final AmazonS3 amazonS3;

    public ImageUploadResponse imageUploadService(Long userId, MultipartFile file,
            String storageName) throws IOException {
        validateImageFile(file);
        String fileName = generateFileName(userId, file);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucketName, fileName, file.getInputStream(), metadata);

        return ImageUploadResponse.builder()
                .imageUrl(amazonS3.getUrl(bucketName, fileName).toString()).build();

    }

    public void imageDeleteService(String imageUrl) {

        String objectKey = extractObjectNameFromUrl(imageUrl); // 예: "images/somefile.png"

        try {
            amazonS3.deleteObject(bucketName, objectKey);
        } catch (AmazonServiceException e) {
            throw new StorageException.ImageDeleteException();
        }
    }

    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new StorageException.NotImageException();
        }
    }

    private String generateFileName(Long userId, MultipartFile file) {
        String extension = Objects.requireNonNull(file.getOriginalFilename())
                .substring(file.getOriginalFilename().lastIndexOf("."));
        return "user/" + userId + "/" + UUID.randomUUID() + extension;
    }

    private String extractObjectNameFromUrl(String imageUrl) {
        String prefix = "https://storage.googleapis.com/" + bucketName + "/";
        if (!imageUrl.startsWith(prefix)) {
            throw new StorageException.InvalidImageUrlException();
        }
        return imageUrl.substring(prefix.length());
    }
}

