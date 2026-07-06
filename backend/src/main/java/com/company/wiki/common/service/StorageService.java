package com.company.wiki.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;

    @Value("${storage.s3.bucket}")
    private String bucket;

    /**
     * 파일을 MinIO에 업로드한다.
     * storage key 규칙: "contents/{contentId}/{UUID}_{originalFileName}"
     */
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(req, RequestBody.fromInputStream(inputStream, contentLength));
        return key;
    }

    public ResponseInputStream<GetObjectResponse> download(String key) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
