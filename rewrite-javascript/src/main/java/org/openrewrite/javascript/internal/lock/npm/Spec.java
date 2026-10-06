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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.semver.NodeSemver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;
import static org.openrewrite.javascript.internal.lock.npm.UriComponent.decodeURIComponent;
import static org.openrewrite.javascript.internal.lock.npm.UriComponent.encodeURIComponent;

/**
 * Port of npm-package-arg 13.0.2 ({@code lib/npa.js}, its {@code Result}) on POSIX paths: what a
 * dependency spec asks for.
 */
@Getter
public final class Spec {

    public enum Type {
        VERSION, RANGE, TAG, ALIAS, GIT, FILE, DIRECTORY, REMOTE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
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
    private static final Pattern IS_PORT_NUMBER = Pattern.compile(":[0-9]+(/|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_POSIX_FILE = Pattern.compile("^(?:[.]|~[/]|[/]|[a-zA-Z]:)");
    private static final Pattern GIT_SSH = Pattern.compile("^git\\+ssh://([^:#]+:[^#]+(?:\\.git)?)(?:#(.*))?$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SCOPED_PACKAGE = Pattern.compile("^(?:@([^/]+?)[/])?([^/]+?)$", Pattern.DOTALL);

    // npm-package-arg resolves '~' against os.homedir(); the virtual filesystem's home is its root.
    static final String HOME = PosixPath.CWD;

    private @Nullable Type type;
    private @Nullable Boolean registry;
    private @Nullable String where;
    private final String raw;
    private @Nullable String name;
    private @Nullable String escapedName;
    private @Nullable String scope;
    private final String rawSpec;
    private @Nullable String saveSpec;
    private @Nullable String fetchSpec;
    private @Nullable String gitRange;
    private @Nullable String gitCommittish;
    private @Nullable String gitSubdir;
    private @Nullable GitHost hosted;
    private @Nullable Spec subSpec;

    private Spec(@Nullable String raw, @Nullable String name, @Nullable String rawSpec) {
        this.raw = raw != null ? raw :
                name != null && !name.isEmpty() ? name + "@" + (rawSpec == null ? "undefined" : rawSpec) :
                        rawSpec == null ? "" : rawSpec;
        this.rawSpec = rawSpec == null ? "" : rawSpec;
        if (name != null && !name.isEmpty()) {
            setName(name);
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
    }

    public boolean isRegistry() {
        return Boolean.TRUE.equals(registry);
    }

    public static Spec npa(String arg) {
        return npa(arg, null);
    }

    public static Spec npa(Spec arg, @Nullable String where) {
        if (where == null || where.isEmpty() || where.equals(arg.where)) {
            return arg;
        }
        if (arg.name != null && !arg.name.isEmpty() && !arg.rawSpec.isEmpty()) {
            return resolve(arg.name, arg.rawSpec, where);
        }
        return npa(arg.raw, where);
    }

    /**
     * {@code npa(arg, where)} for a value read from package or lock data, which may not be a string.
     *
     * @throws IllegalArgumentException npa's TypeError for a value of another type
     */
    public static Spec npa(@Nullable Object arg, @Nullable String where) {
        if (arg instanceof Spec) {
            return npa((Spec) arg, where);
        }
        if (arg instanceof String) {
            return npa((String) arg, where);
        }
        if (arg == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'name')");
        }
        if (arg == Js.UNDEFINED) {
            throw new IllegalArgumentException("Cannot read properties of undefined (reading 'indexOf')");
        }
        if (!(arg instanceof Map) && !(arg instanceof List)) {
            throw new IllegalArgumentException("arg.indexOf is not a function");
        }
        Object name = Js.get(arg, "name");
        Object rawSpec = Js.get(arg, "rawSpec");
        Object argWhere = Js.get(arg, "where");
        String resolvedWhere = truthy(where) ? where : argWhere instanceof String ? (String) argWhere : null;
        if (truthy(name) && truthy(rawSpec)) {
            if (!(name instanceof String)) {
                String pkgid = Js.string(name) + "@" + Js.string(rawSpec);
                throw new InvalidSpecException("Invalid package name \"" + Js.string(name) + "\" of package \"" +
                                               pkgid + "\": name must be a string.", "EINVALIDPACKAGENAME");
            }
            if (!(rawSpec instanceof String)) {
                throw new IllegalArgumentException("spec.toLowerCase is not a function");
            }
            return resolve((String) name, (String) rawSpec, resolvedWhere);
        }
        return npa(Js.get(arg, "raw"), resolvedWhere);
    }

    public static Spec npa(String arg, @Nullable String where) {
        String name = null;
        String spec;
        int nameEndsAt = arg.indexOf('@', 1);
        String namePart = nameEndsAt > 0 ? arg.substring(0, nameEndsAt) : arg;
        if (IS_URL.matcher(arg).find()) {
            spec = arg;
        } else if (IS_GIT.matcher(arg).find()) {
            spec = "git+ssh://" + arg;
        } else if (!namePart.startsWith("@") && (namePart.contains("/") || IS_FILE_TYPE.matcher(namePart).find())) {
            spec = arg;
        } else if (nameEndsAt > 0) {
            name = namePart;
            spec = arg.substring(nameEndsAt + 1);
            if (spec.isEmpty()) {
                spec = "*";
            }
        } else {
            if (validatePackageNameErrors(arg).isEmpty()) {
                name = arg;
                spec = "*";
            } else {
                spec = arg;
            }
        }
        return resolve(name, spec, where, arg);
    }

    public static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where) {
        return resolve(name, spec, where, null);
    }

    private static Spec resolve(@Nullable String name, @Nullable String spec, @Nullable String where,
                                @Nullable String arg) {
        Spec res = new Spec(arg, name, spec);
        if (where == null || where.isEmpty()) {
            where = PosixPath.CWD;
        }
        if (isFileSpec(spec)) {
            return fromFile(res, where);
        } else if (isAliasSpec(spec)) {
            return fromAlias(res, where);
        }

        GitHost hosted = spec == null ? null : GitHost.fromUrl(spec, GitHost.Opts.of(true, true));
        if (hosted != null) {
            return fromHostedGit(res, hosted);
        } else if (spec != null && !spec.isEmpty() && IS_URL.matcher(spec).find()) {
            return fromURL(res);
        } else if (spec != null && !spec.isEmpty() && (spec.contains("/") || IS_FILE_TYPE.matcher(spec).find())) {
            return fromFile(res, where);
        }
        return fromRegistry(res);
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

    private void setName(String name) {
        List<String> errors = validatePackageNameErrors(name);
        if (!errors.isEmpty()) {
            throw new InvalidSpecException("Invalid package name \"" + name + "\" of package \"" + raw + "\": " +
                                           String.join("; ", errors) + ".", "EINVALIDPACKAGENAME");
        }
        this.name = name;
        this.scope = name.charAt(0) == '@' ? name.substring(0, name.indexOf('/')) : null;
        this.escapedName = name.replaceFirst("/", "%2f");
    }

    /**
     * The spec as {@code OverrideSet} reads a key after clearing its name.
     */
    Spec withoutName() {
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

    private static Spec fromHostedGit(Spec res, GitHost hosted) {
        res.type = Type.GIT;
        res.hosted = hosted;
        res.saveSpec = hosted.toString(GitHost.Opts.of(false, false));
        res.fetchSpec = "shortcut".equals(hosted.getDefaultRepresentation()) ?
                null : hosted.toString(GitHost.Opts.NONE);
        setGitAttrs(res, hosted.getCommittish());
        return res;
    }

    private static Spec fromURL(Spec res) {
        String rawSpec = res.rawSpec;
        res.saveSpec = rawSpec;
        if (rawSpec.startsWith("git+ssh:")) {
            Matcher matched = GIT_SSH.matcher(rawSpec);
            if (matched.matches() && !IS_PORT_NUMBER.matcher(matched.group(1)).find()) {
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

    private static Spec fromRegistry(Spec res) {
        res.registry = true;
        String spec = Js.trim(res.rawSpec);
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
                throw new InvalidSpecException("Invalid tag name \"" + spec + "\" of package \"" + res.raw +
                                               "\": Tags may not have any characters that encodeURIComponent encodes.",
                        "EINVALIDTAGNAME");
            }
            res.type = Type.TAG;
        }
        return res;
    }

    // The errors of validate-npm-package-name 7.0.2; npa only asks validForOldPackages.
    static List<String> validatePackageNameErrors(String name) {
        List<String> errors = new ArrayList<>();
        if (name.isEmpty()) {
            errors.add("name length must be greater than zero");
        }
        if (name.startsWith(".")) {
            errors.add("name cannot start with a period");
        }
        if (name.startsWith("-")) {
            errors.add("name cannot start with a hyphen");
        }
        if (name.startsWith("_")) {
            errors.add("name cannot start with an underscore");
        }
        if (!Js.trim(name).equals(name)) {
            errors.add("name cannot contain leading or trailing spaces");
        }
        for (String excludedName : new String[]{"node_modules", "favicon.ico"}) {
            if (name.toLowerCase(Locale.ROOT).equals(excludedName)) {
                errors.add(excludedName + " is not a valid package name");
            }
        }
        if (!encodeURIComponent(name).equals(name)) {
            Matcher nameMatch = SCOPED_PACKAGE.matcher(name);
            if (nameMatch.matches()) {
                String user = nameMatch.group(1);
                String pkg = nameMatch.group(2);
                if (pkg.startsWith(".")) {
                    errors.add("name cannot start with a period");
                }
                if (user != null && encodeURIComponent(user).equals(user) && encodeURIComponent(pkg).equals(pkg)) {
                    return errors;
                }
            }
            errors.add("name can only contain URL-friendly characters");
        }
        return errors;
    }
}
