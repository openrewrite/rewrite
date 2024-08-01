package org.openrewrite.scm;

import lombok.Getter;

/**
 * Simple as in just split by origin/path, and where the path is organization/repositoryName, like GitHub
 */
@Getter
public class SimpleScm implements Scm {
    private final String origin;

    public SimpleScm(String origin) {
        if (origin.startsWith("ssh://") || origin.startsWith("http://") || origin.startsWith("https://")) {
            origin = cleanHostAndPath(origin);
        }
        this.origin = origin.replaceFirst("/$", "");
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
