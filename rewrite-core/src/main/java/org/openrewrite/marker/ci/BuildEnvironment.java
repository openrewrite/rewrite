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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.marker.Marker;

import java.util.function.UnaryOperator;

public interface BuildEnvironment extends Marker {
    @Nullable
    static BuildEnvironment build(UnaryOperator<String> environment) {
        if (environment.apply("BITBUCKET_COMMIT") != null) {
            return BitbucketBuildEnvironment.build(environment);
        }
        if (environment.apply("CUSTOM_CI") != null) {
            return CustomBuildEnvironment.build(environment);
        }
        if (environment.apply("BUILD_NUMBER") != null && environment.apply("JOB_NAME") != null) {
            return JenkinsBuildEnvironment.build(environment);
        }
        if (environment.apply("GITLAB_CI") != null) {
            return GitlabBuildEnvironment.build(environment);
        }
        if (environment.apply("CI") != null && environment.apply("GITHUB_ACTION") != null
                && environment.apply("GITHUB_RUN_ID") != null) {
            return GithubActionsBuildEnvironment.build(environment);
        }
        if (environment.apply("DRONE") != null) {
            return DroneBuildEnvironment.build(environment);
        }
        if (environment.apply("CIRCLECI") != null) {
            return CircleCiBuildEnvironment.build(environment);
        }
        if (environment.apply("TEAMCITY_VERSION") != null) {
            return TeamcityBuildEnvironment.build(environment);
        }
        if (environment.apply("TRAVIS") != null) {
            return TravisBuildEnvironment.build(environment);
        }
        return null;
    }

    GitProvenance buildGitProvenance() throws IncompleteGitConfigException;
}
