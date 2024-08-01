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

import org.openrewrite.internal.lang.Nullable;

public interface Scm extends Comparable<Scm> {
    String getOrigin();

    String cleanHostAndPath(String cloneUrl);

    default boolean belongsToScm(String cloneUrl) {
        return cleanHostAndPath(cloneUrl).startsWith(getOrigin());
    }

    @Nullable
    default String determineOrganization(String path) {
        return path.substring(0, path.indexOf("/"));
    }

    @Nullable
    default String determineGroupPath(String path) {
        return null;
    }

    @Nullable
    default String determineProject(String path) {
        return null;
    }

    default String determineRepositoryName(String path) {
        return path.substring(path.indexOf("/") + 1);
    }

    default ScmUrlComponents determineScmUrlComponents(String cloneUrl) {
        String path = cleanHostAndPath(cloneUrl).substring(getOrigin().length() + 1);
        return new ScmUrlComponents(getOrigin(), determineOrganization(path), determineGroupPath(path), determineProject(path), determineRepositoryName(path));
    }

    @Override
    default int compareTo(Scm o) {
        return getOrigin().compareTo(o.getOrigin());
    }

}
