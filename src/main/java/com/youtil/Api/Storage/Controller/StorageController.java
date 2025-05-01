package com.youtil.Api.Storage.Controller;

import com.youtil.Api.Storage.Dto.StorageResponseDTO.ImageUploadResponse;
import com.youtil.Api.Storage.Service.StorageService;
import com.youtil.Common.ApiResponse;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "클라우드 스토리지 관련")
@RequestMapping("/api/v1/{storageName}")
@RestController
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> uploadImage(
            @Schema(description = "이미지 파일입니다.")
            @RequestPart("image") MultipartFile file,
            @Schema(description = "스토리지 이름입니다. GCP 또는 AWS을 적어주시면됩니다.", example = "GCP")
            @PathVariable String storageName) {

        return new ApiResponse<>("이미지 호스팅에 성공했습니다", "200",
                storageService.imageUploadService(JwtUtil.getAuthenticatedUserId(), file,
                        storageName));
    }
}
