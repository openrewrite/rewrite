import * as rpc from "vscode-jsonrpc/node";
import {RecipeRegistry} from "../../recipe";

export interface InstallRecipesResponse {
    recipesInstalled: number
}

export class InstallRecipes {
    constructor(private readonly recipeModule: string) {
    }

    static handle(connection: rpc.MessageConnection): void {
        connection.onRequest(new rpc.RequestType<InstallRecipes, InstallRecipesResponse, Error>("InstallRecipes"), async (request) => {
            try {
                const {spawn} = require('child_process');
                const path = require('path');

                // Install the requested recipe module asynchronously
                await new Promise<void>((resolve, reject) => {
                    const installer = spawn('npm', ['install', request.recipeModule], {
                        cwd: process.cwd(),
                        stdio: 'inherit'
                    });
                    installer.on('error', reject);
                    installer.on('close', (exitCode: number) => {
                        if (exitCode === 0) {
                            resolve();
                        } else {
                            reject(new Error(`npm install exited with code ${exitCode}`));
                        }
                    });
                });

                const beforeInstall = RecipeRegistry.all.size

                import(request.recipeModule)

                return {recipesInstalled: RecipeRegistry.all.size - beforeInstall};
            } catch (error: any) {
                throw new Error(`Failed to install or require module ${request.recipeModule}: ${error?.message}`);
            }
        });
    }
}
