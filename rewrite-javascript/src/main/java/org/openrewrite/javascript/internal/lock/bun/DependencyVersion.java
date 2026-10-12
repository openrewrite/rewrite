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

/**
 * Port of bun's src/install/dependency.zig {@code Dependency.Version} with {@code parseWithTag} and
 * {@code Tag.infer} (bun 1.3.10), with dependency.rs (1.4.x) beside it: a spec, its tag and the tag's value.
 */
final class DependencyVersion {

    enum Tag {
        UNINITIALIZED, NPM, DIST_TAG, TARBALL, FOLDER, SYMLINK, WORKSPACE, GIT, GITHUB, CATALOG;

        boolean isNPM() {
            return ordinal() < 3;
        }

        static Tag infer(String dependency) {
            return infer(dependency, BunRelease.PORTED);
        }

        /**
         * {@code Tag.infer} as the release infers a spec's kind: before 1.2.14 no {@code catalog:} spec, which then
         * reads as a git one; from 1.4.0 a tarball by its {@code .tar} too, in any case
         * ({@link Dependency#isTarball(String, BunRelease)}).
         */
        static Tag infer(String dependency, BunRelease release) {
            if (dependency.isEmpty()) {
                return DIST_TAG;
            }
            if (startsWithWindowsDriveLetter(dependency) &&
                (dependency.charAt(2) == '/' || dependency.charAt(2) == '\\')) {
                return Dependency.isTarball(dependency, release) ? TARBALL : FOLDER;
            }
            char first = dependency.charAt(0);
            switch (first) {
                case '=':
                case '>':
                case '<':
                case '^':
                case '*':
                case '|':
                    return NPM;
                case '.':
                    return Dependency.isTarball(dependency, release) ? TARBALL : FOLDER;
                case '~':
                    if (dependency.length() > 1 && dependency.charAt(1) == '/') {
                        return Dependency.isTarball(dependency, release) ? TARBALL : FOLDER;
                    }
                    return NPM;
                case '/':
                    return Dependency.isTarball(dependency, release) ? TARBALL : FOLDER;
                case 'f':
                    if (dependency.startsWith("file:")) {
                        return Dependency.isTarball(dependency, release) ? TARBALL : FOLDER;
                    }
                    break;
                case 'c':
                    if (dependency.startsWith("catalog:") && release.parsesCatalogSpecs()) {
                        return CATALOG;
                    }
                    break;
                case 'g':
                    if (dependency.startsWith("git")) {
                        String url = dependency.substring("git".length());
                        if (url.length() > 2) {
                            char c = url.charAt(0);
                            if (c == ':') {
                                if (url.startsWith("://")) {
                                    url = url.substring("://".length());
                                    if (url.startsWith("github.com/") &&
                                        isGitHubShorthand(url.substring("github.com/".length()), release)) {
                                        return GITHUB;
                                    }
                                    return release.readsGitSpecsAsHostedGitInfoDoes() ? hgiToTag(dependency, GIT) :
                                            GIT;
                                }
                            } else if (c == '+') {
                                if (url.startsWith("+ssh:") || url.startsWith("+file:")) {
                                    return GIT;
                                }
                                if (url.startsWith("+http")) {
                                    url = url.substring("+http".length());
                                    boolean isHttp = false;
                                    if (url.length() > 2) {
                                        if (url.charAt(0) == ':' && url.startsWith("://")) {
                                            url = url.substring("://".length());
                                            isHttp = true;
                                        } else if (url.charAt(0) == 's' && url.startsWith("s://")) {
                                            url = url.substring("s://".length());
                                            isHttp = true;
                                        }
                                    }
                                    if (isHttp) {
                                        if (url.startsWith("github.com/") &&
                                            isGitHubShorthand(url.substring("github.com/".length()), release)) {
                                            return GITHUB;
                                        }
                                        return release.readsGitSpecsAsHostedGitInfoDoes() ?
                                                hgiToTag(dependency, GIT) : GIT;
                                    }
                                }
                            } else if (c == 'h') {
                                if (url.startsWith("hub:") &&
                                    isGitHubShorthand(url.substring("hub:".length()), release)) {
                                    return GITHUB;
                                }
                            }
                        }
                    }
                    break;
                case 'h':
                    if (dependency.startsWith("http")) {
                        String url = dependency.substring("http".length());
                        if (url.length() > 2) {
                            if (url.charAt(0) == ':') {
                                if (url.startsWith("://")) {
                                    url = url.substring("://".length());
                                }
                            } else if (url.charAt(0) == 's') {
                                if (url.startsWith("s://")) {
                                    url = url.substring("s://".length());
                                }
                            }
                            if (url.startsWith("github.com/")) {
                                String path = url.substring("github.com/".length());
                                if (Dependency.isGitHubTarballPath(path, release)) {
                                    return TARBALL;
                                }
                                if (isGitHubShorthand(path, release)) {
                                    return GITHUB;
                                }
                            }
                            if (!release.readsGitSpecsAsHostedGitInfoDoes()) {
                                return isGitHost(url) ? GIT : TARBALL;
                            }
                            return hgiToTag(dependency, TARBALL);
                        }
                    }
                    break;
                case 's':
                    if (dependency.startsWith("ssh")) {
                        String url = dependency.substring("ssh".length());
                        if (url.length() > 2) {
                            if (!release.readsGitSpecsAsHostedGitInfoDoes()) {
                                if (url.charAt(0) == ':' && url.startsWith("://")) {
                                    url = url.substring("://".length());
                                }
                                if (url.length() > 4 && url.startsWith("git@")) {
                                    url = url.substring("git@".length());
                                }
                                if (isGitHost(url)) {
                                    return GIT;
                                }
                                break;
                            }
                            return hgiToTag(dependency, GIT);
                        }
                    }
                    break;
                case 'l':
                    if (dependency.startsWith("link:")) {
                        return SYMLINK;
                    }
                    break;
                case 'n':
                    if (dependency.startsWith("npm:") && dependency.length() > "npm:".length()) {
                        int start = "npm:".length() + (dependency.charAt("npm:".length()) == '@' ? 1 : 0);
                        String remain = dependency.substring(start);
                        for (int i = 0; i < remain.length(); i++) {
                            if (remain.charAt(i) == '@') {
                                return infer(remain.substring(i + 1), release);
                            }
                        }
                        return NPM;
                    }
                    break;
                case 'v':
                    if (Dependency.isTarball(dependency, release)) {
                        return TARBALL;
                    }
                    if (isGitHubShorthand(dependency, release)) {
                        return GITHUB;
                    }
                    if (Dependency.isSCPLikePath(dependency)) {
                        return GIT;
                    }
                    if (dependency.length() == 1) {
                        return DIST_TAG;
                    }
                    return dependency.charAt(1) >= '0' && dependency.charAt(1) <= '9' ? NPM : DIST_TAG;
                case 'w':
                    if (dependency.startsWith("workspace:")) {
                        return WORKSPACE;
                    }
                    break;
                case 'x':
                case 'X':
                    if (dependency.length() == 1) {
                        return NPM;
                    }
                    if (dependency.charAt(1) == '.') {
                        return NPM;
                    }
                    break;
                case 'p':
                    if (dependency.startsWith("patch:")) {
                        return NPM;
                    }
                    break;
                default:
                    if (first >= '0' && first <= '9') {
                        return Dependency.isTarball(dependency, release) ? TARBALL : NPM;
                    }
                    break;
            }
            if (Dependency.isTarball(dependency, release)) {
                return TARBALL;
            }
            if (isGitHubShorthand(dependency, release)) {
                return GITHUB;
            }
            if (Dependency.isSCPLikePath(dependency)) {
                return release.readsGitSpecsAsHostedGitInfoDoes() ? hgiToTag(dependency, GIT) : GIT;
            }
            if (dependency.indexOf('|') < 0) {
                return DIST_TAG;
            }
            return NPM;
        }

