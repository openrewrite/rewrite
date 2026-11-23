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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.pnpm.ExtendInstallOptions.InstallOptions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Port of @pnpm/installing.commands src/recursive.ts (pnpm 11.2.2) for {@code pnpm install}: every project of the
 * workspace into the lock of lockfileDir, or without one each into its own with its hooks and packageConfigs.
 */
final class Recursive {

    private final PnpmLockRegenerator.Inputs inputs;
    private final RegistryMetadata registry;
    private final CreateNewStoreController stores;
    private final Instant now;
    private final Install install;

    Recursive(PnpmLockRegenerator.Inputs inputs, RegistryMetadata registry, CreateNewStoreController stores,
              Instant now, Install install) {
        this.inputs = inputs;
        this.registry = registry;
        this.stores = stores;
        this.now = now;
        this.install = install;
    }

    /**
     * What the run installs into the lock, after the other projects' installs of the same run: with lockfileDir,
     * mutateModules over every project; else the install of the project in the lock's directory.
     */
    Install.Result recursive(List<Verification.Project> allProjects, PnpmOptions opts, NpmResolver storeController) {
        String lockDir = install.getLockDir();
        String lockfileDir = opts.getLockfileDir(lockDir);
        Consumer<List<ResolveDependencies.PolicyViolation>> policyHandlers = InstallDeps.setupPolicyHandlers(opts);
        InstallOptions installOpts = new InstallOptions(opts, opts.getOverrides(), allProjects, true,
                storeController, policyHandlers);
        Map<String, Map<String, @Nullable Object>> projectConfigRecord =
                ProjectConfigs.createProjectConfigRecord(opts.get("packageConfigs"));
        if (lockfileDir != null) {
            failOnAnInvalidDependenciesField(allProjects, opts.getRelease());
            if (!lockfileDir.equals(lockDir)) {
                return install.unchanged(installOpts);
            }
            List<Verification.Project> importers = new ArrayList<>();
            for (Verification.Project project : allProjects) {
                importers.add(new Verification.Project(project.getRootDir(), FindPackages.getLockfileImporterId(
                        RootRelativePath.relative(lockDir, project.getRootDir())), project.getManifest()));
            }
            return install.mutateModules(importers, null, installOpts);
        }
        Verification.Project own = null;
        for (Verification.Project project : allProjects) {
            if (project.getRootDir().equals(lockDir)) {
                own = project;
            }
        }
        Install.Result result;
        PnpmError ownInvalid = null;
        if (own == null) {
            result = install.unchanged(installOpts);
        } else {
            requireHooks(opts, lockDir);
            Map<String, String> overrides = projectOverrides(ProjectConfigs.getProjectConfig(projectConfigRecord,
                    own.getManifest().get("name")), opts.getOverrides());
            result = install.install(new Verification.Project(lockDir, ".", own.getManifest()),
                    new InstallOptions(opts, overrides, allProjects, true, storeController, policyHandlers));
            // from 11.9.0 the project's manifest, written once its lock is, fails on a dependencies field that is no
            // object (PnpmRelease#rejectsANonObjectDependenciesFieldOnWrite)
            ownInvalid = Manifests.invalidDependenciesField(own.getManifest(), opts.getRelease());
        }
        requireOtherInstalls(allProjects, opts, storeController, projectConfigRecord, policyHandlers,
                ownInvalid != null && !opts.isTrue("bail") ? 1 : 0);
        if (ownInvalid != null) {
            // under bail it ends the run
            install.failAfterWrite(ownInvalid);
        }
        return result;
    }

    // the manifest writes of the projects mutateModules installed into one lock, each once the lock is written, from
    // 11.9.0 failing on a dependencies field that is no object (PnpmRelease#rejectsANonObjectDependenciesFieldOnWrite):
    // the first project's failure ends the run
    private void failOnAnInvalidDependenciesField(List<Verification.Project> projects, PnpmRelease release) {
        PnpmError first = null;
        String firstDir = null;
        for (Verification.Project project : projects) {
            PnpmError invalid = Manifests.invalidDependenciesField(project.getManifest(), release);
            if (invalid == null) {
                continue;
            }
            if (first == null) {
                first = invalid;
                firstDir = project.getRootDir();
            } else if (!String.valueOf(first.getMessage()).equals(invalid.getMessage())) {
                install.failAfterWrite(new CannotKnow("which of the manifests of " + project(firstDir) + " and " +
                                                      project(project.getRootDir()) + " pnpm writes first once it " +
                                                      "wrote the lock (each has a dependencies field that is no " +
                                                      "object)"));
                return;
            }
        }
        if (first != null) {
            install.failAfterWrite(first);
        }
    }

