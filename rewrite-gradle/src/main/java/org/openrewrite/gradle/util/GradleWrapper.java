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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.CheckedFunction1;
import lombok.Value;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;
import org.openrewrite.remote.RemoteArchive;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Value
public class GradleWrapper {
    public static final Path WRAPPER_JAR_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.jar");
    public static final Path WRAPPER_PROPERTIES_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.properties");
    public static final Path WRAPPER_SCRIPT_LOCATION = Paths.get("gradlew");
    public static final Path WRAPPER_BATCH_LOCATION = Paths.get("gradlew.bat");

    private static final String GRADLE_VERSIONS_CACHE_KEY = "GRADLE_VERSIONS";
    private static final Cache<String, List<String>> VALIDATION_CACHE =
            Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofDays(1))
                    .build();

    private static final RetryConfig retryConfig = RetryConfig.custom()
            .retryOnException(throwable -> throwable instanceof UncheckedIOException)
            .build();

    private static final RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);

    private static final Retry gradleDistributionsRetry = retryRegistry.retry("GradleDistributions");

    String version;
    DistributionType distributionType;

    public static Validated validate(String version, @Nullable String distributionTypeName, HttpSender httpSender) {
        //noinspection unchecked
        return new Validated.Both(
                Validated.test("distributionType", "must be a valid distribution type", distributionTypeName, dt -> {
                    if (distributionTypeName == null) {
                        return true;
                    }
                    try {
                        DistributionType.valueOf(dt);
                        return true;
                    } catch (Throwable e) {
                        return false;
                    }
                }),
                Semver.validate(version, null)
        ) {

            @Override
            public boolean isValid() {
                if (!super.isValid()) {
                    return false;
                }
                try {
                    buildWrapper();
                    return true;
                } catch (Throwable t) {
                    return false;
                }
            }

            @Override
            public GradleWrapper getValue() {
                return buildWrapper();
            }

            private GradleWrapper buildWrapper() {
                DistributionType distributionType = distributionTypeName == null ?
                        DistributionType.Bin :
                        Arrays.stream(DistributionType.values())
                                .filter(dt -> dt.name().equalsIgnoreCase(distributionTypeName))
                                .findAny()
                                .orElseThrow(() -> new IllegalArgumentException("Unknown distribution type " + distributionTypeName));
                VersionComparator versionComparator = requireNonNull(Semver.validate(version, null).getValue());

                List<String> allVersions = VALIDATION_CACHE.getIfPresent(GRADLE_VERSIONS_CACHE_KEY);

                if (allVersions == null) {
                    HttpSender.Request distributionsRequest = httpSender.get("https://services.gradle.org/distributions").build();
                    CheckedFunction1<HttpSender.Request, HttpSender.Response> sendRequest = Retry.decorateCheckedFunction(
                            gradleDistributionsRetry,
                            httpSender::send);

                    try (HttpSender.Response response = sendRequest.apply(distributionsRequest)){
                        try (InputStream is = response.getBody()) {

                            allVersions = new ArrayList<>();
                            Scanner scanner = new Scanner(is);
                            Pattern wrapperPattern = Pattern.compile("gradle-(.+)-" +
                                    distributionType.toString().toLowerCase() + "\\.zip(?!\\.sha)");

                            while (scanner.findWithinHorizon(wrapperPattern, 0) != null) {
                                allVersions.add(scanner.match().group(1));
                            }

                            scanner.close();

                            VALIDATION_CACHE.put(GRADLE_VERSIONS_CACHE_KEY, allVersions);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

                String version = allVersions.stream()
                        .filter(v -> versionComparator.isValid(null, v))
                        .max((v1, v2) -> versionComparator.compare(null, v1, v2))
                        .orElseThrow(() -> new IllegalStateException("Expected to find at least one Gradle wrapper version to select from."));

                return new GradleWrapper(version, distributionType);
            }
        };
    }

    public String getDistributionUrl() {
        return "https://services.gradle.org/distributions/gradle-" + version + "-" + distributionType.toString().toLowerCase() + ".zip";
    }

    public String getPropertiesFormattedUrl() {
        return getDistributionUrl().replaceAll("(?<!\\\\)://", "\\\\://");
    }

    static final FileAttributes WRAPPER_JAR_FILE_ATTRIBUTES = new FileAttributes(null, null, null, true, true, false, 0);

    public RemoteArchive asRemote() {
        return new RemoteArchive(Tree.randomId(), GradleWrapper.WRAPPER_JAR_LOCATION, Markers.EMPTY,
                URI.create(getDistributionUrl()), null, false, WRAPPER_JAR_FILE_ATTRIBUTES,
                "gradle-wrapper.jar is part of the gradle wrapper",
                Paths.get("gradle-" + version + "/lib/gradle-wrapper-" + version + ".jar!gradle-wrapper.jar"));
    }

    public enum DistributionType {
        Bin, All
    }
}