        private static boolean isGitHubShorthand(String dependency, BunRelease release) {
            return release.readsGitSpecsAsHostedGitInfoDoes() ? HostedGitInfo.isGitHubShorthand(dependency) :
                    Dependency.isGitHubRepoPath(dependency);
        }

        // before 1.3.2: the host before the url's first dot is one of Repository.Hosts
        private static boolean isGitHost(String url) {
            int dot = url.indexOf('.');
            return dot >= 0 && Repository.HOSTS.containsKey(url.substring(0, dot));
        }

        // only a git host's shortcut representation is .github; when no host is recognized the caller's default stands
        private static Tag hgiToTag(String dependency, Tag withoutHost) {
            HostedGitInfo info = HostedGitInfo.fromUrl(dependency);
            if (info == null) {
                return withoutHost;
            }
            return info.hostProvider == HostedGitInfo.Provider.GITHUB &&
                   info.defaultRepresentation == HostedGitInfo.Representation.SHORTCUT ? GITHUB : GIT;
        }

        private static boolean startsWithWindowsDriveLetter(String s) {
            return s.length() > 2 && s.charAt(1) == ':' &&
                   (s.charAt(0) >= 'a' && s.charAt(0) <= 'z' || s.charAt(0) >= 'A' && s.charAt(0) <= 'Z');
        }
    }

