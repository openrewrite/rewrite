package org.openrewrite.scm;

import lombok.Getter;

@Getter
public class BitbucketServerScm implements Scm {
    private final String origin;

    public BitbucketServerScm(String origin) {
        if (origin.startsWith("ssh://") || origin.startsWith("http://") || origin.startsWith("https://")) {
            origin = cleanHostAndPath(origin);
        }
        this.origin = origin;
    }

    @Override
    public String cleanHostAndPath(String url) {
        UrlComponents uri = UrlComponents.parse(url);
        String hostAndPath = uri.getHost() + uri.maybePort() + "/" + uri.getPath()
                .replaceFirst("^/", "")
                .replaceFirst("scm/", "")
                .replaceFirst("\\.git$", "");
        return hostAndPath.replaceFirst("/$", "");
    }
}
