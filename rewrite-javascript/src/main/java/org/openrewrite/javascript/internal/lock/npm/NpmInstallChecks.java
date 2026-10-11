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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.semver.NodeSemver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of npm-install-checks 8.0.0 ({@code checkEngine}, {@code checkPlatform}). The machine's own values come from a
 * {@link Host}. A check that reads an unknown value passes or fails where it does so on every host, and raises
 * {@link CannotKnow} where hosts differ.
 */
final class NpmInstallChecks {

    /**
     * {@code EBADENGINE}
     */
    @Getter
    static final class UnsupportedEngine extends IllegalStateException implements NpmError {
        private final String pkgid;
        private final Map<String, @Nullable String> current;
        private final Object required;

        UnsupportedEngine(String pkgid, Map<String, @Nullable String> current, Object required) {
            super("Unsupported engine");
            this.pkgid = pkgid;
            this.current = current;
            this.required = required;
        }

        @Override
        public String getCode() {
            return "EBADENGINE";
        }
    }

    /**
     * {@code EBADPLATFORM}
     */
    @Getter
    static final class UnsupportedPlatform extends IllegalStateException implements NpmError {
        private final String pkgid;
        private final Map<String, @Nullable String> current;
        private final Map<String, @Nullable Object> required;

        UnsupportedPlatform(String pkgid, Map<String, @Nullable String> current,
                Map<String, @Nullable Object> required) {
            super("Unsupported platform");
            this.pkgid = pkgid;
            this.current = current;
            this.required = required;
        }

        @Override
        public String getCode() {
            return "EBADPLATFORM";
        }
    }

    /**
     * The node releases that count as hosts. Below its own engines npm 11.17.0 warns and runs on (lib/cli/entry.js),
     * as it does on node 18.20.8. No older release was tried.
     */
    static final String NODE_HOSTS = ">=18.0.0";

    private NpmInstallChecks() {
    }

    static void checkEngine(Map<String, Object> target, @Nullable String npmVer, @Nullable String nodeVer,
                            boolean force) {
        String nodev = force ? null : nodeVer;
        Object eng = target.get("engines");
        if (!truthy(eng)) {
            return;
        }
        Object node = Js.get(eng, "node");
        Boolean everyNode = !force && nodeVer == null && truthy(node) ? everyHost(node) : Boolean.TRUE;
        boolean nodeFail = truthy(nodev) && truthy(node) && !satisfies(nodev, node) ||
                           Boolean.FALSE.equals(everyNode);
        boolean npmFail = truthy(npmVer) && truthy(Js.get(eng, "npm")) && !satisfies(npmVer, Js.get(eng, "npm"));
        if (nodeFail || npmFail) {
            Map<String, @Nullable String> current = new LinkedHashMap<>();
            current.put("node", nodeVer);
            current.put("npm", npmVer);
            throw new UnsupportedEngine(Js.string(target.get("_id")), current, eng);
        }
        if (everyNode == null) {
            throw new CannotKnow("the node version npm runs on (" + Js.string(target.get("_id")) +
                                 " requires node " + Js.string(node) + ")");
        }
    }

    // TRUE when every node release in NODE_HOSTS satisfies the range, FALSE when none does, null when some do
    private static @Nullable Boolean everyHost(Object range) {
        String valid = range instanceof String ?
                NodeSemver.validRange((String) range, NodeSemver.Options.DEFAULT) : null;
        if (valid == null) {
            return Boolean.FALSE;
        }
        if (NodeSemver.subset(NODE_HOSTS, valid, NodeSemver.Options.DEFAULT)) {
            return Boolean.TRUE;
        }
        return NodeSemver.intersects(NODE_HOSTS, valid, NodeSemver.Options.DEFAULT) ? null : Boolean.FALSE;
    }

    private static boolean satisfies(String version, @Nullable Object range) {
        return NodeSemver.satisfies(version, range, NodeSemver.Options.INCLUDE_PRERELEASE);
    }

