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
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.EcmaScript.UNDEFINED;
import static org.openrewrite.javascript.internal.lock.EcmaScript.string;
import static org.openrewrite.javascript.internal.lock.EcmaScript.trim;
import static org.openrewrite.javascript.internal.lock.EcmaScript.truthy;
import static org.openrewrite.javascript.internal.lock.UriComponent.decodeURIComponent;
import static org.openrewrite.javascript.internal.lock.UriComponent.encodeURIComponent;

/**
 * Port of npm-package-arg ({@code npa.js}, its {@code Result}) on POSIX paths: 13.0.2, which npm 11.17.0 bundles, and
 * pnpm 11.2.2's fork of npa 8, {@code @pnpm/npm-package-arg} 2.0.0. {@link #ofDependency} also names other protocols.
 */
@Getter
public final class Spec {

    public enum Release {
        PNPM_2_0_0,
        V13_0_2
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
    private static final Pattern IS_GIT = Pattern.compile("^[^@]+@[^:.]+\\.[^:]+:.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_FILE_TYPE = Pattern.compile("[.](?:tgz|tar\\.gz|tar)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_FILENAME_PNPM = Pattern.compile("[.](?:tgz|tar.gz|tar)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_PORT_NUMBER = Pattern.compile(":[0-9]+(/|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_PORT_NUMBER_PNPM = V8RegExp.of(":[0-9]+\\/?.*$");
    private static final Pattern FILE_PROTOCOL = Pattern.compile("^file:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABSOLUTE_PATH = Pattern.compile("^[/]|^[A-Za-z]:");
    private static final Pattern IS_POSIX_FILE = Pattern.compile("^(?:[.]|~[/]|[/]|[a-zA-Z]:)");
    private static final Pattern GIT_SSH = Pattern.compile("^git\\+ssh://([^:#]+:[^#]+(?:\\.git)?)(?:#(.*))?$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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

    private Spec(@Nullable String raw, @Nullable String name, @Nullable String rawSpec, Release release) {
        this.raw = raw != null ? raw :
                name != null && !name.isEmpty() ? name + "@" + (rawSpec == null ? "undefined" : rawSpec) :
                        rawSpec == null ? "" : rawSpec;
        this.rawSpec = rawSpec == null ? "" : rawSpec;
        if (name != null && !name.isEmpty()) {
            setName(name, release);
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
        return npa(arg, where, Release.V13_0_2);
    }

    private static Spec npa(Spec arg, @Nullable String where, Release release) {
        if (where == null || where.isEmpty() || where.equals(arg.where)) {
            return arg;
        }
        if (arg.name != null && !arg.name.isEmpty() && !arg.rawSpec.isEmpty()) {
            return resolve(arg.name, arg.rawSpec, where, release);
        }
        return npa(arg.raw, where, release);
    }

    /**
     * {@code npa(arg, where)} for a value read from package or lock data, which may not be a string.
     * @throws IllegalArgumentException npa's TypeError for a value of another type
     */
    public static Spec npa(@Nullable Object arg, @Nullable String where) {
        return npa(arg, where, Release.V13_0_2);
    }

    public static Spec npa(@Nullable Object arg, @Nullable String where, Release release) {
        if (arg instanceof Spec) {
            return npa((Spec) arg, where, release);
        }
        if (arg instanceof String) {
            return npa((String) arg, where, release);
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
            return resolve((String) name, (String) rawSpec, resolvedWhere, release);
        }
        return npa(get(arg, "raw"), resolvedWhere, release);
    }

    // arg[key] of the object or array npa was given, for a key no prototype has
    private static @Nullable Object get(Object arg, String key) {
        return arg instanceof Map && ((Map<?, ?>) arg).containsKey(key) ? ((Map<?, ?>) arg).get(key) : UNDEFINED;
    }

    public static Spec npa(String arg, @Nullable String where) {
        return npa(arg, where, Release.V13_0_2);
    }

    public static Spec npa(String arg, @Nullable String where, Release release) {
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
            if (spec.isEmpty() && !pnpm) {
                spec = "*";
            }
        } else {
            if (ValidateNpmPackageName.validForOldPackages(arg, validateRelease(release))) {
                name = arg;
                spec = pnpm ? null : "*";
            } else {
                spec = arg;
            }
        }
        return resolve(name, spec, where, arg, release);
    }

    public static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where) {
        return resolve(name, spec, where, null, Release.V13_0_2);
    }

    public static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where,
                               Release release) {
        return resolve(name, spec, where, null, release);
    }

    private static boolean isFileType(String spec, Release release) {
        return (release == Release.PNPM_2_0_0 ? IS_FILENAME_PNPM : IS_FILE_TYPE).matcher(spec).find();
    }

    // the validate-npm-package-name the release bundles
    private static ValidateNpmPackageName.Release validateRelease(Release release) {
        return release == Release.PNPM_2_0_0 ? ValidateNpmPackageName.Release.V4_0_0 :
                ValidateNpmPackageName.Release.V7_0_2;
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
                Spec res = new Spec(null, name, value, Release.V13_0_2);
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
            Spec res = new Spec(null, name, value, Release.V13_0_2);
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

    private static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where,
                                @Nullable String arg, Release release) {
        boolean pnpm = release == Release.PNPM_2_0_0;
        Spec res = new Spec(arg, name, spec, release);
        if (where == null || where.isEmpty()) {
            where = PosixPath.CWD;
        }
        if (pnpm && spec != null && (IS_POSIX_FILE.matcher(spec).find() || FILE_PROTOCOL.matcher(spec).find())) {
            return fromFilePnpm(res, where);
        } else if (pnpm && spec != null && spec.startsWith("npm:")) {
            Spec aliased = npa(spec.substring(4), where, release);
            aliased.alias = name;
            aliased.raw = res.raw;
            aliased.rawSpec = res.rawSpec;
            return aliased;
        } else if (!pnpm && isFileSpec(spec)) {
            return fromFile(res, where);
        } else if (!pnpm && isAliasSpec(spec)) {
            return fromAlias(res, where);
        }

        GitHost hosted = spec == null ? null : GitHost.fromUrl(spec, GitHost.Opts.of(true, true),
                pnpm ? GitHost.Release.V4_1_0 : GitHost.Release.V9_0_3);
        if (hosted != null) {
            return fromHostedGit(res, hosted);
        } else if (spec != null && !spec.isEmpty() && IS_URL.matcher(spec).find()) {
            return fromURL(res, release);
        } else if (spec != null && !spec.isEmpty() && (spec.contains("/") || isFileType(spec, release))) {
            return pnpm ? fromFilePnpm(res, where) : fromFile(res, where);
        }
        return fromRegistry(res, release);
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

    private void setName(String name, Release release) {
        List<String> errors = ValidateNpmPackageName.errors(name, validateRelease(release));
        if (!errors.isEmpty()) {
            throw new InvalidSpecException(release == Release.PNPM_2_0_0 ?
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

    private static void setGitAttrs(Spec res, @Nullable String committish) {
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

    private static Spec fromFile(Spec res, String where) {
        res.type = IS_FILE_TYPE.matcher(res.rawSpec).find() ? Type.FILE : Type.DIRECTORY;
        res.where = where;

        String rawSpec = pathToFileURL(res.rawSpec);
        if (rawSpec.startsWith("file:/")) {
            if (rawSpec.matches("^file://[^/][\\s\\S]*")) {
                rawSpec = "file:/" + rawSpec.substring(5);
            }
            if (Pattern.compile("^/{1,3}\\.\\.?(/|$)").matcher(rawSpec.substring(5)).find()) {
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
        if (Pattern.compile("^/~(/|$)").matcher(specPath).find()) {
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

    // the fork's fromFile, npa 8's: the path as given, resolved against where
    private static Spec fromFilePnpm(Spec res, String where) {
        res.type = IS_FILENAME_PNPM.matcher(res.rawSpec).find() ? Type.FILE : Type.DIRECTORY;
        res.where = where;
        String spec = res.rawSpec.replace('\\', '/')
                .replaceFirst("^file:[/]*([A-Za-z]:)", "$1")
                .replaceFirst("^file:(?:[/]*([~./]))?", "$1");
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

    private static Spec fromHostedGit(Spec res, GitHost hosted) {
        res.type = Type.GIT;
        res.hosted = hosted;
        res.saveSpec = hosted.toString(GitHost.Opts.of(false, false));
        res.fetchSpec = "shortcut".equals(hosted.getDefaultRepresentation()) ?
                null : hosted.toString(GitHost.Opts.NONE);
        setGitAttrs(res, hosted.getCommittish());
        return res;
    }

    private static Spec fromURL(Spec res, Release release) {
        String rawSpec = res.rawSpec;
        res.saveSpec = rawSpec;
        if (rawSpec.startsWith("git+ssh:")) {
            Matcher matched = GIT_SSH.matcher(rawSpec);
            Pattern port = release == Release.PNPM_2_0_0 ? IS_PORT_NUMBER_PNPM : IS_PORT_NUMBER;
            if (matched.matches() && !port.matcher(matched.group(1)).find()) {
                res.type = Type.GIT;
                setGitAttrs(res, matched.group(2));
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
                setGitAttrs(res, parsedUrl.getHash().isEmpty() ? "" : parsedUrl.getHash().substring(1));
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

    private static Spec fromAlias(Spec res, String where) {
        Spec subSpec = npa(res.rawSpec.substring(4), where);
        if (subSpec.type == Type.ALIAS) {
            throw new InvalidSpecException("nested aliases not supported", null);
        }
        if (!subSpec.isRegistry()) {
            throw new InvalidSpecException("aliases only work for registry deps", null);
        }
        if (subSpec.name == null || subSpec.name.isEmpty()) {
            throw new InvalidSpecException("aliases must have a name", null);
        }
        res.subSpec = subSpec;
        res.registry = true;
        res.type = Type.ALIAS;
        res.saveSpec = null;
        res.fetchSpec = null;
        return res;
    }

    private static Spec fromRegistry(Spec res, Release release) {
        boolean pnpm = release == Release.PNPM_2_0_0;
        res.registry = true;
        String spec = !pnpm ? trim(res.rawSpec) : res.rawSpec.isEmpty() ? "latest" : res.rawSpec;
        res.saveSpec = null;
        res.fetchSpec = spec;
        String version = NodeSemver.valid(spec, NodeSemver.Options.LOOSE);
        String range = NodeSemver.validRange(spec, NodeSemver.Options.LOOSE);
        if (version != null) {
            res.type = Type.VERSION;
        } else if (range != null) {
            res.type = Type.RANGE;
        } else {
            if (!encodeURIComponent(spec).equals(spec)) {
                throw new InvalidSpecException("Invalid tag name \"" + spec + "\"" +
                                               (pnpm ? "" : " of package \"" + res.raw + "\"") +
                                               ": Tags may not have any characters that encodeURIComponent encodes.",
                        "EINVALIDTAGNAME");
            }
            res.type = Type.TAG;
        }
        return res;
    }
}