    static final class NpmInfo {
        final String name;
        final SemverQuery.Group version;
        final boolean isAlias;

        NpmInfo(String name, SemverQuery.Group version, boolean isAlias) {
            this.name = name;
            this.version = version;
            this.isAlias = isAlias;
        }

        boolean eql(NpmInfo that) {
            return name.equals(that.name) && version.eql(that.version);
        }
    }

    static final class TagInfo {
        final String name;
        final String tag;

        TagInfo(String name, String tag) {
            this.name = name;
            this.tag = tag;
        }
    }

    static final class TarballInfo {
        final boolean remote;
        final String uri;
        final String packageName;

        TarballInfo(boolean remote, String uri, String packageName) {
            this.remote = remote;
            this.uri = uri;
            this.packageName = packageName;
        }

        boolean eql(TarballInfo that) {
            return remote == that.remote && uri.equals(that.uri);
        }
    }

    static final DependencyVersion ZEROED = new DependencyVersion(Tag.UNINITIALIZED, "", null);

    final Tag tag;
    final String literal;

    /**
     * The value of the tag: an {@link NpmInfo}, {@link TagInfo}, {@link TarballInfo}, {@link Repository}, or the
     * string of a folder, symlink, workspace or catalog.
     */
    final @Nullable Object value;

    DependencyVersion(Tag tag, String literal, @Nullable Object value) {
        this.tag = tag;
        this.literal = literal;
        this.value = value;
    }

    NpmInfo npm() {
        return (NpmInfo) value;
    }

    TagInfo distTag() {
        return (TagInfo) value;
    }

    TarballInfo tarball() {
        return (TarballInfo) value;
    }

    Repository repository() {
        return (Repository) value;
    }

    String string() {
        return (String) value;
    }

    /**
     * {@code mapDepToPkg} and the workspace link of {@code parseDependency}: the same literal, now a workspace path.
     */
    DependencyVersion asWorkspace(String path) {
        return new DependencyVersion(Tag.WORKSPACE, literal, path);
    }

    boolean eql(DependencyVersion rhs, BunRelease release) {
        if (tag != rhs.tag) {
            return false;
        }
        switch (tag) {
            case NPM:
                return literal.equals(rhs.literal) || npm().eql(rhs.npm());
            case FOLDER:
            case DIST_TAG:
                return literal.equals(rhs.literal);
            case GIT:
            case GITHUB:
                return repository().eql(rhs.repository());
            case TARBALL:
                return tarball().eql(rhs.tarball());
            case SYMLINK:
            case WORKSPACE:
                return string().equals(rhs.string());
            case CATALOG:
                return !release.comparesCatalogNames() || string().equals(rhs.string());
            default:
                return true;
        }
    }

    static @Nullable DependencyVersion parse(String alias, @Nullable Long aliasHash, String dependency) {
        return parse(alias, aliasHash, dependency, BunRelease.PORTED);
    }

    static @Nullable DependencyVersion parse(String alias, @Nullable Long aliasHash, String dependency,
                                             BunRelease release) {
        String dep = Strings.trimLeft(dependency, " \t\n\r");
        return parseWithTag(alias, aliasHash, dep, Tag.infer(dep, release), dependency, release);
    }

