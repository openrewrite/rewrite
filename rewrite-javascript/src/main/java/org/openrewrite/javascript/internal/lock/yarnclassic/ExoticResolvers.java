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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of yarn src/resolvers/index.js {@code getExoticResolver} with each exotic resolver's {@code isVersion}
 * (resolvers/exotics, yarn 1.22.22), and of exotics/registry-resolver.js's fragment parsing for {@code npm:} and
 * {@code yarn:} aliases.
 */
final class ExoticResolvers {

    /**
     * The exotic resolvers in the order yarn tries them.
     */
    enum Kind {
        GIT, TARBALL, GITHUB, FILE, LINK, GITLAB, GIST, BITBUCKET, NPM, YARN
    }

    private static final List<String> GIT_HOSTS = Arrays.asList("github.com", "gitlab.com", "bitbucket.com",
            "bitbucket.org");
    private static final List<Pattern> GIT_PATTERN_MATCHERS = Arrays.asList(
            Pattern.compile("^git:"), Pattern.compile("^git\\+.+:"), Pattern.compile("^ssh:"),
            Pattern.compile("^https?:.+\\.git$"), Pattern.compile("^https?:.+\\.git#.+"));
    private static final Pattern GITHUB_SHORTHAND = Pattern.compile(
            "^[^:@%/" + Js.WHITESPACE_CLASS + ".-][^:@%/" + Js.WHITESPACE_CLASS + "]*[/][^:@" + Js.WHITESPACE_CLASS +
            "/%]+(?:#.*)?$");
    private static final Pattern FILE_PREFIX = Pattern.compile("^\\.{1,2}/");
    private static final Pattern REGISTRY_FRAGMENT = Pattern.compile("^([^" + Js.WHITESPACE_CLASS +
                                                                     "]+):(@?.*?)(@(.*?)|)$");

    private ExoticResolvers() {
    }

    static @Nullable Kind getExoticResolver(String pattern) {
        for (Kind kind : Kind.values()) {
            if (isVersion(kind, pattern)) {
                return kind;
            }
        }
        return null;
    }

    static boolean isVersion(Kind kind, String pattern) {
        switch (kind) {
            case GIT:
                return isGit(pattern);
            case TARBALL:
                if (isGit(pattern)) {
                    return false;
                }
                if (pattern.startsWith("http://") || pattern.startsWith("https://")) {
                    return true;
                }
                return pattern.indexOf('@') < 0 && (pattern.endsWith(".tgz") || pattern.endsWith(".tar.gz"));
            case GITHUB:
                return pattern.startsWith("github:") || GITHUB_SHORTHAND.matcher(pattern).find();
            case FILE:
                return pattern.startsWith("file:") || FILE_PREFIX.matcher(pattern).find() || pattern.startsWith("/");
            case LINK:
                return pattern.startsWith("link:");
            case GITLAB:
                return pattern.startsWith("gitlab:");
            case GIST:
                return pattern.startsWith("gist:");
            case BITBUCKET:
                return pattern.startsWith("bitbucket:");
            case NPM:
                return pattern.startsWith("npm:");
            case YARN:
            default:
                return pattern.startsWith("yarn:");
        }
    }

    private static boolean isGit(String pattern) {
        for (Pattern matcher : GIT_PATTERN_MATCHERS) {
            if (matcher.matcher(pattern).find()) {
                return true;
            }
        }
        LegacyUrl url = LegacyUrl.parse(pattern);
        if (url.hostname != null && url.path != null && GIT_HOSTS.contains(url.hostname)) {
            int segments = 0;
            for (String p : url.path.split("/", -1)) {
                if (!p.isEmpty()) {
                    segments++;
                }
            }
            return segments == 2;
        }
        return false;
    }

    /**
     * An {@code npm:} or {@code yarn:} alias: the registry, the real name and its range.
     */
    @Value
    static class RegistryFragment {
        String registry;
        String name;
        String range;
    }

    static RegistryFragment parseRegistryFragment(String fragment) {
        Matcher match = REGISTRY_FRAGMENT.matcher(fragment);
        if (!match.find()) {
            throw new MessageError(Lang.format("Invalid fragment $0.", fragment));
        }
        String range = match.group(4) == null || match.group(4).isEmpty() ? "latest" : match.group(4);
        return new RegistryFragment(fragment.startsWith("npm:") ? "npm" : "yarn", match.group(2), range);
    }

    /**
     * Node's legacy {@code url.parse}, as far as yarn reads it here: the host name and the path.
     */
    static final class LegacyUrl {
        private static final Pattern PROTOCOL = Pattern.compile("^([a-z0-9.+-]+:)", Pattern.CASE_INSENSITIVE);
        private static final Pattern AUTH_AFTER_SLASHES = Pattern.compile("^//[^@/]+@[^@/]+");
        private static final String HOST_ENDING = "/?#";
        private static final String NON_HOST = "%/?;#'{}|\\^`<>\" \r\n\t";

        @Nullable
        String protocol;
        @Nullable
        String hostname;
        @Nullable
        String path;

        static LegacyUrl parse(String input) {
            LegacyUrl url = new LegacyUrl();
            String rest = Js.trim(input);
            Matcher proto = PROTOCOL.matcher(rest);
            if (proto.find()) {
                url.protocol = proto.group(1).toLowerCase(Locale.ROOT);
                rest = rest.substring(proto.group(1).length());
            }
            boolean slashes = false;
            if (url.protocol != null || AUTH_AFTER_SLASHES.matcher(rest).find()) {
                slashes = rest.startsWith("//");
                if (slashes && !"javascript:".equals(url.protocol)) {
                    rest = rest.substring(2);
                }
            }
            if (!"javascript:".equals(url.protocol) &&
                (slashes || (url.protocol != null && !isSlashedProtocol(url.protocol)))) {
                int hostEnd = indexOfAny(rest, HOST_ENDING);
                int at = hostEnd == -1 ? rest.lastIndexOf('@') : rest.lastIndexOf('@', hostEnd);
                if (at != -1) {
                    rest = rest.substring(at + 1);
                }
                hostEnd = indexOfAny(rest, NON_HOST);
                String host = hostEnd == -1 ? rest : rest.substring(0, hostEnd);
                rest = hostEnd == -1 ? "" : rest.substring(hostEnd);
                int colon = host.lastIndexOf(':');
                if (colon >= 0 && host.substring(colon + 1).matches("[0-9]*")) {
                    host = host.substring(0, colon);
                }
                url.hostname = host.toLowerCase(Locale.ROOT);
            }
            int hash = rest.indexOf('#');
            if (hash >= 0) {
                rest = rest.substring(0, hash);
            }
            if (!rest.isEmpty()) {
                url.path = rest;
            } else if (url.hostname != null && isSlashedProtocol(url.protocol)) {
                url.path = "/";
            }
            return url;
        }

        private static int indexOfAny(String s, String chars) {
            for (int i = 0; i < s.length(); i++) {
                if (chars.indexOf(s.charAt(i)) >= 0) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean isSlashedProtocol(@Nullable String protocol) {
            return protocol != null && Arrays.asList("http:", "https:", "ftp:", "gopher:", "file:", "ws:", "wss:")
                    .contains(protocol);
        }
    }
}
