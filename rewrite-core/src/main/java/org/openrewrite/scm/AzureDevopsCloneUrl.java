package org.openrewrite.scm;

import lombok.Value;

@Value
public class AzureDevopsCloneUrl implements CloneUrl {
    String cloneUrl;
    String origin;
    String path;

    String getProject() {
        try {
            String path = getPath();
            return path.substring(path.indexOf("/") + 1, path.lastIndexOf("/"));
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalStateException("Azure DevOps clone url path must contain organization, project and repository", ex);
        }
    }
}
