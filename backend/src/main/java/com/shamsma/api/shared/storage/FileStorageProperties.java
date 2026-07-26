package com.shamsma.api.shared.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code publicEndpoint} is separate from {@code endpoint} because inside Docker Compose the API
 * reaches MinIO at the internal service hostname ({@code http://minio:9000}), but a presigned URL
 * handed to a browser must resolve from outside that network (e.g. {@code http://localhost:9000}) —
 * same class of issue as the DB_HOST-inside-vs-outside-compose bug from Story 0.1.
 */
@ConfigurationProperties(prefix = "app.file-storage")
public record FileStorageProperties(
    String endpoint, String publicEndpoint, String accessKey, String secretKey, String bucket) {}
