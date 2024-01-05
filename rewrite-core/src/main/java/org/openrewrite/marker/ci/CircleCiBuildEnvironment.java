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
package org.openrewrite.marker.ci;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.GitProvenance;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.OperatingSystemProvenance.hostname;

@Value
@EqualsAndHashCode(callSuper = false)
public class CircleCiBuildEnvironment implements BuildEnvironment {
    @With
    UUID id;

    String buildId;
    String buildUrl;
    String host;
    String job;

    String branch;

    String repositoryURL;

    String sha1;

    String tag;

    public static CircleCiBuildEnvironment build(UnaryOperator<String> environment) {
        return new CircleCiBuildEnvironment(
                randomId(),
                environment.apply("CIRCLE_BUILD_NUM"),
                environment.apply("CIRCLE_BUILD_URL"),
                hostname(),
                environment.apply("CIRCLE_JOB"),
                environment.apply("CIRCLE_BRANCH"),
                environment.apply("CIRCLE_REPOSITORY_URL"),
                environment.apply("CIRCLE_SHA1"),
                environment.apply("CIRCLE_TAG")
        );
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(repositoryURL) || StringUtils.isBlank(sha1) || (
                StringUtils.isBlank(branch) && StringUtils.isBlank(tag))) {
            throw new IncompleteGitConfigException();
        }
        return new GitProvenance(UUID.randomUUID(), repositoryURL, StringUtils.isBlank(branch) ? tag : branch,
                sha1, null, null, emptyList());
    }
}
