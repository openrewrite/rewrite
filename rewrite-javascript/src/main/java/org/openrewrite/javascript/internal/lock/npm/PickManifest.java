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
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.internal.lock.V8Date;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;
import static org.openrewrite.javascript.semver.NodeSemver.Options.LOOSE;
import static org.openrewrite.javascript.semver.NodeSemver.Options.LOOSE_INCLUDE_PRERELEASE;

/**
 * Port of npm-pick-manifest 11.0.3 (npm 11.17.0): the version of a packument a spec selects.
 */
public final class PickManifest {

    @Value
    @Builder(toBuilder = true)
    public static class Options {
        public static final Options DEFAULT = Options.builder().build();

        @Builder.Default
        String defaultTag = "latest";

        /**
         * A date string, a time value in milliseconds, an {@link Instant} or a {@link Date}.
         */
        @Nullable
        Object before;

        /**
         * {@code process.version} upstream; null checks no node engine.
         */
        @Nullable
        String nodeVersion;

        @Nullable
        String npmVersion;

        boolean includeStaged;

        @Nullable
        String avoid;

        boolean avoidStrict;
    }

    /**
     * {@code ETARGET}, {@code E403} or {@code ENOVERSIONS}.
     */
    @Getter
    public static final class PickFailure extends IllegalStateException implements NpmError {
        private final String code;
        private final @Nullable Object name;
        private final @Nullable String wanted;
        private final List<String> versions;
        private final @Nullable String avoid;

        PickFailure(String code, String message, @Nullable Object name, @Nullable String wanted,
                    List<String> versions, @Nullable String avoid) {
            super(message);
            this.code = code;
            this.name = name;
            this.wanted = wanted;
            this.versions = versions;
            this.avoid = avoid;
        }
    }

    private PickManifest() {
    }

