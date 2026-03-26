package com.example.msassignment.service;

import com.example.msassignment.config.MinioConfig;
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
    private final MinioConfig minioConfig;

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Bucket init failed", e);
        }
    }

    public String uploadFile(MultipartFile file, String folder) {
        try {
            String original = file.getOriginalFilename();
            String cleanName = (original == null)
                    ? "file"
                    : original.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            String fileName = folder + "/" + UUID.randomUUID() + "_" + cleanName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    public String getPresignedUrl(String fileName) {
        try {
            // daxili minio client ilə imzala
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );

            // brauzer üçün host-u dəyiş
            return url.replace("http://minio:9000", "http://localhost:9000");

        } catch (Exception e) {
            log.error("URL generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("URL generation failed", e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Delete failed", e);
        }
    }
}