    /**
     * @param environment the {@code os}, {@code cpu} and {@code libc} the caller overrides, if any
     * @param current     the machine's own
     */
    static void checkPlatform(Map<String, Object> target, boolean force, Host environment, Host current) {
        if (force) {
            return;
        }
        String os = truthy(environment.getOs()) ? environment.getOs() : current.getOs();
        String cpu = truthy(environment.getCpu()) ? environment.getCpu() : current.getCpu();
        // currentEnv.libc(os) detects a libc on linux only
        String libc = truthy(environment.getLibc()) ? environment.getLibc() :
                "linux".equals(os) ? current.getLibc() : null;

        // a machine that reports an os but no libc has none
        boolean libcUnknown = libc == null && current.getOs() == null && (os == null || "linux".equals(os));

        Boolean osOk = truthy(target.get("os")) ? check(os, target.get("os")) : Boolean.TRUE;
        Boolean cpuOk = truthy(target.get("cpu")) ? check(cpu, target.get("cpu")) : Boolean.TRUE;
        Boolean libcOk = Boolean.TRUE;
        if (truthy(target.get("libc"))) {
            // checkList runs over an undefined libc too, which then fails whatever the list
            boolean listed = checkList(libc == null ? "" : libc, target.get("libc"));
            libcOk = libcUnknown ? null : libc != null && listed;
        }

        if (Boolean.FALSE.equals(osOk) || Boolean.FALSE.equals(cpuOk) || Boolean.FALSE.equals(libcOk)) {
            Map<String, @Nullable String> currentValues = new LinkedHashMap<>();
            currentValues.put("os", os);
            currentValues.put("cpu", cpu);
            currentValues.put("libc", libc);
            Map<String, @Nullable Object> required = new LinkedHashMap<>();
            required.put("os", target.get("os"));
            required.put("cpu", target.get("cpu"));
            required.put("libc", target.get("libc"));
            throw new UnsupportedPlatform(Js.string(target.get("_id")), currentValues, required);
        }
        if (osOk == null || cpuOk == null || libcOk == null) {
            throw new CannotKnow("the platform npm runs on (" + Js.string(target.get("_id")) + " restricts it by " +
                                 (osOk == null ? "os" : cpuOk == null ? "cpu" : "libc") + ")");
        }
    }

    // an unknown value passes only ["any"], which checkList passes whatever the value
    private static @Nullable Boolean check(@Nullable String value, Object list) {
        if (value != null) {
            return checkList(value, list);
        }
        boolean any = checkList("", list) && ("any".equals(list) ||
                                               list instanceof List && ((List<?>) list).size() == 1 &&
                                               "any".equals(((List<?>) list).get(0)));
        return any ? Boolean.TRUE : null;
    }

    @SuppressWarnings("unchecked")
    private static boolean checkList(String value, Object list) {
        List<Object> entries;
        if (list instanceof String) {
            entries = Collections.singletonList(list);
        } else if (list instanceof List) {
            entries = (List<Object>) list;
        } else {
            throw new IllegalArgumentException("list is not iterable");
        }
        if (entries.size() == 1 && "any".equals(entries.get(0))) {
            return true;
        }
        int negated = 0;
        boolean match = false;
        for (Object entry : entries) {
            if (entry == null || entry == Js.UNDEFINED) {
                throw new IllegalArgumentException("Cannot read properties of " + entry + " (reading 'charAt')");
            }
            if (!(entry instanceof String)) {
                throw new IllegalArgumentException("entry.charAt is not a function");
            }
            String s = (String) entry;
            boolean negate = s.startsWith("!");
            String test = negate ? s.substring(1) : s;
            if (negate) {
                negated++;
                if (value.equals(test)) {
                    return false;
                }
            } else {
                match = match || value.equals(test);
            }
        }
        return match || negated == entries.size();
    }
}
