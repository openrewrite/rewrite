package org.openrewrite.scm;

import lombok.Getter;

import java.util.Arrays;

/**
 * Fallback that assumes the last 2 path segments of a cloneUrl contains the repository path, not the same as SimpleScm
 * which splits on a known origin.
 */
@Getter
public class UnknownScm extends SimpleScm {
    public UnknownScm(String cloneUrl) {
        super(cloneUrlToOrigin(cloneUrl));
    }

    private static String cloneUrlToOrigin(String cloneUrl) {
        UrlComponents uri = UrlComponents.parse(cloneUrl);
        String fullPath = uri.getPath()
                .replaceFirst("^/", "")
                .replaceFirst("\\.git$", "");

        String[] segments = fullPath.split("/");
        if (segments.length <= 2) {
            return uri.getHost() + uri.maybePort();
        }
        String contextPath = String.join("/", Arrays.copyOfRange(segments, 0, segments.length - 2));
        return uri.getHost() + uri.maybePort() + "/" + contextPath;
    }
}
