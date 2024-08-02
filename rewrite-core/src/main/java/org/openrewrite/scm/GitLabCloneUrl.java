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

import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value
public class GitLabCloneUrl implements CloneUrl {
    String cloneUrl;
    String origin;
    String path;
    List<String> groups;
    String organization;
    String repositoryName;

    public GitLabCloneUrl(String cloneUrl, String origin, String path) {
        this.cloneUrl = cloneUrl;
        this.origin = origin;
        this.path = path;
        if (!this.path.contains("/")) {
            throw new IllegalArgumentException("GitLab path must contain 1 or more groups and a repository name");
        }
        String[] parts = this.path.split("/");
        groups = Arrays.asList(parts).subList(0, parts.length - 1);
        organization = String.join("/", groups);
        repositoryName = parts[parts.length - 1];
    }
}
