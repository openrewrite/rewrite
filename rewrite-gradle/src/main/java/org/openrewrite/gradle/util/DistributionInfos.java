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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.ipc.http.HttpSender;

import java.io.IOException;
import java.net.URI;

@Value
public class DistributionInfos {
    String downloadUrl;

    @Nullable
    Checksum checksum;

    @Nullable
    Checksum wrapperJarChecksum;

    public static DistributionInfos fetch(GradleWrapper.DistributionType distributionType,
                                   GradleWrapper.GradleVersion gradleVersion, ExecutionContext ctx) throws IOException {
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        String downloadUrl = toDistTypeUrl(distributionType, gradleVersion.getDownloadUrl());
        Checksum checksum = fetchChecksum(httpSender, toDistTypeUrl(distributionType, gradleVersion.getChecksumUrl()));
        Checksum jarChecksum = gradleVersion.getWrapperChecksumUrl() == null ?
                null :
                fetchChecksum(httpSender, gradleVersion.getWrapperChecksumUrl());

        return new DistributionInfos(downloadUrl, checksum, jarChecksum);
    }

    private static String toDistTypeUrl(GradleWrapper.DistributionType distributionType, String binUrl) {
        if (distributionType == GradleWrapper.DistributionType.All) {
            return binUrl.replace("-bin.zip", "-all.zip");
        }
        return binUrl;
    }

    private static Checksum fetchChecksum(HttpSender httpSender, String checksumUrl) {
        return Checksum.fromUri(httpSender, URI.create(checksumUrl));
    }
}
