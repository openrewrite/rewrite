package org.openrewrite.scm;

import org.openrewrite.internal.lang.Nullable;

public class AzureDevOpsScm implements Scm {

    @Override
    public String getOrigin() {
        return "dev.azure.com";
    }

    @Override
    public String cleanHostAndPath(String url) {
        UrlComponents uri = UrlComponents.parse(url);
        String host = uri.getHost();
        String path = uri.getPath();
        if (uri.isSsh() && host.startsWith("ssh.")) {
            host = host.substring(4);
            path = path.replaceFirst("v3/", "");
        } else {
            path = path.replaceFirst("_git/", "");
        }
        String hostAndPath = host + "/" + path
                .replaceFirst("\\.git$", "");
        return hostAndPath.replaceFirst("/$", "");
    }

    @Override
    public String determineRepositoryName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    @Nullable
    @Override
    public String determineProject(String path) {
        return path.substring(path.indexOf("/") + 1, path.lastIndexOf("/"));
    }
}
