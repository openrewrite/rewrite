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
package org.openrewrite.javascript.internal.lock;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.EcmaScript.isWhiteSpace;
import static org.openrewrite.javascript.internal.lock.EcmaScript.truthy;
import static org.openrewrite.javascript.internal.lock.UriComponent.decodeURIComponent;
import static org.openrewrite.javascript.internal.lock.UriComponent.encodeURIComponent;

/**
 * Port of hosted-git-info ({@code fromUrl} and the URL forms npm-package-arg writes): 9.0.3, which npm 11.17.0
 * bundles, 9.0.2, which npm 11.6.2 to 11.14.1 bundle and whose answers 8.0.2 to 9.0.0 (npm 11.0.0 to 11.6.1) give
 * too, 4.1.0, which pnpm 11.2.2's npm-package-arg fork calls and whose answers 5.0.0 and 5.1.0 give too (npm 8.3.1
 * to 8.19.2), 4.0.2 (npm 7.7.0 to 8.3.0), 4.1.0 without the sourcehut host, and 3.0.8 (npm 7.5.0 to 7.6.3) and 3.0.7
 * (npm 7.0.1 to 7.4.3), which read a URL with Node's legacy {@code url.parse} and an scp-style string with a pattern
 * of their own, and fill string templates.
 */
public final class GitHost {

    public enum Release {
        V3_0_7,
        V3_0_8,
        V4_0_2,
        V4_1_0,
        V9_0_2,
        V9_0_3
    }