    // the other projects install in the same run: one that resolves shares pnpm's resolution cache with this lock's;
    // under bail its failure stops the run whether or not this lock is written yet, else throwOnCommandFail ends it
    private void requireOtherInstalls(List<Verification.Project> allProjects, PnpmOptions opts,
                                      NpmResolver storeController,
                                      @Nullable Map<String, Map<String, @Nullable Object>> projectConfigRecord,
                                      @Nullable Consumer<List<ResolveDependencies.PolicyViolation>> policyHandlers,
                                      int ownFailures) {
        String lockDir = install.getLockDir();
        boolean bail = opts.isTrue("bail");
        int failures = ownFailures;
        Map<String, String> ownOverrides = null;
        for (Verification.Project project : allProjects) {
            String dir = project.getRootDir();
            if (dir.equals(lockDir)) {
                continue;
            }
            try {
                requireHooks(opts, dir);
            } catch (PnpmError e) {
                failed(dir, e, bail);
                failures++;
                continue;
            } catch (CannotKnow e) {
                // getContext runs its readPackage hooks over its manifest, so its lock cannot say whether it resolves
                throw sharesCacheWith(dir);
            }
            String lockPath = RootRelativePath.join(dir, LockfileReader.WANTED_LOCKFILE);
            String lock = CannotKnow.requireRead(inputs.getFiles().get(lockPath), inputs.getPaths(), lockPath);
            if (lock == null && inputs.getPaths() == null) {
                throw new CannotKnow("whether " + lockfilePath(dir) + ", which the install of " + project(dir) +
                                     " in the same run reads, exists (the host does not say which files the " +
                                     "project holds)");
            }
            if (ownOverrides == null) {
                ownOverrides = projectOverrides(projectConfigRecord, lockDir, allProjects, opts);
            }
            Map<String, String> overrides = projectOverrides(projectConfigRecord, dir, allProjects, opts);
            // without a lock of its own the project installs from scratch
            Install other = new Install(inputs, dir, new EnvLockfile.OnDisk(lock, opts.getRelease()), registry,
                    stores, now);
            Install.Result otherInstall;
            try {
                otherInstall = other.install(new Verification.Project(dir, ".", project.getManifest()),
                        new InstallOptions(opts, overrides, allProjects, true, storeController, policyHandlers));
            } catch (RuntimeException e) {
                failed(dir, e, bail);
                failures++;
                continue;
            }
            if (otherInstall.getResolution() != null) {
                if (!ownOverrides.equals(overrides)) {
                    throw sharesCacheWith(dir);
                }
                try {
                    other.installInContext(otherInstall);
                } catch (RuntimeException e) {
                    failed(dir, e, bail);
                    failures++;
                    continue;
                }
            }
            // from 11.9.0 its manifest, written once its lock is, fails on a dependencies field that is no object
            // (PnpmRelease#rejectsANonObjectDependenciesFieldOnWrite)
            PnpmError invalid = Manifests.invalidDependenciesField(project.getManifest(), opts.getRelease());
            if (invalid != null) {
                failed(dir, invalid, bail);
                failures++;
            }
        }
        if (failures > 0) {
            // recursiveSummary.ts' throwOnCommandFail, once every project's install has run
            throw new PnpmError("RECURSIVE_FAIL", "\"pnpm recursive install\" failed in " + failures + " packages");
        }
    }

    // under bail the failure stops the run, which may or may not have written this lock yet; else pnpm counts it
    private void failed(String dir, RuntimeException e, boolean bail) {
        if (bail) {
            throw failsBeside(dir, e);
        }
        if (!(e instanceof PnpmError) && !(e instanceof JsError)) {
            throw new CannotKnow("whether the install of " + project(dir) + ", in the same run, fails (" +
                                 e.getMessage() + ")");
        }
    }

    private static Map<String, String> projectOverrides(
            @Nullable Map<String, Map<String, @Nullable Object>> projectConfigRecord, String dir,
            List<Verification.Project> allProjects, PnpmOptions opts) {
        for (Verification.Project project : allProjects) {
            if (project.getRootDir().equals(dir)) {
                return projectOverrides(ProjectConfigs.getProjectConfig(projectConfigRecord,
                        project.getManifest().get("name")), opts.getOverrides());
            }
        }
        return opts.getOverrides();
    }

    // a project's packageConfigs overrides replace the workspace's for its own lock
    private static Map<String, String> projectOverrides(@Nullable Map<String, @Nullable Object> localConfig,
                                                        Map<String, String> overrides) {
        if (localConfig == null || !localConfig.containsKey("overrides")) {
            return overrides;
        }
        Map<String, @Nullable Object> local = ProjectConfigs.properties(localConfig.get("overrides"));
        Map<String, String> out = new LinkedHashMap<>();
        for (String selector : Js.keys(local)) {
            Object spec = local.get(selector);
            if (!(spec instanceof String)) {
                throw new CannotKnow("what pnpm makes of packageConfigs overrides." + selector + " (" +
                                     Js.typeOf(spec) + ", where pnpm reads a string)");
            }
            out.put(selector, (String) spec);
        }
        return out;
    }

    // requireHooks(rootDir, opts) for a project installed into its own lock: its default pnpmfile and the pnpmfiles
    // setting (which getConfig's requireHooks does not read)
    private void requireHooks(PnpmOptions opts, String dir) {
        if (opts.isTrue("ignorePnpmfile")) {
            return;
        }
        Object pnpmfiles = opts.get("pnpmfiles");
        if (Js.truthy(pnpmfiles) && !(pnpmfiles instanceof List)) {
            throw new CannotKnow("what pnpm makes of pnpmfiles (" + Js.typeOf(pnpmfiles) + ", where pnpm reads a " +
                                 "list)");
        }
        new RequireHooks(inputs).requireHooks(dir, opts.get("globalPnpmfile"), Js.truthy(pnpmfiles) ?
                (List<?>) pnpmfiles : Collections.emptyList(), Main.tryLoadDefaultPnpmfile(opts));
    }

    private CannotKnow sharesCacheWith(String dir) {
        return new CannotKnow("what the install of " + project(dir) + ", in the same run with readPackage hooks of " +
                              "its own, leaves in the resolution cache " + lockfilePath(install.getLockDir()) +
                              " reads");
    }

    private CannotKnow failsBeside(String dir, RuntimeException e) {
        return new CannotKnow("whether pnpm writes " + lockfilePath(install.getLockDir()) + " before the install of " +
                              project(dir) + ", in the same run, fails (" + e.getMessage() + ")");
    }

    private static String project(String dir) {
        return dir.isEmpty() ? "the root project" : dir;
    }

    private static String lockfilePath(String dir) {
        return RootRelativePath.join(dir, LockfileReader.WANTED_LOCKFILE);
    }
}
