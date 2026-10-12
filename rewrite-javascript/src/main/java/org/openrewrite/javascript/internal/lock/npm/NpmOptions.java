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

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.Ini;
import org.openrewrite.javascript.internal.lock.V8Date;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * The npm configuration arborist reads, under npm's own names and with npm's defaults.
 */
@Value
@Builder(toBuilder = true)
public class NpmOptions {

    public static final NpmOptions DEFAULT = NpmOptions.builder().build();

    private static final Pattern ENV_EXPR = Pattern.compile("(?<!\\\\)(\\\\*)\\$\\{([^${}?]+)(\\?)?\\}");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern NUMBER = Pattern.compile("^[+-]?(\\d+\\.?\\d*(e[+-]?\\d+)?|\\.\\d+(e[+-]" +
                                                          "?\\d+)?|Infinity)$",
            Pattern.CASE_INSENSITIVE);
    private static final List<String> KEYS = Arrays.asList(
            "lockfile-version", "legacy-peer-deps", "install-links", "omit-lockfile-registry-resolved",
            "prefer-dedupe", "strict-peer-deps", "force", "install-strategy", "global-style", "legacy-bundling",
            "engine-strict", "before", "min-release-age", "min-release-age-exclude", "omit", "include", "only",
            "production", "dev", "optional", "also", "allow-git", "allow-file", "allow-directory", "allow-remote",
            "os", "cpu", "libc", "save", "tag", "format-package-lock", "dry-run", "global", "location", "registry",
            "scope");
    private static final Pattern SCOPED_REGISTRY = Pattern.compile("(@[^:]+):registry");

    // @npmcli/config definitions.js: every key npm 12.0.0 defines, in its order, which no release to 12.2.0 changes
    private static final Set<String> DEFINITIONS_12 = new LinkedHashSet<>(Arrays.asList(
            "_auth", "access", "all", "allow-same-version", "allow-directory", "allow-file", "allow-git",
            "allow-remote", "allow-scripts", "also", "audit", "audit-level", "auth-type", "before", "bin-links",
            "browser", "bypass-2fa", "ca", "cache", "cache-max", "cache-min", "cafile", "call", "cert", "cidr",
            "color", "commit-hooks", "cpu", "dangerously-allow-all-scripts", "depth", "description", "dev", "diff",
            "diff-ignore-all-space", "diff-name-only", "diff-no-prefix", "diff-dst-prefix", "diff-src-prefix",
            "diff-text", "diff-unified", "dry-run", "editor", "engine-strict", "expect-result-count",
            "expect-results", "expires", "extension-file", "fetch-retries", "fetch-retry-factor",
            "fetch-retry-maxtimeout", "fetch-retry-mintimeout", "fetch-timeout", "force", "foreground-scripts",
            "format-package-lock", "fund", "git", "git-tag-version", "global", "globalconfig", "global-ignore-file",
            "global-style", "heading", "https-proxy", "if-present", "ignore-extension", "ignore-scripts", "include",
            "include-staged", "include-workspace-root", "include-attestations", "init-author-email",
            "init-author-name", "init-author-url", "init-license", "init-module", "init-type", "init-version",
            "init-private", "init.author.email", "init.author.name", "init.author.url", "init.license", "init.module",
            "init.version", "install-links", "install-strategy", "json", "key", "legacy-bundling", "legacy-peer-deps",
            "libc", "link", "local-address", "location", "lockfile-version", "loglevel", "logs-dir", "logs-max",
            "long", "name", "maxsockets", "message", "min-release-age", "min-release-age-exclude", "node-gyp",
            "node-options", "noproxy", "offline", "omit", "omit-lockfile-registry-resolved", "only", "orgs",
            "optional", "os", "otp", "package", "package-lock", "package-lock-only", "pack-destination", "packages",
            "patches-dir", "allow-unused-patches", "ignore-patch-failures", "edit-dir", "ignore-existing",
            "keep-edit-dir", "parseable", "allow-scripts-pending", "allow-scripts-pin", "prefer-dedupe",
            "prefer-offline", "prefer-online", "prefix", "preid", "production", "progress", "provenance",
            "provenance-file", "proxy", "read-only", "rebuild-bundle", "registry", "replace-registry-host", "save",
            "save-bundle", "save-dev", "save-exact", "save-optional", "save-peer", "save-prefix", "save-prod",
            "sbom-format", "sbom-type", "scope", "scopes", "packages-all", "packages-and-scopes-permission",
            "orgs-permission", "password", "token-description", "script-shell", "searchexclude", "searchlimit",
            "searchopts", "searchstaleness", "shell", "sign-git-commit", "sign-git-tag", "strict-peer-deps",
            "strict-allow-scripts", "strict-npmrc", "strict-ssl", "tag", "tag-version-prefix", "timing", "to",
            "umask", "unicode", "update-notifier", "usage", "user-agent", "userconfig", "version", "versions",
            "viewer", "which", "workspace", "workspaces", "workspaces-update", "yes"));
    // definitions/index.js nerfDarts: what a key scoped to a registry may end in without a definition of its own
    private static final List<String> NERF_DARTS = Arrays.asList("_auth", "_authToken", "_password", "certfile",
            "email", "keyfile", "username");
    // index.js internalEnv: what npm sets for an npm it runs
    private static final List<String> INTERNAL_ENV = Arrays.asList("npm-version", "global-prefix", "local-prefix");
    private static final String STRICT_NPMRC = "strict-npmrc";
    // index.js validate: the auth keys an npmrc file may not hold, those it may only hold scoped to a registry, and
    // the ones npm 12.0.0 adds to these
    private static final List<String> AUTH_NOT_ALLOWED = Arrays.asList("_authtoken", "-authtoken");
    private static final List<String> AUTH_SCOPED = Arrays.asList("_auth", "_authToken", "username", "_password");
    private static final List<String> AUTH_SCOPED_12 = Arrays.asList("email", "certfile", "keyfile");
    private static final Object UNSET = new Object();

