/*
 * Copyright 2025 the original author or authors.
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
import * as rpc from "vscode-jsonrpc/node";
import {RecipeRegistry} from "../../recipe";
import * as path from "path";
import * as fs from "fs";
import {spawn} from "child_process";
import {withMetrics} from "./metrics";

export interface InstallRecipesResponse {
    recipesInstalled: number
}

/**
 * Helper function to spawn npm/npx commands with logging and promise handling
 */
async function spawnNpmCommand(
    command: string,
    args: string[],
    cwd: string,
    logger?: rpc.Logger,
    logPrefix?: string
): Promise<void> {
    const child = spawn(command, args, {cwd});

    if (logger) {
        const prefix = logPrefix ? `${logPrefix}: ` : '';
        child.stdout.on("data", (data: any) => {
            logger.info(`${prefix}${data.toString().trim()}`);
        });
        child.stderr.on("data", (data: any) => {
            logger.error(`${prefix}${data.toString().trim()}`);
        });
    }

    return new Promise<void>((resolve, reject) => {
        child.on("error", reject);
        child.on("close", (exitCode: number) => {
            if (exitCode === 0) {
                resolve();
            } else {
                const commandStr = `${command} ${args.join(' ')}`;
                reject(new Error(`${commandStr} exited with code ${exitCode}`));
            }
        });
    });
}

export class InstallRecipes {
    /**
     * Install a recipe from a local path or an NPM package.
     * @param recipes The path to a file on disk that provides an activate function or the
     * name of an NPM package.
     */
    constructor(private readonly recipes: string | { packageName: string, version?: string }) {
    }

    static handle(connection: rpc.MessageConnection, installDir: string, registry: RecipeRegistry,
                  logger?: rpc.Logger, metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType<InstallRecipes, InstallRecipesResponse, Error>("InstallRecipes"),
            withMetrics<InstallRecipes, InstallRecipesResponse>(
                "InstallRecipes",
                metricsCsv,
                (context) => async (request) => {
                    context.target = typeof request.recipes === "object" ? request.recipes.packageName : request.recipes;
                    const beforeInstall = registry.all.size;
                    let resolvedPath;
                    let recipesName = request.recipes;

                    if (typeof request.recipes === "object") {
                        const recipePackage = request.recipes;
                        const absoluteInstallDir = path.isAbsolute(installDir) ? installDir : path.join(process.cwd(), installDir);

                        // Ensure directory exists
                        if (!fs.existsSync(absoluteInstallDir)) {
                            fs.mkdirSync(absoluteInstallDir, {recursive: true});
                        }

                        // Check if package.json exists, if not create one
                        const packageJsonPath = path.join(absoluteInstallDir, "package.json");
                        if (!fs.existsSync(packageJsonPath)) {
                            // Create a minimal package.json with a custom name
                            const packageJson = {
                                name: "openrewrite-recipes",
                                version: "1.0.0",
                                description: "OpenRewrite recipe marketplace",
                                private: true
                            };
                            fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2));
                            if (logger) {
                                logger.info("Created package.json for recipe dependencies");
                            }
                        }

                        // Rather than using npm on PATH, use `node_cli.js`.
                        // https://stackoverflow.com/questions/15957529/can-i-install-a-npm-package-from-javascript-running-in-node-js
                        const packageSpec = recipePackage.packageName + (recipePackage.version ? `@${recipePackage.version}` : "");
                        await spawnNpmCommand("npm", ["install", packageSpec, "--no-fund"], absoluteInstallDir, logger);
                        resolvedPath = require.resolve(path.join(absoluteInstallDir, "node_modules", recipePackage.packageName));
                        recipesName = request.recipes.packageName;
                    } else {
                        resolvedPath = request.recipes;
                    }

                    let recipeModule;
                    try {
                        // Pre-load core modules that are used by recipes but loaded lazily
                        // This ensures they're in require.cache before setupSharedDependencies runs
                        preloadCoreModules(logger);

                        setupSharedDependencies(resolvedPath, logger);
                        recipeModule = require(resolvedPath);
                    } catch (e: any) {
                        throw new Error(`Failed to load recipe module from ${resolvedPath}: ${e.stack}`);
                    }

                    if (typeof recipeModule.activate === "function") {
                        // noinspection JSVoidFunctionReturnValueUsed
                        const activatePromise = recipeModule.activate(registry);
                        // noinspection SuspiciousTypeOfGuard
                        if (activatePromise instanceof Promise) {
                            await activatePromise;
                        }
                    } else {
                        throw new Error(`${recipesName} does not export an 'activate' function`);
                    }

                    return {recipesInstalled: registry.all.size - beforeInstall};
                }
            )
        );
    }
}

