package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.exception.FileStorageException;
import com.vitaliy.medcard.exception.UnsupportedFileTypeException;
import com.vitaliy.medcard.service.FileStorageService;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public FileStorageServiceImpl(
            S3Client s3Client,
            @Value("${app.storage.bucket:medical-card-documents}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        ensureBucketExists();
    }

    @Override
    public String store(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "file"));

        if (originalFileName.contains("..")) {
            throw new UnsupportedFileTypeException(
                    "Filename contains an invalid path sequence: " + originalFileName + "!");
        }

        String key = UUID.randomUUID() + "_" + originalFileName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            throw new FileStorageException("Failed to store file: " + originalFileName + "!");
        }

        return key;
    }

    @Override
    public Resource load(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            return new InputStreamResource(s3Client.getObject(request));
        } catch (NoSuchKeyException e) {
            throw new FileStorageException("File not found: " + key + "!");
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to load file: " + key + "!");
        }
    }

    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to delete file: " + key + "!");
        }
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            log.info("Storage bucket '{}' not found, creating it", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }
}
