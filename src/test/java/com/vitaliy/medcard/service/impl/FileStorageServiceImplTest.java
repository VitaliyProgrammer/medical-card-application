package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.exception.FileStorageException;
import com.vitaliy.medcard.exception.UnsupportedFileTypeException;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    private static final String BUCKET = "medical-card-documents";

    @Mock
    private S3Client s3Client;

    @BeforeEach
    void setUp() {
        lenient().when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(HeadBucketResponse.builder().build());
    }

    @Test
    @DisplayName("constructor: creates the bucket if it doesn't exist yet")
    void constructor_createsMissingBucket() {
        S3Client freshClient = mock(S3Client.class);
        when(freshClient.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().build());

        new FileStorageServiceImpl(freshClient, BUCKET);

        verify(freshClient).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    @DisplayName("constructor: does not recreate an existing bucket")
    void constructor_bucketAlreadyExists() {
        new FileStorageServiceImpl(s3Client, BUCKET);

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    @DisplayName("store: uploads the file and returns a UUID-prefixed key")
    void store_success() {
        FileStorageServiceImpl storageService = new FileStorageServiceImpl(s3Client, BUCKET);

        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());

        String key = storageService.store(file);

        assertThat(key).endsWith("_report.pdf");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("store: rejects a filename with a path traversal sequence")
    void store_pathTraversal_isRejected() {
        FileStorageServiceImpl storageService = new FileStorageServiceImpl(s3Client, BUCKET);

        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd", "application/pdf", "content".getBytes());

        assertThrows(UnsupportedFileTypeException.class, () -> storageService.store(file));
    }

    @Test
    @DisplayName("load: returns a resource wrapping the S3 object stream")
    void load_success() {
        FileStorageServiceImpl storageService = new FileStorageServiceImpl(s3Client, BUCKET);

        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream("content".getBytes()));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        Resource resource = storageService.load("uuid_report.pdf");

        assertThat(resource).isNotNull();
    }

    @Test
    @DisplayName("load: throws when the key doesn't exist")
    void load_missingKey() {
        FileStorageServiceImpl storageService = new FileStorageServiceImpl(s3Client, BUCKET);

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThrows(FileStorageException.class, () -> storageService.load("missing.pdf"));
    }

    @Test
    @DisplayName("delete: removes the object from the bucket")
    void delete_success() {
        FileStorageServiceImpl storageService = new FileStorageServiceImpl(s3Client, BUCKET);

        storageService.delete("uuid_report.pdf");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