    // the definition's type, which npm's parseField reads from types[key]: Boolean, String, Number, or a
    // list of literal values
    private enum Type {
        BOOLEAN, STRING, NUMBER, LITERAL
    }

    /**
     * The npm release that reads the configuration: its defaults, and what its arborist does with each option.
     */
    @Builder.Default
    NpmRelease release = NpmRelease.PORTED;

    /**
     * {@code npmVersion}, npm's own version: what {@code engines.npm} and {@code devEngines.packageManager} are held
     * to, and the one place an install reads the release's text.
     */
    public String getNpmVersion() {
        return release.toString();
    }

    /**
     * {@code lockfile-version}: the version to write, or null to keep the lock's own (v1 is upgraded).
     */
    @Nullable
    Integer lockfileVersion;

    /**
     * {@code legacy-peer-deps}
     */
    boolean legacyPeerDeps;

    /**
     * {@code install-links}; null for the release's default.
     */
    @Nullable
    Boolean installLinks;

    public boolean isInstallLinks() {
        return installLinks != null ? installLinks : release.installLinksDefault();
    }

    /**
     * {@code omit-lockfile-registry-resolved}
     */
    boolean omitLockfileRegistryResolved;

    /**
     * {@code prefer-dedupe}
     */
    boolean preferDedupe;

    /**
     * {@code strict-peer-deps}
     */
    boolean strictPeerDeps;

    /**
     * {@code force}
     */
    boolean force;

    /**
     * {@code install-strategy}
     */
    @Builder.Default
    InstallStrategy installStrategy = InstallStrategy.HOISTED;

    /**
     * {@code engine-strict}
     */
    boolean engineStrict;

    /**
     * {@code before} as the time value of the Date npm parses: NaN for an invalid date (npm exits at its first fetch),
     * the text itself when only V8's legacy date parser reads it, null when unset or false.
     */
    @Nullable
    Object before;

    /**
     * Whether the config names {@code before} at all: within one source it wins over {@code min-release-age}.
     */
    boolean beforeConfigured;

    /**
     * {@code min-release-age} as npm's config parses it: a number of days, or the text when it is not one.
     */
    @Nullable
    Object minReleaseAge;

    /**
     * {@code min-release-age-exclude}: package names or minimatch globs.
     */
    @Builder.Default
    List<Object> minReleaseAgeExclude = Collections.emptyList();

    /**
     * The dependency types left out, as npm flattens {@code omit}, {@code include}, {@code only},
     * {@code production}, {@code dev}, {@code optional} and {@code also} into one list.
     */
    @Builder.Default
    List<String> omit = Collections.emptyList();

    /**
     * {@code allow-git}: {@code all}, {@code none} or {@code root}; null for the release's default.
     */
    @Nullable
    String allowGit;

    @Builder.Default
    String allowFile = "all";

    @Builder.Default
    String allowDirectory = "all";

    /**
     * {@code allow-remote}; null for the release's default.
     */
    @Nullable
    String allowRemote;

    public String getAllowGit() {
        return allowGit != null ? allowGit : release.allowGitDefault();
    }

    public String getAllowRemote() {
        return allowRemote != null ? allowRemote : release.allowRemoteDefault();
    }

    /**
     * {@code registry}, {@code @scope:registry} and {@code scope}: where npm asks for a package's documents.
     */
    @Builder.Default
    String registry = "https://registry.npmjs.org/";

    @Builder.Default
    Map<String, String> scopedRegistries = Collections.emptyMap();

    @Nullable
    String scope;

    /**
     * {@code os}, {@code cpu}, {@code libc}: the platform optional dependencies are checked against instead
     * of the host's.
     */
    @Nullable
    String os;

    @Nullable
    String cpu;

    @Nullable
    String libc;

    /**
     * {@code save}
     */
    @Builder.Default
    boolean save = true;

    /**
     * {@code format-package-lock}
     */
    @Builder.Default
    boolean formatPackageLock = true;

    /**
     * {@code dry-run}
     */
    boolean dryRun;

    /**
     * {@code global}
     */
    boolean global;

    /**
     * {@code tag}: the dist-tag a bare name or {@code *} range resolves through; null for an empty or
     * null config value, which pacote reads as {@code latest}.
     */
    @Nullable
    @Builder.Default
    String tag = "latest";

    public enum InstallStrategy {
        HOISTED("hoisted"), NESTED("nested"), SHALLOW("shallow"), LINKED("linked");

        private final String value;