    /**
     * The options hosted-git-info's templates read; an unset option defers to the host's own.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Opts {
        public static final Opts NONE = new Opts(null, null);

        private final @Nullable Boolean noGitPlus;
        private final @Nullable Boolean noCommittish;

        public static Opts of(@Nullable Boolean noGitPlus, @Nullable Boolean noCommittish) {
            return new Opts(noGitPlus, noCommittish);
        }

        Opts merge(Opts over) {
            return new Opts(over.noGitPlus != null ? over.noGitPlus : noGitPlus,
                    over.noCommittish != null ? over.noCommittish : noCommittish);
        }
    }

    @RequiredArgsConstructor
    private static final class Segments {
        final @Nullable String user;
        final String project;
        final @Nullable String committish;
    }

    @RequiredArgsConstructor
    private static final class Fill {
        final String type;
        final String domain;
        final @Nullable String user;
        final @Nullable String auth;
        final String project;
        final @Nullable String committish;
    }

    @RequiredArgsConstructor
    private static final class HostInfo {
        final List<String> protocols;
        final String domain;
        final Function<WhatwgUrl, @Nullable Segments> extract;
        final Map<String, Function<Fill, String>> templates;
    }

    private static final Map<String, Function<Fill, String>> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, HostInfo> HOSTS = new LinkedHashMap<>();
    private static final Map<String, String> BY_SHORTCUT = new LinkedHashMap<>();
    private static final Map<String, String> BY_DOMAIN = new LinkedHashMap<>();
    private static final Map<String, @Nullable String> PROTOCOL_NAMES = new LinkedHashMap<>();
    private static final List<String> AUTH_PROTOCOLS =
            Arrays.asList("git+https:", "git:", "http:", "https:", "git+http:");
    private static final List<String> KNOWN_PROTOCOLS_4_1_0 =
            Arrays.asList("http:", "https:", "git:", "git+ssh:", "git+https:", "ssh:");
    private static final Function<Fill, String> SOURCEHUT_HTTPS_DOT_GIT = t ->
            "https://" + t.domain + "/" + t.user + "/" + t.project + ".git" + maybeJoin("#", t.committish);

    static {
        PROTOCOL_NAMES.put("git+ssh:", "sshurl");
        PROTOCOL_NAMES.put("ssh:", "sshurl");
        PROTOCOL_NAMES.put("git+https:", "https");
        PROTOCOL_NAMES.put("git:", null);
        PROTOCOL_NAMES.put("http:", null);
        PROTOCOL_NAMES.put("https:", null);
        PROTOCOL_NAMES.put("git+http:", null);

        DEFAULTS.put("ssh", t ->
                "git@" + t.domain + ":" + t.user + "/" + t.project + ".git" + maybeJoin("#", t.committish));
        DEFAULTS.put("sshurl", t -> "git+ssh://git@" + t.domain + "/" + t.user + "/" + t.project + ".git" +
                                    maybeJoin("#", t.committish));
        DEFAULTS.put("https", t -> "git+https://" + maybeJoin(t.auth, "@") + t.domain + "/" + t.user + "/" + t.project +
                                   ".git" + maybeJoin("#", t.committish));
        DEFAULTS.put("shortcut", t -> t.type + ":" + t.user + "/" + t.project + maybeJoin("#", t.committish));
        DEFAULTS.put("path", t -> t.user + "/" + t.project + maybeJoin("#", t.committish));

        Map<String, Function<Fill, String>> github = new LinkedHashMap<>();
        github.put("git", t -> "git://" + maybeJoin(t.auth, "@") + t.domain + "/" + t.user + "/" + t.project + ".git" +
                               maybeJoin("#", t.committish));
        List<String> githubProtocols = Arrays.asList("git:", "http:", "git+ssh:", "git+https:", "ssh:", "https:");
        addHost("github", githubProtocols, "github.com", url -> {
            String[] parts = split(url.getPathname(), 5);
            String user = part(parts, 1);
            String project = part(parts, 2);
            String type = part(parts, 3);
            String committish = part(parts, 4);
            if (truthy(type) && !"tree".equals(type)) {
                return null;
            }
            if (!truthy(type)) {
                committish = hashOf(url);
            }
            if (project != null && project.endsWith(".git")) {
                project = project.substring(0, project.length() - 4);
            }
            if (!truthy(user) || !truthy(project)) {
                return null;
            }
            return new Segments(user, project, committish);
        }, github);

        addHost("bitbucket", Arrays.asList("git+ssh:", "git+https:", "ssh:", "https:"), "bitbucket.org", url -> {
            String[] parts = split(url.getPathname(), 4);
            String user = part(parts, 1);
            String project = part(parts, 2);
            if ("get".equals(part(parts, 3))) {
                return null;
            }
            if (project != null && project.endsWith(".git")) {
                project = project.substring(0, project.length() - 4);
            }
            if (!truthy(user) || !truthy(project)) {
                return null;
            }
            return new Segments(user, project, hashOf(url));
        }, Collections.emptyMap());

        addHost("gitlab", Arrays.asList("git+ssh:", "git+https:", "ssh:", "https:"), "gitlab.com", url -> {
            String path = url.getPathname().substring(1);
            if (path.contains("/-/") || path.contains("/archive.tar.gz")) {
                return null;
            }
            List<String> segments = new ArrayList<>(Arrays.asList(path.split("/", -1)));
            String project = segments.remove(segments.size() - 1);
            if (project.endsWith(".git")) {
                project = project.substring(0, project.length() - 4);
            }
            String user = String.join("/", segments);
            if (user.isEmpty() || project.isEmpty()) {
                return null;
            }
            return new Segments(user, project, hashOf(url));
        }, Collections.emptyMap());

        Map<String, Function<Fill, String>> gist = new LinkedHashMap<>();
        gist.put("ssh", t -> "git@" + t.domain + ":" + t.project + ".git" + maybeJoin("#", t.committish));
        gist.put("sshurl", t -> "git+ssh://git@" + t.domain + "/" + t.project + ".git" + maybeJoin("#", t.committish));
        gist.put("https", t -> "git+https://" + t.domain + "/" + t.project + ".git" + maybeJoin("#", t.committish));
        gist.put("shortcut", t -> t.type + ":" + t.project + maybeJoin("#", t.committish));
        gist.put("path", t -> t.project + maybeJoin("#", t.committish));
        gist.put("git", t -> "git://" + t.domain + "/" + t.project + ".git" + maybeJoin("#", t.committish));
        addHost("gist", Arrays.asList("git:", "git+ssh:", "git+https:", "ssh:", "https:"), "gist.github.com", url -> {
            String[] parts = split(url.getPathname(), 4);
            String user = part(parts, 1);
            String project = part(parts, 2);
            if ("raw".equals(part(parts, 3))) {
                return null;
            }
            if (!truthy(project)) {
                if (!truthy(user)) {
                    return null;
                }
                project = user;
                user = null;
            }
            if (project.endsWith(".git")) {
                project = project.substring(0, project.length() - 4);
            }
            return new Segments(user, project, hashOf(url));
        }, gist);

        Map<String, Function<Fill, String>> sourcehut = new LinkedHashMap<>();
        sourcehut.put("https", t ->
                "https://" + t.domain + "/" + t.user + "/" + t.project + maybeJoin("#", t.committish));
        addHost("sourcehut", Arrays.asList("git+ssh:", "https:"), "git.sr.ht", url -> {
            String[] parts = split(url.getPathname(), 4);
            String user = part(parts, 1);
            String project = part(parts, 2);
            if ("archive".equals(part(parts, 3))) {
                return null;
            }
            if (project != null && project.endsWith(".git")) {
                project = project.substring(0, project.length() - 4);
            }
            if (!truthy(user) || !truthy(project)) {
                return null;
            }
            return new Segments(user, project, hashOf(url));
        }, sourcehut);
    }

    private static void addHost(String name, List<String> protocols, String domain,
                                Function<WhatwgUrl, @Nullable Segments> extract,
                                Map<String, Function<Fill, String>> overrides) {
        Map<String, Function<Fill, String>> templates = new LinkedHashMap<>(DEFAULTS);
        templates.putAll(overrides);
        HOSTS.put(name, new HostInfo(protocols, domain, extract, templates));
        BY_DOMAIN.put(domain, name);
        BY_SHORTCUT.put(name + ":", name);
        PROTOCOL_NAMES.put(name + ":", name);
    }

    @Getter
    private final String type;

    @Getter
    private final @Nullable String user;

    @Getter
    private final @Nullable String auth;

    @Getter
    private final String project;

    @Getter
    private final @Nullable String committish;

    private final @Nullable String defaultRepresentation;
    private final Opts opts;
    private final HostInfo info;
    private final Release release;

    private GitHost(String type, @Nullable String user, @Nullable String auth, String project,
                    @Nullable String committish, @Nullable String defaultRepresentation, Opts opts, Release release) {
        this.type = type;
        this.user = user;
        this.auth = auth;
        this.project = project;
        this.committish = committish;
        this.defaultRepresentation = defaultRepresentation;
        this.opts = opts;
        this.info = HOSTS.get(type);
        this.release = release;
    }

    public String getDomain() {
        return info.domain;
    }

    public static @Nullable GitHost fromUrl(@Nullable String giturl) {
        return fromUrl(giturl, Opts.NONE);
    }

    public static @Nullable GitHost fromUrl(@Nullable String giturl, Opts opts) {
        return fromUrl(giturl, opts, Release.V9_0_3);
    }

    /**
     * @throws JsError where hosted-git-info 3 throws: the {@code URIError} or {@code ERR_INVALID_URL} of
     *                 {@code url.parse}, or the {@code ERR_INVALID_URL} of the WHATWG URL it reads an auth with
     */
    public static @Nullable GitHost fromUrl(@Nullable String giturl, Opts opts, Release release) {
        if (giturl == null || giturl.isEmpty()) {
            return null;
        }
        if (release == Release.V3_0_7 || release == Release.V3_0_8) {
            return fromUrl3(giturl, opts, release);
        }
        WhatwgUrl parsed;
        if (release == Release.V4_1_0 || release == Release.V4_0_2) {
            String url = isGitHubShorthand(giturl) ? "github:" + giturl : correctProtocol410(giturl, release);
            parsed = WhatwgUrl.tryParse(url);
            if (parsed == null) {
                String corrected = correctUrl410(url);
                parsed = corrected == null ? null : WhatwgUrl.tryParse(corrected);
            }
        } else {
            parsed = parseUrl(isGitHubShorthand(giturl) ? "github:" + giturl : giturl, true);
        }
        if (parsed == null) {
            return null;
        }
        String gitHostShortcut = knownHost(BY_SHORTCUT.get(parsed.getProtocol()), release);
        String hostname = parsed.getHostname();
        String gitHostDomain = knownHost(BY_DOMAIN.get(hostname.startsWith("www.") ? hostname.substring(4) :
                hostname), release);
        String gitHostName = gitHostShortcut != null ? gitHostShortcut : gitHostDomain;
        if (gitHostName == null) {
            return null;
        }
        HostInfo gitHostInfo = HOSTS.get(gitHostName);
        String auth = null;
        if (AUTH_PROTOCOLS.contains(parsed.getProtocol()) &&
            (!parsed.getUsername().isEmpty() || !parsed.getPassword().isEmpty())) {
            auth = parsed.getUsername() + (parsed.getPassword().isEmpty() ? "" : ":" + parsed.getPassword());
        }

        String committish = null;
        String user = null;
        String project;
        String defaultRepresentation;
        try {
            if (gitHostShortcut != null) {
                String pathname = parsed.getPathname().startsWith("/") ?
                        parsed.getPathname().substring(1) : parsed.getPathname();
                int firstAt = pathname.indexOf('@');
                if (firstAt > -1) {
                    pathname = pathname.substring(firstAt + 1);
                }
                int lastSlash = pathname.lastIndexOf('/');
                if (lastSlash > -1) {
                    user = decodeURIComponent(pathname.substring(0, lastSlash));
                    if (user.isEmpty()) {
                        user = null;
                    }
                    project = decodeURIComponent(pathname.substring(lastSlash + 1));
                } else {
                    project = decodeURIComponent(pathname);
                }
                if (project.endsWith(".git")) {
                    project = project.substring(0, project.length() - 4);
                }
                if (!parsed.getHash().isEmpty()) {
                    committish = decodeURIComponent(parsed.getHash().substring(1));
                }
                defaultRepresentation = "shortcut";
            } else {
                if (!gitHostInfo.protocols.contains(parsed.getProtocol())) {
                    return null;
                }
                Segments segments = gitHostInfo.extract.apply(parsed);
                if (segments == null) {
                    return null;
                }
                user = segments.user == null || segments.user.isEmpty() ?
                        segments.user : decodeURIComponent(segments.user);
                project = decodeURIComponent(segments.project);
                committish = segments.committish == null ? "undefined" : decodeURIComponent(segments.committish);
                String name = PROTOCOL_NAMES.get(parsed.getProtocol());
                defaultRepresentation = name != null ? name :
                        parsed.getProtocol().substring(0, parsed.getProtocol().length() - 1);
            }
        } catch (IllegalArgumentException uriError) {
            return null;
        }
        return new GitHost(gitHostName, user, auth, project, committish, defaultRepresentation, opts, release);
    }

