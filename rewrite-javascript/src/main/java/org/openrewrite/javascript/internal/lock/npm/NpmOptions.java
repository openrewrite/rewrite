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

    // the definition's type, which npm's parseField reads from types[key]: Boolean, String, Number, or a
    // list of literal values
    private enum Type {
        BOOLEAN, STRING, NUMBER, LITERAL
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
     * {@code install-links}
     */
    boolean installLinks;

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
     * {@code allow-git}: {@code all}, {@code none} or {@code root}.
     */
    @Builder.Default
    String allowGit = "all";

    @Builder.Default
    String allowFile = "all";

    @Builder.Default
    String allowDirectory = "all";

    @Builder.Default
    String allowRemote = "all";

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
     * npm's config loads them; a {@code ${VAR}} in a key read, or one it could expand to, is cannot-know.
     */
    public static NpmOptions fromNpmrc(@Nullable String userNpmrc, @Nullable String npmrc) {
        // npm reads no file that sets a variable, and the host does not know the environment npm runs in
        HostEnvironment env = HostEnvironment.UNKNOWN;
        NpmOptionsBuilder options = builder();
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
        // global-style and legacy-bundling flatten by writing install-strategy into the config they come from,
        // which install-strategy's own flatten then reads, wherever it stands in the file
        String installStrategyWritten = null;
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
                    options.lockfileVersion(lockfileVersion(parseField(value, Type.LITERAL, env)));
                    break;
                case "legacy-peer-deps":
                    options.legacyPeerDeps(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "install-links":
                    options.installLinks(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "omit-lockfile-registry-resolved":
                    options.omitLockfileRegistryResolved(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "prefer-dedupe":
                    options.preferDedupe(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "strict-peer-deps":
                    options.strictPeerDeps(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "force":
                    options.force(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "engine-strict":
                    options.engineStrict(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "save":
                    options.save(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "format-package-lock":
                    options.formatPackageLock(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "dry-run":
                    options.dryRun(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "global":
                    options.global(truthy(parseField(value, Type.BOOLEAN, env)));
                    break;
                case "location":
                    // location's flatten turns global on, in the file's own order with global's
                    if ("global".equals(parseField(value, Type.LITERAL, env))) {
                        options.global(true);
                    }
                    break;
                case "install-strategy":
                    options.installStrategy(InstallStrategy.of(installStrategyWritten != null ? installStrategyWritten :
                            parseField(value, Type.LITERAL, env)));
                    break;
                case "global-style":
                    if (truthy(parseField(value, Type.BOOLEAN, env))) {
                        installStrategyWritten = "shallow";
                        options.installStrategy(InstallStrategy.SHALLOW);
                    }
                    break;
                case "legacy-bundling":
                    if (truthy(parseField(value, Type.BOOLEAN, env))) {
                        installStrategyWritten = "nested";
                        options.installStrategy(InstallStrategy.NESTED);
                    }
                    break;
                case "before":
                    Object before = parseField(value, Type.LITERAL, env);
                    options.beforeConfigured(true);
                    options.before(before == null || before == Js.UNDEFINED || Boolean.FALSE.equals(before) ? null :
                            before instanceof String ? dateField((String) before) : Double.NaN);
                    break;
                case "min-release-age":
                    options.minReleaseAge(parseField(value, Type.NUMBER, env));
                    break;
                case "min-release-age-exclude":
                    options.minReleaseAgeExclude(parseListField(value, Type.STRING, env));
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
                    options.allowGit(allowField(value, env));
                    break;
                case "allow-file":
                    options.allowFile(allowField(value, env));
                    break;
                case "allow-directory":
                    options.allowDirectory(allowField(value, env));
                    break;
                case "allow-remote":
                    options.allowRemote(allowField(value, env));
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
        options.omit(buildOmitList(omit, include, only, production, dev, optional, also));
        options.scopedRegistries(scopedRegistries);
        return options.build();
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
     * @npmcli/config {@code loadLocalPrefix}: how many directories above the one it runs in npm installs, at the
     * nearest of {@code parentTrees} whose workspaces map it; 0 when it installs there.
     */
    static int loadLocalPrefix(@Nullable String manifest,
                               Map<String, Map<String, @Nullable String>> parentTrees) {
        if (manifest == null) {
            return 0;
        }
        for (Map.Entry<String, Map<String, @Nullable String>> parent : parentTrees.entrySet()) {
            VirtualTreeLoader.ProjectFiles files = parentFiles(parent.getKey(), manifest, parent.getValue());
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
            if (MapWorkspaces.mapWorkspaces("/", pkg, files).containsValue("/" + parent.getKey())) {
                return parent.getKey().split("/").length;
            }
        }
        return 0;
    }

    private static VirtualTreeLoader.ProjectFiles parentFiles(String runsIn, String manifest,
                                                              Map<String, @Nullable String> tree) {
        VirtualTreeLoader.ProjectFiles.ProjectFilesBuilder files = VirtualTreeLoader.ProjectFiles.builder()
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
