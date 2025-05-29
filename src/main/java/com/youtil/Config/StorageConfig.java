package com.youtil.Config;

import com.google.cloud.storage.Storage;
import com.youtil.Api.Storage.Service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${spring.cloud.bucket}")
    private String bucketName;

    @Bean
    public StorageService storageService(Storage storage) {
        return new StorageService(storage, bucketName);
    }
}