    private static @Nullable String fill(@Nullable Function<Fill, String> template, @Nullable GitHost host, Opts over) {
        if (template == null || host == null) {
            return null;
        }
        Opts options = host.opts.merge(over);
        String committish = Boolean.TRUE.equals(options.noCommittish) ? null : host.committish;
        String result = template.apply(
                new Fill(host.type, host.info.domain, host.user, host.auth, host.project, committish));
        return Boolean.TRUE.equals(options.noGitPlus) && result.startsWith("git+") ? result.substring(4) : result;
    }

    private @Nullable String fill(String representation, Opts over) {
        if (release == Release.V3_0_7 || release == Release.V3_0_8) {
            return fill3(Objects.requireNonNull(HOSTS_3.get(type)).templates.get(representation), over);
        }
        return fill(info.templates.get(representation), this, over);
    }

    public String hash() {
        return truthy(committish) ? "#" + committish : "";
    }

    public @Nullable String ssh(Opts over) {
        return fill("ssh", over);
    }

    public @Nullable String sshurl(Opts over) {
        return fill("sshurl", over);
    }

    // 9.0.3 drops the .git sourcehut's https template ends in
    public @Nullable String https(Opts over) {
        return release != Release.V9_0_3 && "sourcehut".equals(type) ? fill(SOURCEHUT_HTTPS_DOT_GIT, this, over) :
                fill("https", over);
    }

