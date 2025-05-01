package com.youtil.Api.Storage.Dto;

import lombok.Builder;
import lombok.Getter;

public class StorageResponseDTO {

    @Builder
    @Getter
    public static class ImageUploadResponse {

        private String imageUrl;
    }
}
