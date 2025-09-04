/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ipc.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpUrlConnectionSenderTest {

    @Test
    void withMultipartContentWithApplicationJsonType() throws Exception {
        HttpSender.Request request =
          new HttpUrlConnectionSender().newRequest("http://github.com")
            .withMultipartContent(HttpSender.Request.Builder.APPLICATION_JSON,
              "json", "{\"foo\":\"bar\"}")
            .build();
        assertThat(new String(request.getEntity())).matches(Pattern.compile("""
          --.*\r
          Content-Disposition: form-data; name="json"\r
          Content-Type: application/json\r
          \r
          \\{"foo":"bar"}\r
          --.*--\r
          """));
    }

    @Test
    void withMultipartContentWithApplicationJsonAndTextPlainType() throws Exception {
        HttpSender.Request request = new HttpUrlConnectionSender().newRequest("http://github.com")
          .withMultipartContent(HttpSender.Request.Builder.APPLICATION_JSON, "json", "{\"foo\":\"bar\"}")
          .withMultipartContent(HttpSender.Request.Builder.TEXT_PLAIN, "text", "abc123")
          .build();
        assertThat(new String(request.getEntity())).matches(Pattern.compile("""
          --.*\r
          Content-Disposition: form-data; name="json"\r
          Content-Type: application/json\r
          \r
          \\{"foo":"bar"}\r
          --.*\r
          Content-Disposition: form-data; name="text"\r
          Content-Type: text/plain\r
          \r
          abc123\r
          --.*--\r
          """));
    }

    @Test
    void withMultipartContentWithTextPlainAndFileType(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.resolve("file.dat");
        Files.writeString(path, "The quick brown fox...");
        HttpSender.Request request = new HttpUrlConnectionSender().newRequest("http://github.com")
          .withMultipartContent(HttpSender.Request.Builder.TEXT_PLAIN, "text", "abc123")
          .withMultipartFile(path, "file")
          .build();
        assertThat(new String(request.getEntity())).matches(Pattern.compile("""
          --.*\r
          Content-Disposition: form-data; name="text"\r
          Content-Type: text/plain\r
          \r
          abc123\r
          --.*\r
          Content-Disposition: form-data; name="file"; filename="file.dat"\r
          Content-Type: application/octet-stream\r
          \r
          The quick brown fox...\r
          --.*--\r
          """));
    }
}
