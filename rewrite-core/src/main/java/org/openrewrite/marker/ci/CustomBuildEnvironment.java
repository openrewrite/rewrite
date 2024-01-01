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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.GitProvenance;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class CustomBuildEnvironment implements BuildEnvironment {
    @With
    UUID id;
    String cloneURL;
    String ref;
    String sha;

    public static CustomBuildEnvironment build(UnaryOperator<String> environment) {
        return new CustomBuildEnvironment(
                randomId(),
                environment.apply("CUSTOM_GIT_CLONE_URL"),
                environment.apply("CUSTOM_GIT_REF"),
                environment.apply("CUSTOM_GIT_SHA"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(cloneURL)
            || StringUtils.isBlank(ref)
            || StringUtils.isBlank(sha)) {
            throw new IncompleteGitConfigException();
        } else {
            return new GitProvenance(UUID.randomUUID(), cloneURL, ref, sha,
                    null, null, emptyList());
        }
    }

}
