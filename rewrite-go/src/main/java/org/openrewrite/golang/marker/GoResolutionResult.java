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
package org.openrewrite.golang.marker;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

/**
 * Metadata parsed from a Go module's go.mod (and optionally go.sum) file.
 * Attached as a {@link Marker} to a PlainText document representing the go.mod file.
 * <p>
 * Mirrors {@code NodeResolutionResult} / {@code PythonResolutionResult} for cross-language parity.
 * <p>
 * Separates requested dependencies (Require) from resolutions (Resolved):
 * <ul>
 *   <li>{@link #requires} — what the module asked for in go.mod</li>
 *   <li>{@link #resolvedDependencies} — what go.sum recorded (with content hashes)</li>
 * </ul>
 * <p>
 * Replace, exclude, and retract directives are captured as separate lists because
 * they have distinct semantics (version redirection, version blacklisting, version
 * self-retraction respectively).
 */
@Value
@With
public class GoResolutionResult implements Marker, RpcCodec<GoResolutionResult> {
    UUID id;

    /**
     * Module import path from the {@code module} directive, e.g. {@code github.com/foo/bar}.
     */
    @ToString.Include
    String modulePath;

    /**
     * Toolchain version from the {@code go} directive, e.g. {@code "1.21"}.
     */
    @ToString.Include
    @Nullable String goVersion;

    /**
     * Minimum toolchain version from the {@code toolchain} directive, e.g. {@code "go1.22.0"}.
     */
    @Nullable String toolchain;

    /**
     * Absolute or repo-relative path to the go.mod file.
     */
    @ToString.Include
    String path;

    /**
     * Direct requires from {@code require} directives. Includes both {@code require foo v1}
     * and items from {@code require ( ... )} blocks. Indirect requires are flagged on {@link Require#indirect}.
     */
    List<Require> requires;

    /**
     * Replace directives: {@code replace old => new} or {@code replace old v1 => new v2}.
     */
    List<Replace> replaces;

    /**
     * Exclude directives: {@code exclude foo v1} — blacklists a specific version.
     */
    List<Exclude> excludes;

    /**
     * Retract directives: {@code retract v1.0.0} or {@code retract [v1.0.0, v1.1.0]} — module
     * self-retraction of buggy published versions.
     */
    List<Retract> retracts;

    /**
     * The resolved build list. One node per selected {@code module@version}, carrying go.sum
     * content hashes and — when parse-time resolution ran — {@code go list -m} build-list metadata
     * and {@code go mod graph} edges (see {@link ResolvedDependency#deps}).
     */
    List<ResolvedDependency> resolvedDependencies;

    /**
     * Import-path -&gt; providing-module map from {@code go list -deps -json ./...}.
     * <p>
     * Go-specific: no sibling ecosystem has an import-to-coordinate map because their import name is
     * (near enough) the coordinate; Go's import path and module path diverge, so this must be
     * toolchain-derived. Empty unless parse-time resolution ran.
     */
    List<PackageModule> packageModules;

    public @Nullable Require findRequire(String module) {
        for (Require r : requires) {
            if (r.getModulePath().equals(module)) {
                return r;
            }
        }
        return null;
    }

    public @Nullable ResolvedDependency findResolved(String module) {
        for (ResolvedDependency rd : resolvedDependencies) {
            if (rd.getModulePath().equals(module)) {
                return rd;
            }
        }
        return null;
    }

    public @Nullable PackageModule findPackageModule(String importPath) {
        for (PackageModule pm : packageModules) {
            if (pm.getImportPath().equals(importPath)) {
                return pm;
            }
        }
        return null;
    }

    @Override
    public void rpcSend(GoResolutionResult after, RpcSendQueue q) {
        q.getAndSend(after, GoResolutionResult::getId);
        q.getAndSend(after, GoResolutionResult::getModulePath);
        q.getAndSend(after, GoResolutionResult::getGoVersion);
        q.getAndSend(after, GoResolutionResult::getToolchain);
        q.getAndSend(after, GoResolutionResult::getPath);
        q.getAndSendListAsRef(after, r -> r.getRequires() != null ? r.getRequires() : emptyList(),
                req -> req.getModulePath() + "@" + req.getVersion(),
                req -> req.rpcSend(req, q));
        q.getAndSendListAsRef(after, r -> r.getReplaces() != null ? r.getReplaces() : emptyList(),
                rep -> rep.getOldPath() + "@" + rep.getOldVersion() + "=>" + rep.getNewPath() + "@" + rep.getNewVersion(),
                rep -> rep.rpcSend(rep, q));
        q.getAndSendListAsRef(after, r -> r.getExcludes() != null ? r.getExcludes() : emptyList(),
                exc -> exc.getModulePath() + "@" + exc.getVersion(),
                exc -> exc.rpcSend(exc, q));
        q.getAndSendListAsRef(after, r -> r.getRetracts() != null ? r.getRetracts() : emptyList(),
                ret -> ret.getVersionRange(),
                ret -> ret.rpcSend(ret, q));
        q.getAndSendListAsRef(after, r -> r.getResolvedDependencies() != null ? r.getResolvedDependencies() : emptyList(),
                rd -> rd.getModulePath() + "@" + rd.getVersion(),
                rd -> rd.rpcSend(rd, q));
        q.getAndSendListAsRef(after, r -> r.getPackageModules() != null ? r.getPackageModules() : emptyList(),
                PackageModule::getImportPath,
                pm -> pm.rpcSend(pm, q));
    }