    public @Nullable String git(Opts over) {
        return fill("git", over);
    }

    public @Nullable String shortcut(Opts over) {
        return fill("shortcut", over);
    }

    public @Nullable String path(Opts over) {
        return fill("path", over);
    }

    public @Nullable String getDefaultRepresentation() {
        return defaultRepresentation;
    }

    public @Nullable String toString(Opts over) {
        if (defaultRepresentation != null) {
            switch (defaultRepresentation) {
                case "ssh":
                    return ssh(over);
                case "sshurl":
                    return sshurl(over);
                case "https":
                    return https(over);
                case "git":
                    return git(over);
                case "shortcut":
                    return shortcut(over);
                case "path":
                    return path(over);
                default:
                    break;
            }
        }
        return sshurl(over);
    }

    @Override
    public String toString() {
        return String.valueOf(toString(Opts.NONE));
    }

    // hosted-git-info 3 (git-host-info.js): each host's protocols, treepath, pathmatch and templates, its own over the
    // defaults, in the order fromUrl tries them
    @RequiredArgsConstructor
    private static final class Host3 {
        final List<String> protocols;
        final String domain;
        final @Nullable String treepath;
        final Pattern pathmatch;
        final Map<String, String> templates;
    }

    private static final Map<String, Host3> HOSTS_3 = new LinkedHashMap<>();
    private static final List<String> AUTH_PROTOCOLS_3 =
            Arrays.asList("git:", "https:", "git+https:", "http:", "git+http:");
    private static final Pattern GITHUB_SHORTHAND_3 = V8RegExp.of("^[^:@%/\\s.-][^:@%/\\s]*[/][^:@\\s/%]+(?:#.*)?$");
    private static final Pattern SCP_3 = V8RegExp.of("^([^@]+)@([^:/]+):[/]?((?:[^/]+[/])?[^/]+?)(?:[.]git)?(#.*)?$");
    private static final Pattern AUTH_3 = V8RegExp.of("[^@]+@[^:/]+");
    private static final Pattern SHORTCUT_3_0_7 =
            V8RegExp.of("^([^:]+):(?:(?:[^@:]+(?:[^@]+)?@)?([^/]*))[/](.+?)(?:[.]git)?($|#)");
    private static final Pattern SHORTCUT_3_0_8 = V8RegExp.of("^([^:]+):(?:[^@]+@)?(?:([^/]*)\\/)?([^#]+)");
    private static final Pattern DOT_GIT_3 = V8RegExp.of("\\.git$");
    private static final Pattern WWW_3 = V8RegExp.of("^www[.]");
    private static final Pattern COLON_3 = V8RegExp.of("^:");

