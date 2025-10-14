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
                        setupSharedDependencies(resolvedPath);
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
 * Ensures dynamically loaded modules share the same class instances as the host
 * by mapping require.cache entries. This prevents instanceof failures when the
 * same package is installed in multiple node_modules directories.
 */
function setupSharedDependencies(targetModulePath: string) {
    const sharedDeps = ['@openrewrite/rewrite', 'vscode-jsonrpc'];
    const targetDir = path.dirname(targetModulePath);

    sharedDeps.forEach(depName => {
        const depPattern = path.sep + 'node_modules' + path.sep + depName.replace('/', path.sep);

        for (const cachedPath of Object.keys(require.cache)) {
            if (!cachedPath.includes(depPattern)) continue;

            try {
                // Extract subpath: /path/node_modules/@pkg/dist/tree.js -> dist/tree.js
                const pkgIndex = cachedPath.indexOf(depPattern);
                let subpath = cachedPath.substring(pkgIndex + depPattern.length)
                    .replace(/^[/\\]/, '')           // Remove leading slash
                    .replace(/\.(js|ts)$/, '')        // Remove extension
                    .replace(/^dist[/\\]/, '')        // Remove dist/ prefix if present
                    .replace(/[/\\]index$/, '');      // Remove /index suffix

                // Build require path: @pkg or @pkg/subpath
                const requirePath = subpath ? `${depName}/${subpath}` : depName;

                // Resolve from target's perspective and map cache
                const targetDepPath = require.resolve(requirePath, {paths: [targetDir]});
                require.cache[targetDepPath] = require.cache[cachedPath];
            } catch (e) {
                // Target can't resolve this path, skip
            }
        }
    });
}
