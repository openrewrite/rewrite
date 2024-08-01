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

import lombok.Getter;
import org.openrewrite.internal.lang.Nullable;

@Getter
public class GitLabScm implements Scm {
    private final String origin;

    public GitLabScm() {
        this.origin = "gitlab.com";
    }

    @Override
    public @Nullable String determineGroupPath(String path) {
        return path.substring(0, path.lastIndexOf("/"));
    }

    @Override
    public String determineRepositoryName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    @Override
    public String cleanHostAndPath(String url) {
        UrlComponents uri = UrlComponents.parse(url);
        String hostAndPath = uri.getHost() + uri.maybePort() + "/" + uri.getPath()
                .replaceFirst("^/", "")
                .replaceFirst("\\.git$", "");
        return hostAndPath.replaceFirst("/$", "");
    }
}
