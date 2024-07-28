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

@Value
@EqualsAndHashCode(callSuper = false)
public class GitlabBuildEnvironment implements BuildEnvironment {
    @With
    UUID id;

    String buildId;
    String buildUrl;
    String host;
    String job;
    String ciRepositoryUrl;
    String ciCommitRefName;
    String ciCommitSha;

    public static GitlabBuildEnvironment build(UnaryOperator<String> environment) {
        return new GitlabBuildEnvironment(
                randomId(),
                environment.apply("CI_BUILD_ID"),
                environment.apply("CI_JOB_URL"),
                environment.apply("CI_SERVER_HOST"),
                environment.apply("CI_BUILD_NAME"),
                environment.apply("CI_REPOSITORY_URL"),
                environment.apply("CI_COMMIT_REF_NAME"),
                environment.apply("CI_COMMIT_SHA")
        );
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(ciRepositoryUrl)
            || StringUtils.isBlank(ciCommitRefName)
            || StringUtils.isBlank(ciCommitSha)) {
            throw new IncompleteGitConfigException();
        }
        return new GitProvenance(UUID.randomUUID(), ciRepositoryUrl, ciCommitRefName, ciCommitSha,
                null, null, emptyList());
    }
}
