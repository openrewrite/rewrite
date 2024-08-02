package org.openrewrite.scm;

import lombok.Value;

@Value
public class AzureDevopsCloneUrl implements CloneUrl {
    String cloneUrl;
    String origin;
    String path;
    String organization;
    String project;
    String repositoryName;

    public AzureDevopsCloneUrl(String cloneUrl, String origin, String path) {
        this.cloneUrl = cloneUrl;
        this.origin = origin;
        this.path = path;
        if (!path.contains("/")) {
            throw new IllegalArgumentException("Azure DevOps clone url path must contain organization, project and repository");
        }
        this.organization = path.substring(0, path.indexOf("/"));
        this.project = path.substring(path.indexOf("/") + 1, path.lastIndexOf("/"));
        this.repositoryName = path.substring(path.lastIndexOf("/") + 1);
    }
}
