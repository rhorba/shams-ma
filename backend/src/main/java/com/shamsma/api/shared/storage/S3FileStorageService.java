package com.shamsma.api.shared.storage;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Backed by MinIO (S3-compatible) via the AWS SDK v2 — path-style access + endpoint override, a
 * placeholder region (MinIO ignores it but the SDK requires one to be set).
 */
@Service
class S3FileStorageService implements FileStorageService {

  private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final String bucket;

  S3FileStorageService(FileStorageProperties properties) {
    var credentials =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    URI endpoint = URI.create(properties.endpoint());
    this.bucket = properties.bucket();
    ClientOverrideConfiguration overrideConfiguration =
        ClientOverrideConfiguration.builder()
            .apiCallTimeout(Duration.ofSeconds(5))
            .apiCallAttemptTimeout(Duration.ofSeconds(5))
            .build();
    this.s3Client =
        S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .overrideConfiguration(overrideConfiguration)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    URI publicEndpoint =
        properties.publicEndpoint() != null ? URI.create(properties.publicEndpoint()) : endpoint;
    this.s3Presigner =
        S3Presigner.builder()
            .endpointOverride(publicEndpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
  }

  /**
   * Best-effort — not allowed to fail application startup if object storage happens to be
   * unreachable (e.g. a test context that doesn't need uploads). A real upload attempt will still
   * surface a clear error if the bucket genuinely can't be created/reached.
   */
  @PostConstruct
  void ensureBucketExists() {
    try {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    } catch (BucketAlreadyOwnedByYouException e) {
      // already provisioned — nothing to do
    } catch (SdkException e) {
      log.warn(
          "Could not verify/create file-storage bucket '{}' at startup: {}",
          bucket,
          e.getMessage());
    }
  }

  @Override
  public String upload(String key, byte[] content, String contentType) {
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
        RequestBody.fromBytes(content));
    return key;
  }

  @Override
  public URI presignedUrl(String key, Duration ttl) {
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
            .build();
    return URI.create(s3Presigner.presignGetObject(presignRequest).url().toString());
  }
}
