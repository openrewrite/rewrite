package org.openrewrite.scm;

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
class UrlComponents {

    @Nullable
    String username;
    @Nullable
    String password;

    @Nullable
    String scheme;
    String host;
    String path;
    @Nullable
    String query;
    @Nullable
    String fragment;

    @Nullable
    Integer port;

    boolean isSsh() {
        return scheme != null && scheme.equals("ssh");
    }

    public String toString() {
        return scheme + "://" + host + maybePort() + path;
    }

    String maybePort() {
        if (port == null) {
            return "";
        }
        return ":" + port;
    }

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(?:(?<scheme>[^:/?#]+)://)?(?:(?<userinfo>[^@]+)@)?(?<host>[^:/?#]+)?(?::(?<port>\\d+))?(?<path>[^?#]*)?(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?$"
    );

    private static final Pattern USERINFO_PATTERN = Pattern.compile(
            "(?<username>[^:]+)(?::(?<password>.*))?"
    );

    private static final Pattern SCP_PATTERN = Pattern.compile(
            "^(?<username>[^@]+)@(?<host>[^:/?#]+):(?<path>[^?#]+)$"
    );

    /**
     * Custom parse method that can handle SCP-like URLs (git@github.com:path)
     * @param url URL to parse
     * @return URL components
     */
    static UrlComponents parse(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid URL format: " + url);
        }

        String scheme = matcher.group("scheme");
        String userinfo = matcher.group("userinfo");
        String host = matcher.group("host");
        String path = matcher.group("path");
        String query = matcher.group("query");
        String fragment = matcher.group("fragment");
        String portStr = matcher.group("port");
        Integer port = portStr != null ? Integer.valueOf(portStr) : null;

        String username = null;
        String password = null;
        if (userinfo != null) {
            Matcher userinfoMatcher = USERINFO_PATTERN.matcher(userinfo);
            if (userinfoMatcher.matches()) {
                username = userinfoMatcher.group("username");
                password = userinfoMatcher.group("password");
            }
        }

        // Handle SCP-like URLs (git@github.com:path)
        if (scheme == null && host == null) {
            Matcher scpMatcher = SCP_PATTERN.matcher(url);
            if (scpMatcher.matches()) {
                scheme = "ssh";
                username = scpMatcher.group("username");
                host = scpMatcher.group("host");
                path = "/" + scpMatcher.group("path");
                port = null; // Ports are not typically specified in SCP-like URLs
            }
        }

        if (scheme == null && path.startsWith(":")) {
            scheme = "ssh";
        }

        return new UrlComponents(username, password, scheme, host, path.replaceFirst("^/", "").replaceFirst("^:", ""), query, fragment, port);
    }

}
