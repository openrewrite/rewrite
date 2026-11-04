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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.EcmaScript.UNDEFINED;
import static org.openrewrite.javascript.internal.lock.EcmaScript.string;
import static org.openrewrite.javascript.internal.lock.EcmaScript.trim;
import static org.openrewrite.javascript.internal.lock.EcmaScript.truthy;
import static org.openrewrite.javascript.internal.lock.UriComponent.decodeURIComponent;
import static org.openrewrite.javascript.internal.lock.UriComponent.encodeURIComponent;

/**
 * Port of npm-package-arg ({@code npa.js}, its {@code Result}) on POSIX paths: 13.0.2, which npm 11.17.0 bundles, the
 * releases npm 7 to 11 bundled before it, and pnpm 11.2.2's fork of npa 8, {@code @pnpm/npm-package-arg} 2.0.0.
 * {@link #ofDependency} also names other protocols.
 */
@Getter
public final class Spec {

    /**
     * The npm-package-arg whose answers the functions give: 13.0.2, whose answers 13.0.1 gives too (npm 11.6.2 on);
     * 13.0.0, whose answers 12.0.2 gives too (npm 11.2.0 to 11.6.1); 12.0.1 (npm 11.0.0 and 11.1.0); 10.1.0, whose
     * answers 10.0.0 gives too (npm 9); 9.1.0 (npm 8.14.0 to 8.19.4); 9.0.0, whose answers 9.0.1 and 9.0.2 give too
     * (npm 8.5.2 to 8.13.2); 8.1.5 (npm 7.18.0 to 8.5.1); 8.1.4 (npm 7.16.0 and 7.17.0), which reads a file: spec
     * as a path rather than a URL; 8.1.1, whose answers 8.1.2 gives too (npm 7.5.5 to 7.15.1), which also strips
     * the slashes after file: before any dot and trims no registry spec, as 8.1.0 (npm 7.0.0 to 7.5.4) does, which
     * besides reads a whole scp-style git string ({@code npa('git@host:path')}, never a lock's) as a name and a
     * spec; or pnpm's fork.
     */
    public enum Release {
        PNPM_2_0_0,
        V8_1_1,
        V8_1_4,
        V8_1_5,
        V9_0_0,
        V9_1_0,
        V10_1_0,
        V12_0_1,
        V13_0_0,
        V13_0_2
    }

    /**
     * An npm-package-arg with the validate-npm-package-name, hosted-git-info and node-semver it calls, which each npm
     * release bundles in its own combination.
     */
    @Value(staticConstructor = "of")
    public static class Options {
        public static final Options PNPM_2_0_0 = of(Release.PNPM_2_0_0, ValidateNpmPackageName.Release.V4_0_0,
                GitHost.Release.V4_1_0, NodeSemver.Release.V7_8_4);
        public static final Options V13_0_2 = of(Release.V13_0_2, ValidateNpmPackageName.Release.V7_0_2,
                GitHost.Release.V9_0_3, NodeSemver.Release.V7_8_4);

        Release release;
        ValidateNpmPackageName.Release validateNpmPackageName;
        GitHost.Release hostedGitInfo;
        NodeSemver.Release semver;

        /**
         * The release with the libraries of the newest npm that bundles it: 11.17.0, 11.6.1, 11.1.0, 9.9.4, 8.19.4,
         * 8.13.2, 8.5.1, 7.17.0 and 7.15.1.
         */
        public static Options of(Release release) {
            switch (release) {
                case PNPM_2_0_0:
                    return PNPM_2_0_0;
                case V8_1_1:
                case V8_1_4:
                    return of(release, ValidateNpmPackageName.Release.V6_0_0, GitHost.Release.V4_0_2,
                            NodeSemver.Release.V7_7_2);
                case V8_1_5:
                case V9_0_0:
                    return of(release, ValidateNpmPackageName.Release.V6_0_0, GitHost.Release.V4_1_0,
                            NodeSemver.Release.V7_7_2);
                case V9_1_0:
                case V10_1_0:
                    return of(release, ValidateNpmPackageName.Release.V6_0_0, GitHost.Release.V9_0_2,
                            NodeSemver.Release.V7_7_2);
                case V12_0_1:
                    return of(release, ValidateNpmPackageName.Release.V6_0_0, GitHost.Release.V9_0_2,
                            NodeSemver.Release.V7_7_2);
                case V13_0_0:
                    return of(release, ValidateNpmPackageName.Release.V6_0_2, GitHost.Release.V9_0_2,
                            NodeSemver.Release.V7_7_2);
                default:
                    return V13_0_2;
            }
        }
    }

