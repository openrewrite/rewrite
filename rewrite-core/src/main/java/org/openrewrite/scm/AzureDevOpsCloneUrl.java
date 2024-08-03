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

@Value
public class AzureDevOpsCloneUrl implements CloneUrl {
    String cloneUrl;
    String origin;
    String path;
    String organization;
    String repositoryName;

    public AzureDevOpsCloneUrl(String cloneUrl, String origin, String path) {
        this.cloneUrl = cloneUrl;
        this.origin = origin;
        this.path = path;
        if (!this.path.contains("/")) {
            throw new IllegalArgumentException("Azure DevOps clone url path must contain organization, project and repository");
        }
        String azureDevOpsOrganization = path.substring(0, path.indexOf("/"));
        String azureDevOpsProject = path.substring(path.indexOf("/") + 1, path.lastIndexOf("/"));
        this.repositoryName = path.substring(path.lastIndexOf("/") + 1);
        this.organization = azureDevOpsOrganization + '/' + azureDevOpsProject;
    }
}
