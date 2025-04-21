import * as rpc from "vscode-jsonrpc/node";
import {RecipeRegistry} from "../../recipe";
import * as path from "path";
import * as fs from "fs";
import {spawn} from "child_process";

export interface InstallRecipesResponse {
    recipesInstalled: number
}

export class InstallRecipes {
    constructor(
        private readonly packageName: string,
        private readonly version?: string) {
    }

    static handle(connection: rpc.MessageConnection, relativeInstallDir: string, registry: RecipeRegistry): void {
        connection.onRequest(new rpc.RequestType<InstallRecipes, InstallRecipesResponse, Error>("InstallRecipes"), async (request) => {
            const installDir = path.join(process.cwd(), relativeInstallDir);
            const beforeInstall = registry.all.size;

            if (!fs.existsSync(installDir)) {
                fs.mkdirSync(installDir, {recursive: true});
                fs.writeFileSync(path.join(installDir, "package.json"),
                    `{"name": "please-work"}`)
            }

            await new Promise<void>((resolve, reject) => {
                // Rather than using npm on PATH, use `node_cli.js`.
                // https://stackoverflow.com/questions/15957529/can-i-install-a-npm-package-from-javascript-running-in-node-js
                const packageSpec = request.packageName + (request.version ? `@${request.version}` : "");
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

            const resolvedPath = require.resolve(path.join(installDir, "node_modules", request.packageName));
            const recipeModule = require(resolvedPath);

            if (typeof recipeModule.activate === "function") {
                recipeModule.activate(registry);
            } else {
                throw new Error(`${request.packageName} does not export an 'activate' function`);
            }

            return {recipesInstalled: registry.all.size - beforeInstall};
        });
    }
}