        InstallStrategy(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        // arborist compares with === against nested, shallow and linked: any other value hoists
        static InstallStrategy of(@Nullable Object value) {
            for (InstallStrategy strategy : values()) {
                if (strategy.value.equals(value)) {
                    return strategy;
                }
            }
            return HOISTED;
        }
    }

    /**
     * The {@code before} filter in effect at {@code now}: {@code before} itself, else the window
     * {@code min-release-age} opens, as npm flattens the two.
     */
    public @Nullable Object before(Instant now) {
        if (!release.yieldsMinReleaseAgeToBefore()) {
            // min-release-age's flatten sets before whenever it is not null, and a config naming both is refused
            return minReleaseAge != null ?
                    (Object) V8Date.timeClip(now.toEpochMilli() - 86400000d * Js.toNumber(minReleaseAge)) : before;
        }
        if (beforeConfigured) {
            return before;
        }
        if (minReleaseAge != null && minReleaseAge != Js.UNDEFINED) {
            if (!truthy(minReleaseAge)) {
                return null;
            }
            return V8Date.timeClip(now.toEpochMilli() - 86400000d * Js.toNumber(minReleaseAge));
        }
        return null;
    }

    /**
     * The options of the user's .npmrc (the host's registries) and a project's over it, each null without one, as
     * {@code release}'s config loads them; a {@code ${VAR}} in a key read, or one it could expand to, is cannot-know.
     */
    public static NpmOptions fromNpmrc(NpmRelease release, @Nullable String userNpmrc, @Nullable String npmrc) {
        // npm reads no file that sets a variable, and the host does not know the environment npm runs in
        HostEnvironment env = HostEnvironment.UNKNOWN;
        NpmOptionsBuilder options = builder().release(release);
        Map<String, String> scopedRegistries = new LinkedHashMap<>();
        Object omit = null;
        Object include = null;
        Object only = null;
        Object production = null;
        Object dev = null;
        Object optional = null;
        Object also = null;
        List<Map.Entry<String, @Nullable Object>> entries = new ArrayList<>();
        for (String file : Arrays.asList(userNpmrc, npmrc)) {
            if (file != null) {
                entries.addAll(Ini.decode(file, Ini.Release.V6_0_0).entrySet());
            }
        }
        if (release.definesMinReleaseAge() && !release.yieldsMinReleaseAgeToBefore()) {
            checkExclusive(npmrc, userNpmrc);
        }
        // what the command checks of its config before it reads any of it
        validate(release, userNpmrc, npmrc, env);
        validateCli(release, userNpmrc, npmrc, env);
        // global-style and legacy-bundling flatten by writing install-strategy into the config they come from,
        // which install-strategy's own flatten then reads, wherever it stands in the file
        String installStrategyWritten = null;
        // before npm 9.0.0 they are arborist's own options, legacyBundling read first
        boolean globalStyle = false;
        boolean legacyBundling = false;
        boolean validated = release.flattensValidatedConfig();
        for (Map.Entry<String, @Nullable Object> entry : entries) {
            String key;
            try {
                key = envReplace(entry.getKey(), env);
            } catch (CannotKnow e) {
                Pattern expansions = Objects.requireNonNull(expansions(entry.getKey()));
                for (String name : KEYS) {
                    if (expansions.matcher(name).matches()) {
                        throw e;
                    }
                }
                continue;
            }
            Object value = entry.getValue();
            switch (key) {
                case "lockfile-version":
                    if (!release.definesLockfileVersion()) {
                        break;
                    }
                    Object lfv = parseField(value, Type.LITERAL, env);
                    if (validated && !(lfv == null ||
                                       release.typesLockfileVersionAsStrings() &&
                                       ("1".equals(lfv) || "2".equals(lfv) || "3".equals(lfv)))) {
                        // nopt's clean drops a value of none of the definition's types
                        break;
                    }
                    options.lockfileVersion(lockfileVersion(lfv));
                    break;
                case "legacy-peer-deps":
                    options.legacyPeerDeps(booleanField(value, env, validated));
                    break;
                case "install-links":
                    if (release.definesInstallLinks()) {
                        options.installLinks(booleanField(value, env, validated));
                    }
                    break;
                case "omit-lockfile-registry-resolved":
                    if (release.definesOmitLockfileRegistryResolved()) {
                        options.omitLockfileRegistryResolved(booleanField(value, env, validated));
                    }
                    break;
                case "prefer-dedupe":
                    if (release.definesPreferDedupe()) {
                        options.preferDedupe(booleanField(value, env, validated));
                    } else if (release.flattensUndeclaredPreferDedupe()) {
                        // a key no definition declares: parsed as a literal, kept by nopt, and read by arborist as !!
                        options.preferDedupe(truthy(parseField(value, Type.LITERAL, env)));
                    }
                    break;
                case "strict-peer-deps":
                    options.strictPeerDeps(booleanField(value, env, validated));
                    break;
                case "force":
                    options.force(booleanField(value, env, validated));
                    break;
                case "engine-strict":
                    options.engineStrict(booleanField(value, env, validated));
                    break;
                case "save":
                    options.save(booleanField(value, env, validated));
                    break;
                case "format-package-lock":
                    options.formatPackageLock(booleanField(value, env, validated));
                    break;
                case "dry-run":
                    options.dryRun(booleanField(value, env, validated));
                    break;
                case "global":
                    options.global(booleanField(value, env, validated));
                    break;
                case "location":
                    // location's flatten turns global on, in the file's own order with global's
                    if (release.setsGlobalFromLocation() && "global".equals(parseField(value, Type.LITERAL, env))) {
                        options.global(true);
                    }
                    break;
                case "install-strategy":
                    if (!release.definesInstallStrategy()) {
                        break;
                    }
                    Object strategy = installStrategyWritten != null ? installStrategyWritten :
                            parseField(value, Type.LITERAL, env);
                    boolean known = "hoisted".equals(strategy) || "nested".equals(strategy) ||
                                    "shallow".equals(strategy) ||
                                    "linked".equals(strategy) && release.definesLinkedInstallStrategy();
                    if (validated && !known) {
                        break;
                    }
                    // arborist before npm 9.4.0 knows no linked strategy, and hoists it
                    options.installStrategy("linked".equals(strategy) && !release.definesLinkedInstallStrategy() ?
                            InstallStrategy.HOISTED : InstallStrategy.of(strategy));
                    break;
                case "global-style":
                    if (!release.definesInstallStrategy()) {
                        globalStyle = booleanField(value, env, validated);
                    } else if (truthy(parseField(value, Type.BOOLEAN, env))) {
                        installStrategyWritten = "shallow";
                        options.installStrategy(InstallStrategy.SHALLOW);
                    }
                    break;
                case "legacy-bundling":
                    if (!release.definesInstallStrategy()) {
                        legacyBundling = booleanField(value, env, validated);
                    } else if (truthy(parseField(value, Type.BOOLEAN, env))) {
                        installStrategyWritten = "nested";
                        options.installStrategy(InstallStrategy.NESTED);
                    }
                    break;
                case "before":
                    Object before = parseField(value, Type.LITERAL, env);
                    Object date = before instanceof String ? dateField((String) before) : null;
                    if (validated && date instanceof Double && Double.isNaN((Double) date)) {
                        // nopt's validateDate drops what Date.parse cannot read, and makes the rest a Date
                        break;
                    }
                    options.beforeConfigured(true);
                    if (release.keepsNpmrcBeforeAString() && "".equals(before)) {
                        // an empty string is no before to pacote
                        options.before(null);
                        break;
                    }
                    options.before(before == null || before == Js.UNDEFINED || Boolean.FALSE.equals(before) ? null :
                            before instanceof String ? date : Double.NaN);
                    break;
                case "min-release-age":
                    if (release.definesMinReleaseAge()) {
                        options.minReleaseAge(parseField(value, Type.NUMBER, env));
                    }
                    break;
                case "min-release-age-exclude":
                    if (release.definesMinReleaseAgeExclude()) {
                        options.minReleaseAgeExclude(parseListField(value, Type.STRING, env));
                    }
                    break;
                case "omit":
                    omit = parseListField(value, Type.LITERAL, env);
                    break;
                case "include":
                    include = parseListField(value, Type.LITERAL, env);
                    break;
                case "only":
                    only = parseField(value, Type.LITERAL, env);
                    break;
                case "production":
                    production = parseField(value, Type.BOOLEAN, env);
                    break;
                case "dev":
                    dev = parseField(value, Type.BOOLEAN, env);
                    break;
                case "optional":
                    optional = parseField(value, Type.BOOLEAN, env);
                    break;
                case "also":
                    also = parseField(value, Type.LITERAL, env);
                    break;
                case "allow-git":
                    if (release.definesAllowGit()) {
                        options.allowGit(allowField(value, env));
                    }
                    break;
                case "allow-file":
                    if (release.definesAllowFile()) {
                        options.allowFile(allowField(value, env));
                    }
                    break;
                case "allow-directory":
                    if (release.definesAllowDirectory()) {
                        options.allowDirectory(allowField(value, env));
                    }
                    break;
                case "allow-remote":
                    if (release.definesAllowRemote()) {
                        options.allowRemote(allowField(value, env));
                    }
                    break;
                case "os":
                    options.os(platformField(value, env));
                    break;
                case "cpu":
                    options.cpu(platformField(value, env));
                    break;
                case "libc":
                    options.libc(platformField(value, env));
                    break;
                case "tag":
                    Object tag = parseField(value, Type.STRING, env);
                    options.tag(truthy(tag) ? Js.string(tag) : null);
                    break;
                case "registry":
                    Object registry = parseField(value, Type.STRING, env);
                    if (truthy(registry)) {
                        options.registry(Js.string(registry));
                    }
                    break;
                case "scope":
                    Object scope = parseField(value, Type.STRING, env);
                    options.scope(truthy(scope) ? Js.string(scope) : null);
                    break;
                default:
                    Matcher scoped = SCOPED_REGISTRY.matcher(key);
                    if (scoped.matches() && value instanceof String) {
                        scopedRegistries.put(scoped.group(1), envReplace((String) value, env));
                    }
                    break;
            }
        }
        if (!release.definesInstallStrategy()) {
            options.installStrategy(legacyBundling ? InstallStrategy.NESTED :
                    globalStyle ? InstallStrategy.SHALLOW : InstallStrategy.HOISTED);
        }
        options.omit(buildOmitList(omit, include, only, production, dev, optional, also));
        options.scopedRegistries(scopedRegistries);
        return options.build();
    }

