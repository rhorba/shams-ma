package com.shamsma.api.shared.storage;

import java.net.URI;
import java.time.Duration;

public interface FileStorageService {

  /** Uploads content under the given key, returning the storage key (not a public URL). */
  String upload(String key, byte[] content, String contentType);

  /** Generates a time-limited signed URL to retrieve the object at the given key. */
  URI presignedUrl(String key, Duration ttl);
}
