#!/usr/bin/env node
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
import {Command} from 'commander';
import * as fsp from 'fs/promises';
import * as path from 'path';
import {RecipeRegistry} from '../recipe';
import {ExecutionContext} from '../execution';
import {scheduleRun} from '../run';
import {TreePrinters} from '../print';
import {activate} from '../index';
import {
    colorizeDiff,
    discoverFiles,
    findRecipe,
    installRecipePackage,
    loadLocalRecipes,
    parseFiles,
    parseRecipeOptions,
    parseRecipeSpec
} from './cli-utils';

// Import language modules to register printers
import '../text';
import '../json';
import '../java';
import '../javascript';

// Set stack size for complex parsing
require('v8').setFlagsFromString('--stack-size=8000');

const DEFAULT_RECIPE_DIR = path.join(process.env.HOME || '', '.rewrite', 'javascript', 'recipes');

interface CliOptions {
    dryRun: boolean;
    recipeDir?: string;
    option: string[];
    verbose: boolean;
}

async function main() {
    const program = new Command();
    program
        .name('rewrite')
        .description('Run OpenRewrite recipes on your codebase')
        .argument('<recipe>', 'Recipe to run in format "package:recipe" (e.g., "@openrewrite/recipes-nodejs:replace-deprecated-slice")')
        .option('-n, --dry-run', 'Show what changes would be made without applying them', false)
        .option('-d, --recipe-dir <dir>', 'Directory for recipe installation', DEFAULT_RECIPE_DIR)
        .option('-o, --option <option...>', 'Recipe options in format "key=value"', [])
        .option('-v, --verbose', 'Enable verbose output', false)
        .parse();

    const recipeArg = program.args[0];
    const opts = program.opts() as CliOptions;
    opts.option = opts.option || [];

    // Parse recipe specification
    const recipeSpec = parseRecipeSpec(recipeArg);
    if (!recipeSpec) {
        console.error(`Invalid recipe format: ${recipeArg}`);
        console.error('Expected format: "package:recipe" (e.g., "@openrewrite/recipes-nodejs:replace-deprecated-slice")');
        process.exit(1);
    }

    if (opts.verbose) {
        console.log(`Package: ${recipeSpec.packageName}`);
        console.log(`Recipe: ${recipeSpec.recipeName}`);
    }

    // Parse recipe options
    const recipeOptions = parseRecipeOptions(opts.option);
    if (opts.verbose && Object.keys(recipeOptions).length > 0) {
        console.log(`Options: ${JSON.stringify(recipeOptions)}`);
    }

    // Set up recipe registry
    const registry = new RecipeRegistry();

    // Register built-in recipes
    await activate(registry);

    // Load recipes from local path or install from NPM
    try {
        if (recipeSpec.isLocalPath) {
            await loadLocalRecipes(recipeSpec.packageName, registry, opts.verbose);
        } else {
            await installRecipePackage(recipeSpec.packageName, opts.recipeDir || DEFAULT_RECIPE_DIR, registry, opts.verbose);
        }
    } catch (e: any) {
        console.error(`Failed to load recipes: ${e.message}`);
        process.exit(1);
    }

    // Find the recipe
    const recipe = findRecipe(registry, recipeSpec.recipeName, recipeOptions);
    if (!recipe) {
        process.exit(1);
    }

    if (opts.verbose) {
        console.log(`Running recipe: ${recipe.name}`);
    }

    // Discover source files
    const projectRoot = process.cwd();
    const sourceFiles = await discoverFiles(projectRoot, opts.verbose);

    if (sourceFiles.length === 0) {
        console.log('No source files found to process.');
        return;
    }

    if (opts.verbose) {
        console.log(`Found ${sourceFiles.length} source files`);
    }

    // Parse all source files
    const parsedFiles = await parseFiles(sourceFiles, projectRoot, opts.verbose);

    if (parsedFiles.length === 0) {
        console.log('No files could be parsed.');
        return;
    }

    // Run the recipe
    const ctx = new ExecutionContext();
    const {changeset} = await scheduleRun(recipe, parsedFiles, ctx);

    if (changeset.length === 0) {
        console.log('No changes to make.');
        return;
    }

    console.log(`\n${changeset.length} file(s) ${opts.dryRun ? 'would be ' : ''}changed:\n`);

    // Handle results
    for (const result of changeset) {
        if (opts.dryRun) {
            // Print colorized diff
            const diff = await result.diff();
            console.log(colorizeDiff(diff));
        } else {
            // Apply changes
            if (result.after) {
                const filePath = path.join(projectRoot, result.after.sourcePath);
                const content = await TreePrinters.print(result.after);

                // Ensure directory exists
                await fsp.mkdir(path.dirname(filePath), {recursive: true});
                await fsp.writeFile(filePath, content);

                if (result.before) {
                    console.log(`  Modified: ${result.after.sourcePath}`);
                } else {
                    console.log(`  Created: ${result.after.sourcePath}`);
                }
            } else if (result.before) {
                const filePath = path.join(projectRoot, result.before.sourcePath);
                await fsp.unlink(filePath);
                console.log(`  Deleted: ${result.before.sourcePath}`);
            }
        }
    }

    if (!opts.dryRun) {
        console.log('\nChanges applied successfully.');
    }
}

main().catch((err) => {
    console.error('Fatal error:', err.message);
    process.exit(1);
});
