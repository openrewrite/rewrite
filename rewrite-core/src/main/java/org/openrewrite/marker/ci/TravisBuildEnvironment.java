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
import org.openrewrite.marker.GitProvenance;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.OperatingSystemProvenance.hostname;

@Value
@EqualsAndHashCode(callSuper = false)
public class TravisBuildEnvironment implements BuildEnvironment {
    @With
    UUID id;

    String buildNumber;
    String buildId;
    String buildUrl;
    String host;
    String job;
    String branch;
    String commit;

    String repoSlug;

    String tag;

    public static TravisBuildEnvironment build(UnaryOperator<String> environment) {
        return new TravisBuildEnvironment(
                randomId(),
                environment.apply("TRAVIS_BUILD_NUMBER"),
                environment.apply("TRAVIS_BUILD_ID"),
                environment.apply("TRAVIS_BUILD_WEB_URL"),
                hostname(),
                environment.apply("TRAVIS_REPO_SLUG"),
                environment.apply("TRAVIS_BRANCH"),
                environment.apply("TRAVIS_COMMIT"),
                environment.apply("TRAVIS_REPO_SLUG"),
                environment.apply("TRAVIS_TAG")
        );
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        //travis generates the .config directory and it is not possible to obtain the clone URL from any env
        throw new IncompleteGitConfigException();
    }
}
