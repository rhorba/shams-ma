package com.shamsma.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class RateLimitFilterTest {

  private static final String CLIENT_IP = "203.0.113.7";

  @Test
  void exhaustingOnePathDoesNotLimitADifferentPathForTheSameClient() throws Exception {
    RateLimitFilter filter = new RateLimitFilter();

    // Drain the 10-request/minute budget for /api/v1/roi/estimate.
    for (int i = 0; i < 10; i++) {
      assertThat(callFilter(filter, "/api/v1/roi/estimate")).isEqualTo(200);
    }
    assertThat(callFilter(filter, "/api/v1/roi/estimate")).isEqualTo(429);

    // A different limited path, same client, must still have its own untouched budget.
    assertThat(callFilter(filter, "/api/v1/installers/browse")).isEqualTo(200);
  }

  @Test
  void returns429OnceCapacityIsExceeded() throws Exception {
    RateLimitFilter filter = new RateLimitFilter();

    for (int i = 0; i < 10; i++) {
      assertThat(callFilter(filter, "/api/v1/auth/login")).isEqualTo(200);
    }

    assertThat(callFilter(filter, "/api/v1/auth/login")).isEqualTo(429);
  }

  private static int callFilter(RateLimitFilter filter, String path) throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getRequestURI()).thenReturn(path);
    when(request.getRemoteAddr()).thenReturn(CLIENT_IP);
    when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

    int[] status = {200};
    org.mockito.Mockito.doAnswer(
            invocation -> {
              status[0] = invocation.getArgument(0);
              return null;
            })
        .when(response)
        .setStatus(org.mockito.ArgumentMatchers.anyInt());

    filter.doFilterInternal(request, response, chain);

    if (status[0] == 200) {
      verify(chain, times(1)).doFilter(any(), any());
    }
    return status[0];
  }
}
