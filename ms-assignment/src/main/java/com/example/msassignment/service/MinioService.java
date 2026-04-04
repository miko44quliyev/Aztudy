package com.example.msassignment.service;

import com.example.msassignment.dto.ContentFileInfo;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:content}")
    private String bucketName;

    @Value("${minio.url:http://localhost:9000}")
    private String minioUrl;

    // Overload method - default expiry ilə
    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(objectName, 3600);
    }

    public void createBucketIfNotExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("✅ Bucket yaradıldı: {}", bucketName);
                setBucketPolicy();
            }
        } catch (Exception e) {
            log.error("❌ Bucket yoxlanılamadı: {}", e.getMessage());
            throw new RuntimeException("MinIO bucket hazırlanarkən xəta: " + e.getMessage());
        }
    }

    private void setBucketPolicy() {
        try {
            String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
            log.info("🔓 Bucket policy tətbiq edildi (public read)");
        } catch (Exception e) {
            log.warn("Bucket policy tətbiq olunmadı: {}", e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file, String folder) {
        try {
            createBucketIfNotExists();

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + extension;
            String objectName = folder + "/" + uniqueFileName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("📁 Fayl yükləndi: {} ({} bytes)", objectName, file.getSize());
            return objectName;

        } catch (Exception e) {
            log.error("❌ Fayl yüklənərkən xəta: {}", e.getMessage());
            throw new RuntimeException("Fayl yüklənə bilmədi: " + e.getMessage());
        }
    }

    public String uploadFile(InputStream inputStream, String objectName, String contentType, long size) {
        try {
            createBucketIfNotExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("📁 InputStream faylı yükləndi: {}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("❌ InputStream fayl yüklənərkən xəta: {}", e.getMessage());
            throw new RuntimeException("Fayl yüklənə bilmədi: " + e.getMessage());
        }
    }

    public InputStream downloadFile(String objectName) {
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("📥 Fayl endirildi: {}", objectName);
            return inputStream;

        } catch (Exception e) {
            log.error("❌ Fayl endirilərkən xəta: {}", e.getMessage());
            throw new RuntimeException("Fayl endirilə bilmədi: " + e.getMessage());
        }
    }

    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("🗑️ Fayl silindi: {}", objectName);

        } catch (Exception e) {
            log.error("❌ Fayl silinərkən xəta: {}", e.getMessage());
            throw new RuntimeException("Fayl silinə bilmədi: " + e.getMessage());
        }
    }

    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ContentFileInfo getFileInfo(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            return ContentFileInfo.builder()
                    .fileName(objectName)
                    .fileSize(stat.size())
                    .contentType(stat.contentType())
                    .lastModified(stat.lastModified())
                    .etag(stat.etag())
                    .build();

        } catch (Exception e) {
            log.error("❌ Fayl məlumatları alınarkən xəta: {}", e.getMessage());
            throw new RuntimeException("Fayl məlumatları alınmadı: " + e.getMessage());
        }
    }

    public String getPresignedUrl(String objectName, int expiryInSeconds) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(io.minio.http.Method.GET)
                            .expiry(expiryInSeconds)
                            .build()
            );
            log.info("🔗 Presigned URL yaradıldı: {} (expiry: {}s)", objectName, expiryInSeconds);
            return url;

        } catch (Exception e) {
            log.error("❌ Presigned URL yaradılarkən xəta: {}", e.getMessage());
            throw new RuntimeException("URL yaradıla bilmədi: " + e.getMessage());
        }
    }

    public String getPublicUrl(String objectName) {
        return String.format("%s/%s/%s", minioUrl, bucketName, objectName);
    }

    public void copyFile(String sourceObjectName, String destObjectName) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(destObjectName)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(sourceObjectName)
                                    .build())
                            .build()
            );
            log.info("📋 Fayl kopyalandı: {} -> {}", sourceObjectName, destObjectName);

        } catch (Exception e) {
            log.error("❌ Fayl kopyalanarkən xəta: {}", e.getMessage());
            throw new RuntimeException("Fayl kopyalana bilmədi: " + e.getMessage());
        }
    }

    public void deleteFolder(String folderPath) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(folderPath)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build()
                );
                log.info("🗑️ Qovluq faylı silindi: {}", item.objectName());
            }

        } catch (Exception e) {
            log.error("❌ Qovluq silinərkən xəta: {}", e.getMessage());
            throw new RuntimeException("Qovluq silinə bilmədi: " + e.getMessage());
        }
    }
}