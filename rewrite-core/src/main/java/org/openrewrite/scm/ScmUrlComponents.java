/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.scm;

import lombok.Builder;
import lombok.Value;
import org.openrewrite.internal.lang.Nullable;

@Value
@Builder
public class ScmUrlComponents {

    @Nullable
    String origin;

    @Nullable
    String organization;

    @Nullable
    String groupPath;

    @Nullable
    String project;

    String repositoryName;

    public String getPath() {
        if (groupPath != null) {
            return groupPath + "/" + repositoryName;
        }
        if (organization != null) {
            if (project != null) {
                return organization + "/" + project + "/" + repositoryName;
            }
            return organization + "/" + repositoryName;
        }
        return repositoryName;
    }
}
