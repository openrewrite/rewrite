/*
 * Copyright 2023 the original author or authors.
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

@Value
@EqualsAndHashCode(callSuper = false)
public class TeamcityBuildEnvironment implements BuildEnvironment {
    @With
    UUID id;
    String projectName;
    String buildNumber;
    String buildUrl;
    String version;

    public static TeamcityBuildEnvironment build(UnaryOperator<String> environment) {
        return new TeamcityBuildEnvironment(
                randomId(),
                environment.apply("TEAMCITY_PROJECT_NAME"),
                environment.apply("BUILD_NUMBER"),
                environment.apply("BUILD_URL"),
                environment.apply("TEAMCITY_VERSION"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        // Not enough information to build GitProvenance
        // https://www.jetbrains.com/help/teamcity/predefined-build-parameters.html#Predefined+Server+Build+Parameters
        throw new IncompleteGitConfigException();
    }
}