    public enum Type {
        VERSION, RANGE, TAG, ALIAS, GIT, FILE, DIRECTORY, REMOTE,
        CATALOG("catalog:"), WORKSPACE("workspace:"), LINK("link:"), PORTAL("portal:"), PATCH("patch:"), JSR("jsr:"),
        EXEC("exec:"), UNKNOWN_PROTOCOL;

        private final @Nullable String prefix;

        Type() {
            this(null);
        }

        Type(@Nullable String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }

    /**
     * An error npm-package-arg throws; {@code code} is its {@code err.code} when it sets one.
     */
    @Getter
    public static final class InvalidSpecException extends IllegalArgumentException {
        private final @Nullable String code;

        InvalidSpecException(String message, @Nullable String code) {
            super(message);
            this.code = code;
        }

        InvalidSpecException(String message, @Nullable String code, Throwable cause) {
            super(message, cause);
            this.code = code;
        }
    }

    private static final Pattern IS_URL = Pattern.compile("^(?:git[+])?[a-z]+:", Pattern.CASE_INSENSITIVE);
    // npa.js's literals with their i flag, as V8 reads them: a $ that a trailing newline does not satisfy, a . that
    // matches no line terminator
    private static final Pattern IS_GIT = ignoringCase("^[^@]+@[^:.]+\\.[^:]+:.+$");
    private static final Pattern IS_FILE_TYPE = ignoringCase("[.](?:tgz|tar\\.gz|tar)$");
    // before 13.0.1 any character stands between tar and gz
    private static final Pattern IS_FILENAME = ignoringCase("[.](?:tgz|tar.gz|tar)$");
    private static final Pattern IS_PORT_NUMBER = ignoringCase(":[0-9]+(\\/|$)");
    private static final Pattern IS_PORT_NUMBER_PNPM = V8RegExp.of(":[0-9]+\\/?.*$");
    private static final Pattern FILE_PROTOCOL = Pattern.compile("^file:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABSOLUTE_PATH = Pattern.compile("^[/]|^[A-Za-z]:");
    private static final Pattern RELATIVE_AFTER_SLASHES = Pattern.compile("^/{1,3}\\.\\.?(/|$)");
    private static final Pattern IS_POSIX_FILE = Pattern.compile("^(?:[.]|~[/]|[/]|[a-zA-Z]:)");
    private static final Pattern GIT_SSH = ignoringCase("^git\\+ssh:\\/\\/([^:#]+:[^#]+(?:\\.git)?)(?:#(.*))?$");

    // npm-package-arg resolves '~' against os.homedir(); the virtual filesystem's home is its root.
    static final String HOME = PosixPath.CWD;

    private @Nullable Type type;
    private @Nullable Boolean registry;
    private @Nullable String where;
    private String raw;
    private @Nullable String name;
    private @Nullable String escapedName;
    private @Nullable String scope;
    private String rawSpec;
    private @Nullable String saveSpec;
    private @Nullable String fetchSpec;
    private @Nullable String gitRange;
    private @Nullable String gitCommittish;
    private @Nullable String gitSubdir;
    private @Nullable GitHost hosted;
    private @Nullable Spec subSpec;

    /**
     * The fork's {@code alias}: the name an {@code npm:} spec was given, on the result of the spec it names.
     */
    private @Nullable String alias;

    private Spec(@Nullable String raw, @Nullable String name, @Nullable String rawSpec, Options options) {
        this.raw = raw != null ? raw :
                name != null && !name.isEmpty() ? name + "@" + (rawSpec == null ? "undefined" : rawSpec) :
                        rawSpec == null ? "" : rawSpec;
        this.rawSpec = rawSpec == null ? "" : rawSpec;
        if (name != null && !name.isEmpty()) {
            setName(name, options);
        }
    }

    private Spec(Spec copy) {
        this.type = copy.type;
        this.registry = copy.registry;
        this.where = copy.where;
        this.raw = copy.raw;
        this.name = copy.name;
        this.escapedName = copy.escapedName;
        this.scope = copy.scope;
        this.rawSpec = copy.rawSpec;
        this.saveSpec = copy.saveSpec;
        this.fetchSpec = copy.fetchSpec;
        this.gitRange = copy.gitRange;
        this.gitCommittish = copy.gitCommittish;
        this.gitSubdir = copy.gitSubdir;
        this.hosted = copy.hosted;
        this.subSpec = copy.subSpec;
        this.alias = copy.alias;
    }

    public boolean isRegistry() {
        return Boolean.TRUE.equals(registry);
    }

    public static Spec npa(String arg) {
        return npa(arg, null);
    }

    public static Spec npa(Spec arg, @Nullable String where) {
        return npa(arg, where, Options.V13_0_2);
    }

    public static Spec npa(Spec arg, @Nullable String where, Options options) {
        if (where == null || where.isEmpty() || where.equals(arg.where)) {
            return arg;
        }
        if (arg.name != null && !arg.name.isEmpty() && !arg.rawSpec.isEmpty()) {
            return resolve(arg.name, arg.rawSpec, where, options);
        }
        return npa(arg.raw, where, options);
    }

    /**
     * {@code npa(arg, where)} for a value read from package or lock data, which may not be a string.
     * @throws IllegalArgumentException npa's TypeError for a value of another type
     */
    public static Spec npa(@Nullable Object arg, @Nullable String where) {
        return npa(arg, where, Options.V13_0_2);
    }

    public static Spec npa(@Nullable Object arg, @Nullable String where, Release release) {
        return npa(arg, where, Options.of(release));
    }

    public static Spec npa(@Nullable Object arg, @Nullable String where, Options options) {
        Release release = options.release;
        if (arg instanceof Spec) {
            return npa((Spec) arg, where, options);
        }
        if (arg instanceof String) {
            return npa((String) arg, where, options);
        }
        if (arg == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'name')");
        }
        if (arg == UNDEFINED) {
            throw new IllegalArgumentException("Cannot read properties of undefined (reading 'indexOf')");
        }
        if (!(arg instanceof Map) && !(arg instanceof List)) {
            throw new IllegalArgumentException("arg.indexOf is not a function");
        }
        Object name = get(arg, "name");
        Object rawSpec = get(arg, "rawSpec");
        Object argWhere = get(arg, "where");
        String resolvedWhere = truthy(where) ? where : argWhere instanceof String ? (String) argWhere : null;
        if (truthy(name) && truthy(rawSpec)) {
            if (!(name instanceof String)) {
                String pkgid = string(name) + "@" + string(rawSpec);
                throw new InvalidSpecException(release == Release.PNPM_2_0_0 ?
                        "Invalid package name \"" + string(name) + "\": name must be a string" :
                        "Invalid package name \"" + string(name) + "\" of package \"" + pkgid +
                        "\": name must be a string.", "EINVALIDPACKAGENAME");
            }
            if (!(rawSpec instanceof String)) {
                throw new IllegalArgumentException(release == Release.PNPM_2_0_0 ?
                        "spec.startsWith is not a function" : "spec.toLowerCase is not a function");
            }
            return resolve((String) name, (String) rawSpec, resolvedWhere, options);
        }
        return npa(get(arg, "raw"), resolvedWhere, options);
    }

