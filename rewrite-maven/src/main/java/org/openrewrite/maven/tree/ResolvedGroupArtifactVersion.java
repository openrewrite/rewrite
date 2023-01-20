/*
 * Copyright 2020 the original author or authors.
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

package org.openrewrite.maven.tree;

import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;

import java.io.Serializable;

@Value
@With
public class ResolvedGroupArtifactVersion implements Serializable {
    @Nullable
    String repository;

    String groupId;
    String artifactId;
    String version;

    /**
     * In the form "${version}-${timestamp}-${buildNumber}", e.g. for the artifact rewrite-testing-frameworks-1.7.0-20210614.172805-1.jar,
     * the dated snapshot version is "1.7.0-20210614.172805-1".
     */
    @Nullable
    String datedSnapshotVersion;

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + (datedSnapshotVersion == null ? version : datedSnapshotVersion);
    }
}