    static {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("ssh", "git@{domain}:{user}/{project}.git{#committish}");
        defaults.put("sshurl", "git+ssh://git@{domain}/{user}/{project}.git{#committish}");
        defaults.put("https", "git+https://{auth@}{domain}/{user}/{project}.git{#committish}");
        defaults.put("shortcut", "{type}:{user}/{project}{#committish}");
        defaults.put("path", "{user}/{project}{#committish}");
        Pattern pathmatch = V8RegExp.of("^[/]([^/]+)[/]([^/]+?)(?:[.]git|[/])?$");

        Map<String, String> github = new LinkedHashMap<>(defaults);
        github.put("git", "git://{auth@}{domain}/{user}/{project}.git{#committish}");
        HOSTS_3.put("github", new Host3(Arrays.asList("git", "http", "git+ssh", "git+https", "ssh", "https"),
                "github.com", "tree", pathmatch, github));

        HOSTS_3.put("bitbucket", new Host3(Arrays.asList("git+ssh", "git+https", "ssh", "https"), "bitbucket.org",
                "src", pathmatch, defaults));

        Map<String, String> gitlab = new LinkedHashMap<>(defaults);
        gitlab.put("https", "git+https://{auth@}{domain}/{user}/{projectPath}.git{#committish}");
        HOSTS_3.put("gitlab", new Host3(Arrays.asList("git+ssh", "git+https", "ssh", "https"), "gitlab.com", "tree",
                V8RegExp.of("^\\/([^/]+)\\/((?!.*(\\/-\\/|\\/repository(\\/[^/]+)?\\/archive\\.tar\\.gz)).*?)" +
                            "(?:\\.git|\\/)?$"), gitlab));

        Map<String, String> gist = new LinkedHashMap<>(defaults);
        gist.put("git", "git://{domain}/{project}.git{#committish}");
        gist.put("ssh", "git@{domain}:/{project}.git{#committish}");
        gist.put("sshurl", "git+ssh://git@{domain}/{project}.git{#committish}");
        gist.put("https", "git+https://{domain}/{project}.git{#committish}");
        gist.put("shortcut", "{type}:{project}{#committish}");
        gist.put("path", "{project}{#committish}");
        HOSTS_3.put("gist", new Host3(Arrays.asList("git", "git+ssh", "git+https", "ssh", "https"),
                "gist.github.com", null, V8RegExp.of("^[/](?:([^/]+)[/])?([a-z0-9]{7,})(?:[.]git)?$"), gist));
    }

    // what hosted-git-info 3 reads of a URL: url.parse's, or what its pattern takes from an scp-style string
    @RequiredArgsConstructor
    private static final class Parsed3 {
        final @Nullable String protocol;
        final @Nullable String auth;
        final @Nullable String host;
        final @Nullable String path;
        final @Nullable String hash;
    }