    // arg[key] of the object or array npa was given, for a key no prototype has
    private static @Nullable Object get(Object arg, String key) {
        return arg instanceof Map && ((Map<?, ?>) arg).containsKey(key) ? ((Map<?, ?>) arg).get(key) : UNDEFINED;
    }

    public static Spec npa(String arg, @Nullable String where) {
        return npa(arg, where, Options.V13_0_2);
    }

    public static Spec npa(String arg, @Nullable String where, Release release) {
        return npa(arg, where, Options.of(release));
    }

    public static Spec npa(String arg, @Nullable String where, Options options) {
        Release release = options.release;
        boolean pnpm = release == Release.PNPM_2_0_0;
        String name = null;
        String spec;
        int nameEndsAt = arg.indexOf('@', 1);
        String namePart = nameEndsAt > 0 ? arg.substring(0, nameEndsAt) : arg;
        if (IS_URL.matcher(arg).find()) {
            spec = arg;
        } else if (!pnpm && IS_GIT.matcher(arg).find()) {
            spec = "git+ssh://" + arg;
        } else if (!namePart.startsWith("@") && (namePart.contains("/") || isFileType(namePart, release))) {
            spec = arg;
        } else if (nameEndsAt > 0) {
            name = namePart;
            spec = arg.substring(nameEndsAt + 1);
            if (spec.isEmpty() && !pnpm && !before10(release)) {
                spec = "*";
            }
        } else {
            if (ValidateNpmPackageName.validForOldPackages(arg, options.validateNpmPackageName)) {
                name = arg;
                spec = pnpm || before10(release) ? null : "*";
            } else {
                spec = arg;
            }
        }
        return resolve(name, spec, where, arg, options);
    }

    public static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where) {
        return resolve(name, spec, where, null, Options.V13_0_2);
    }

