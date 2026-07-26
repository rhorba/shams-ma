package com.shamsma.api.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class UploadValidatorTest {

  @Test
  void detectsPdf() {
    byte[] content = {0x25, 0x50, 0x44, 0x46, 0x01, 0x02};
    assertThat(UploadValidator.detectContentType(content)).isEqualTo("application/pdf");
    assertThat(UploadValidator.extensionFor("application/pdf")).isEqualTo("pdf");
  }

  @Test
  void detectsJpeg() {
    byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    assertThat(UploadValidator.detectContentType(content)).isEqualTo("image/jpeg");
  }

  @Test
  void detectsPng() {
    byte[] content = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    assertThat(UploadValidator.detectContentType(content)).isEqualTo("image/png");
  }

  @Test
  void rejectsSpoofedContentType() {
    // A .txt file's actual bytes, regardless of what Content-Type header a client might send.
    byte[] content = "not a real pdf".getBytes();
    assertThatThrownBy(() -> UploadValidator.detectContentType(content))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unsupported file type");
  }

  @Test
  void rejectsEmptyFile() {
    assertThatThrownBy(() -> UploadValidator.detectContentType(new byte[0]))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("empty");
  }

  @Test
  void rejectsOversizedFile() {
    byte[] content = new byte[(int) UploadValidator.MAX_SIZE_BYTES + 1];
    // Give it valid PDF magic bytes so only the size check can reject it.
    content[0] = 0x25;
    content[1] = 0x50;
    content[2] = 0x44;
    content[3] = 0x46;
    Arrays.fill(content, 4, content.length, (byte) 0x01);

    assertThatThrownBy(() -> UploadValidator.detectContentType(content))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("maximum size");
  }
}