    // hosted-git-info 3's index.js fromUrl
    private static @Nullable GitHost fromUrl3(String giturl, Opts opts, Release release) {
        String url = fixupUnqualifiedGist3(GITHUB_SHORTHAND_3.matcher(giturl).find() ? "github:" + giturl : giturl);
        Parsed3 parsed = parseGitUrl3(url);
        Matcher shortcutMatch = (release == Release.V3_0_7 ? SHORTCUT_3_0_7 : SHORTCUT_3_0_8).matcher(url);
        boolean shortcut = shortcutMatch.find();
        List<GitHost> matches = new ArrayList<>();
        for (Map.Entry<String, Host3> entry : HOSTS_3.entrySet()) {
            String gitHostName = entry.getKey();
            Host3 gitHostInfo = entry.getValue();
            try {
                String auth = null;
                if (truthy(parsed.auth) && AUTH_PROTOCOLS_3.contains(parsed.protocol)) {
                    auth = parsed.auth;
                }
                String committish = truthy(parsed.hash) ?
                        decodeURIComponent(Objects.requireNonNull(parsed.hash).substring(1)) : null;
                String user = null;
                String project;
                String defaultRepresentation;
                if (shortcut && gitHostName.equals(shortcutMatch.group(1))) {
                    String userGroup = shortcutMatch.group(2);
                    user = userGroup == null || userGroup.isEmpty() ? userGroup : decodeURIComponent(userGroup);
                    String projectGroup = shortcutMatch.group(3);
                    project = decodeURIComponent(release == Release.V3_0_7 ? projectGroup :
                            V8RegExp.replace(projectGroup, DOT_GIT_3, false, ""));
                    defaultRepresentation = "shortcut";
                } else {
                    if (truthy(parsed.host) && !gitHostInfo.domain.equals(parsed.host) &&
                        !gitHostInfo.domain.equals(V8RegExp.replace(Objects.requireNonNull(parsed.host), WWW_3, false,
                                ""))) {
                        continue;
                    }
                    if (parsed.protocol == null || !parsed.protocol.endsWith(":") ||
                        !gitHostInfo.protocols.contains(parsed.protocol.substring(0, parsed.protocol.length() - 1))) {
                        continue;
                    }
                    if (!truthy(parsed.path)) {
                        continue;
                    }
                    Matcher matched = gitHostInfo.pathmatch.matcher(Objects.requireNonNull(parsed.path));
                    if (!matched.find()) {
                        continue;
                    }
                    if (matched.group(1) != null) {
                        user = decodeURIComponent(V8RegExp.replace(matched.group(1), COLON_3, false, ""));
                    }
                    project = decodeURIComponent(matched.group(2));
                    defaultRepresentation = protocolToRepresentation3(parsed.protocol);
                }
                matches.add(new GitHost(gitHostName, user, auth, project, committish, defaultRepresentation, opts,
                        release));
            } catch (IllegalArgumentException uriError) {
                // a URIError leaves the host out
            }
        }
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private static String protocolToRepresentation3(String protocol) {
        switch (protocol) {
            case "git+ssh:":
            case "ssh:":
                return "sshurl";
            case "git+https:":
                return "https";
            case "git:":
                return "git";
            default:
                return protocol.substring(0, protocol.length() - 1);
        }
    }

    // necessary for round-tripping gists
    private static String fixupUnqualifiedGist3(String giturl) {
        LegacyUrl parsed = LegacyUrl.parse(giturl);
        if ("gist:".equals(parsed.getProtocol()) && truthy(parsed.getHost()) && !truthy(parsed.getPath())) {
            return parsed.getProtocol() + "/" + parsed.getHost();
        }
        return giturl;
    }

    private static Parsed3 parseGitUrl3(String giturl) {
        Matcher matched = SCP_3.matcher(giturl);
        if (!matched.find()) {
            LegacyUrl legacy = LegacyUrl.parse(giturl);
            String auth = legacy.getAuth();
            if (truthy(auth)) {
                // the legacy parser decodes the auth; the WHATWG one keeps it escaped
                Matcher authmatch = AUTH_3.matcher(giturl);
                if (authmatch.find()) {
                    WhatwgUrl whatwg = WhatwgUrl.tryParse(authmatch.group());
                    if (whatwg == null) {
                        throw new JsError(JsError.TYPE_ERROR, "Invalid URL", "ERR_INVALID_URL");
                    }
                    auth = whatwg.getUsername();
                    if (!whatwg.getPassword().isEmpty()) {
                        auth += ":" + whatwg.getPassword();
                    }
                }
            }
            return new Parsed3(legacy.getProtocol(), auth, legacy.getHost(), legacy.getPath(), legacy.getHash());
        }
        return new Parsed3("git+ssh:", matched.group(1), matched.group(2), "/" + matched.group(3), matched.group(4));
    }

    // git-host.js _fill: each {name} of the template replaced, in the order the variables come, by the host's value,
    // encoded but for the domain, auth@ and #committish, with JavaScript's $ patterns of a replacement string
    private @Nullable String fill3(@Nullable String template, Opts over) {
        if (template == null) {
            return null;
        }
        Opts options = opts.merge(over);
        Host3 host = Objects.requireNonNull(HOSTS_3.get(type));
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("path", "");
        vars.put("type", encodeURIComponent(type));
        vars.put("domain", host.domain);
        if (host.treepath != null) {
            vars.put("treepath", encodeURIComponent(host.treepath));
        }
        if (user != null) {
            vars.put("user", encodeURIComponent(user));
        }
        if (auth != null) {
            vars.put("auth", encodeURIComponent(auth));
        }
        vars.put("project", encodePath(project));
        if (committish != null) {
            vars.put("committish", encodeURIComponent(committish));
        }
        vars.put("auth@", truthy(auth) ? auth + "@" : "");
        vars.put("#fragment", "");
        vars.put("fragment", "");
        vars.put("#path", "");
        vars.put("/path", "");
        vars.put("projectPath", encodePath(project));
        if (Boolean.TRUE.equals(options.noCommittish)) {
            vars.put("#committish", "");
            vars.put("/tree/committish", "");
            vars.put("/committish", "");
            vars.put("committish", "");
        } else {
            String encoded = vars.get("committish");
            vars.put("#committish", truthy(committish) ? "#" + committish : "");
            vars.put("/tree/committish", truthy(encoded) ?
                    "/" + Objects.toString(vars.get("treepath"), "undefined") + "/" + encoded : "");
            vars.put("/committish", truthy(encoded) ? "/" + encoded : "");
            vars.put("committish", truthy(encoded) ? encoded : "master");
        }
        String res = template;
        for (Map.Entry<String, String> v : vars.entrySet()) {
            res = V8RegExp.replace(res, Pattern.compile(Pattern.quote("{" + v.getKey() + "}")), true, v.getValue());
        }
        return Boolean.TRUE.equals(options.noGitPlus) && res.startsWith("git+") ? res.substring(4) : res;
    }

    // path.split('/').map(encodeURIComponent).join('/')
    private static String encodePath(String path) {
        StringBuilder out = new StringBuilder();
        String[] parts = EcmaScript.splitString(path, "/");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append('/');
            }
            out.append(encodeURIComponent(parts[i]));
        }
        return out.toString();
    }

    // from-url.js: github shorthand inputs such as npm/cli.
    static boolean isGitHubShorthand(String arg) {
        int firstHash = arg.indexOf('#');
        int firstSlash = arg.indexOf('/');
        int secondSlash = arg.indexOf('/', firstSlash + 1);
        int firstColon = arg.indexOf(':');
        int firstSpace = -1;
        for (int i = 0; i < arg.length(); i++) {
            if (isWhiteSpace(arg.charAt(i))) {
                firstSpace = i;
                break;
            }
        }
        int firstAt = arg.indexOf('@');

        boolean spaceOnlyAfterHash = firstSpace == -1 || (firstHash > -1 && firstSpace > firstHash);
        boolean atOnlyAfterHash = firstAt == -1 || (firstHash > -1 && firstAt > firstHash);
        boolean colonOnlyAfterHash = firstColon == -1 || (firstHash > -1 && firstColon > firstHash);
        boolean secondSlashOnlyAfterHash = secondSlash == -1 || (firstHash > -1 && secondSlash > firstHash);
        boolean hasSlash = firstSlash > 0;
        boolean doesNotEndWithSlash = firstHash > -1 ?
                firstHash == 0 || arg.charAt(firstHash - 1) != '/' : !arg.endsWith("/");
        boolean doesNotStartWithDot = !arg.startsWith(".");

        return spaceOnlyAfterHash && hasSlash && doesNotEndWithSlash && doesNotStartWithDot &&
               atOnlyAfterHash && colonOnlyAfterHash && secondSlashOnlyAfterHash;
    }

    // parse-url.js
    static @Nullable WhatwgUrl parseUrl(String giturl, boolean withProtocols) {
        String withProtocol = withProtocols ? correctProtocol(giturl) : giturl;
        WhatwgUrl url = WhatwgUrl.tryParse(withProtocol);
        return url != null ? url : WhatwgUrl.tryParse(correctUrl(withProtocol));
    }

    private static String correctProtocol(String arg) {
        int firstColon = arg.indexOf(':');
        String proto = arg.substring(0, firstColon + 1);
        if (PROTOCOL_NAMES.containsKey(proto)) {
            return arg;
        }
        if ("://".equals(substr(arg, firstColon, 3))) {
            return arg;
        }
        int firstAt = arg.indexOf('@');
        if (firstAt > -1) {
            if (firstAt > firstColon) {
                return "git+ssh://" + arg;
            }
            return arg;
        }
        return arg.substring(0, firstColon + 1) + "//" + arg.substring(firstColon + 1);
    }

    private static String correctUrl(String giturl) {
        int firstAt = lastIndexOfBefore(giturl, '@', '#');
        int lastColonBeforeHash = lastIndexOfBefore(giturl, ':', '#');
        if (lastColonBeforeHash > firstAt) {
            giturl = giturl.substring(0, lastColonBeforeHash) + "/" + giturl.substring(lastColonBeforeHash + 1);
        }
        if (lastIndexOfBefore(giturl, ':', '#') == -1 && !giturl.contains("//")) {
            giturl = "git+ssh://" + giturl;
        }
        return giturl;
    }

    // a host the release knows: 4.0.2 has no sourcehut
    private static @Nullable String knownHost(@Nullable String name, Release release) {
        return release == Release.V4_0_2 && "sourcehut".equals(name) ? null : name;
    }

    // 4.1.0's correctProtocol, which knows fewer protocols and looks for // only after an @
    private static String correctProtocol410(String arg, Release release) {
        int firstColon = arg.indexOf(':');
        String proto = arg.substring(0, firstColon + 1);
        if (KNOWN_PROTOCOLS_4_1_0.contains(proto) || knownHost(BY_SHORTCUT.get(proto), release) != null) {
            return arg;
        }
        int firstAt = arg.indexOf('@');
        if (firstAt > -1) {
            return firstAt > firstColon ? "git+ssh://" + arg : arg;
        }
        if (arg.indexOf("//") == firstColon + 1) {
            return arg;
        }
        return arg.substring(0, firstColon + 1) + "//" + arg.substring(firstColon + 1);
    }

    // 4.1.0's correctUrl: the first @ and the last : before the last #; null where it leaves the URL undefined
    private static @Nullable String correctUrl410(String giturl) {
        int firstAt = giturl.indexOf('@');
        int lastHash = giturl.lastIndexOf('#');
        int firstColon = giturl.indexOf(':');
        int lastColon = giturl.lastIndexOf(':', lastHash > -1 ? lastHash : giturl.length());
        String corrected = null;
        if (lastColon > firstAt) {
            corrected = giturl.substring(0, lastColon) + "/" + giturl.substring(lastColon + 1);
            firstColon = corrected.indexOf(':');
        }
        if (firstColon == -1 && !giturl.contains("//")) {
            corrected = "git+ssh://" + (corrected == null ? "undefined" : corrected);
        }
        return corrected;
    }

    private static int lastIndexOfBefore(String str, char c, char beforeChar) {
        int startPosition = str.indexOf(beforeChar);
        return str.lastIndexOf(c, startPosition > -1 ? startPosition : str.length());
    }

    // String.prototype.substr: a negative start counts back from the end.
    private static String substr(String s, int start, int length) {
        int from = start < 0 ? Math.max(s.length() + start, 0) : Math.min(start, s.length());
        return s.substring(from, Math.min(from + length, s.length()));
    }

    private static String maybeJoin(@Nullable String... args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            if (!truthy(arg)) {
                return "";
            }
            sb.append(arg);
        }
        return sb.toString();
    }

    // String.prototype.split with a limit keeps only the first `limit` pieces.
    private static String[] split(String s, int limit) {
        String[] all = EcmaScript.splitString(s, "/");
        return Arrays.copyOf(all, Math.min(all.length, limit));
    }

    private static @Nullable String part(String[] parts, int i) {
        return i < parts.length ? parts[i] : null;
    }

    private static String hashOf(WhatwgUrl url) {
        return url.getHash().isEmpty() ? "" : url.getHash().substring(1);
    }
}
