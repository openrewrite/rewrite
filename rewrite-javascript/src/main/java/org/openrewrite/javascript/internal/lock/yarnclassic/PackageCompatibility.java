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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Port of yarn src/package-compatibility.js (yarn 1.22.22): the os, cpu and engines a package declares, checked
 * against the host. A required package that does not match fails the install; an optional one is skipped.
 */
final class PackageCompatibility {

    // process.versions of the Node 24 build the recordings ran on, other than node itself
    private static final List<String> NODE_COMPONENTS = Arrays.asList("acorn", "ada", "amaro", "ares", "brotli",
            "cldr", "icu", "llhttp", "merve", "modules", "napi", "nbytes", "ncrypto", "nghttp2", "nghttp3", "ngtcp2",
            "openssl", "simdjson", "simdutf", "sqlite", "tz", "undici", "unicode", "uv", "uvwasi", "v8", "zlib",
            "zstd");

    private final Config config;
    private final Host host;
    private final List<String> errors;

    PackageCompatibility(Config config, Host host, List<String> errors) {
        this.config = config;
        this.host = host;
        this.errors = errors;
    }

    private static boolean isValid(List<?> items, String actual) {
        boolean isNotWhitelist = true;
        boolean isBlacklist = false;
        for (Object item : items) {
            if ("!".equals(Js.get(item, "0"))) {
                isBlacklist = true;
                if (actual.equals(Js.string(item).substring(1))) {
                    return false;
                }
            } else {
                isNotWhitelist = false;
                if (actual.equals(item)) {
                    return true;
                }
            }
        }
        return isBlacklist && isNotWhitelist;
    }

    /**
     * Whether the engine's version satisfies the range; node above 1 also passes as 0.10 to 0.13 of its major.
     */
    static boolean testEngine(String name, @Nullable Object range, String actual, boolean looseSemver) {
        NodeSemver.Options options = looseSemver ? Config.LOOSE_SEMVER : Config.SEMVER;
        if (actual.isEmpty() || NodeSemver.valid(actual, options) == null) {
            return false;
        }
        if (NodeSemver.satisfies(actual, range, options)) {
            return true;
        }
        if ("node".equals(name) && NodeSemver.compare(actual, "1.0.0", options) > 0) {
            String major = NodeSemver.valid(actual, options).split("\\.", -1)[0];
            for (String fake : Arrays.asList("0.10." + major, "0.11." + major, "0.12." + major, "0.13." + major)) {
                if (NodeSemver.satisfies(fake, range, options)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The host's value of {@code process.versions[name]}, {@code ""} when Node has no such component, null when the
     * host does not say.
     */
    private @Nullable String version(String name) {
        if ("yarn".equals(name)) {
            return config.getRelease().toString();
        }
        if ("node".equals(name)) {
            String node = host.getNodeVersion();
            return node == null ? null : node.startsWith("v") ? node.substring(1) : node;
        }
        if (NODE_COMPONENTS.contains(name)) {
            if (host.getNodeVersion() == null) {
                return null;
            }
            throw new CannotKnow("the host Node's process.versions." + name + ", which an engines field names");
        }
        return "";
    }

    void checkOne(Manifest info, boolean ignoreEngines) {
        boolean didError = false;
        String human = Js.templateString(info.getName()) + "@" + Js.templateString(info.getVersion());
        List<String> pushed = new ArrayList<>();
        Object os = info.get("os");
        Object cpu = info.get("cpu");
        Object engines = info.get("engines");
        if (shouldCheckPlatform(os, config.isIgnorePlatform()) && host.getOs() != null &&
            !isValid((List<?>) os, host.getOs())) {
            pushed.add(Lang.format(config.getRelease(), "The platform $0 is incompatible with this module.", host.getOs()));
        }
        if (shouldCheckCpu(cpu, config.isIgnorePlatform()) && host.getCpu() != null &&
            !isValid((List<?>) cpu, host.getCpu())) {
            pushed.add(Lang.format(config.getRelease(), "The CPU architecture $0 is incompatible with this module.", host.getCpu()));
        }
        if (shouldCheckEngines(engines, ignoreEngines)) {
            for (String key : Js.keys(engines)) {
                String name = "iojs".equals(key) ? "node" : key;
                Object range = Js.get(engines, key);
                String actual = version(name);
                if (actual == null) {
                    continue;
                }
                if (!actual.isEmpty() && !testEngine(name, range, actual, config.isLooseSemver())) {
                    pushed.add(config.getRelease().namesTheEngineVersion() ?
                            Lang.format(config.getRelease(), "The engine $0 is incompatible with this module. Expected version $1. Got $2",
                                    name, range, actual) :
                            Lang.format(config.getRelease(), "The engine $0 is incompatible with this module. Expected version $1.", name,
                                    range));
                }
            }
        }
        for (String msg : pushed) {
            PackageReference ref = info.reference;
            if (ref != null && Boolean.TRUE.equals(ref.optional)) {
                ref.ignore = true;
            } else {
                errors.add(human + ": " + msg);
                didError = true;
            }
        }
        if (didError) {
            throw new MessageError(config.getRelease().endsFoundIncompatibleWithAPeriod() ?
                    "Found incompatible module." : "Found incompatible module");
        }
    }

    void check(List<Manifest> infos, boolean ignoreEngines) {
        for (Manifest info : infos) {
            checkOne(info, ignoreEngines);
        }
    }

    private static boolean shouldCheckCpu(@Nullable Object cpu, boolean ignorePlatform) {
        return !ignorePlatform && cpu instanceof List && !((List<?>) cpu).isEmpty();
    }

    private static boolean shouldCheckPlatform(@Nullable Object os, boolean ignorePlatform) {
        return !ignorePlatform && os instanceof List && !((List<?>) os).isEmpty();
    }

    private static boolean shouldCheckEngines(@Nullable Object engines, boolean ignoreEngines) {
        return !ignoreEngines && (engines instanceof Map || engines instanceof List);
    }

    static boolean shouldCheck(Manifest manifest, boolean ignoreEngines, boolean ignorePlatform) {
        return shouldCheckCpu(manifest.get("cpu"), ignorePlatform) ||
               shouldCheckPlatform(manifest.get("os"), ignorePlatform) ||
               shouldCheckEngines(manifest.get("engines"), ignoreEngines);
    }

    /**
     * install.js {@code init} before 1.15.0: the root is checked where its engines is an object, {@code typeof null}
     * among them, and {@code --ignore-engines} is not given.
     */
    static boolean checksRootEngines(Manifest manifest, boolean ignoreEngines) {
        Object engines = manifest.get("engines");
        return !ignoreEngines && (engines instanceof Map || engines instanceof List ||
                                  engines == null && manifest.data.containsKey("engines"));
    }
}
