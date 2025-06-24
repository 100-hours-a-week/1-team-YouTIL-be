package com.youtil.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class GcpStorageConfig {

    @Value("${spring.cloud.project-id}")
    String projectId;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public Storage storage() throws IOException {
        // 환경별 JSON 파일 경로 설정
        String jsonPath = "youtil-cloud-storage.json";

//        jsonPath = "shared-config/backend/youtil-cloud-storage.json";
        ClassPathResource resource = new ClassPathResource(jsonPath);
        try (InputStream credentialsStream = resource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);

            return StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .build()
                    .getService();
        }
    }

}