    private static boolean engineOk(@Nullable Object manifest, @Nullable String npmVersion,
            @Nullable String nodeVersion) {
        if (!(manifest instanceof Map)) {
            return true;
        }
        try {
            NpmInstallChecks.checkEngine(asMap(manifest), npmVersion, nodeVersion, false);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean isBefore(@Nullable Object verTimes, @Nullable Object ver, double time) {
        Object verTime = truthy(verTimes) ? Js.get(verTimes, Js.string(ver)) : null;
        return !truthy(verTimes) || !truthy(verTime) || V8Date.dateParse(Js.string(verTime)) <= time;
    }

    private static boolean shouldAvoid(@Nullable Object ver, @Nullable String avoid) {
        return truthy(avoid) && NodeSemver.satisfies(ver, avoid, LOOSE_INCLUDE_PRERELEASE);
    }

    private static @Nullable Object decorateAvoid(@Nullable Object result, @Nullable String avoid) {
        if (truthy(result) && shouldAvoid(Js.get(result, "version"), avoid)) {
            Map<String, Object> decorated = spread(result);
            decorated.put("_shouldAvoid", true);
            return decorated;
        }
        return result;
    }

    // { ...object }: nothing from null or undefined
    private static Map<String, Object> spread(@Nullable Object object) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (object == null || object == Js.UNDEFINED) {
            return copy;
        }
        for (Map.Entry<String, @Nullable Object> entry : Js.entries(object)) {
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object object) {
        return (Map<String, Object>) object;
    }

    /**
     * {@code +(new Date(before))}
     */
    static double timeValue(Object before) {
        if (before instanceof Number) {
            return ((Number) before).doubleValue();
        }
        if (before instanceof Instant) {
            return (double) ((Instant) before).toEpochMilli();
        }
        if (before instanceof Date) {
            return (double) ((Date) before).getTime();
        }
        if (before instanceof Boolean) {
            return (Boolean) before ? 1 : 0;
        }
        return V8Date.dateParse(Js.string(before));
    }

    private static @Nullable Object pick(Map<String, Object> packument, @Nullable String wanted, Options opts) {
        String defaultTag = opts.getDefaultTag();
        Object before = opts.getBefore();
        String avoid = opts.getAvoid();

        Object name = Js.get(packument, "name");
        Object verTimes = packument.get("time");
        Object versions = truthy(packument.get("versions")) ? packument.get("versions") : new LinkedHashMap<>();

        if (opts.isAvoidStrict()) {
            Options looseOpts = opts.toBuilder().avoidStrict(false).build();

            Object result = pick(packument, wanted, looseOpts);
            if (!truthy(result) || !truthy(Js.get(result, "_shouldAvoid"))) {
                return result;
            }

            Object caret = pick(packument, "^" + Js.string(Js.get(result, "version")), looseOpts);
            if (!truthy(caret) || !truthy(Js.get(caret, "_shouldAvoid"))) {
                Map<String, Object> outside = spread(caret);
                outside.put("_outsideDependencyRange", true);
                outside.put("_isSemVerMajor", false);
                return outside;
            }

            Object star = pick(packument, "*", looseOpts);
            if (!truthy(star) || !truthy(Js.get(star, "_shouldAvoid"))) {
                Map<String, Object> outside = spread(star);
                outside.put("_outsideDependencyRange", true);
                outside.put("_isSemVerMajor", true);
                return outside;
            }

            throw new PickFailure("ETARGET", "No avoidable versions for " + Js.string(name), name, wanted,
                    Js.keys(versions), avoid);
        }

        Object stagedVersions = opts.isIncludeStaged() && truthy(packument.get("stagedVersions")) ?
                Js.get(packument.get("stagedVersions"), "versions") : null;
        Object staged = truthy(stagedVersions) ? stagedVersions : new LinkedHashMap<>();
        Object restrictedVersions = truthy(packument.get("policyRestrictions")) ?
                Js.get(packument.get("policyRestrictions"), "versions") : null;
        Object restricted = truthy(restrictedVersions) ? restrictedVersions : new LinkedHashMap<>();

        double time = truthy(before) && truthy(verTimes) ? timeValue(before) : Double.POSITIVE_INFINITY;
        Spec spec = Spec.resolve(name instanceof String ? (String) name : null, truthy(wanted) ? wanted : defaultTag,
                null);
        Spec.Type type = spec.getType();
        Object distTags = truthy(packument.get("dist-tags")) ? packument.get("dist-tags") : new LinkedHashMap<>();

        if (type != Spec.Type.TAG && type != Spec.Type.VERSION && type != Spec.Type.RANGE) {
            throw new IllegalArgumentException("Only tag, version, and range are supported");
        }

        if (truthy(wanted) && type == Spec.Type.TAG) {
            Object ver = Js.get(distTags, wanted);
            if (isBefore(verTimes, ver, time)) {
                return decorateAvoid(firstTruthy(Js.get(versions, Js.string(ver)), Js.get(staged, Js.string(ver)),
                        Js.get(restricted, Js.string(ver))), avoid);
            } else {
                return pick(packument, "<=" + Js.string(ver), opts);
            }
        }

        if (truthy(wanted) && type == Spec.Type.VERSION) {
            String ver = clean(wanted);
            Object mani = firstTruthy(Js.get(versions, Js.string(ver)), Js.get(staged, Js.string(ver)),
                    Js.get(restricted, Js.string(ver)));
            return isBefore(verTimes, ver, time) ? decorateAvoid(mani, avoid) : null;
        }

        String range = type == Spec.Type.RANGE ? wanted : "*";

        Object defaultVer = Js.get(distTags, defaultTag);
        if (truthy(defaultVer) &&
            ("*".equals(range) || NodeSemver.satisfies(defaultVer, range, LOOSE)) &&
            !truthy(Js.get(restricted, Js.string(defaultVer))) &&
            !shouldAvoid(defaultVer, avoid)) {
            Object mani = Js.get(versions, Js.string(defaultVer));
            boolean ok = truthy(mani) &&
                         isBefore(verTimes, defaultVer, time) &&
                         engineOk(mani, opts.getNpmVersion(), opts.getNodeVersion()) &&
                         !truthy(Js.get(mani, "deprecated")) &&
                         !truthy(Js.get(staged, Js.string(defaultVer)));
            if (ok) {
                return mani;
            }
        }

        List<Map.Entry<String, @Nullable Object>> allEntries = new ArrayList<>();
        for (Map.Entry<String, @Nullable Object> entry : concat(Js.entries(versions), Js.entries(staged),
                Js.entries(restricted))) {
            if (isBefore(verTimes, entry.getKey(), time)) {
                allEntries.add(entry);
            }
        }

        if (allEntries.isEmpty()) {
            throw new PickFailure("ENOVERSIONS", "No versions available for " + Js.string(name), name, wanted,
                    Js.keys(versions), avoid);
        }

        List<Map.Entry<String, @Nullable Object>> entries = new ArrayList<>();
        for (Map.Entry<String, @Nullable Object> entry : allEntries) {
            if (NodeSemver.satisfies(entry.getKey(), range, LOOSE)) {
                entries.add(entry);
            }
        }
        String npmVersion = opts.getNpmVersion();
        String nodeVersion = opts.getNodeVersion();
        entries.sort((a, b) -> {
            String vera = a.getKey();
            Object mania = a.getValue();
            String verb = b.getKey();
            Object manib = b.getValue();
            int notavoida = shouldAvoid(vera, avoid) ? 0 : 1;
            int notavoidb = shouldAvoid(verb, avoid) ? 0 : 1;
            int notrestra = truthy(Js.get(restricted, vera)) ? 0 : 1;
            int notrestrb = truthy(Js.get(restricted, verb)) ? 0 : 1;
            int notstagea = truthy(Js.get(staged, vera)) ? 0 : 1;
            int notstageb = truthy(Js.get(staged, verb)) ? 0 : 1;
            int notdepra = truthy(Js.get(mania, "deprecated")) ? 0 : 1;
            int notdeprb = truthy(Js.get(manib, "deprecated")) ? 0 : 1;
            int enginea = engineOk(mania, npmVersion, nodeVersion) ? 1 : 0;
            int engineb = engineOk(manib, npmVersion, nodeVersion) ? 1 : 0;
            int result = notavoidb - notavoida;
            if (result == 0) {
                result = notrestrb - notrestra;
            }
            if (result == 0) {
                result = notstageb - notstagea;
            }
            if (result == 0) {
                result = (notdeprb & engineb) - (notdepra & enginea);
            }
            if (result == 0) {
                result = engineb - enginea;
            }
            if (result == 0) {
                result = notdeprb - notdepra;
            }
            if (result == 0) {
                result = NodeSemver.compare(verb, vera, LOOSE);
            }
            return result;
        });

        return decorateAvoid(entries.isEmpty() ? null : entries.get(0).getValue(), avoid);
    }

    // semver.clean(version, { loose: true })
    private static @Nullable String clean(String version) {
        return NodeSemver.valid(Js.trim(version).replaceFirst("^[=v]+", ""), LOOSE);
    }

    private static @Nullable Object firstTruthy(@Nullable Object a, @Nullable Object b, @Nullable Object c) {
        return truthy(a) ? a : truthy(b) ? b : c;
    }

    @SafeVarargs
    private static List<Map.Entry<String, @Nullable Object>> concat(
            List<Map.Entry<String, @Nullable Object>>... lists) {
        List<Map.Entry<String, @Nullable Object>> all = new ArrayList<>();
        for (List<Map.Entry<String, @Nullable Object>> list : lists) {
            all.addAll(list);
        }
        return all;
    }

    public static Map<String, Object> pickManifest(Map<String, Object> packument, @Nullable String wanted) {
        return pickManifest(packument, wanted, Options.DEFAULT);
    }

    /**
     * The manifest of the version {@code wanted} (a tag, version or range; null or empty for {@code defaultTag})
     * selects, its {@code bin} normalized in place; throws PickFailure when nothing matches.
     */
    public static Map<String, Object> pickManifest(Map<String, Object> packument, @Nullable String wanted,
                                                   Options opts) {
        Object mani = pick(packument, wanted, opts);
        Object picked = mani;
        if (truthy(mani) && mani instanceof Map) {
            PackageJson.normalizeBin(asMap(mani));
        }
        Object policyRestrictions = packument.get("policyRestrictions");
        Object restrictedVersions = truthy(policyRestrictions) ? Js.get(policyRestrictions, "versions") : null;
        Object restricted = truthy(restrictedVersions) ? restrictedVersions : new LinkedHashMap<>();

        if (truthy(picked) && !truthy(Js.get(restricted, Js.string(Js.get(picked, "version"))))) {
            return picked instanceof Map ? asMap(picked) : spread(picked);
        }

        Object before = opts.getBefore();
        String bstr = truthy(before) ? Instant.ofEpochMilli((long) timeValue(before)).toString() : "";
        Object name = Js.get(packument, "name");
        String pckg = Js.string(name) + "@" + (wanted == null ? "undefined" : wanted) +
                      (truthy(before) ? " with a date before " + bstr : "");

        boolean isForbidden = truthy(picked) && truthy(Js.get(restricted, Js.string(Js.get(picked, "version"))));
        String polMsg = isForbidden ? Js.string(Js.get(policyRestrictions, "message")) : "";

        String msg = !isForbidden ? "No matching version found for " + pckg + "." :
                "Could not download " + pckg + " due to policy violations:\n" + polMsg;

        String code = isForbidden ? "E403" : "ETARGET";
        throw new PickFailure(code, msg, name, wanted,
                Js.keys(truthy(packument.get("versions")) ? packument.get("versions") : new LinkedHashMap<>()),
                opts.getAvoid());
    }
}
