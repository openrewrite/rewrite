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
package org.openrewrite.javascript.internal.lock.pnpm;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of @pnpm/config.reader src/types.ts and src/npmConfigTypes.ts (pnpm 11.2.2).
 */
final class ConfigTypes {

    enum Type {
        ARRAY, BOOLEAN, NUMBER, STRING, PATH, URL
    }

    /**
     * The {@code null} variant of a union.
     */
    static final Object NULL = new Object();

    /**
     * Each setting's schema: a {@link Type}, or a union of types, string and boolean literals and {@link #NULL}.
     */
    static final Map<String, Object> TYPES;

    private static final Type ARRAY = Type.ARRAY;
    private static final Type BOOLEAN = Type.BOOLEAN;
    private static final Type NUMBER = Type.NUMBER;
    private static final Type STRING = Type.STRING;
    private static final Type PATH = Type.PATH;
    private static final Type URL = Type.URL;

    static {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("auto-install-peers", BOOLEAN);
        t.put("bail", BOOLEAN);
        t.put("ci", BOOLEAN);
        t.put("cache-dir", STRING);
        t.put("catalog-mode", union("strict", "prefer", "manual"));
        t.put("child-concurrency", NUMBER);
        t.put("merge-git-branch-lockfiles", BOOLEAN);
        t.put("merge-git-branch-lockfiles-branch-pattern", ARRAY);
        t.put("color", union("always", "auto", "never"));
        t.put("config-dir", STRING);
        t.put("dangerously-allow-all-builds", BOOLEAN);
        t.put("deploy-all-files", BOOLEAN);
        t.put("dedupe-peer-dependents", BOOLEAN);
        t.put("dedupe-peers", BOOLEAN);
        t.put("dedupe-direct-deps", BOOLEAN);
        t.put("dedupe-injected-deps", BOOLEAN);
        t.put("dev", union(NULL, true));
        t.put("dir", STRING);
        t.put("disallow-workspace-cycles", BOOLEAN);
        t.put("enable-modules-dir", BOOLEAN);
        t.put("enable-pre-post-scripts", BOOLEAN);
        t.put("enable-global-virtual-store", BOOLEAN);
        t.put("exclude-links-from-lockfile", BOOLEAN);
        t.put("extend-node-path", BOOLEAN);
        t.put("fetch-timeout", NUMBER);
        t.put("fetch-warn-timeout-ms", NUMBER);
        t.put("fetch-min-speed-ki-bps", NUMBER);
        t.put("fetching-concurrency", NUMBER);
        t.put("filter", union(STRING, ARRAY));
        t.put("filter-prod", union(STRING, ARRAY));
        t.put("force-legacy-deploy", BOOLEAN);
        t.put("frozen-lockfile", BOOLEAN);
        t.put("git-checks", BOOLEAN);
        t.put("git-shallow-hosts", ARRAY);
        t.put("global-bin-dir", STRING);
        t.put("global-dir", STRING);
        t.put("global-path", STRING);
        t.put("global-pnpmfile", STRING);
        t.put("git-branch-lockfile", BOOLEAN);
        t.put("hoist", BOOLEAN);
        t.put("http-proxy", union(NULL, STRING));
        t.put("hoist-pattern", ARRAY);
        t.put("hoist-workspace-packages", BOOLEAN);
        t.put("ignore-compatibility-db", BOOLEAN);
        t.put("ignore-pnpmfile", BOOLEAN);
        t.put("ignore-workspace", BOOLEAN);
        t.put("ignore-workspace-cycles", BOOLEAN);
        t.put("ignore-workspace-root-check", BOOLEAN);
        t.put("optimistic-repeat-install", BOOLEAN);
        t.put("include-workspace-root", BOOLEAN);
        t.put("init-package-manager", BOOLEAN);
        t.put("init-type", union("commonjs", "module"));
        t.put("inject-workspace-packages", BOOLEAN);
        t.put("legacy-dir-filtering", BOOLEAN);
        t.put("link-workspace-packages", union(BOOLEAN, "deep"));
        t.put("lockfile", BOOLEAN);
        t.put("lockfile-dir", STRING);
        t.put("lockfile-include-tarball-url", BOOLEAN);
        t.put("lockfile-only", BOOLEAN);
        t.put("loglevel", union("silent", "error", "warn", "info", "debug"));
        t.put("maxsockets", NUMBER);
        t.put("modules-cache-max-age", NUMBER);
        t.put("dlx-cache-max-age", NUMBER);
        t.put("minimum-release-age", NUMBER);
        t.put("minimum-release-age-exclude", union(STRING, ARRAY));
        t.put("minimum-release-age-ignore-missing-time", BOOLEAN);
        t.put("minimum-release-age-strict", BOOLEAN);
        t.put("modules-dir", STRING);
        t.put("network-concurrency", NUMBER);
        t.put("node-linker", union("pnp", "isolated", "hoisted"));
        t.put("noproxy", STRING);
        t.put("npm-path", STRING);
        t.put("npmrc-auth-file", PATH);
        t.put("offline", BOOLEAN);
        t.put("pack-destination", STRING);
        t.put("pack-gzip-level", NUMBER);
        t.put("package-import-method", union("auto", "hardlink", "clone", "copy"));
        t.put("patches-dir", STRING);
        t.put("pnpmfile", STRING);
        t.put("pm-on-fail", union("download", "error", "warn", "ignore"));
        t.put("prefer-frozen-lockfile", BOOLEAN);
        t.put("prefer-offline", BOOLEAN);
        t.put("prefer-symlinked-executables", BOOLEAN);
        t.put("prefer-workspace-packages", BOOLEAN);
        t.put("preserve-absolute-paths", BOOLEAN);
        t.put("production", union(NULL, true));
        t.put("public-hoist-pattern", ARRAY);
        t.put("publish-branch", STRING);
        t.put("recursive-install", BOOLEAN);
        t.put("block-exotic-subdeps", BOOLEAN);
        t.put("reporter", STRING);
        t.put("resolution-mode", union("highest", "time-based", "lowest-direct"));
        t.put("resolve-peers-from-workspace-root", BOOLEAN);
        t.put("runtime", BOOLEAN);
        t.put("runtime-on-fail", union("ignore", "warn", "error", "download"));
        t.put("aggregate-output", BOOLEAN);
        t.put("reporter-hide-prefix", BOOLEAN);
        t.put("save-peer", BOOLEAN);
        t.put("save-catalog-name", STRING);
        t.put("save-workspace-protocol", BOOLEAN);
        t.put("script-shell", STRING);
        t.put("shamefully-hoist", BOOLEAN);
        t.put("shared-workspace-lockfile", BOOLEAN);
        t.put("shell-emulator", BOOLEAN);
        t.put("side-effects-cache", BOOLEAN);
        t.put("side-effects-cache-readonly", BOOLEAN);
        t.put("symlink", BOOLEAN);
        t.put("sort", BOOLEAN);
        t.put("state-dir", STRING);
        t.put("store-dir", STRING);
        t.put("stream", BOOLEAN);
        t.put("strict-dep-builds", BOOLEAN);
        t.put("strict-store-pkg-content-check", BOOLEAN);
        t.put("strict-peer-dependencies", BOOLEAN);
        t.put("trust-policy", union("off", "no-downgrade"));
        t.put("trust-policy-exclude", union(STRING, ARRAY));
        t.put("trust-policy-ignore-after", NUMBER);
        t.put("use-beta-cli", BOOLEAN);
        t.put("use-stderr", BOOLEAN);
        t.put("verify-deps-before-run", BOOLEAN);
        t.put("verify-store-integrity", BOOLEAN);
        t.put("global-virtual-store-dir", STRING);
        t.put("virtual-store-dir", STRING);
        t.put("virtual-store-only", BOOLEAN);
        t.put("virtual-store-dir-max-length", NUMBER);
        t.put("peers-suffix-max-length", NUMBER);
        t.put("workspace-concurrency", NUMBER);
        t.put("workspace-packages", union(STRING, ARRAY));
        t.put("workspace-root", BOOLEAN);
        t.put("yes", BOOLEAN);
        t.put("test-pattern", union(STRING, ARRAY));
        t.put("changed-files-ignore-pattern", union(STRING, ARRAY));
        t.put("embed-readme", BOOLEAN);
        t.put("update-notifier", BOOLEAN);
        t.put("agent", union(NULL, STRING));
        t.put("registry-supports-time-field", BOOLEAN);
        t.put("fail-if-no-match", BOOLEAN);
        t.put("sync-injected-deps-after-scripts", ARRAY);
        t.put("cpu", union(STRING, ARRAY));
        t.put("libc", union(STRING, ARRAY));
        t.put("os", union(STRING, ARRAY));
        t.put("audit-level", union("low", "moderate", "high", "critical"));

        t.put("access", union(NULL, "restricted", "public"));
        t.put("allow-same-version", BOOLEAN);
        t.put("bin-links", BOOLEAN);
        t.put("ca", union(NULL, STRING, ARRAY));
        t.put("cafile", PATH);
        t.put("cert", union(NULL, STRING));
        t.put("commit-hooks", BOOLEAN);
        t.put("depth", NUMBER);
        t.put("description", BOOLEAN);
        t.put("dev", BOOLEAN);
        t.put("dry-run", BOOLEAN);
        t.put("engine-strict", BOOLEAN);
        t.put("fetch-retries", NUMBER);
        t.put("fetch-retry-factor", NUMBER);
        t.put("fetch-retry-mintimeout", NUMBER);
        t.put("fetch-retry-maxtimeout", NUMBER);
        t.put("force", BOOLEAN);
        t.put("git", STRING);
        t.put("git-tag-version", BOOLEAN);
        t.put("global", BOOLEAN);
        t.put("https-proxy", union(NULL, URL));
        t.put("ignore-scripts", BOOLEAN);
        t.put("init-author-name", STRING);
        t.put("init-author-email", STRING);
        t.put("init-author-url", union("", URL));
        t.put("init-license", STRING);
        t.put("init-version", STRING);
        t.put("json", BOOLEAN);
        t.put("key", union(NULL, STRING));
        t.put("local-address", STRING);
        t.put("long", BOOLEAN);
        t.put("maxsockets", NUMBER);
        t.put("message", STRING);
        t.put("node-options", union(NULL, STRING));
        t.put("node-version", union(NULL, STRING));
        t.put("no-proxy", union(NULL, STRING, ARRAY));
        t.put("offline", BOOLEAN);
        t.put("only", union(NULL, "dev", "development", "prod", "production"));
        t.put("optional", BOOLEAN);
        t.put("otp", union(NULL, STRING));
        t.put("package-lock", BOOLEAN);
        t.put("parseable", BOOLEAN);
        t.put("prefer-offline", BOOLEAN);
        t.put("prefix", PATH);
        t.put("production", BOOLEAN);
        t.put("progress", BOOLEAN);
        t.put("provenance", BOOLEAN);
        t.put("proxy", union(NULL, false, URL));
        t.put("registry", union(NULL, URL));
        t.put("save", BOOLEAN);
        t.put("save-dev", BOOLEAN);
        t.put("save-exact", BOOLEAN);
        t.put("save-optional", BOOLEAN);
        t.put("save-prefix", STRING);
        t.put("save-prod", BOOLEAN);
        t.put("scope", STRING);
        t.put("script-shell", union(NULL, STRING));
        t.put("scripts-prepend-node-path", union(false, true, "auto", "warn-only"));
        t.put("sign-git-tag", BOOLEAN);
        t.put("strict-ssl", BOOLEAN);
        t.put("tag", STRING);
        t.put("tag-version-prefix", STRING);
        t.put("unsafe-perm", BOOLEAN);
        t.put("user-agent", STRING);
        t.put("userconfig", PATH);
        t.put("umask", NUMBER);
        t.put("version", BOOLEAN);
        TYPES = Collections.unmodifiableMap(t);
    }

    private ConfigTypes() {
    }

    private static Object union(Object... variants) {
        return Collections.unmodifiableList(Arrays.asList(variants));
    }
}
