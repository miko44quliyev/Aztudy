package com.example.mscontent;

import com.example.mscontent.dto.ContentFileInfo;
import com.example.mscontent.service.MinioService;
import io.minio.*;
import io.minio.messages.Item;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private MinioService minioService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(minioService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(minioService, "endpoint", "http://localhost:9000");
    }

    // ─── createBucketIfNotExists ───
    @Test
    void createBucketIfNotExists_bucketExists() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);

        assertDoesNotThrow(() -> minioService.createBucketIfNotExists());

        verify(minioClient, never()).makeBucket(any());
    }

    @Test
    void createBucketIfNotExists_createNewBucket() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(false);

        minioService.createBucketIfNotExists();

        verify(minioClient).makeBucket(any());
    }

    // ─── uploadFile Multipart ───
    @Test
    void uploadFile_success() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);

        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

        String result = minioService.uploadFile(file, "course-1");

        assertNotNull(result);
        assertTrue(result.contains("course-1/"));

        verify(minioClient).putObject(any());
    }

    @Test
    void uploadFile_exception() throws Exception {
        when(minioClient.bucketExists(any())).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class,
                () -> minioService.uploadFile(file, "course-1"));
    }

    // ─── uploadFile InputStream ───
    @Test
    void uploadFile_inputStream_success() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);

        InputStream is = new ByteArrayInputStream("data".getBytes());

        String result = minioService.uploadFile(is, "file.txt", "text/plain", 4);

        assertEquals("file.txt", result);
    }

    // ─── downloadFile ───
    @Test
    void downloadFile_success() throws Exception {
        GetObjectResponse response = mock(GetObjectResponse.class);

        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(response);

        InputStream result = minioService.downloadFile("file.txt");

        assertNotNull(result);
    }

    @Test
    void downloadFile_exception() throws Exception {
        when(minioClient.getObject(any())).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class,
                () -> minioService.downloadFile("file.txt"));
    }

    // ─── deleteFile ───
    @Test
    void deleteFile_success() throws Exception {
        doNothing().when(minioClient).removeObject(any());

        assertDoesNotThrow(() -> minioService.deleteFile("file.txt"));
    }

    // ─── fileExists ───
    @Test
    void fileExists_true() throws Exception {
        when(minioClient.statObject(any())).thenReturn(mock(StatObjectResponse.class));

        assertTrue(minioService.fileExists("file.txt"));
    }

    @Test
    void fileExists_false() throws Exception {
        when(minioClient.statObject(any())).thenThrow(new RuntimeException());

        assertFalse(minioService.fileExists("file.txt"));
    }

    // ─── getFileInfo ───
    @Test
    void getFileInfo_success() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);

        when(stat.size()).thenReturn(100L);
        when(stat.contentType()).thenReturn("text/plain");
        when(stat.lastModified()).thenReturn(ZonedDateTime.now());
        when(stat.etag()).thenReturn("etag123");

        when(minioClient.statObject(any())).thenReturn(stat);

        ContentFileInfo info = minioService.getFileInfo("file.txt");

        assertEquals(100L, info.getFileSize());
        assertEquals("text/plain", info.getContentType());
    }

    @Test
    void getFileInfo_exception() throws Exception {
        when(minioClient.statObject(any())).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class,
                () -> minioService.getFileInfo("file.txt"));
    }

    // ─── getPresignedUrl ───
    @Test
    void getPresignedUrl_success() throws Exception {
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://url");

        String url = minioService.getPresignedUrl("file.txt", 60);

        assertEquals("http://url", url);
    }

    // ─── getPublicUrl ───
    @Test
    void getPublicUrl_success() {
        String url = minioService.getPublicUrl("file.txt");

        assertEquals("http://localhost:9000/test-bucket/file.txt", url);
    }

    // ─── copyFile ───
    @Test
    void copyFile_success() throws Exception {
        when(minioClient.copyObject(any(CopyObjectArgs.class)))
                .thenReturn(mock(ObjectWriteResponse.class));

        assertDoesNotThrow(() ->
                minioService.copyFile("src.txt", "dest.txt"));
    }

    // ─── deleteFolder ───
    @Test
    void deleteFolder_success() throws Exception {
        Item item = mock(Item.class);
        when(item.objectName()).thenReturn("folder/file.txt");

        Result<Item> result = mock(Result.class);
        when(result.get()).thenReturn(item);

        Iterable<Result<Item>> results = java.util.List.of(result);

        when(minioClient.listObjects(any())).thenReturn(results);
        doNothing().when(minioClient).removeObject(any());

        assertDoesNotThrow(() ->
                minioService.deleteFolder("folder/"));

        verify(minioClient).removeObject(any());
    }
}