    // a Boolean definition's value as arborist reads it: where the flat options are built from the validated config,
    // nopt's validateBoolean has made a string that is a number the boolean of that number (0 is false)
    private static boolean booleanField(@Nullable Object f, HostEnvironment env, boolean validated) {
        Object value = parseField(f, Type.BOOLEAN, env);
        if (validated && value instanceof String) {
            double number = Js.toNumber((String) value);
            return Double.isNaN(number) || number != 0;
        }
        return truthy(value);
    }

    // config index.js #loadObject while before and min-release-age are `exclusive` of each other: a key is refused
    // once its sibling is loaded, the project's file loading before the user's
    private static void checkExclusive(@Nullable String npmrc, @Nullable String userNpmrc) {
        Set<String> loaded = new LinkedHashSet<>();
        for (String file : Arrays.asList(npmrc, userNpmrc)) {
            if (file == null) {
                continue;
            }
            for (String key : Ini.decode(file, Ini.Release.V6_0_0).keySet()) {
                String exclusive = "before".equals(key) ? "min-release-age" :
                        "min-release-age".equals(key) ? "before" : null;
                if (exclusive != null && loaded.contains(exclusive)) {
                    throw new IllegalArgumentException("--" + key + " cannot be provided when using --" + exclusive);
                }
                try {
                    loaded.add(envReplace(key, HostEnvironment.UNKNOWN));
                } catch (CannotKnow e) {
                    // the loop over the keys read decides what an unknown variable costs
                }
            }
        }
    }

