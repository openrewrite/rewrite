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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.semver.NodeSemver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Port of corepack sources/specUtils.ts {@code parseSpec}, {@code parsePackageJSON} and {@code loadSpec}, of
 * Engine.ts {@code findProjectSpec} and of the version corepackUtils.ts {@code installVersion} runs (corepack 0.35.0).
 */
public final class SpecUtils {

    private static final Pattern NODE_MODULES = Pattern.compile("[\\\\/]node_modules[\\\\/](@[^\\\\/]*[\\\\/])?" +
                                                                "([^@\\\\/][^\\\\/]*)$");
    private static final List<String> SUPPORTED = Arrays.asList("npm", "pnpm", "yarn");

    private SpecUtils() {
    }

    /**
     * A package manager and the version, range or URL a project names for it.
     */
    @Value
    public static class Descriptor {
        String name;
        String range;
    }

    /**
     * What {@code loadSpec} finds: the package.json it settled on ({@code target}, by path from the project's
     * directory) and the spec parsePackageJSON read from it, {@code UNDEFINED} for none; no target, no package.json.
     */
    @Value
    public static class Lookup {
        @Nullable
        String target;

        @Nullable
        Object rawPmSpec;

        /**
         * {@code path.relative(initialCwd, manifestPath)}, as corepack's messages name the file.
         */
        @Nullable
        String source;

        /**
         * {@code getSpec} of a lookup that found a spec, null for none; throws Rejected where parseSpec refuses it.
         */
        public @Nullable Descriptor getSpec(boolean enforceExactVersion, HostEnvironment env) {
            if (target == null || rawPmSpec == EcmaScript.UNDEFINED) {
                return null;
            }
            return parseSpec(rawPmSpec, String.valueOf(source), enforceExactVersion, env);
        }
    }

    /**
     * {@code loadSpec} from {@code initialCwd} (a directory relative to the project's, {@code ../} above it) up to
     * {@code levels} directories above the project's; {@code manifest} answers a directory's package.json text.
     */
    public static Lookup loadSpec(String initialCwd, int levels, Function<String, @Nullable String> manifest) {
        Object data = null;
        String target = null;
        String source = null;
        String currCwd = PosixPath.normalize(initialCwd.isEmpty() ? "." : initialCwd);
        String up = "";
        while (true) {
            String dir = ".".equals(currCwd) ? "" : currCwd;
            String text = NODE_MODULES.matcher("/" + dir).find() ? null : manifest.apply(dir);
            if (text != null) {
                Object parsed;
                try {
                    parsed = EcmaScript.jsonParse(text);
                } catch (EcmaScript.JsonSyntaxError e) {
                    parsed = null;
                }
                if (!(parsed instanceof Map) && !(parsed instanceof List)) {
                    throw corepackRejects("Invalid package.json in " + up + "package.json");
                }
                data = parsed;
                target = dir.isEmpty() ? "package.json" : dir + "/package.json";
                source = up + "package.json";
                if (EcmaScript.truthy(get(data, "packageManager"))) {
                    break;
                }
            }
            if ((dir.isEmpty() || dir.startsWith("..")) && aboveBy(currCwd) >= levels) {
                break;
            }
            currCwd = PosixPath.normalize(currCwd + "/..");
            up += "../";
        }
        return new Lookup(target, data == null ? EcmaScript.UNDEFINED : parsePackageJSON(data), source);
    }

    // how many directories above the project's this one is
    private static int aboveBy(String dir) {
        int depth = 0;
        String rest = dir;
        if (!"..".equals(rest) && !rest.startsWith("../")) {
            return 0;
        }
        while ("..".equals(rest) || rest.startsWith("../")) {
            depth++;
            rest = "..".equals(rest) ? "" : rest.substring(3);
        }
        return depth;
    }

    /**
     * {@code parsePackageJSON}: {@code packageManager}, checked against {@code devEngines.packageManager}, or the
     * latter as a spec when the former is absent; corepack's warnings change nothing here.
     */
    static @Nullable Object parsePackageJSON(Object packageJSONContent) {
        Object pm = get(packageJSONContent, "packageManager");
        Object packageManager = get(get(packageJSONContent, "devEngines"), "packageManager");
        if (packageManager == null || packageManager == EcmaScript.UNDEFINED) {
            return pm;
        }
        if (!(packageManager instanceof Map) && !(packageManager instanceof List)) {
            return pm;
        }
        if (packageManager instanceof List) {
            return pm;
        }
        Object name = get(packageManager, "name");
        Object version = get(packageManager, "version");
        Object onFail = get(packageManager, "onFail");
        if (!(name instanceof String) || ((String) name).contains("@")) {
            warnOrThrow("The value of devEngines.packageManager.name " + json(name) +
                        " is not a supported string value", onFail);
            return pm;
        }
        boolean hasVersion = version != null && version != EcmaScript.UNDEFINED;
        if (hasVersion && (!(version instanceof String) ||
                           NodeSemver.validRange((String) version, NodeSemver.Options.DEFAULT) == null)) {
            warnOrThrow("The value of devEngines.packageManager.version " + json(version) +
                        " is not a valid semver range", onFail);
            return pm;
        }
        if (EcmaScript.truthy(pm)) {
            if (!(pm instanceof String) || !((String) pm).startsWith(name + "@")) {
                warnOrThrow("\"packageManager\" field is set to " + json(pm) + " which does not match the " +
                            "\"devEngines.packageManager\" field set to " + json(name), onFail);
            } else if (hasVersion && !satisfies(((String) pm).substring(((String) name).length() + 1),
                    (String) version)) {
                warnOrThrow("\"packageManager\" field is set to " + json(pm) + " which does not match the value " +
                            "defined in \"devEngines.packageManager\" for " + json(name) + " of " + json(version),
                        onFail);
            }
            return pm;
        }
        return name + "@" + (hasVersion ? version : "*");
    }

