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
package org.openrewrite.gradle.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class GradleWrapperTest {

    public static void mockGradleServices(Consumer<MockWebServer> block) {
        mockGradleServices(List.of("7.5.1", "7.6", "7.6-milestone-1", "7.6-rc-1"), block);
    }

    public static void mockGradleServices(Collection<String> gradleVersions, Consumer<MockWebServer> block) {
        try (MockWebServer mockRepo = new MockWebServer()) {
            List<GradleWrapper.GradleVersion> versions = gradleVersions.stream()
              .map(version -> {
                  String urlPrefix = "http://%s:%d/distributions/gradle-%s".formatted(mockRepo.getHostName(), mockRepo.getPort(), version);
                  return new GradleWrapper.GradleVersion(version,
                    urlPrefix + "-bin.zip",
                    urlPrefix + "-bin.zip.sha256",
                    urlPrefix + "-wrapper.jar.sha256");
              })
              .toList();

            String availableVersions = new ObjectMapper()
              .registerModule(new ParameterNamesModule())
              .writeValueAsString(versions);

            mockRepo.setDispatcher(new Dispatcher() {

                @NonNull
                @Override
                public MockResponse dispatch(RecordedRequest recordedRequest) {
                    String path = recordedRequest.getPath();
                    if(path != null) {
                        if ("/versions/all".equals(path)) {
                            return new MockResponse().setResponseCode(200).setBody(availableVersions);
                        } else if (path.startsWith("/distributions/gradle-")) {
                            String version = path.substring("/distributions/gradle-".length(), path.lastIndexOf("-"));
                            String extension = path.substring(path.lastIndexOf("-bin.") + "-bin.".length());
                            String resourcePath = "gradle-%s-bin.%s".formatted(version, extension);
                            try (
                              InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)
                            ) {
                                String body = StringUtils.readFully(is);
                                return new MockResponse().setResponseCode(200).setBody(body);
                            } catch (IOException e) {
                                return new MockResponse().setResponseCode(500).setBody("Unable to load resource " + resourcePath);
                            }
                        }
                    }
                    return new MockResponse().setResponseCode(400).setBody("Mockserver not configured for path " + recordedRequest.getPath());
                }
            });

            block.accept(mockRepo);
            assertThat(mockRepo.getRequestCount())
              .as("The mock repository received no requests. The test is not using it.")
              .isGreaterThan(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