    @Override
    public GoResolutionResult rpcReceive(GoResolutionResult before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.id, UUID::fromString))
                .withModulePath(q.receive(before.modulePath))
                .withGoVersion(q.receive(before.goVersion))
                .withToolchain(q.receive(before.toolchain))
                .withPath(q.receive(before.path))
                .withRequires(q.receiveList(before.requires, r -> r.rpcReceive(r, q)))
                .withReplaces(q.receiveList(before.replaces, r -> r.rpcReceive(r, q)))
                .withExcludes(q.receiveList(before.excludes, r -> r.rpcReceive(r, q)))
                .withRetracts(q.receiveList(before.retracts, r -> r.rpcReceive(r, q)))
                .withResolvedDependencies(q.receiveList(before.resolvedDependencies, r -> r.rpcReceive(r, q)))
                .withPackageModules(q.receiveList(before.packageModules, pm -> pm.rpcReceive(pm, q)));
    }

    /**
     * A {@code require} directive entry.
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class Require implements RpcCodec<Require> {
        String modulePath;
        String version;
        /**
         * True if marked {@code // indirect} — brought in transitively, not directly imported by this module.
         */
        boolean indirect;

        @Override
        public void rpcSend(Require after, RpcSendQueue q) {
            q.getAndSend(after, Require::getModulePath);
            q.getAndSend(after, Require::getVersion);
            q.getAndSend(after, Require::isIndirect);
        }

        @Override
        public Require rpcReceive(Require before, RpcReceiveQueue q) {
            return before
                    .withModulePath(q.receive(before.modulePath))
                    .withVersion(q.receive(before.version))
                    .withIndirect(q.receive(before.indirect));
        }
    }

    /**
     * A {@code replace} directive, e.g. {@code replace github.com/old => github.com/new v1.2.3}
     * or {@code replace github.com/old v1.0.0 => ../local/path}.
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class Replace implements RpcCodec<Replace> {
        String oldPath;
        /** Null when the replace targets all versions of oldPath. */
        @Nullable String oldVersion;
        String newPath;
        /** Null when newPath is a local filesystem path (no version). */
        @Nullable String newVersion;

        @Override
        public void rpcSend(Replace after, RpcSendQueue q) {
            q.getAndSend(after, Replace::getOldPath);
            q.getAndSend(after, Replace::getOldVersion);
            q.getAndSend(after, Replace::getNewPath);
            q.getAndSend(after, Replace::getNewVersion);
        }

        @Override
        public Replace rpcReceive(Replace before, RpcReceiveQueue q) {
            return before
                    .withOldPath(q.receive(before.oldPath))
                    .withOldVersion(q.receive(before.oldVersion))
                    .withNewPath(q.receive(before.newPath))
                    .withNewVersion(q.receive(before.newVersion));
        }
    }

    /**
     * An {@code exclude} directive — blacklists a specific version from resolution.
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class Exclude implements RpcCodec<Exclude> {
        String modulePath;
        String version;

        @Override
        public void rpcSend(Exclude after, RpcSendQueue q) {
            q.getAndSend(after, Exclude::getModulePath);
            q.getAndSend(after, Exclude::getVersion);
        }

        @Override
        public Exclude rpcReceive(Exclude before, RpcReceiveQueue q) {
            return before
                    .withModulePath(q.receive(before.modulePath))
                    .withVersion(q.receive(before.version));
        }
    }

    /**
     * A {@code retract} directive. Either a single version or a range like {@code [v1.0.0, v1.1.0]}.
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class Retract implements RpcCodec<Retract> {
        /** Raw range expression as written in go.mod, e.g. {@code "v1.0.0"} or {@code "[v1.0.0, v1.1.0]"}. */
        String versionRange;
        /** Optional rationale from a {@code // ...} comment on the retract line. */
        @Nullable String rationale;

        @Override
        public void rpcSend(Retract after, RpcSendQueue q) {
            q.getAndSend(after, Retract::getVersionRange);
            q.getAndSend(after, Retract::getRationale);
        }

        @Override
        public Retract rpcReceive(Retract before, RpcReceiveQueue q) {
            return before
                    .withVersionRange(q.receive(before.versionRange))
                    .withRationale(q.receive(before.rationale));
        }
    }

    /**
     * A resolved module in the build list. Merges go.sum content hashes with {@code go list -m}
     * build-list metadata and {@code go mod graph} edges. The toolchain-sourced fields are
     * empty/false/null when parse-time resolution did not run (go.sum-only fallback).
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class ResolvedDependency implements RpcCodec<ResolvedDependency> {
        @ToString.Include
        String modulePath;

        @ToString.Include
        String version;

        /**
         * Module zip hash from go.sum (e.g. {@code h1:abc123...}). Null when only the go.mod hash is recorded.
         */
        @Nullable String moduleHash;

        /**
         * Hash of this module's own go.mod file from go.sum. Null when unavailable.
         */
        @Nullable String goModHash;

        /** From {@code go list -m}: this module is present only transitively. */
        boolean indirect;

        /** From {@code go list -m}: this is the main module. */
        boolean main;

        /** Toolchain-applied {@code replace} target path, null if none. */
        @Nullable String replacePath;

        @Nullable String replaceVersion;

        /** This module's own {@code go} directive version, from {@code go list -m}. */
        @Nullable String moduleGoVersion;

        /**
         * Direct module dependencies of this node (from {@code go mod graph}), by {@code module@version}
         * edge reference. Resolve against {@link #resolvedDependencies}. Null when the graph is
         * unavailable. Edges (not nested nodes) keep this cycle-safe; Go's MVS gives one selected
         * version per module path.
         */
        @Nullable List<ModuleRef> deps;

        @Override
        public void rpcSend(ResolvedDependency after, RpcSendQueue q) {
            q.getAndSend(after, ResolvedDependency::getModulePath);
            q.getAndSend(after, ResolvedDependency::getVersion);
            q.getAndSend(after, ResolvedDependency::getModuleHash);
            q.getAndSend(after, ResolvedDependency::getGoModHash);
            q.getAndSend(after, ResolvedDependency::isIndirect);
            q.getAndSend(after, ResolvedDependency::isMain);
            q.getAndSend(after, ResolvedDependency::getReplacePath);
            q.getAndSend(after, ResolvedDependency::getReplaceVersion);
            q.getAndSend(after, ResolvedDependency::getModuleGoVersion);
            q.getAndSendListAsRef(after, r -> r.getDeps() != null ? r.getDeps() : emptyList(),
                    ref -> ref.getModulePath() + "@" + ref.getVersion(),
                    ref -> ref.rpcSend(ref, q));
        }

        @Override
        public ResolvedDependency rpcReceive(ResolvedDependency before, RpcReceiveQueue q) {
            return before
                    .withModulePath(q.receive(before.modulePath))
                    .withVersion(q.receive(before.version))
                    .withModuleHash(q.receive(before.moduleHash))
                    .withGoModHash(q.receive(before.goModHash))
                    .withIndirect(q.receive(before.indirect))
                    .withMain(q.receive(before.main))
                    .withReplacePath(q.receive(before.replacePath))
                    .withReplaceVersion(q.receive(before.replaceVersion))
                    .withModuleGoVersion(q.receive(before.moduleGoVersion))
                    .withDeps(q.receiveList(before.deps, ref -> ref.rpcReceive(ref, q)));
        }
    }

    /**
     * A {@code module@version} reference — a {@code go mod graph} edge target.
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class ModuleRef implements RpcCodec<ModuleRef> {
        String modulePath;
        String version;

        @Override
        public void rpcSend(ModuleRef after, RpcSendQueue q) {
            q.getAndSend(after, ModuleRef::getModulePath);
            q.getAndSend(after, ModuleRef::getVersion);
        }

        @Override
        public ModuleRef rpcReceive(ModuleRef before, RpcReceiveQueue q) {
            return before
                    .withModulePath(q.receive(before.modulePath))
                    .withVersion(q.receive(before.version));
        }
    }

    /**
     * Which module provides an imported package, from {@code go list -deps -json ./...}.
     * {@link #modulePath} is null for the standard library ({@link #standard} true).
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class PackageModule implements RpcCodec<PackageModule> {
        String importPath;

        @Nullable String modulePath;

        @Nullable String version;

        boolean standard;

        @Override
        public void rpcSend(PackageModule after, RpcSendQueue q) {
            q.getAndSend(after, PackageModule::getImportPath);
            q.getAndSend(after, PackageModule::getModulePath);
            q.getAndSend(after, PackageModule::getVersion);
            q.getAndSend(after, PackageModule::isStandard);
        }

        @Override
        public PackageModule rpcReceive(PackageModule before, RpcReceiveQueue q) {
            return before
                    .withImportPath(q.receive(before.importPath))
                    .withModulePath(q.receive(before.modulePath))
                    .withVersion(q.receive(before.version))
                    .withStandard(q.receive(before.standard));
        }
    }
}
