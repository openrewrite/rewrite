/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.remote;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.MockHttpSender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.*;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class RemoteFileTest {

    @Test
    void gradleWrapperProperties() throws Exception {
        URL distributionUrl = requireNonNull(RemoteFileTest.class.getClassLoader().getResource("gradle-wrapper.properties"));
        ExecutionContext ctx = new InMemoryExecutionContext();
        HttpSenderExecutionContextView.view(ctx)
          .setLargeFileHttpSender(new MockHttpSender(distributionUrl::openStream));

        RemoteFile remoteFile = Remote
          .builder(
            Paths.get("gradle/wrapper/gradle-wrapper.properties"),
            distributionUrl.toURI()
          )
          .build();

        byte[] actual = readAll(remoteFile.getInputStream(ctx));
        assertThat(actual).hasSizeGreaterThanOrEqualTo(223);
    }

    @Test
    void gradleWrapperPropertiesConcurrent() throws Exception {
        int NUM_EXECUTIONS = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_EXECUTIONS);
        CompletionService<byte[]> completionService = new ExecutorCompletionService<>(executorService);

        for (int i = 0; i < NUM_EXECUTIONS; i++) {
            completionService.submit(() -> {
                URL distributionUrl = requireNonNull(RemoteFileTest.class.getClassLoader().getResource("gradle-wrapper.properties"));

                ExecutionContext ctx = new InMemoryExecutionContext();
                HttpSenderExecutionContextView.view(ctx)
                  .setLargeFileHttpSender(new MockHttpSender(distributionUrl::openStream));

                RemoteFile remoteFile = Remote
                  .builder(
                    Paths.get("gradle/wrapper/gradle-wrapper.properties"),
                    distributionUrl.toURI()
                  )
                  .build();
                
                return readAll(remoteFile.getInputStream(ctx));
            });
        }

        for (int i = 0; i < NUM_EXECUTIONS; i++) {
            Future<byte[]> result = completionService.take();
            byte[] actual = result.get();
            assertThat(actual).hasSizeGreaterThanOrEqualTo(223);
        }

        executorService.shutdown();
    }

    private byte[] readAll(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
