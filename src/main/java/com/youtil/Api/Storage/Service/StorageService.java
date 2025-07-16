package com.youtil.Api.Storage.Service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.youtil.Api.Storage.Dto.StorageResponseDTO.ImageUploadResponse;
import com.youtil.Common.Enums.ErrorMessageCode;
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

    private final Storage storage;
    private final String bucketName;
    private final AmazonS3 amazonS3;

    public ImageUploadResponse imageUploadService(Long userId, MultipartFile file,
            String storageName) throws IOException {
        validateImageFile(file);
        String fileName = generateFileName(userId, file);
        if (storageName.equals("GCP")) {
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
        } else {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(bucketName, fileName, file.getInputStream(), metadata);

            return ImageUploadResponse.builder()
                    .imageUrl(amazonS3.getUrl(bucketName, fileName).toString()).build();
        }
    }

    public void imageDeleteService(String imageUrl) {
        String objectName = extractObjectNameFromUrl(imageUrl);
        boolean deleted = storage.delete(BlobId.of(bucketName, objectName));
        if (!deleted) {
            log.error("삭제 실패!");
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
            log.error(ErrorMessageCode.IMAGE_NOT_FOUND.getMessage() + ": " + imageUrl);

        }
        return imageUrl.substring(prefix.length());
    }
}

