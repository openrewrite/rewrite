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

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Port of @pnpm/installing.deps-installer src/install/extendInstallOptions.ts (pnpm 11.2.2): extendOptions, as far
 * as a lock-only install reads it.
 */
final class ExtendInstallOptions {

    private ExtendInstallOptions() {
    }

    /**
     * The InstallOptions installDeps and recursive.ts give the install: the config and what they set over it.
     */
    @Value
    static class InstallOptions {
        PnpmOptions config;

        /**
         * The overrides of the project installed, which its packageConfigs may replace.
         */
        Map<String, String> overrides;

        /**
         * Every project of the workspace, which {@code workspace:} specs resolve to; null outside a workspace.
         */
        @Nullable
        List<Verification.Project> allProjects;

        boolean pruneLockfileImporters;

        /**
         * The npm resolver of the store controller installDeps creates.
         */
        NpmResolver storeController;

        /**
         * setupPolicyHandlers' handleResolutionPolicyViolations; null when no policy has a handler.
         */
        @Nullable
        Consumer<List<ResolveDependencies.PolicyViolation>> handleResolutionPolicyViolations;
    }

    /**
     * ProcessedInstallOptions: the options with the overrides parsed and the readPackage hook made of them.
     */
    @Value
    static class StrictInstallOptions {
        InstallOptions opts;

        List<ParseOverrides.VersionOverride> parsedOverrides;

        @Nullable
        ReadPackageHook readPackageHook;

        PnpmOptions getConfig() {
            return opts.getConfig();
        }
    }

    static StrictInstallOptions extendOptions(InstallOptions opts, String lockfileDir) {
        PnpmOptions config = opts.getConfig();
        List<ParseOverrides.VersionOverride> parsedOverrides = ParseOverrides.parseOverrides(opts.getOverrides(),
                config.getCatalogs(), config.getRelease());
        ReadPackageHook readPackageHook = ReadPackageHook.createReadPackageHook(
                config.isTrue("ignoreCompatibilityDb"), lockfileDir, parsedOverrides,
                config.get("ignoredOptionalDependencies"), config.get("packageExtensions"), config.getRelease());
        if (Js.truthy(config.get("virtualStoreOnly")) && !Js.truthy(config.get("enableModulesDir")) &&
            !Js.truthy(config.get("enableGlobalVirtualStore"))) {
            throw new PnpmError("CONFIG_CONFLICT_VIRTUAL_STORE_ONLY_WITH_NO_MODULES_DIR", "Cannot use " +
                    "virtualStoreOnly when enableModulesDir is false (the standard virtual store requires " +
                    "node_modules/.pnpm)");
        }
        if (!config.isTrue("useLockfile")) {
            throw new PnpmError("CONFIG_CONFLICT_LOCKFILE_ONLY_WITH_NO_LOCKFILE", "Cannot generate a " +
                    LockfileReader.WANTED_LOCKFILE + " because lockfile is set to false");
        }
        return new StrictInstallOptions(opts, parsedOverrides, readPackageHook);
    }
}
