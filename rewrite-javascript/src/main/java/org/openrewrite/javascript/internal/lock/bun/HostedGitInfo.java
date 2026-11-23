/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Port of bun's src/install/hosted_git_info.zig (bun 1.3.10): {@code isGitHubShorthand} and {@code fromUrl}, without
 * the formatters, parsing with {@link WhatwgUrl} where bun uses WebKit's WHATWG URL parser.
 */
final class HostedGitInfo {

    enum Provider {
        BITBUCKET("bitbucket.org", "bitbucket"),
        GIST("gist.github.com", "gist"),
        GITHUB("github.com", "github"),
        GITLAB("gitlab.com", "gitlab"),
        SOURCEHUT("git.sr.ht", "sourcehut");

        final String domain;
        final String shortcut;

        Provider(String domain, String shortcut) {
            this.domain = domain;
            this.shortcut = shortcut;
        }

        static @Nullable Provider fromShortcut(String shortcutWithoutColon) {
            for (Provider p : values()) {
                if (p.shortcut.equals(shortcutWithoutColon)) {
                    return p;
                }
            }
            return null;
        }

        static @Nullable Provider fromDomain(String domain) {
            for (Provider p : values()) {
                if (p.domain.equals(domain)) {
                    return p;
                }
            }
            return null;
        }

        // bun's url.hostname() is the host with its port
        static @Nullable Provider fromUrlDomain(WhatwgUrl url) {
            String hostname = url.getHost();
            return fromDomain(hostname.startsWith("www.") ? hostname.substring(4) : hostname);
        }

        static @Nullable Provider fromUrl(WhatwgUrl url) {
            String protocol = url.getProtocol();
            Provider p = fromShortcut(protocol.endsWith(":") ? protocol.substring(0, protocol.length() - 1) : protocol);
            return p != null ? p : fromUrlDomain(url);
        }
    }

    private enum WellDefinedProtocol {
        GIT("git"), GIT_PLUS_FILE("git+file"), GIT_PLUS_FTP("git+ftp"), GIT_PLUS_HTTP("git+http"),
        GIT_PLUS_HTTPS("git+https"), GIT_PLUS_RSYNC("git+rsync"), GIT_PLUS_SSH("git+ssh"), HTTP("http"),
        HTTPS("https"), SSH("ssh"), GITHUB("github"), BITBUCKET("bitbucket"), GITLAB("gitlab"), GIST("gist"),
        SOURCEHUT("sourcehut");

        final String name;

        WellDefinedProtocol(String name) {
            this.name = name;
        }

        static @Nullable WellDefinedProtocol fromStringWithColon(String protocolWithColon) {
            if (protocolWithColon.isEmpty()) {
                return null;
            }
            String name = protocolWithColon.endsWith(":") ?
                    protocolWithColon.substring(0, protocolWithColon.length() - 1) : protocolWithColon;
            for (WellDefinedProtocol p : values()) {
                if (p.name.equals(name)) {
                    return p;
                }
            }
            return null;
        }

        String concatenationToken() {
            return isShortcut() ? "" : "//";
        }

        Representation defaultRepresentation() {
            switch (this) {
                case GIT_PLUS_SSH:
                case SSH:
                case GIT_PLUS_HTTP:
                    return Representation.SSHURL;
                case GIT_PLUS_HTTPS:
                case HTTPS:
                    return Representation.HTTPS;
                case GIT_PLUS_FILE:
                case GIT_PLUS_FTP:
                case GIT_PLUS_RSYNC:
                case GIT:
                    return Representation.GIT;
                case HTTP:
                    return Representation.HTTP;
                default:
                    return Representation.SHORTCUT;
            }
        }

        @Nullable
        Provider hostProvider() {
            switch (this) {
                case GITHUB:
                    return Provider.GITHUB;
                case BITBUCKET:
                    return Provider.BITBUCKET;
                case GITLAB:
                    return Provider.GITLAB;
                case GIST:
                    return Provider.GIST;
                case SOURCEHUT:
                    return Provider.SOURCEHUT;
                default:
                    return null;
            }
        }

        boolean isShortcut() {
            return hostProvider() != null;
        }
    }

    /**
     * {@code UrlProtocolPair}: the url without its protocol, and the protocol, well defined, custom (with its
     * colon) or unknown (both null).
     */
    private static final class UrlProtocolPair {
        final String url;
        final @Nullable WellDefinedProtocol wellFormed;
        final @Nullable String custom;

        UrlProtocolPair(String url, @Nullable WellDefinedProtocol wellFormed, @Nullable String custom) {
            this.url = url;
            this.wellFormed = wellFormed;
            this.custom = custom;
        }

        boolean isUnknown() {
            return wellFormed == null && custom == null;
        }

        Representation defaultRepresentation() {
            return wellFormed != null ? wellFormed.defaultRepresentation() : Representation.SSHURL;
        }

        @Nullable
        WhatwgUrl toUrl() {
            if (wellFormed != null) {
                return WhatwgUrl.tryParse(wellFormed.name + ":" + wellFormed.concatenationToken() + url);
            }
            if (custom != null) {
                return WhatwgUrl.tryParse(custom + "//" + url);
            }
            return WhatwgUrl.tryParse("git+ssh://" + url);
        }
    }