    private static boolean satisfies(String version, String range) {
        try {
            return NodeSemver.satisfies(version, range, NodeSemver.Options.DEFAULT);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // only a failure is corepack's verdict here; its warnings go to stderr
    private static void warnOrThrow(String errorMessage, @Nullable Object onFail) {
        if ("error".equals(onFail) || onFail == EcmaScript.UNDEFINED) {
            throw corepackRejects(errorMessage);
        }
    }

    // JSON.stringify as a template literal spells it
    private static String json(@Nullable Object value) {
        String json = EcmaScript.jsonStringify(value);
        return json == null ? "undefined" : json;
    }

    /**
     * {@code parseSpec}, reading COREPACK_ENABLE_UNSAFE_CUSTOM_URLS from {@code env}; throws Rejected as corepack words
     * its usage error.
     */
    public static Descriptor parseSpec(@Nullable Object raw, String source, boolean enforceExactVersion,
                                       HostEnvironment env) {
        if (!(raw instanceof String)) {
            throw corepackRejects("Invalid package manager specification in " + source + "; expected a string");
        }
        String spec = (String) raw;
        int atIndex = spec.indexOf('@');
        if (atIndex == -1 || atIndex == spec.length() - 1) {
            if (enforceExactVersion) {
                throw corepackRejects("No version specified for " + spec + " in \"packageManager\" of " + source);
            }
            String name = atIndex == -1 ? spec : spec.substring(0, spec.length() - 1);
            if (!SUPPORTED.contains(name)) {
                throw corepackRejects("Unsupported package manager specification (" + name + ")");
            }
            return new Descriptor(name, "*");
        }
        String name = spec.substring(0, atIndex);
        String range = spec.substring(atIndex + 1);
        if (WhatwgUrl.tryParse(range) == null) {
            if (enforceExactVersion && NodeSemver.valid(range, NodeSemver.Options.DEFAULT) == null) {
                throw corepackRejects("Invalid package manager specification in " + source + " (" + spec + "); " +
                                      "expected a semver version");
            }
            if (!SUPPORTED.contains(name)) {
                throw corepackRejects("Unsupported package manager specification (" + spec + ")");
            }
        } else if (SUPPORTED.contains(name) && !"1".equals(env.setting("COREPACK_ENABLE_UNSAFE_CUSTOM_URLS"))) {
            throw corepackRejects("Illegal use of URL for known package manager. Instead, select a specific " +
                                  "version, or set COREPACK_ENABLE_UNSAFE_CUSTOM_URLS=1 in your environment (" + spec +
                                  ")");
        }
        return new Descriptor(name, range);
    }

    /**
     * {@code findProjectSpec} for the package manager {@code name} run in {@code initialCwd}: the spec the project
     * names, or null when corepack falls back to its own version; throws Rejected where corepack refuses to run.
     */
    public static @Nullable Descriptor findProjectSpec(String initialCwd, int levels,
                                                       Function<String, @Nullable String> manifest, String name,
                                                       HostEnvironment env) {
        if ("0".equals(env.setting("COREPACK_ENABLE_PROJECT_SPEC"))) {
            return null;
        }
        Lookup lookup = loadSpec(initialCwd, levels, manifest);
        Descriptor spec = lookup.getSpec(true, env);
        if (spec == null || spec.getName().equals(name)) {
            return spec;
        }
        if ("0".equals(env.setting("COREPACK_ENABLE_STRICT"))) {
            return null;
        }
        throw corepackRejects("This project is configured to use " + spec.getName() + " because " +
                              lookup.getTarget() + " has a \"packageManager\" field");
    }

    /**
     * The version corepackUtils.ts {@code installVersion} installs and runs for a spec findProjectSpec returned,
     * {@code semver.parse(range).version}; null for a URL.
     */
    public static @Nullable String installVersion(Descriptor spec) {
        return WhatwgUrl.tryParse(spec.getRange()) == null ? NodeSemver.valid(spec.getRange(),
                NodeSemver.Options.DEFAULT) : null;
    }

    // object[key], UNDEFINED where it lacks the key or is no object
    private static @Nullable Object get(@Nullable Object object, String key) {
        return object instanceof Map && ((Map<?, ?>) object).containsKey(key) ? ((Map<?, ?>) object).get(key) :
                EcmaScript.UNDEFINED;
    }

    private static Rejected corepackRejects(String message) {
        return Rejected.rejects(Reason.MALFORMED_MANIFEST, message + " (corepack)");
    }
}
