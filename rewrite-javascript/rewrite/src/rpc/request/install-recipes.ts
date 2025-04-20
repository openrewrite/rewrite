import * as rpc from "vscode-jsonrpc/node";
import {RecipeRegistry} from "../../recipe";
import * as path from "path";
import * as fs from "fs";
import {spawn} from "child_process";

export type RecipeDependency = string | { packageName: string, version?: string };

export interface InstallRecipesResponse {
    recipesInstalled: number
}

export class InstallRecipes {
    constructor(private readonly recipeDependency: RecipeDependency,
                private readonly installDir: string) {
    }

    static handle(connection: rpc.MessageConnection): void {
        connection.onRequest(new rpc.RequestType<InstallRecipes, InstallRecipesResponse, Error>("InstallRecipes"), async (request) => {
            const installDir = request.installDir;

            try {
                const allRecipes = RecipeRegistry.all;
                const beforeInstall = allRecipes.size;

                if (typeof request.recipeDependency === "object") {
                    if (!fs.existsSync(installDir)) {
                        fs.mkdirSync(installDir, {recursive: true});
                        fs.writeFileSync(path.join(installDir, "package.json"),
                            `{"name": "please-work"}`)
                    }
                    const recipePkg = request.recipeDependency.packageName;

                    await new Promise<void>((resolve, reject) => {
                        // Rather than using npm on PATH, use `node_cli.js`.
                        // https://stackoverflow.com/questions/15957529/can-i-install-a-npm-package-from-javascript-running-in-node-js
                        const installer = spawn("npm", ["install", recipePkg], {
                            cwd: installDir
                        });
                        installer.stdout.on("data", (data: any) => {
                            // TODO write this to rpc log instead
                            console.log(data.toString());
                        });
                        installer.stderr.on("data", (data: any) => {
                            // TODO write this to rpc log instead
                            console.log(data.toString());
                        });
                        installer.on("error", reject);
                        installer.on("close", (exitCode: number) => {
                            if (exitCode === 0) {
                                resolve();
                            } else {
                                reject(new Error(`npm install exited with code ${exitCode}`));
                            }
                        });
                    });

                    // Locate the host’s copy of @openrewrite/rewrite on disk
                    const hostRewritePath = path.dirname(
                        require.resolve(".")
                    );

                    // Where the recipe’s node_modules put @openrewrite/rewrite
                    const recipeRewritePath = path.join(
                        installDir, "node_modules", "@openrewrite", "rewrite"
                    );

                    // Delete whatever npm dropped there, then link
                    fs.rmSync(recipeRewritePath, {recursive: true, force: true});
                    fs.mkdirSync(path.dirname(recipeRewritePath), {recursive: true});
                    fs.symlinkSync(hostRewritePath, recipeRewritePath, "junction");

                    try {
                        const resolvedPath = require.resolve(path.join(
                            installDir, "node_modules", recipePkg
                        ));
                        console.log("Resolved path:", resolvedPath);
                        const recipeExports = require(resolvedPath);
                        console.log("Loaded module exports:", recipeExports);
                    } catch (error) {
                        console.log(error);
                    }
                } else {
                    require(request.recipeDependency);
                }

                return {recipesInstalled: allRecipes.size - beforeInstall};
            } catch (error: any) {
                console.log(error.stack);
                throw new Error(`Failed to install or require module ${request.recipeDependency}: ${error?.message}`);
            }
        });
    }
}
