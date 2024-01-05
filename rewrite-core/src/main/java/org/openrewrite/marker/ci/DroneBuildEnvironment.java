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
public class DroneBuildEnvironment implements BuildEnvironment {
    @With
    UUID id;

    String buildId;
    String host;
    String job;

    String branch;

    String tag;

    String remoteURL;

    String commitSha;


    public static DroneBuildEnvironment build(UnaryOperator<String> environment) {
        return new DroneBuildEnvironment(
                randomId(),
                environment.apply("DRONE_BUILD_NUMBER"),
                hostname(),
                environment.apply("DRONE_REPO"),
                environment.apply("DRONE_BRANCH"),
                environment.apply("DRONE_TAG"),
                environment.apply("DRONE_REMOTE_URL"),
                environment.apply("DRONE_COMMIT_SHA")
        );
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(remoteURL)
            || (StringUtils.isBlank(branch) && StringUtils.isBlank(tag))
            || StringUtils.isBlank(commitSha)) {
            throw new IncompleteGitConfigException();
        }
        return new GitProvenance(UUID.randomUUID(), remoteURL,
                StringUtils.isBlank(branch) ? tag : branch, commitSha,
                null, null, emptyList());
    }

    public String getBuildUrl() {
        return "http://" + host + "/build/" + buildId;
    }
}
