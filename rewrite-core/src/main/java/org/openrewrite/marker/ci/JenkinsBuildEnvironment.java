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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class JenkinsBuildEnvironment implements BuildEnvironment {
    @With
    UUID id;

    String buildNumber;
    String buildId;
    String buildUrl;
    String host;
    String job;

    /**
     * Local branch name, e.g. main.
     * <p>
     * When the option to set the local branch is disabled in Jenkins this won't be present.
     */
    @Nullable
    String localBranch;

    /**
     * Remote branch name, e.g. origin/main.
     */
    String branch;

    public static JenkinsBuildEnvironment build(UnaryOperator<String> environment) {
        return new JenkinsBuildEnvironment(
                randomId(),
                environment.apply("BUILD_NUMBER"),
                environment.apply("BUILD_ID"),
                environment.apply("BUILD_URL"),
                environment.apply("JENKINS_URL"),
                environment.apply("JOB_NAME"),
                environment.apply("GIT_LOCAL_BRANCH"),
                environment.apply("GIT_BRANCH")
        );
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        throw new IncompleteGitConfigException();
    }
}