    static @Nullable DependencyVersion parseWithOptionalTag(String alias, @Nullable Long aliasHash, String dependency,
                                                            @Nullable Tag tag, BunRelease release) {
        String dep = Strings.trimLeft(dependency, " \t\n\r");
        return parseWithTag(alias, aliasHash, dep, tag != null ? tag : Tag.infer(dep, release), dependency, release);
    }

    /**
     * {@code literal} is the whole sliced string the spec came from, as bun keeps it; {@code dependency} the
     * spec itself.
     */
    static @Nullable DependencyVersion parseWithTag(String alias, @Nullable Long aliasHash, String dependency, Tag tag,
                                                    String literal, BunRelease release) {
        switch (tag) {
            case NPM: {
                String input = dependency;
                boolean isAlias = false;
                String name = alias;
                if (input.startsWith("npm:")) {
                    isAlias = true;
                    String str = input.substring("npm:".length());
                    int i = !str.isEmpty() && str.charAt(0) == '@' ? 1 : 0;
                    boolean found = false;
                    for (; i < str.length(); i++) {
                        if (str.charAt(i) == '@') {
                            input = str.substring(i + 1);
                            name = str.substring(0, i);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        input = str.substring(i);
                        name = str.substring(0, i);
                    }
                }
                isAlias = isAlias && aliasHash != null;
                if (input.length() > 1 && input.charAt(0) == 'v') {
                    input = input.substring(1);
                }
                return new DependencyVersion(Tag.NPM, literal,
                        new NpmInfo(name, SemverQuery.parse(input, release), isAlias));
            }
            case DIST_TAG: {
                String tagToUse = literal;
                String actual = alias;
                if (dependency.startsWith("npm:") && dependency.length() > "npm:".length()) {
                    int i = "npm:".length();
                    if (dependency.charAt(i) == '@') {
                        i++;
                    }
                    while (i < dependency.length() && dependency.charAt(i) != '@') {
                        i++;
                    }
                    tagToUse = i + 1 <= dependency.length() ?
                            dependency.substring(Math.min(i + 1, dependency.length())) : "";
                    actual = dependency.substring("npm:".length(), i);
                }
                return new DependencyVersion(Tag.DIST_TAG, literal,
                        new TagInfo(actual, tagToUse.isEmpty() ? "latest" : tagToUse));
            }
            case GIT: {
                String input = dependency;
                if (input.startsWith("git+")) {
                    input = input.substring("git+".length());
                }
                int hash = input.lastIndexOf('#');
                String repo = hash >= 0 ? input.substring(0, hash) : input;
                String committish = hash >= 0 ? input.substring(hash + 1) : "";
                return new DependencyVersion(Tag.GIT, literal, new Repository("", repo, committish, "", ""));
            }
            case GITHUB: {
                if (!release.readsGitSpecsAsHostedGitInfoDoes()) {
                    return parseGitHubRepoPath(dependency, literal);
                }
                HostedGitInfo info = HostedGitInfo.fromUrl(dependency);
                if (info == null) {
                    return null;
                }
                String ownerStr = info.user == null ? "" : info.user;
                String committishStr = info.committish == null ? "" : info.committish;
                String owner = dependency.contains(ownerStr) ? ownerStr : "";
                String repo = dependency.contains(info.project) ? info.project : "";
                String committish = !committishStr.isEmpty() && dependency.contains(committishStr) ? committishStr : "";
                return new DependencyVersion(Tag.GITHUB, literal, new Repository(owner, repo, committish, "", ""));
            }
            case TARBALL:
                if (Dependency.isRemoteTarball(dependency)) {
                    return new DependencyVersion(Tag.TARBALL, literal, new TarballInfo(true, dependency, ""));
                } else if (dependency.startsWith("file://")) {
                    String path = dependency.substring("file://".length());
                    return new DependencyVersion(Tag.TARBALL, literal, new TarballInfo(false, path, ""));
                } else if (dependency.startsWith("file:")) {
                    String path = dependency.substring("file:".length());
                    return new DependencyVersion(Tag.TARBALL, literal, new TarballInfo(false, path, ""));
                } else if (dependency.contains("://")) {
                    return null;
                }
                return new DependencyVersion(Tag.TARBALL, literal, new TarballInfo(false, literal, ""));
            case FOLDER: {
                int protocol = dependency.indexOf(':');
                if (protocol >= 0) {
                    if ("file".equals(dependency.substring(0, protocol))) {
                        String folder;
                        if (dependency.length() > protocol + 1 && dependency.charAt(protocol + 1) == '/') {
                            String maybeDotDot;
                            if (dependency.length() > protocol + 2 && dependency.charAt(protocol + 2) == '/') {
                                if (dependency.length() > protocol + 3 && dependency.charAt(protocol + 3) == '/') {
                                    maybeDotDot = dependency.substring(protocol + 4);
                                } else {
                                    maybeDotDot = dependency.substring(protocol + 3);
                                }
                            } else {
                                maybeDotDot = dependency.substring(protocol + 2);
                            }
                            if (maybeDotDot.length() > 1 && maybeDotDot.charAt(0) == '.' &&
                                maybeDotDot.charAt(1) == '.') {
                                return new DependencyVersion(Tag.FOLDER, literal, maybeDotDot);
                            }
                        }
                        folder = dependency.substring(protocol + 1);
                        return new DependencyVersion(Tag.FOLDER, literal, folder);
                    }
                    return null;
                }
                return new DependencyVersion(Tag.FOLDER, literal, literal);
            }
            case UNINITIALIZED:
                // from 1.4.0 a catalog: edge stripped outside the workspaces keeps its literal
                return release.stripsCatalogReferencesOutsideTheWorkspaces() ?
                        new DependencyVersion(Tag.UNINITIALIZED, literal, null) : null;
            case SYMLINK: {
                int colon = dependency.indexOf(':');
                String target = colon >= 0 ? dependency.substring(colon + 1) : literal;
                return new DependencyVersion(Tag.SYMLINK, literal, target);
            }
            case WORKSPACE: {
                String input = dependency;
                if (input.startsWith("workspace:")) {
                    input = input.substring("workspace:".length());
                }
                return new DependencyVersion(Tag.WORKSPACE, literal, input);
            }
            default: {
                String group = dependency.substring("catalog:".length());
                return new DependencyVersion(Tag.CATALOG, literal, Strings.trim(group, Strings.WHITESPACE_CHARS));
            }
        }
    }

    /**
     * A GitHub spec until 1.3.2: without its {@code github:}, {@code git://github.com/} or
     * {@code [git+]http[s]://github.com/}, the owner is what precedes the last slash before any {@code #}, the repo
     * what follows it, without a URL's {@code .git}, and the committish what follows the {@code #}.
     */
    private static DependencyVersion parseGitHubRepoPath(String dependency, String literal) {
        boolean fromUrl = false;
        String input = dependency;
        if (input.startsWith("github:")) {
            input = input.substring("github:".length());
        } else if (input.startsWith("git://github.com/")) {
            input = input.substring("git://github.com/".length());
            fromUrl = true;
        } else {
            if (input.startsWith("git+")) {
                input = input.substring("git+".length());
            }
            if (input.startsWith("http")) {
                String url = input.substring("http".length());
                if (url.length() > 2) {
                    if (url.charAt(0) == ':' && url.startsWith("://")) {
                        url = url.substring("://".length());
                    } else if (url.charAt(0) == 's' && url.startsWith("s://")) {
                        url = url.substring("s://".length());
                    }
                    if (url.startsWith("github.com/")) {
                        input = url.substring("github.com/".length());
                        fromUrl = true;
                    }
                }
            }
        }
        int hashIndex = 0;
        int slashIndex = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '/') {
                slashIndex = i;
            } else if (c == '#') {
                hashIndex = i;
                break;
            }
        }
        String repo = hashIndex == 0 ? input.substring(slashIndex + 1) : input.substring(slashIndex + 1, hashIndex);
        if (fromUrl && repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - ".git".length());
        }
        String committish = hashIndex == 0 ? "" : input.substring(hashIndex + 1);
        return new DependencyVersion(Tag.GITHUB, literal,
                new Repository(input.substring(0, slashIndex), repo, committish, "", ""));
    }

    @Override
    public String toString() {
        return literal;
    }
}
