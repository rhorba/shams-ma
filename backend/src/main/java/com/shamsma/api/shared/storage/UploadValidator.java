package com.shamsma.api.shared.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates uploaded certification documents by magic bytes (not the client-declared Content-Type,
 * which is trivially spoofable) per Security doc section 6.
 */
public final class UploadValidator {

  public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

  private static final Map<String, byte[]> MAGIC_BYTES = new LinkedHashMap<>();
  private static final Map<String, String> EXTENSIONS =
      Map.of(
          "application/pdf", "pdf",
          "image/jpeg", "jpg",
          "image/png", "png");

  static {
    MAGIC_BYTES.put("application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46}); // %PDF
    MAGIC_BYTES.put("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
    MAGIC_BYTES.put(
        "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
  }

  private UploadValidator() {}

  /** Returns the detected content type, or throws if the content isn't an allowed type/size. */
  public static String detectContentType(byte[] content) {
    if (content == null || content.length == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
    }
    if (content.length > MAX_SIZE_BYTES) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "File exceeds maximum size of 10MB");
    }
    for (Map.Entry<String, byte[]> entry : MAGIC_BYTES.entrySet()) {
      if (startsWith(content, entry.getValue())) {
        return entry.getKey();
      }
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Unsupported file type — only PDF, JPEG and PNG are allowed");
  }

  public static String extensionFor(String contentType) {
    return EXTENSIONS.get(contentType);
  }

  private static boolean startsWith(byte[] content, byte[] prefix) {
    if (content.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (content[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