    /**
     * How a url is reported when it is formatted as a string.
     */
    enum Representation {
        SHORTCUT, SSHURL, SSH, HTTPS, GIT, HTTP
    }

    final Provider hostProvider;
    final Representation defaultRepresentation;
    final @Nullable String user;
    final String project;
    final @Nullable String committish;

    private HostedGitInfo(Provider hostProvider, Representation defaultRepresentation, @Nullable String user,
                          String project, @Nullable String committish) {
        this.hostProvider = hostProvider;
        this.defaultRepresentation = defaultRepresentation;
        this.user = user;
        this.project = project;
        this.committish = committish;
    }

    static boolean isGitHubShorthand(String npaStr) {
        if (npaStr.isEmpty()) {
            return false;
        }
        if (npaStr.charAt(0) == '.' || npaStr.charAt(0) == '/') {
            return false;
        }
        int poundIdx = -1;
        boolean seenSlash = false;
        for (int i = 0; i < npaStr.length(); i++) {
            char c = npaStr.charAt(i);
            switch (c) {
                case ':':
                case '@':
                    if (poundIdx < 0) {
                        return false;
                    }
                    break;
                case '#':
                    poundIdx = i;
                    break;
                case '/':
                    if (seenSlash && poundIdx < 0) {
                        return false;
                    }
                    seenSlash = true;
                    break;
                default:
                    if ((c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000B' || c == '\u000C') &&
                        poundIdx < 0) {
                        return false;
                    }
                    break;
            }
        }
        boolean doesNotEndWithSlash = poundIdx >= 0 ?
                poundIdx == 0 || npaStr.charAt(poundIdx - 1) != '/' :
                npaStr.charAt(npaStr.length() - 1) != '/';
        return seenSlash && doesNotEndWithSlash;
    }

    /**
     * The repository a git url or shortcut names; null when no known host has it or it does not parse.
     */
    static @Nullable HostedGitInfo fromUrl(String gitUrl) {
        String gitUrlMut = isGitHubShorthand(gitUrl) ? "github:" + gitUrl : gitUrl;
        UrlProtocolPair pair = normalizeProtocol(gitUrlMut);
        WhatwgUrl url = pair.toUrl();
        if (url == null) {
            pair = correctUrl(pair);
            url = pair.toUrl();
            if (url == null) {
                return null;
            }
        }
        Provider hostProvider;
        if (pair.wellFormed != null) {
            hostProvider = pair.wellFormed.hostProvider();
            if (hostProvider == null) {
                hostProvider = Provider.fromUrlDomain(url);
            }
        } else if (pair.custom != null) {
            hostProvider = Provider.fromUrl(url);
        } else {
            hostProvider = Provider.fromUrlDomain(url);
        }
        if (hostProvider == null) {
            return null;
        }
        boolean isShortcut = pair.wellFormed != null && pair.wellFormed.isShortcut();
        if (!isShortcut) {
            return extract(hostProvider, url, pair.defaultRepresentation());
        }

        String pathname = url.getPathname();
        if (pathname.startsWith("/")) {
            pathname = pathname.substring(1);
        }
        int firstAt = pathname.indexOf('@');
        if (firstAt >= 0) {
            pathname = pathname.substring(firstAt + 1);
        }
        String userPart = null;
        String projectPart;
        int lastSlash = pathname.lastIndexOf('/');
        if (lastSlash >= 0) {
            String userStr = pathname.substring(0, lastSlash);
            if (!userStr.isEmpty()) {
                userPart = userStr;
            }
            projectPart = pathname.substring(lastSlash + 1);
        } else {
            projectPart = pathname;
        }
        String fragment = fragmentIdentifier(url);
        String committish = fragment.isEmpty() ? null : fragment;
        try {
            return new HostedGitInfo(hostProvider, Representation.SHORTCUT, userPart == null ? null : decode(userPart),
                    decode(trimGit(projectPart)), committish == null ? null : decode(committish));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String fragmentIdentifier(WhatwgUrl url) {
        String hash = url.getHash();
        return hash.startsWith("#") ? hash.substring(1) : hash;
    }

    private static UrlProtocolPair normalizeProtocol(String npaStr) {
        int firstColonIdx = npaStr.indexOf(':');
        String protoSlice = npaStr.substring(0, firstColonIdx + 1);
        WellDefinedProtocol urlProtocol = WellDefinedProtocol.fromStringWithColon(protoSlice);
        if (urlProtocol != null) {
            String postColon = npaStr.substring(firstColonIdx + 1);
            String rest = postColon.startsWith("//") ? postColon.substring(2) : postColon;
            return new UrlProtocolPair(rest, urlProtocol, null);
        }
        int firstAtIdx = npaStr.indexOf('@');
        if (firstAtIdx >= 0) {
            if (firstColonIdx != -1) {
                if (firstAtIdx > firstColonIdx) {
                    return new UrlProtocolPair(npaStr, WellDefinedProtocol.GIT_PLUS_SSH, null);
                }
                return new UrlProtocolPair(npaStr, null, null);
            }
            return new UrlProtocolPair(npaStr, WellDefinedProtocol.GIT_PLUS_SSH, null);
        }
        int dupSlashIdx = npaStr.indexOf("//");
        if (dupSlashIdx >= 0 && dupSlashIdx == firstColonIdx + 1) {
            return new UrlProtocolPair(npaStr.substring(dupSlashIdx + 2), null, npaStr.substring(0, dupSlashIdx));
        }
        if (firstColonIdx != -1) {
            return new UrlProtocolPair(npaStr.substring(firstColonIdx + 1), null,
                    npaStr.substring(0, firstColonIdx + 1));
        }
        return new UrlProtocolPair(npaStr, null, null);
    }

    private static UrlProtocolPair correctUrl(UrlProtocolPair pair) {
        String url = pair.url;
        int hash = url.indexOf('#');
        String beforeHash = hash >= 0 ? url.substring(0, hash) : url;
        int atIdx = beforeHash.lastIndexOf('@');
        int colIdx = beforeHash.lastIndexOf(':');
        if (colIdx > atIdx) {
            return new UrlProtocolPair(url.substring(0, colIdx) + "/" + url.substring(colIdx + 1),
                    WellDefinedProtocol.GIT_PLUS_SSH, null);
        }
        if (colIdx == -1 && pair.isUnknown()) {
            return new UrlProtocolPair(url, WellDefinedProtocol.GIT_PLUS_SSH, null);
        }
        return pair;
    }

    private static @Nullable HostedGitInfo extract(Provider provider, WhatwgUrl url, Representation representation) {
        String pathname = url.getPathname();
        if (pathname.startsWith("/")) {
            pathname = pathname.substring(1);
        }
        String fragment = fragmentIdentifier(url);
        String user;
        String project;
        String committish;
        switch (provider) {
            case GITHUB: {
                String[] parts = pathname.split("/", -1);
                if (parts.length < 2) {
                    return null;
                }
                user = parts[0];
                project = trimGit(parts[1]);
                String typePart = parts.length > 2 ? parts[2] : null;
                String committishPart = parts.length > 3 ? parts[3] : null;
                if (user.isEmpty() || project.isEmpty()) {
                    return null;
                }
                if (typePart != null && !"tree".equals(typePart)) {
                    return null;
                }
                committish = typePart == null ? (fragment.isEmpty() ? null : fragment) : committishPart;
                break;
            }
            case BITBUCKET: {
                String[] parts = pathname.split("/", -1);
                if (parts.length < 2) {
                    return null;
                }
                if (parts.length > 2 && "get".equals(parts[2])) {
                    return null;
                }
                user = parts[0];
                project = trimGit(parts[1]);
                if (user.isEmpty() || project.isEmpty()) {
                    return null;
                }
                committish = fragment.isEmpty() ? null : fragment;
                break;
            }
            case GITLAB: {
                if (pathname.contains("/-/") || pathname.contains("/archive.tar.gz")) {
                    return null;
                }
                int endSlash = pathname.lastIndexOf('/');
                if (endSlash < 0) {
                    return null;
                }
                user = pathname.substring(0, endSlash);
                project = trimGit(pathname.substring(endSlash + 1));
                if (user.isEmpty() || project.isEmpty()) {
                    return null;
                }
                committish = fragment.isEmpty() ? null : fragment;
                break;
            }
            case GIST: {
                String[] parts = pathname.split("/", -1);
                String userPart = parts[0];
                String projectPart = parts.length > 1 ? parts[1] : null;
                if (parts.length > 2 && "raw".equals(parts[2])) {
                    return null;
                }
                if (projectPart == null || projectPart.isEmpty()) {
                    projectPart = userPart;
                    userPart = "";
                }
                project = trimGit(projectPart);
                user = userPart.isEmpty() ? null : userPart;
                if (project.isEmpty()) {
                    return null;
                }
                committish = fragment.isEmpty() ? null : fragment;
                break;
            }
            default: {
                String[] parts = pathname.split("/", -1);
                if (parts.length < 2) {
                    return null;
                }
                if (parts.length > 2 && "archive".equals(parts[2])) {
                    return null;
                }
                user = parts[0];
                project = trimGit(parts[1]);
                if (user.isEmpty() || project.isEmpty()) {
                    return null;
                }
                committish = fragment.isEmpty() ? null : fragment;
                break;
            }
        }
        try {
            return new HostedGitInfo(provider, representation, user == null ? null : decode(user), decode(project),
                    committish == null ? null : decode(committish));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trimGit(String s) {
        return s.endsWith(".git") ? s.substring(0, s.length() - 4) : s;
    }

    // url.zig PercentEncoding.decode
    private static String decode(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '%') {
                if (i + 2 >= bytes.length) {
                    throw new IllegalArgumentException("DecodingError");
                }
                int hi = Character.digit(bytes[i + 1], 16);
                int lo = Character.digit(bytes[i + 2], 16);
                if (hi < 0 || lo < 0) {
                    throw new IllegalArgumentException("DecodingError");
                }
                out.write(hi << 4 | lo);
                i += 2;
            } else {
                out.write(bytes[i]);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
