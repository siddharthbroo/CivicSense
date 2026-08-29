package com.civicsense.config;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageKitConfig {

    @Bean
    public ImageKitClient imageKitClient(
            @Value("${imagekit.private-key}") String privateKey
    ) {

        return ImageKitOkHttpClient.builder()
                .privateKey(privateKey)
                .build();
    }
}