    /**
     * @npmcli/config index.js {@code validate}, which a command's constructor runs over the loaded config from npm
     * 9.0.0: an auth key outside a registry's scope in the user's or the project's file fails the install ({@code
     * ERR_INVALID_AUTH}), and so does a {@code registry} that is no URL, as each file's turn names the scope such a
     * key moves to ({@code ERR_INVALID_URL}). npm 12.0.0 holds {@code email}, {@code certfile} and {@code keyfile} to
     * it as well.
     */
    private static void validate(NpmRelease release, @Nullable String userNpmrc, @Nullable String npmrc,
                                 HostEnvironment env) {
        // npm's order: the global file, the user's, the project's
        Map<String, Map<String, @Nullable Object>> files = new LinkedHashMap<>();
        files.put("user", userNpmrc == null ? Collections.emptyMap() : Ini.decode(userNpmrc, Ini.Release.V6_0_0));
        files.put("project", npmrc == null ? Collections.emptyMap() : Ini.decode(npmrc, Ini.Release.V6_0_0));
        boolean since9 = false;
        boolean since12 = false;
        for (Map<String, @Nullable Object> file : files.values()) {
            since9 |= mentions(file, AUTH_NOT_ALLOWED) || mentions(file, AUTH_SCOPED);
            since12 |= mentions(file, AUTH_SCOPED_12);
        }
        // nerfDart(this.get('registry')): the project's registry over the user's, as the file holds it; nopt, which
        // drops one without a host from the project's file before that file's turn, is not ported
        Object registry = setIn(files.get("project"), "registry", env);
        if (registry == UNSET) {
            registry = setIn(files.get("user"), "registry", env);
        }
        String nerfedReg = "";
        RuntimeException noUrl = null;
        try {
            nerfedReg = nerfDart(registry == UNSET ? DEFAULT.getRegistry() : parseField(registry, Type.LITERAL, env));
        } catch (RuntimeException e) {
            noUrl = e;
        }
        if (noUrl == null && !since9 && !since12 || !release.refusesUnscopedAuth()) {
            return;
        }
        if (noUrl != null) {
            throw noUrl;
        }
        since12 = since12 && release.refusesUnscopedEmailAndCertificate();

        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Map<String, @Nullable Object>> file : files.entrySet()) {
            String where = file.getKey();
            Map<String, @Nullable Object> entries = file.getValue();
            for (String key : AUTH_NOT_ALLOWED) {
                if (isSet(entries, key, env)) {
                    problems.add(notAllowed(key, where));
                }
            }
            for (String key : AUTH_SCOPED) {
                if (isSet(entries, key, env)) {
                    // username and _password must both exist in the same file to be recognized correctly
                    if ("username".equals(key) && !isSet(entries, "_password", env) ||
                        "_password".equals(key) && !isSet(entries, "username", env)) {
                        problems.add(notAllowed(key, where));
                    } else {
                        problems.add(mustBeRenamed(key, nerfedReg, where));
                    }
                }
            }
            if (since12) {
                if (isSet(entries, "email", env)) {
                    problems.add(mustBeRenamed("email", nerfedReg, where));
                }
                // certfile and keyfile are a pair: a lone one is dropped
                for (String key : Arrays.asList("certfile", "keyfile")) {
                    if (isSet(entries, key, env)) {
                        problems.add(isSet(entries, "certfile".equals(key) ? "keyfile" : "certfile", env) ?
                                mustBeRenamed(key, nerfedReg, where) : notAllowed(key, where));
                    }
                }
            }
        }
        if (!problems.isEmpty()) {
            // errors.js ErrInvalidAuth, its closing backtick as npm prints it
            throw new NpmRejection("ERR_INVALID_AUTH", "Invalid auth configuration found: " +
                                                       String.join(", ", problems) + "\nPlease run `npm config fix` " +
                                                       "to repair your configuration.`", null);
        }
    }

