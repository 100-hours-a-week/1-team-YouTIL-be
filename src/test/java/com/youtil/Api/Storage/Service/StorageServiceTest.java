package com.youtil.Api.Storage.Service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.youtil.Api.Storage.Dto.StorageResponseDTO.ImageUploadResponse;
import static com.youtil.Constants.StorageServiceTestConstants.BUCKET_NAME_KEY;
import static com.youtil.Constants.StorageServiceTestConstants.BUCKET_NAME_VALUE;
import static com.youtil.Constants.StorageServiceTestConstants.CONTENT;
import static com.youtil.Constants.StorageServiceTestConstants.CONTENT_TYPE;
import static com.youtil.Constants.StorageServiceTestConstants.IMAGE_NAME;
import static com.youtil.Constants.StorageServiceTestConstants.STORAGE_NAME;
import static com.youtil.Constants.StorageServiceTestConstants.STORAGE_URL;
import com.youtil.Exception.StorageException.StorageException.ImageUploadException;
import com.youtil.Exception.StorageException.StorageException.NotImageException;
import static com.youtil.Mock.MockUserBuilder.createMockUser;
import com.youtil.Model.User;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class StorageServiceTest {

    private User mockUser;
    private MultipartFile file;


    @Mock
    private Storage storage;
    @InjectMocks
    private StorageService storageService;

    @BeforeEach
    public void setUp() {
        mockUser = createMockUser();
        file = mock(MultipartFile.class);
        ReflectionTestUtils.setField(storageService, BUCKET_NAME_KEY, BUCKET_NAME_VALUE);
    }

    @Test
    @DisplayName("이미지 업로드 - 업로드할 파일이 존재- 성공")
    void imageUpload_withValidImage_success() throws IOException {
        final String IMAGE_PATH = "user/" + mockUser.getId() + "/";

        setUpMockFile();
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(CONTENT));

        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        when(storage.create(blobInfoCaptor.capture(), any(InputStream.class)))
                .thenReturn(mock(Blob.class));

        ImageUploadResponse response = storageService.imageUploadService(mockUser.getId(), file,
                STORAGE_NAME);

        assertNotNull(response);
        assertTrue(response.getImageUrl().contains(STORAGE_URL));

        BlobInfo capturedBlobInfo = blobInfoCaptor.getValue();
        assertEquals(CONTENT_TYPE, capturedBlobInfo.getContentType());
        assertTrue(capturedBlobInfo.getName().startsWith(IMAGE_PATH));
    }

    @ParameterizedTest
    @DisplayName("이미지 업로드 - 허용되지 않는 이미지 형식 - 실패")
    @ValueSource(strings = {
            "application/pdf",
            "image/gif",
            "image/svg+xml",
            "image/webp",
            "image/bmp",
            "application/json",
            "text/plain",
            "",
            "   "
    })
    void imageUpload_withInvalidImageTypes_fail(String contentType) {
        when(file.getContentType()).thenReturn(contentType);

        assertThatThrownBy(
                () -> storageService.imageUploadService(mockUser.getId(), file, STORAGE_NAME))
                .isInstanceOf(NotImageException.class);
    }

    @Test
    @DisplayName("이미지 업로드 - IOException 발생 - 실패")
    void imageUpload_withIOException_fail() throws IOException {

        setUpMockFile();
        when(file.getInputStream()).thenThrow(IOException.class);

        assertThatThrownBy(() -> storageService.imageUploadService(mockUser.getId(), file,
                STORAGE_NAME)).isInstanceOf(
                ImageUploadException.class);
    }

    void setUpMockFile() {
        when(file.getOriginalFilename()).thenReturn(IMAGE_NAME);
        when(file.getContentType()).thenReturn(CONTENT_TYPE);
    }
}
