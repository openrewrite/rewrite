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
public class GithubActionsBuildEnvironment implements BuildEnvironment {
    @With
    UUID id;
    String buildNumber;
    String buildId;
    String host;
    String job;
    String apiURL; //https://api.github.com
    String repository; //e.g octocat/Hello-World
    String ghRef; //// refs/pull/<pr_number>/merge or refs/heads/<branch_name>
    String sha;
    String headRef;

    public static GithubActionsBuildEnvironment build(UnaryOperator<String> environment) {
        return new GithubActionsBuildEnvironment(
                randomId(),
                environment.apply("GITHUB_RUN_NUMBER"),
                environment.apply("GITHUB_RUN_ID"),
                environment.apply("GITHUB_SERVER_URL"),
                environment.apply("GITHUB_ACTION"),
                environment.apply("GITHUB_API_URL"),
                environment.apply("GITHUB_REPOSITORY"),
                environment.apply("GITHUB_REF"),
                environment.apply("GITHUB_SHA"),
                environment.apply("GITHUB_HEAD_REF")
        );
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        String host = getApiURL().replaceFirst("api\\.", "");
        String gitRef = getGhRef();
        if (gitRef.startsWith("refs/pull")) {
            gitRef = getHeadRef();
        } else {
            gitRef = gitRef.replaceFirst("refs/heads/", "");
        }
        if (StringUtils.isBlank(ghRef)
            || StringUtils.isBlank(host)
            || StringUtils.isBlank(repository)
            || StringUtils.isBlank(sha)) {
            throw new IncompleteGitConfigException(
                    String.format("Invalid GitHub environment with host: %s, branch: %s, " +
                                  "repository: %s, sha: %s", host, ghRef, repository, sha));
        }

        return new GitProvenance(UUID.randomUUID(), host + "/" + getRepository()
                                                    + ".git", gitRef, getSha(), null, null, emptyList());
    }
}
