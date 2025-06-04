package com.youtil.Util;

import com.youtil.Exception.StorageException.StorageException;

import java.util.List;

public class ImageValidator {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg");

    public static void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new StorageException.NotImageException();
        }

        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            throw new StorageException.NotImageException();
        }

        String lowerUrl = imageUrl.toLowerCase();
        boolean validExtension = ALLOWED_EXTENSIONS.stream().anyMatch(lowerUrl::endsWith);

        if (!validExtension) {
            throw new StorageException.NotImageException();
        }
    }
}