/**
 * Pre-loads core modules that are typically loaded lazily by recipes.
 * This ensures they're in require.cache before setupSharedDependencies runs,
 * so they can be properly mapped to avoid instanceof failures.
 */
function preloadCoreModules(logger?: rpc.Logger) {
    const modulesToPreload = [
        '../..',
        '../../java',
        '../../javascript',
        '../../json',
        '../../rpc',
        '../../search',
        '../../text',
    ];

    modulesToPreload.forEach(modulePath => {
        try {
            require(modulePath);
            if (logger) {
                logger.info(`[preloadCoreModules] Loaded ${modulePath}`);
            }
        } catch (e) {
            if (logger) {
                logger.warn(`[preloadCoreModules] Failed to load ${modulePath}: ${e}`);
            }
        }
    });
}

/**
 * Ensures dynamically loaded modules share the same class instances as the host
 * by mapping require.cache entries. This prevents instanceof failures when the
 * same package is installed in multiple node_modules directories.
 */
function setupSharedDependencies(targetModulePath: string, logger?: rpc.Logger) {
    const sharedDeps = ['@openrewrite/rewrite', 'vscode-jsonrpc'];
    const targetDir = path.dirname(targetModulePath);

    sharedDeps.forEach(depName => {
        try {
            // Step 1: Find where this package is currently loaded from (host)
            const hostPackageEntry = require.resolve(depName);

            // Step 2: Find the package root by looking for package.json
            let hostPackageRoot = path.dirname(hostPackageEntry);
            while (hostPackageRoot !== path.dirname(hostPackageRoot)) {
                const packageJsonPath = path.join(hostPackageRoot, 'package.json');
                if (fs.existsSync(packageJsonPath)) {
                    try {
                        const pkg = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
                        if (pkg.name === depName) {
                            break; // Found the package root
                        }
                    } catch (e) {
                        // Not a valid package.json, continue
                    }
                }
                hostPackageRoot = path.dirname(hostPackageRoot);
            }

            if (logger) {
                logger.info(`[setupSharedDependencies] Host package root for ${depName}: ${hostPackageRoot}`);
            }

            // Step 3: Find where the target's node_modules has this package
            // We explicitly look in node_modules to avoid finding npm-linked global packages
            let targetPackageRoot: string | undefined;

            // Walk up from targetDir looking for node_modules containing this package
            let searchDir = targetDir;
            while (searchDir !== path.dirname(searchDir)) {
                const nodeModulesPath = path.join(searchDir, 'node_modules', ...depName.split('/'));
                if (fs.existsSync(nodeModulesPath)) {
                    const packageJsonPath = path.join(nodeModulesPath, 'package.json');
                    if (fs.existsSync(packageJsonPath)) {
                        try {
                            const pkg = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
                            if (pkg.name === depName) {
                                targetPackageRoot = nodeModulesPath;
                                break;
                            }
                        } catch (e) {
                            // Not a valid package.json, continue
                        }
                    }
                }
                searchDir = path.dirname(searchDir);
            }

            if (!targetPackageRoot) {
                if (logger) {
                    logger.warn(`[setupSharedDependencies] Could not find ${depName} in target's node_modules`);
                }
                return; // Can't map this package
            }

            if (logger) {
                logger.info(`[setupSharedDependencies] Target package root for ${depName}: ${targetPackageRoot}`);
            }

            // If they're the same, no mapping needed
            if (hostPackageRoot === targetPackageRoot) {
                if (logger) {
                    logger.info(`[setupSharedDependencies] Same package root, no mapping needed for ${depName}`);
                }
                return;
            }

            // Step 4: Map all cached modules from host package to target package
            const hostPrefix = hostPackageRoot + path.sep;

            let mappedCount = 0;
            for (const cachedPath of Object.keys(require.cache)) {
                if (cachedPath.startsWith(hostPrefix)) {
                    // This module belongs to the host package
                    const relativePath = cachedPath.substring(hostPrefix.length);
                    const targetPath = path.join(targetPackageRoot, relativePath);

                    // Map the target path to use the host's cached module
                    require.cache[targetPath] = require.cache[cachedPath];
                    mappedCount++;
                }
            }

            if (logger) {
                logger.info(`[setupSharedDependencies] Mapped ${mappedCount} modules for ${depName}`);
            }
        } catch (e) {
            if (logger) {
                logger.error(`[setupSharedDependencies] Failed to setup ${depName}: ${e}`);
            }
        }
    });
}
