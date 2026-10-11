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
import org.openrewrite.javascript.internal.lock.NodeHosts;
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

    static final String YARN_VERSION = "1.22.22";

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
        return "node".equals(name) && NodeSemver.compare(actual, "1.0.0", options) > 0 &&
               fakeSatisfies(range, Integer.parseInt(NodeSemver.valid(actual, options).split("\\.", -1)[0]), options);
    }

    /**
     * The host's value of {@code process.versions[name]}, {@code ""} when Node has no such component, null when the
     * host does not say.
     */
    private @Nullable String version(String name) {
        if ("yarn".equals(name)) {
            return YARN_VERSION;
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
        // the checks that read what the host does not report and whose answer differs between hosts
        List<String> unknown = new ArrayList<>();
        if (shouldCheckPlatform(os, config.isIgnorePlatform())) {
            if (host.getOs() == null) {
                unknown.add("platform");
            } else if (!isValid((List<?>) os, host.getOs())) {
                pushed.add(Lang.format("The platform $0 is incompatible with this module.", host.getOs()));
            }
        }
        if (shouldCheckCpu(cpu, config.isIgnorePlatform())) {
            if (host.getCpu() == null) {
                unknown.add("CPU architecture");
            } else if (!isValid((List<?>) cpu, host.getCpu())) {
                pushed.add(Lang.format("The CPU architecture $0 is incompatible with this module.", host.getCpu()));
            }
        }
        if (shouldCheckEngines(engines, ignoreEngines)) {
            for (String key : Js.keys(engines)) {
                String name = "iojs".equals(key) ? "node" : key;
                Object range = Js.get(engines, key);
                String actual = version(name);
                if (actual == null) {
                    Boolean every = "node".equals(name) ? everyHost(range, config.isLooseSemver()) : null;
                    if (every == null) {
                        unknown.add("engine " + name);
                    } else if (!every) {
                        pushed.add(Lang.format("The engine $0 is incompatible with this module. Expected version " +
                                               "$1.", name, range));
                    }
                    continue;
                }
                if (!actual.isEmpty() && !testEngine(name, range, actual, config.isLooseSemver())) {
                    pushed.add(Lang.format("The engine $0 is incompatible with this module. Expected version " +
                                           "$1. Got $2", name, range, actual));
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
            throw new MessageError("Found incompatible module.");
        }
        PackageReference ref = info.reference;
        if (!unknown.isEmpty() && !(ref != null && Boolean.TRUE.equals(ref.optional))) {
            throw new CannotKnow("the " + String.join(" and ", unknown) + " yarn runs on, which " + human +
                                 " restricts");
        }
    }

    /**
     * Whether {@link #testEngine} passes for every Node release in {@link NodeHosts#YARN_CLASSIC} (TRUE), for none
     * (FALSE) or for some (null).
     */
    static @Nullable Boolean everyHost(@Nullable Object range, boolean looseSemver) {
        NodeSemver.Options options = looseSemver ? Config.LOOSE_SEMVER : Config.SEMVER;
        String valid = range instanceof String ? NodeSemver.validRange((String) range, options) : null;
        if (valid == null) {
            return Boolean.FALSE;
        }
        String hosts = NodeHosts.YARN_CLASSIC.releases();
        int first = Integer.parseInt(NodeSemver.minVersion(hosts, options).split("\\.", -1)[0]);
        // past every number the range names, each major answers as the first one past them does
        int last = first;
        for (String number : valid.split("[^0-9]+")) {
            if (!number.isEmpty() && number.length() < 6) {
                last = Math.max(last, Integer.parseInt(number) + 1);
            }
        }
        boolean all = true;
        boolean none = true;
        for (int major = first; major <= last; major++) {
            String releases = hosts + " >=" + major + ".0.0 <" + (major + 1) + ".0.0";
            boolean fake = fakeSatisfies(range, major, options);
            all &= fake || NodeSemver.subset(releases, valid, options);
            none &= !fake && !NodeSemver.intersects(releases, valid, options);
        }
        return all ? Boolean.TRUE : none ? Boolean.FALSE : null;
    }

    private static boolean fakeSatisfies(@Nullable Object range, int major, NodeSemver.Options options) {
        for (String fake : Arrays.asList("0.10." + major, "0.11." + major, "0.12." + major, "0.13." + major)) {
            if (NodeSemver.satisfies(fake, range, options)) {
                return true;
            }
        }
        return false;
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
}
