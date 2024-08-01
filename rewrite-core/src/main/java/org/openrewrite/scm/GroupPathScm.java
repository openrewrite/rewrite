package org.openrewrite.scm;

import lombok.Getter;
import org.openrewrite.internal.lang.Nullable;

/**
 * Can be used for GitLab
 */
@Getter
public class GroupPathScm implements Scm {
    private final String origin;

    public GroupPathScm(String origin) {
        if (origin.startsWith("ssh://") || origin.startsWith("http://") || origin.startsWith("https://")) {
            origin = cleanHostAndPath(origin);
        }
        this.origin = origin.replaceFirst("/$", "");
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
