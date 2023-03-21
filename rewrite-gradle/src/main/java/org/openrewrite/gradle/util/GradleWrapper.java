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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.remote.Remote;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Value
public class GradleWrapper {
    public static final Path WRAPPER_JAR_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.jar");
    public static final Path WRAPPER_PROPERTIES_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.properties");
    public static final Path WRAPPER_SCRIPT_LOCATION = Paths.get("gradlew");
    public static final Path WRAPPER_BATCH_LOCATION = Paths.get("gradlew.bat");

    String version;
    DistributionInfos distributionInfos;

    public static Validated validate(
            ExecutionContext ctx,
            String version,
            @Nullable String distribution,
            @Nullable Validated cachedValidation,
            @Nullable String repositoryUrl) {
        if (cachedValidation != null) {
            return cachedValidation;
        }
        String distributionTypeName = distribution != null ? distribution : DistributionType.Bin.name().toLowerCase();
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();

        //noinspection unchecked
        return new Validated.Both(
            Validated.test("distributionType", "must be a valid distribution type", distributionTypeName,
                dt -> Arrays.stream(DistributionType.values())
                    .anyMatch(type -> type.name().equalsIgnoreCase(dt))),
            Semver.validate(version, null)
        ) {
            GradleWrapper wrapper;

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
                if (wrapper != null) {
                    return wrapper;
                }

                DistributionType distributionType = Arrays.stream(DistributionType.values())
                        .filter(dt -> dt.name().equalsIgnoreCase(distributionTypeName))
                        .findAny()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown distribution type " + distributionTypeName));
                VersionComparator versionComparator = requireNonNull(Semver.validate(version, null).getValue());

                String gradleVersionsUrl = (repositoryUrl == null) ?  "https://services.gradle.org/versions/all" : repositoryUrl;
                try (HttpSender.Response resp = httpSender.send(httpSender.get(gradleVersionsUrl).build())) {
                    if (resp.isSuccessful()) {
                        List<GradleVersion> allVersions = new ObjectMapper()
                                .registerModule(new ParameterNamesModule())
                                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                .readValue(resp.getBody(), new TypeReference<List<GradleVersion>>() {
                                });
                        GradleVersion gradleVersion = allVersions.stream()
                                .filter(v -> versionComparator.isValid(null, v.version))
                                .max((v1, v2) -> versionComparator.compare(null, v1.version, v2.version))
                                .orElseThrow(() -> new IllegalStateException("Expected to find at least one Gradle wrapper version to select from."));

                        DistributionInfos infos = DistributionInfos.fetch(httpSender, distributionType, gradleVersion);
                        wrapper = new GradleWrapper(gradleVersion.version, infos);
                        return wrapper;
                    }
                    throw new IOException("Could not get Gradle versions at: " + gradleVersionsUrl);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

        };
    }

    public String getDistributionUrl() {
        return distributionInfos.getDownloadUrl();
    }

    public String getPropertiesFormattedUrl() {
        return getDistributionUrl().replaceAll("(?<!\\\\)://", "\\\\://");
    }

    public Checksum getDistributionChecksum() {
        return distributionInfos.getChecksum();
    }

    static final FileAttributes WRAPPER_JAR_FILE_ATTRIBUTES = new FileAttributes(null, null, null, true, true, false, 0);

    public Remote asRemote() {
        return Remote.builder(
                WRAPPER_JAR_LOCATION,
                URI.create(distributionInfos.getDownloadUrl())
        ).build("gradle-[^\\/]+\\/(?:.*\\/)+gradle-wrapper-(?!shared).*\\.jar");
    }

    public enum DistributionType {
        Bin,
        All
    }

    @Value
    static class GradleVersion {
        String version;
        String downloadUrl;
        String checksumUrl;
        String wrapperChecksumUrl;
    }
}
