package com.example.mscontent.service;

import com.example.mscontent.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig config;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(config.getBucketName()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(config.getBucketName()).build()
                );
            }
            log.info("MinIO bucket '{}' is ready.", config.getBucketName());
        } catch (Exception e) {
            log.error("MinIO initialization failed: {}", e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file, String folder) {
        try {
            String fileName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucketName())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return fileName;
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    public String getPresignedUrl(String fileName) {
        try {
            // ƏGƏR fileName içində "content/" sözü varsa, onu təmizləyirik
            // Çünki bucket(config.getBucketName()) zatən "content" əlavə edir.
            String cleanPath = fileName;
            if (fileName.startsWith(config.getBucketName() + "/")) {
                cleanPath = fileName.substring(config.getBucketName().length() + 1);
            }

            String internalUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(config.getBucketName()) // Burada "content" əlavə olunur
                            .object(cleanPath)             // Burada "course-1/file.pdf" olmalıdır
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );

            // Host əvəzləmə (Docker/Localhost problemi üçün)
            return internalUrl.replace(config.getUrl(), config.getPublicUrl());

        } catch (Exception e) {
            log.error("URL generation failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(config.getBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Delete failed: {}", e.getMessage());
        }
    }
}