    private static String notAllowed(String key, String where) {
        return "`" + key + "` is not allowed in " + where + " config";
    }

    private static String mustBeRenamed(String key, String nerfedReg, String where) {
        return "`" + key + "` must be renamed to `" + nerfedReg + ":" + key + "` in " + where + " config";
    }

    // @npmcli/config nerf-dart.js: the registry's host and directory, as the keys scoped to it start; new URL()'s
    // TypeError for a registry that is no URL, or that is an ini section, which has no string
    private static String nerfDart(@Nullable Object registry) {
        if (registry instanceof Ini.Section) {
            throw new IllegalArgumentException("Cannot convert object to primitive value");
        }
        WhatwgUrl parsed = WhatwgUrl.tryParse(Js.string(registry));
        WhatwgUrl rel = parsed == null ? null :
                WhatwgUrl.tryParse(".", parsed.getProtocol() + "//" + parsed.getHost() + parsed.getPathname());
        if (rel == null) {
            throw new NpmRejection("ERR_INVALID_URL", "Invalid URL", null);
        }
        return "//" + rel.getHost() + rel.getPathname();
    }

    // this.get(key, where), truthy or not, once parseField has read the file's value and nopt has cleaned it: _auth
    // is a [null, String], and none of the other keys validate asks for has a definition
    private static boolean isSet(Map<String, @Nullable Object> file, String key, HostEnvironment env) {
        Object raw = setIn(file, key, env);
        boolean defined = "_auth".equals(key);
        if (raw == UNSET || raw == null || Boolean.FALSE.equals(raw)) {
            return false;
        }
        if (!(raw instanceof String)) {
            if (defined && raw instanceof Ini.Section) {
                // nopt reads a String as String(val), and an ini section has none
                throw new IllegalArgumentException("Cannot convert object to primitive value");
            }
            return true;
        }
        String f = Js.trim((String) raw);
        if (!defined && "undefined".equals(f)) {
            return false;
        }
        String cleaned = Js.trim(envReplace(f, env));
        return !cleaned.isEmpty() && !"null".equals(cleaned) && (defined || !"false".equals(cleaned));
    }

