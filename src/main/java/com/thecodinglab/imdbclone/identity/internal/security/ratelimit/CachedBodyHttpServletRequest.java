package com.thecodinglab.imdbclone.identity.internal.security.ratelimit;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StreamUtils;

class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

  private final byte[] body;

  CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
    super(request);
    this.body = StreamUtils.copyToByteArray(request.getInputStream());
  }

  byte[] body() {
    return body.clone();
  }

  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return inputStream.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Async reads are not supported");
      }

      @Override
      public int read() {
        return inputStream.read();
      }

      @Override
      public int read(byte[] target, int offset, int length) {
        return inputStream.read(target, offset, length);
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
  }
}