    public static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where,
                               Release release) {
        return resolve(name, spec, where, null, Options.of(release));
    }

    public static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where,
                               Options options) {
        return resolve(name, spec, where, null, options);
    }

    private static boolean isFileType(String spec, Release release) {
        return (release == Release.V13_0_2 ? IS_FILE_TYPE : IS_FILENAME).matcher(spec).find();
    }

    // npm-package-arg before 10.0.0: a bare name, or a name with an empty spec, is the default tag; a file: URL with
    // one slash before its leading dots alone is relative
    private static boolean before10(Release release) {
        return before8_1_5(release) || release == Release.V8_1_5 || release == Release.V9_0_0 ||
               release == Release.V9_1_0;
    }

    // npm-package-arg before 8.1.5 reads a file: spec as a path, as the fork does
    private static boolean before8_1_5(Release release) {
        return release == Release.V8_1_1 || release == Release.V8_1_4;
    }

    // npm-package-arg 8 and the fork word an invalid name or tag without the package
    private static boolean npa8(Release release) {
        return release == Release.PNPM_2_0_0 || before8_1_5(release) || release == Release.V8_1_5;
    }

    // npm-package-arg before 11.0.0: a file: URL with a host that is not localhost is refused; also before 11.0.1 the
    // URL error names RFC 8909, before 11.0.3 an alias needs no name, and before 12.0.1 a ':' and digits anywhere in
    // an scp-style git URL are a port
    private static boolean before11(Release release) {
        return before10(release) || release == Release.V10_1_0;
    }

    // npm-package-arg before 9.1.0: a git committish is a semver: range or a ref, no :: separated list
    private static boolean before9_1(Release release) {
        return before8_1_5(release) || release == Release.V8_1_5 || release == Release.V9_0_0;
    }

    /**
     * {@link #ofDependency}, which the recipes ask; null for a value npa rejects outright.
     */
    public static @Nullable Spec ofDependencyOrNull(String name, String value) {
        try {
            return ofDependency(name, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * What a manifest's dependency value is: {@code npa.resolve(name, value)}, a pnpm, Yarn or Bun protocol, or a
     * protocol npa does not support; npa's {@link InvalidSpecException} for a value that is none of those.
     */
    public static Spec ofDependency(String name, String value) {
        for (Type type : Type.values()) {
            if (type.prefix != null && value.startsWith(type.prefix)) {
                Spec res = new Spec(null, name, value, Options.V13_0_2);
                res.type = type;
                return res;
            }
        }
        try {
            return resolve(name, value, null);
        } catch (InvalidSpecException e) {
            if (!"EUNSUPPORTEDPROTOCOL".equals(e.getCode())) {
                throw e;
            }
            Spec res = new Spec(null, name, value, Options.V13_0_2);
            res.type = Type.UNKNOWN_PROTOCOL;
            return res;
        }
    }

    /**
     * The protocol the value names or expands to ({@code user/repo} is {@code github:}); null for a version, range or
     * tag of the named package.
     */
    public @Nullable String getProtocol() {
        if (type == null || isVersionSpec()) {
            return null;
        }
        if (type.prefix != null) {
            return type.prefix;
        }
        switch (type) {
            case ALIAS:
                return "npm:";
            case FILE:
            case DIRECTORY:
                return "file:";
            default:
                if (hosted != null) {
                    return hosted.getType() + ":";
                }
                Matcher scheme = IS_URL.matcher(rawSpec);
                return scheme.find() ? scheme.group().toLowerCase(Locale.ROOT) : "git+ssh:";
        }
    }

    /**
     * A version, range or tag of the package the dependency names, which a version upgrade replaces.
     */
    public boolean isVersionSpec() {
        return type == Type.VERSION || type == Type.RANGE || type == Type.TAG;
    }

    /**
     * Where the package comes from rather than which version: a git host or URL, a path, a tarball URL, a link or a
     * portal.
     */
    public boolean isLocation() {
        return type == Type.GIT || type == Type.FILE || type == Type.DIRECTORY || type == Type.REMOTE ||
               type == Type.LINK || type == Type.PORTAL;
    }

    /**
     * The directory a path, {@code file:}, {@code link:} or {@code portal:} dependency names, from its manifest's
     * ({@code ../lib}); null for any other value, and for a path from the filesystem's root or the home directory.
     */
    public @Nullable String getDirectory() {
        String path;
        if (type == Type.DIRECTORY) {
            // fetchSpec is resolved against where, which a path that climbs above it is clamped to
            path = FILE_PROTOCOL.matcher(rawSpec).replaceFirst("");
            if (path.length() < rawSpec.length() && RELATIVE_AFTER_SLASHES.matcher(path).find()) {
                path = path.replaceFirst("^/{1,3}", "");
            }
        } else if (type == Type.LINK || type == Type.PORTAL) {
            path = rawSpec.substring(Objects.requireNonNull(type.prefix).length());
        } else {
            return null;
        }
        return path.isEmpty() || ABSOLUTE_PATH.matcher(path).find() || path.startsWith("~") ? null : path;
    }

    private static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where,
                                @Nullable String arg, Options options) {
        Release release = options.release;
        boolean pnpm = release == Release.PNPM_2_0_0;
        Spec res = new Spec(arg, name, spec, options);
        if (where == null || where.isEmpty()) {
            where = PosixPath.CWD;
        }
        if (pnpm && spec != null && (IS_POSIX_FILE.matcher(spec).find() || FILE_PROTOCOL.matcher(spec).find())) {
            return fromFilePnpm(res, where);
        } else if (pnpm && spec != null && spec.startsWith("npm:")) {
            Spec aliased = npa(spec.substring(4), where, options);
            aliased.alias = name;
            aliased.raw = res.raw;
            aliased.rawSpec = res.rawSpec;
            return aliased;
        } else if (!pnpm && isFileSpec(spec)) {
            return fromFile(res, where, release);
        } else if (!pnpm && isAliasSpec(spec)) {
            return fromAlias(res, where, options);
        }

        GitHost hosted = spec == null ? null : GitHost.fromUrl(spec, GitHost.Opts.of(true, true),
                options.hostedGitInfo);
        if (hosted != null) {
            return fromHostedGit(res, hosted, release);
        } else if (spec != null && !spec.isEmpty() && IS_URL.matcher(spec).find()) {
            return fromURL(res, release);
        } else if (spec != null && !spec.isEmpty() && (spec.contains("/") || isFileType(spec, release))) {
            return pnpm ? fromFilePnpm(res, where) : fromFile(res, where, release);
        }
        return fromRegistry(res, options);
    }

    private static boolean isFileSpec(@Nullable String spec) {
        if (spec == null || spec.isEmpty()) {
            return false;
        }
        if (spec.toLowerCase(Locale.ROOT).startsWith("file:")) {
            return true;
        }
        return IS_POSIX_FILE.matcher(spec).find();
    }

    private static boolean isAliasSpec(@Nullable String spec) {
        if (spec == null || spec.isEmpty()) {
            return false;
        }
        return spec.toLowerCase(Locale.ROOT).startsWith("npm:");
    }

    private void setName(String name, Options options) {
        List<String> errors = ValidateNpmPackageName.errors(name, options.validateNpmPackageName);
        if (!errors.isEmpty()) {
            // npm-package-arg 8 words it as the fork does
            throw new InvalidSpecException(npa8(options.release) ?
                    "Invalid package name \"" + name + "\": " + String.join("; ", errors) :
                    "Invalid package name \"" + name + "\" of package \"" + raw + "\": " + String.join("; ", errors) +
                    ".", "EINVALIDPACKAGENAME");
        }
        this.name = name;
        this.scope = name.charAt(0) == '@' ? name.substring(0, name.indexOf('/')) : null;
        this.escapedName = name.replaceFirst("/", "%2f");
    }

    /**
     * The spec as {@code OverrideSet} reads a key after clearing its name.
     */
    public Spec withoutName() {
        Spec copy = new Spec(this);
        copy.name = "";
        return copy;
    }

    @Override
    public String toString() {
        List<String> full = new ArrayList<>();
        if (name != null && !name.isEmpty()) {
            full.add(name);
        }
        String spec = saveSpec != null && !saveSpec.isEmpty() ? saveSpec :
                fetchSpec != null && !fetchSpec.isEmpty() ? fetchSpec : rawSpec;
        if (!spec.isEmpty()) {
            full.add(spec);
        }
        return !full.isEmpty() ? String.join("@", full) : raw;
    }

    private static void setGitAttrs(Spec res, @Nullable String committish, Release release) {
        if (before9_1(release)) {
            // setGitCommittish
            if (committish != null && committish.startsWith("semver:")) {
                res.gitRange = decodeURIComponent(committish.substring(7));
                res.gitCommittish = null;
            } else {
                res.gitCommittish = committish == null || committish.isEmpty() ? null : committish;
            }
            return;
        }
        if (committish == null || committish.isEmpty()) {
            res.gitCommittish = null;
            return;
        }
        for (String part : committish.split("::", -1)) {
            if (!part.contains(":")) {
                if (truthy(res.gitRange)) {
                    throw new InvalidSpecException("cannot override existing semver range with a committish", null);
                }
                if (truthy(res.gitCommittish)) {
                    throw new InvalidSpecException(
                            "cannot override existing committish with a second committish", null);
                }
                res.gitCommittish = part;
                continue;
            }
            String[] nameValue = part.split(":", -1);
            String key = nameValue[0];
            String value = nameValue[1];
            if ("semver".equals(key)) {
                if (truthy(res.gitCommittish)) {
                    throw new InvalidSpecException("cannot override existing committish with a semver range", null);
                }
                if (truthy(res.gitRange)) {
                    throw new InvalidSpecException(
                            "cannot override existing semver range with a second semver range", null);
                }
                res.gitRange = decodeURIComponent(value);
                continue;
            }
            if ("path".equals(key)) {
                if (truthy(res.gitSubdir)) {
                    throw new InvalidSpecException("cannot override existing path with a second path", null);
                }
                res.gitSubdir = "/" + value;
            }
        }
    }

    private static final String[][] ENCODED_PATH_CHARS = {
            {"\0", "%00"}, {"\t", "%09"}, {"\n", "%0A"}, {"\r", "%0D"}, {" ", "%20"}, {"\"", "%22"},
            {"#", "%23"}, {"%", "%25"}, {"?", "%3F"}, {"[", "%5B"}, {"\\", "%5C"}, {"]", "%5D"},
            {"^", "%5E"}, {"|", "%7C"}, {"~", "%7E"}
    };

    private static String pathToFileURL(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            String c = String.valueOf(str.charAt(i));
            String encoded = c;
            for (String[] pair : ENCODED_PATH_CHARS) {
                if (pair[0].equals(c)) {
                    encoded = pair[1];
                    break;
                }
            }
            result.append(encoded);
        }
        return result.toString().startsWith("file:") ? result.toString() : "file:" + result;
    }

    private static final Pattern LEADING_DOTS = V8RegExp.of("^\\/{1,3}\\.\\.?(\\/|$)");
    private static final Pattern LEADING_DOT = V8RegExp.of("^\\/\\.\\.?(\\/|$)");
    private static final Pattern HOME_PATH = V8RegExp.of("^\\/~(\\/|$)");

    // a regular expression literal of npa.js with its i flag
    private static Pattern ignoringCase(String source) {
        return Pattern.compile(Objects.requireNonNull(V8RegExp.toJava(source)), Pattern.CASE_INSENSITIVE);
    }

    private static Spec fromFile(Spec res, String where, Release release) {
        if (before8_1_5(release)) {
            return fromFilePath(res, where, release);
        }
        if (release == Release.V12_0_1 || before11(release)) {
            return fromFile12(res, where, release);
        }
        res.type = isFileType(res.rawSpec, release) ? Type.FILE : Type.DIRECTORY;
        res.where = where;

        String rawSpec = pathToFileURL(res.rawSpec);
        if (rawSpec.startsWith("file:/")) {
            if (rawSpec.matches("^file://[^/][\\s\\S]*")) {
                rawSpec = "file:/" + rawSpec.substring(5);
            }
            if (LEADING_DOTS.matcher(rawSpec.substring(5)).find()) {
                rawSpec = rawSpec.replaceFirst("^file:/{1,3}", "file:");
            }
        }

        WhatwgUrl resolvedUrl;
        WhatwgUrl specUrl;
        try {
            resolvedUrl = WhatwgUrl.parse(rawSpec, pathToFileURL(PosixPath.resolve(where)) + "/");
            specUrl = WhatwgUrl.parse(rawSpec);
        } catch (IllegalArgumentException originalError) {
            throw new InvalidSpecException("Invalid file: URL, must comply with RFC 8089", null, originalError);
        }

        String specPath = decodeURIComponent(specUrl.getPathname());
        String resolvedPath = decodeURIComponent(resolvedUrl.getPathname());
        if (HOME_PATH.matcher(specPath).find()) {
            res.saveSpec = "file:" + specPath.substring(1);
            resolvedPath = PosixPath.resolve(HOME, specPath.length() > 3 ? specPath.substring(3) : "");
        } else if (!PosixPath.isAbsolute(rawSpec.substring(5))) {
            res.saveSpec = "file:" + PosixPath.relative(where, resolvedPath);
        } else {
            res.saveSpec = "file:" + PosixPath.resolve(resolvedPath);
        }
        res.fetchSpec = PosixPath.resolve(where, resolvedPath);
        res.saveSpec = res.saveSpec.replace("\\", "/");
        if (res.saveSpec.startsWith("file://")) {
            res.saveSpec = "file:/" + res.saveSpec.substring(7);
        }
        return res;
    }

    // 12.0.1's fromFile, which hands the URL parser the spec as written: a '#' or '?' in it ends the path, a '%'
    // begins an escape, and a host that is not localhost is the path's first segment; before 11.0.0 such a host is
    // refused, and before 10.0.0 a URL with one slash before its leading dots alone is relative
    private static Spec fromFile12(Spec res, String where, Release release) {
        res.type = IS_FILENAME.matcher(res.rawSpec).find() ? Type.FILE : Type.DIRECTORY;
        res.where = where;

        String base = "file://" + PosixPath.resolve(where) + "/";
        String rawWithPrefix = (!res.rawSpec.startsWith("file:") ? "file:" : "") + res.rawSpec;
        String rawNoPrefix = rawWithPrefix.replaceFirst("^file:", "");
        WhatwgUrl resolvedUrl;
        WhatwgUrl specUrl;
        try {
            resolvedUrl = WhatwgUrl.parse(rawWithPrefix, base);
            specUrl = WhatwgUrl.parse(rawWithPrefix);
        } catch (IllegalArgumentException originalError) {
            throw new InvalidSpecException("Invalid file: URL, must comply with RFC " +
                                           (before11(release) ? "8909" : "8089"), null, originalError);
        }

        if (!resolvedUrl.getHost().isEmpty() && !"localhost".equals(resolvedUrl.getHost())) {
            String rawSpec = res.rawSpec.replaceFirst("^file://", "file:///");
            resolvedUrl = WhatwgUrl.parse(rawSpec, base);
            specUrl = WhatwgUrl.parse(rawSpec);
            rawNoPrefix = rawSpec.replaceFirst("^file:", "");
        }
        if ((before10(release) ? LEADING_DOT : LEADING_DOTS).matcher(rawNoPrefix).find()) {
            String rawSpec = res.rawSpec.replaceFirst(before10(release) ? "^file:/" : "^file:/{1,3}", "file:");
            resolvedUrl = WhatwgUrl.parse(rawSpec, base);
            specUrl = WhatwgUrl.parse(rawSpec);
            rawNoPrefix = rawSpec.replaceFirst("^file:", "");
        }
        if (before11(release) && !resolvedUrl.getHost().isEmpty() && !"localhost".equals(resolvedUrl.getHost())) {
            throw new InvalidSpecException("Invalid file: URL, must be absolute if // present", null);
        }

        String specPath = decodeURIComponent(specUrl.getPathname());
        String resolvedPath = decodeURIComponent(resolvedUrl.getPathname());
        if (HOME_PATH.matcher(specPath).find()) {
            res.saveSpec = "file:" + specPath.substring(1);
            resolvedPath = PosixPath.resolve(HOME, specPath.length() > 3 ? specPath.substring(3) : "");
        } else if (!PosixPath.isAbsolute(rawNoPrefix)) {
            res.saveSpec = "file:" + PosixPath.relative(where, resolvedPath);
        } else {
            res.saveSpec = "file:" + PosixPath.resolve(resolvedPath);
        }
        res.fetchSpec = PosixPath.resolve(where, resolvedPath);
        return res;
    }

    // the fork's fromFile, npa 8.1.0's: the path as given, resolved against where
    private static Spec fromFilePnpm(Spec res, String where) {
        return fromFilePath(res, where, Release.PNPM_2_0_0);
    }

    // fromFile before 8.1.5, which the fork keeps: the path as given, a '#', '?' or '%' in it a character of the path,
    // resolved against where; 8.1.4 strips the slashes after file: only before ~/, dots and a slash, or a slash, so
    // that file:/.x is absolute
    private static Spec fromFilePath(Spec res, String where, Release release) {
        res.type = IS_FILENAME.matcher(res.rawSpec).find() ? Type.FILE : Type.DIRECTORY;
        res.where = where;
        String spec = res.rawSpec.replace('\\', '/')
                .replaceFirst("^file:[/]*([A-Za-z]:)", "$1")
                .replaceFirst(release == Release.V8_1_4 ? "^file:(?:[/]*(~/|\\.*/|[/]))?" : "^file:(?:[/]*([~./]))?",
                        "$1");
        if (spec.startsWith("~/")) {
            res.fetchSpec = resolvePath(HOME, spec.substring(2));
            res.saveSpec = "file:" + spec;
        } else {
            res.fetchSpec = resolvePath(where, spec);
            res.saveSpec = "file:" + (ABSOLUTE_PATH.matcher(spec).find() ? spec :
                    PosixPath.relative(where, res.fetchSpec));
        }
        return res;
    }

    private static String resolvePath(String where, String spec) {
        return ABSOLUTE_PATH.matcher(spec).find() ? spec : PosixPath.resolve(where, spec);
    }

    private static Spec fromHostedGit(Spec res, GitHost hosted, Release release) {
        res.type = Type.GIT;
        res.hosted = hosted;
        res.saveSpec = hosted.toString(GitHost.Opts.of(false, false));
        res.fetchSpec = "shortcut".equals(hosted.getDefaultRepresentation()) ?
                null : hosted.toString(GitHost.Opts.NONE);
        setGitAttrs(res, hosted.getCommittish(), release);
        return res;
    }

    // before 11.0.1 npm-package-arg reads the URL with url.parse, which this reads with WHATWG URL as later ones do
    private static Spec fromURL(Spec res, Release release) {
        String rawSpec = res.rawSpec;
        res.saveSpec = rawSpec;
        if (rawSpec.startsWith("git+ssh:")) {
            Matcher matched = GIT_SSH.matcher(rawSpec);
            Pattern port = release == Release.PNPM_2_0_0 || before11(release) ? IS_PORT_NUMBER_PNPM : IS_PORT_NUMBER;
            if (matched.matches() && !port.matcher(matched.group(1)).find()) {
                res.type = Type.GIT;
                setGitAttrs(res, matched.group(2), release);
                res.fetchSpec = matched.group(1);
                return res;
            }
        } else if (rawSpec.startsWith("git+file://")) {
            rawSpec = rawSpec.replace('\\', '/');
        }
        WhatwgUrl parsedUrl;
        try {
            parsedUrl = WhatwgUrl.parse(rawSpec);
        } catch (IllegalArgumentException e) {
            throw new InvalidSpecException("Invalid URL", "ERR_INVALID_URL", e);
        }
        switch (parsedUrl.getProtocol()) {
            case "git:":
            case "git+http:":
            case "git+https:":
            case "git+rsync:":
            case "git+ftp:":
            case "git+file:":
            case "git+ssh:":
                res.type = Type.GIT;
                setGitAttrs(res, parsedUrl.getHash().isEmpty() ? "" : parsedUrl.getHash().substring(1), release);
                if ("git+file:".equals(parsedUrl.getProtocol()) &&
                    Pattern.compile("^git\\+file://[a-z]:", Pattern.CASE_INSENSITIVE).matcher(rawSpec).find()) {
                    res.fetchSpec = "git+file://" + parsedUrl.getHost().toLowerCase(Locale.ROOT) + ":" +
                                    parsedUrl.getPathname();
                } else {
                    parsedUrl.setHash("");
                    res.fetchSpec = parsedUrl.toString();
                }
                if (res.fetchSpec.startsWith("git+")) {
                    res.fetchSpec = res.fetchSpec.substring(4);
                }
                break;
            case "http:":
            case "https:":
                res.type = Type.REMOTE;
                res.fetchSpec = res.saveSpec;
                break;
            default:
                throw new InvalidSpecException("Unsupported URL Type \"" + parsedUrl.getProtocol() + "\": " + rawSpec,
                        "EUNSUPPORTEDPROTOCOL");
        }
        return res;
    }

    private static Spec fromAlias(Spec res, String where, Options options) {
        Spec subSpec = npa(res.rawSpec.substring(4), where, options);
        if (subSpec.type == Type.ALIAS) {
            throw new InvalidSpecException("nested aliases not supported", null);
        }
        if (!subSpec.isRegistry()) {
            throw new InvalidSpecException("aliases only work for registry deps", null);
        }
        if ((subSpec.name == null || subSpec.name.isEmpty()) && !before11(options.release)) {
            throw new InvalidSpecException("aliases must have a name", null);
        }
        res.subSpec = subSpec;
        res.registry = true;
        res.type = Type.ALIAS;
        res.saveSpec = null;
        res.fetchSpec = null;
        return res;
    }

    private static Spec fromRegistry(Spec res, Options options) {
        boolean pnpm = options.release == Release.PNPM_2_0_0;
        NodeSemver.Options loose = NodeSemver.Options.LOOSE.release(options.semver);
        res.registry = true;
        // before 10.0.0, res.rawSpec === '' ? 'latest' : res.rawSpec.trim(); before 8.1.4 untrimmed
        boolean trims = options.release != Release.V8_1_1;
        String spec = before10(options.release) ? res.rawSpec.isEmpty() ? "latest" :
                trims ? trim(res.rawSpec) : res.rawSpec :
                !pnpm ? trim(res.rawSpec) : res.rawSpec.isEmpty() ? "latest" : res.rawSpec;
        res.saveSpec = null;
        res.fetchSpec = spec;
        String version = NodeSemver.valid(spec, loose);
        String range = NodeSemver.validRange(spec, loose);
        if (version != null) {
            res.type = Type.VERSION;
        } else if (range != null) {
            res.type = Type.RANGE;
        } else {
            if (!encodeURIComponent(spec).equals(spec)) {
                throw new InvalidSpecException("Invalid tag name \"" + spec + "\"" +
                                               (npa8(options.release) ? "" :
                                                       " of package \"" + res.raw + "\"") +
                                               ": Tags may not have any characters that encodeURIComponent encodes.",
                        "EINVALIDTAGNAME");
            }
            res.type = Type.TAG;
        }
        return res;
    }
}