    // whether a file has an entry under one of the keys, or under a key that may expand to one
    private static boolean mentions(Map<String, @Nullable Object> file, List<String> names) {
        for (String name : names) {
            if (mentions(file, name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentions(Map<String, @Nullable Object> file, String name) {
        for (String key : file.keySet()) {
            Pattern expansions = expansions(key);
            if (expansions != null ? expansions.matcher(name).matches() :
                    name.equals(envReplace(key, HostEnvironment.UNKNOWN))) {
                return true;
            }
        }
        return false;
    }

    /**
     * base-cmd.js {@code validateCli}, which npm 12.0.0 runs once its config is loaded and before the command reads
     * it: under {@code strict-npmrc}, a key of an npmrc file that @npmcli/config's {@code checkUnknown} finds no
     * definition for fails the install. Only the project's keys are asked: the user's file is the host's rendering of
     * its registries, with keys of the host's own.
     */
    private static void validateCli(NpmRelease release, @Nullable String userNpmrc, @Nullable String npmrc,
                                    HostEnvironment env) {
        if (npmrc == null) {
            return;
        }
        Map<String, @Nullable Object> project = Ini.decode(npmrc, Ini.Release.V6_0_0);
        List<String> unknown = new ArrayList<>();
        for (String key : project.keySet()) {
            String shown = unknownConfig(key);
            if (shown != null) {
                unknown.add(shown);
            }
        }
        if (unknown.isEmpty()) {
            return;
        }
        // config.get('strict-npmrc'): the project's file over the user's
        Object strict = UNSET;
        CannotKnow unknowable = null;
        try {
            strict = setIn(project, STRICT_NPMRC, env);
            if (strict == UNSET && userNpmrc != null) {
                strict = setIn(Ini.decode(userNpmrc, Ini.Release.V6_0_0), STRICT_NPMRC, env);
            }
        } catch (CannotKnow e) {
            unknowable = e;
        }
        if (strict == UNSET && unknowable == null || !release.strictNpmrcRefusesUnknownKeys()) {
            return;
        }
        if (unknowable != null) {
            throw unknowable;
        }
        if (cleanBoolean(parseField(strict, Type.BOOLEAN, env))) {
            StringBuilder message = new StringBuilder("Unknown npm configuration key")
                    .append(unknown.size() > 1 ? "s" : "").append(':');
            for (String shown : unknown) {
                // npm names the file by the path it read it from
                message.append("\n  - project config ").append(shown).append(" from .npmrc");
            }
            throw new NpmRejection("EUNKNOWNCONFIG", message.append("\nSee `npm help npmrc` for supported config " +
                                                                    "options.").toString(), null);
        }
    }

    // @npmcli/config index.js checkUnknown: how validateCli shows a key without a definition, null for one that has
    // one; a key scoped to a registry or a package scope is known by its last part, and `definitions` is a plain
    // object, so what it inherits counts as defined
    private static @Nullable String unknownConfig(String key) {
        if (defines(key) || INTERNAL_ENV.contains(key)) {
            return null;
        }
        String baseKey = key.contains(":") ? key.substring(key.lastIndexOf(':') + 1) : "";
        if (key.contains(":") && (defines(baseKey) || NERF_DARTS.contains(baseKey))) {
            return null;
        }
        return baseKey.isEmpty() ? '"' + key + '"' : '"' + baseKey + "\" (" + key + ")";
    }

    private static boolean defines(String key) {
        return DEFINITIONS_12.contains(key) || Js.objectPrototypeMember(key) != null;
    }

    // what a file's entries leave under a key (conf.data[k] = v, in the file's order), UNSET without one; an entry
    // whose key may expand to it is unknown unless a later one sets the key itself
    private static @Nullable Object setIn(Map<String, @Nullable Object> file, String name, HostEnvironment env) {
        Object value = UNSET;
        CannotKnow unknowable = null;
        for (Map.Entry<String, @Nullable Object> entry : file.entrySet()) {
            String key;
            try {
                key = envReplace(entry.getKey(), env);
            } catch (CannotKnow e) {
                if (Objects.requireNonNull(expansions(entry.getKey())).matcher(name).matches()) {
                    unknowable = e;
                }
                continue;
            }
            if (name.equals(key)) {
                value = entry.getValue();
                unknowable = null;
            }
        }
        if (unknowable != null) {
            throw unknowable;
        }
        return value;
    }

    // nopt's clean of a Boolean (type-defs.js validateBoolean), which the command's constructor runs over each file's
    // values before validateCli reads the config: the flattened options arborist reads are from before it
    private static boolean cleanBoolean(@Nullable Object value) {
        if (!(value instanceof String)) {
            return truthy(value);
        }
        String s = Js.trim((String) value);
        double number = Js.toNumber(s);
        return Double.isNaN(number) ? !"null".equals(s) && !"false".equals(s) : number != 0;
    }

    /**
     * npm-registry-fetch's {@code pickRegistry}: the registry of the package's scope, else of the {@code scope}
     * option's, else {@code registry}.
     */
    public String registryFor(String name) {
        String registry = null;
        if (name.startsWith("@") && name.indexOf('/') > 0) {
            registry = scopedRegistries.get(name.substring(0, name.indexOf('/')));
        }
        if (registry == null && scope != null) {
            registry = scopedRegistries.get(scope.startsWith("@") ? scope : "@" + scope);
        }
        return registry != null ? registry : this.registry;
    }

    // arborist reads the allow-* option as `?? 'all'`
    private static String allowField(@Nullable Object f, HostEnvironment env) {
        Object value = parseField(f, Type.LITERAL, env);
        return value == null || value == Js.UNDEFINED ? "all" : Js.string(value);
    }

    // npm's buildOmitList over the flattened config
    private static List<String> buildOmitList(@Nullable Object omitValue, @Nullable Object includeValue,
                                              @Nullable Object only, @Nullable Object production, @Nullable Object dev,
                                              @Nullable Object optional, @Nullable Object also) {
        List<String> include = strings(includeValue);
        List<String> omit = strings(omitValue);
        if (only instanceof String && ((String) only).matches("^prod(uction)?$") || truthy(production)) {
            omit.add("dev");
        } else if (Boolean.FALSE.equals(production)) {
            include.add("dev");
        }
        if (also instanceof String && ((String) also).startsWith("dev")) {
            include.add("dev");
        }
        if (truthy(dev)) {
            include.add("dev");
        }
        if (Boolean.FALSE.equals(optional)) {
            omit.add("optional");
        } else if (Boolean.TRUE.equals(optional)) {
            include.add("optional");
        }
        List<String> result = new ArrayList<>();
        for (String type : new LinkedHashSet<>(omit)) {
            if (!include.contains(type)) {
                result.add(type);
            }
        }
        return result;
    }

    private static List<String> strings(@Nullable Object list) {
        List<String> out = new ArrayList<>();
        if (list instanceof List) {
            for (Object o : (List<?>) list) {
                out.add(Js.string(o));
            }
        }
        return out;
    }

    // npm's parseField for one value of the definition's type
    private static @Nullable Object parseField(@Nullable Object f, Type type, HostEnvironment env) {
        if (!(f instanceof String)) {
            return f;
        }
        String s = Js.trim((String) f);
        if (type == Type.BOOLEAN && s.isEmpty()) {
            return Boolean.TRUE;
        }
        if (type != Type.STRING && type != Type.NUMBER) {
            switch (s) {
                case "true":
                    return Boolean.TRUE;
                case "false":
                    return Boolean.FALSE;
                case "null":
                    return null;
                case "undefined":
                    return Js.UNDEFINED;
                default:
                    break;
            }
        }
        s = envReplace(s, env);
        if (type == Type.NUMBER && (s.isEmpty() || NUMBER.matcher(s).matches())) {
            return Js.toNumber(s);
        }
        return s;
    }

    // new Date(text): the date-time string format, else V8's legacy parser, which never reads a digit-less string
    private static Object dateField(String text) {
        double time = V8Date.dateParse(text);
        return !Double.isNaN(time) || !DIGIT.matcher(text).find() ? (Object) time : text;
    }

    // a [null, String] definition read where only a truthy string means anything (`environment.os || current`)
    private static @Nullable String platformField(@Nullable Object f, HostEnvironment env) {
        Object value = parseField(f, Type.STRING, env);
        return truthy(value) ? Js.string(value) : null;
    }

    // parseField for an Array definition: a string splits on blank lines, each element parsed as its own type
    private static List<Object> parseListField(@Nullable Object f, Type elementType, HostEnvironment env) {
        List<Object> out = new ArrayList<>();
        if (f instanceof List) {
            for (Object element : (List<?>) f) {
                out.add(element instanceof String ? parseField(element, elementType, env) : element);
            }
        } else if (f instanceof String) {
            for (String element : Js.trim((String) f).split("\n\n")) {
                out.add(parseField(element, elementType, env));
            }
        } else if (f != null) {
            out.add(f);
        }
        return out;
    }

    // @npmcli/config env-replace.js: an unset ${NAME} stays as written, ${NAME?} is empty, an escaped one literal
    static String envReplace(String f, HostEnvironment env) {
        Matcher m = ENV_EXPR.matcher(f);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String esc = m.group(1);
            String replacement;
            if (esc.length() % 2 == 1) {
                replacement = m.group().substring((esc.length() + 1) / 2);
            } else {
                String value = env.reference(m.group(2));
                replacement = esc.substring(esc.length() / 2) +
                              (value != null ? value : m.group(3) != null ? "" : "${" + m.group(2) + "}");
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    // every string a key with references could expand to, or null for a key without one
    private static @Nullable Pattern expansions(String key) {
        Matcher m = ENV_EXPR.matcher(key);
        StringBuilder regex = new StringBuilder();
        int end = 0;
        boolean any = false;
        while (m.find()) {
            String esc = m.group(1);
            regex.append(Pattern.quote(key.substring(end, m.start())));
            if (esc.length() % 2 == 0) {
                regex.append(Pattern.quote(esc.substring(esc.length() / 2))).append(".*");
                any = true;
            } else {
                regex.append(Pattern.quote(m.group().substring((esc.length() + 1) / 2)));
            }
            end = m.end();
        }
        regex.append(Pattern.quote(key.substring(end)));
        return any ? Pattern.compile(regex.toString(), Pattern.DOTALL) : null;
    }

    // the definition's flatten (obj[key] && parseInt(obj[key], 10)), then arborist's own check
    private static @Nullable Integer lockfileVersion(@Nullable Object value) {
        if (value instanceof Ini.Section) {
            throw new IllegalArgumentException("Cannot convert object to primitive value");
        }
        Object lfv = truthy(value) ? (Object) Js.parseInt(Js.string(value), 10) : value;
        if (lfv == null || lfv == Js.UNDEFINED) {
            return null;
        }
        if (lfv instanceof Double) {
            double d = (Double) lfv;
            if (d == 1 || d == 2 || d == 3) {
                return (int) d;
            }
        }
        throw new IllegalArgumentException("Invalid lockfileVersion config: " + Js.string(lfv));
    }

    Set<String> omitSet() {
        return new LinkedHashSet<>(omit);
    }

    /**
     * @npmcli/config {@code loadLocalPrefix}: how many directories above the one it runs in npm {@code release}
     * installs, at the nearest of {@code parentTrees} whose workspaces map it; 0 when it installs there.
     */
    static int loadLocalPrefix(NpmRelease release, @Nullable String manifest,
                               Map<String, Map<String, @Nullable String>> parentTrees) {
        if (manifest == null) {
            return 0;
        }
        for (Map.Entry<String, Map<String, @Nullable String>> parent : parentTrees.entrySet()) {
            VirtualTreeLoader.ProjectFiles files = parentFiles(parent.getKey(), manifest, parent.getValue(), release);
            Map<String, Object> pkg;
            try {
                PackageJson own = files.manifest("");
                if (own == null) {
                    continue;
                }
                pkg = own.getContent();
            } catch (PackageJson.NeedsPackageFiles e) {
                pkg = e.getContent();
            } catch (CannotKnow e) {
                throw e;
            } catch (RuntimeException e) {
                continue;
            }
            if (!truthy(pkg.get("workspaces"))) {
                continue;
            }
            if (MapWorkspaces.mapWorkspaces("/", pkg, files, release).containsValue("/" + parent.getKey())) {
                return parent.getKey().split("/").length;
            }
        }
        return 0;
    }

    private static VirtualTreeLoader.ProjectFiles parentFiles(String runsIn, String manifest,
                                                              Map<String, @Nullable String> tree, NpmRelease release) {
        VirtualTreeLoader.ProjectFiles.ProjectFilesBuilder files = VirtualTreeLoader.ProjectFiles.builder()
                .release(release)
                .manifest(runsIn, manifest);
        for (Map.Entry<String, @Nullable String> held : tree.entrySet()) {
            if (held.getKey().equals(runsIn)) {
                continue;
            }
            if (held.getValue() == null) {
                files.unread(held.getKey());
            } else {
                files.manifest(held.getKey(), held.getValue());
            }
        }
        return files.build();
    }
}
