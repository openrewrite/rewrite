import * as rpc from "vscode-jsonrpc/node";
import {RecipeRegistry} from "../../recipe";
import * as path from "path";
import * as fs from "fs";
import {spawn} from "child_process";

export interface InstallRecipesResponse {
    recipesInstalled: number
}

export class InstallRecipes {
    /**
     * Install a recipe from a local path or an NPM package.
     * @param recipes The path to a file on disk that provides an activate function or the
     * name of an NPM package.
     */
    constructor(private readonly recipes: string | { packageName: string, version?: string }) {
    }

    static handle(connection: rpc.MessageConnection, relativeInstallDir: string, registry: RecipeRegistry): void {
        connection.onRequest(new rpc.RequestType<InstallRecipes, InstallRecipesResponse, Error>("InstallRecipes"), async (request) => {
            const beforeInstall = registry.all.size;
            let resolvedPath;
            let recipesName = request.recipes;

            if (typeof request.recipes === "object") {
                const recipePackage = request.recipes;
                const installDir = path.join(process.cwd(), relativeInstallDir);
                await new Promise<void>((resolve, reject) => {

                    if (!fs.existsSync(installDir)) {
                        fs.mkdirSync(installDir, {recursive: true});
                        fs.writeFileSync(path.join(installDir, "package.json"),
                            `{"name": "please-work"}`)
                    }

                    // Rather than using npm on PATH, use `node_cli.js`.
                    // https://stackoverflow.com/questions/15957529/can-i-install-a-npm-package-from-javascript-running-in-node-js
                    const packageSpec = recipePackage.packageName + (recipePackage.version ? `@${recipePackage.version}` : "");
                    const installer = spawn("npm", ["install", packageSpec], {
                        cwd: installDir
                    });
                    // installer.stdout.on("data", (data: any) => {
                    //     // TODO write this to rpc log instead
                    //     console.log(data.toString());
                    // });
                    // installer.stderr.on("data", (data: any) => {
                    //     // TODO write this to rpc log instead
                    //     console.log(data.toString());
                    // });
                    installer.on("error", reject);
                    installer.on("close", (exitCode: number) => {
                        if (exitCode === 0) {
                            resolve();
                        } else {
                            reject(new Error(`npm install exited with code ${exitCode}`));
                        }
                    });
                });
                resolvedPath = require.resolve(path.join(installDir, "node_modules", recipePackage.packageName));
                recipesName = request.recipes.packageName;
            } else {
                resolvedPath = request.recipes;
            }

            let recipeModule;
            try {
                recipeModule = require(resolvedPath);
            } catch (e: any) {
                throw new Error(`Failed to load recipe module from ${resolvedPath}: ${e.stack}`);
            }

            if (typeof recipeModule.activate === "function") {
                recipeModule.activate(registry);
            } else {
                throw new Error(`${recipesName} does not export an 'activate' function`);
            }

            return {recipesInstalled: registry.all.size - beforeInstall};
        });
    }
}
