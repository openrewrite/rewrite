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
import * as fs from 'fs';
import * as fsp from 'fs/promises';
import * as path from 'path';
import {spawn} from 'child_process';
import {Recipe, RecipeRegistry} from '../recipe';
import {ExecutionContext} from '../execution';
import {ProgressCallback, scheduleRunStreaming} from '../run';
import {TreePrinters} from '../print';
import {activate} from '../index';
import {
    colorizeDiff,
    discoverFiles,
    findRecipe,
    installRecipePackage,
    isAcceptedFile,
    loadLocalRecipes,
    parseFilesStreaming,
    parseRecipeOptions,
    parseRecipeSpec
} from './cli-utils';
import {ValidateParsingRecipe} from './validate-parsing-recipe';

const isTTY = process.stderr.isTTY ?? false;
const SPINNER_FRAMES = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'];

class Spinner {
    private frameIndex = 0;
    private interval: NodeJS.Timeout | null = null;
    private message = '';
    private running = false;

    start(message: string): void {
        if (!isTTY) return;
        this.message = message;
        this.running = true;
        this.render();
        this.interval = setInterval(() => this.render(), 80);
    }

    update(message: string): void {
        this.message = message;
        if (!isTTY) return;
        this.render();
    }

    private render(): void {
        const frame = SPINNER_FRAMES[this.frameIndex];
        this.frameIndex = (this.frameIndex + 1) % SPINNER_FRAMES.length;
        process.stderr.write(`\r\x1b[K${frame} ${this.message}`);
    }

    /**
     * Temporarily clear the spinner line for other output.
     * Call resume() to restore it.
     */
    clear(): void {
        if (!isTTY || !this.running) return;
        process.stderr.write('\r\x1b[K');
    }

    /**
     * Resume the spinner after clear().
     */
    resume(): void {
        if (!isTTY || !this.running) return;
        this.render();
    }

    stop(): void {
        this.running = false;
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
        }
        if (isTTY) {
            process.stderr.write('\r\x1b[K');
        }
    }
}

// Import language modules to register printers
import '../text';
import '../json';
import '../java';
import '../javascript';

// Set stack size for complex parsing
require('v8').setFlagsFromString('--stack-size=8000');

const DEFAULT_RECIPE_DIR = path.join(process.env.HOME || '', '.rewrite', 'javascript', 'recipes');

/**
 * Check if a directory is a git repository root.
 */
function isGitRoot(dir: string): boolean {
    return fs.existsSync(path.join(dir, '.git'));
}

/**
 * Find the project root by scanning upward for a directory containing package.json.
 * Stops at git repository root or filesystem root.
 * Returns the starting directory if no package.json is found.
 */
function findProjectRoot(startDir: string): string {
    let dir = path.resolve(startDir);
    while (true) {
        const packageJsonPath = path.join(dir, 'package.json');
        if (fs.existsSync(packageJsonPath)) {
            return dir;
        }
        // Stop at git root - don't scan beyond the repository
        if (isGitRoot(dir)) {
            return dir;
        }
        const parent = path.dirname(dir);
        if (parent === dir) {
            // Reached filesystem root, return original directory
            return path.resolve(startDir);
        }
        dir = parent;
    }
}

interface CliOptions {
    apply: boolean;
    list: boolean;
    recipeDir?: string;
    option: string[];
    verbose: boolean;
    debug: boolean;
}

/**
 * Convert a project-root-relative path to a CWD-relative path for display.
 */
function toCwdRelative(sourcePath: string, projectRoot: string): string {
    const absolutePath = path.join(projectRoot, sourcePath);
    return path.relative(process.cwd(), absolutePath) || '.';
}

/**
 * Transform diff output to use CWD-relative paths in headers.
 */
function transformDiffPaths(diff: string, projectRoot: string): string {
    return diff.replace(
        /^(---|\+\+\+) (.+)$/gm,
        (_match, prefix, filePath) => {
            if (filePath) {
                const cwdRelative = toCwdRelative(filePath, projectRoot);
                return `${prefix} ${cwdRelative}`;
            }
            return _match;
        }
    );
}

async function main() {
    const program = new Command();
    program
        .name('rewrite')
        .description('Run OpenRewrite recipes on your codebase')
        .argument('<recipe>', 'Recipe to run in format "package:recipe" (e.g., "@openrewrite/recipes-nodejs:replace-deprecated-slice")')
        .argument('[paths...]', 'Files or directories to process (defaults to project root)')
        .option('--apply', 'Apply changes to files (default is dry-run showing diffs)', false)
        .option('-l, --list', 'Only list paths of files that would be changed', false)
        .option('-d, --recipe-dir <dir>', 'Directory for recipe installation', DEFAULT_RECIPE_DIR)
        .option('-o, --option <option...>', 'Recipe options in format "key=value"', [])
        .option('-v, --verbose', 'Enable verbose output', false)
        .option('--debug', 'Start with Node.js debugger (--inspect-brk)', false)
        .parse();

    const recipeArg = program.args[0];
    const pathArgs = program.args.slice(1);
    const opts = program.opts() as CliOptions;
    opts.option = opts.option || [];

    // Handle --debug by re-spawning with --inspect-brk
    if (opts.debug) {
        const args = process.argv.slice(2).filter(arg => arg !== '--debug');
        const child = spawn(process.execPath, ['--inspect-brk', process.argv[1], ...args], {
            stdio: 'inherit'
        });
        child.on('exit', (code) => process.exit(code ?? 0));
        return;
    }

    // Handle special built-in recipes
    let recipe: Recipe | undefined;
    const isValidateParsing = recipeArg === 'validate-parsing';

    if (!isValidateParsing) {
        // Parse recipe specification
        const recipeSpec = parseRecipeSpec(recipeArg);
        if (!recipeSpec) {
            console.error(`Invalid recipe format: ${recipeArg}`);
            console.error('Expected format: "package:recipe" (e.g., "@openrewrite/recipes-nodejs:replace-deprecated-slice")');
            console.error('Or use "validate-parsing" to check for parse errors and idempotence.');
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
        const foundRecipe = findRecipe(registry, recipeSpec.recipeName, recipeOptions);
        if (!foundRecipe) {
            process.exit(1);
        }
        recipe = foundRecipe;

        if (opts.verbose) {
            console.log(`Running recipe: ${recipe.name}`);
        }
    }

    const spinner = new Spinner();

    // Determine paths to process and project root
    let targetPaths: string[];
    let projectRoot: string;

    if (pathArgs.length > 0) {
        // Explicit paths provided - find project root and validate they're all in the same project
        targetPaths = pathArgs.map(p => path.resolve(p));
        const projectRoots = new Set<string>();
        for (const targetPath of targetPaths) {
            const pathDir = fs.statSync(targetPath).isDirectory() ? targetPath : path.dirname(targetPath);
            projectRoots.add(findProjectRoot(pathDir));
        }

        if (projectRoots.size > 1) {
            console.error('Error: Specified paths belong to different projects:');
            for (const root of projectRoots) {
                console.error(`  - ${root}`);
            }
            console.error('\nAll paths must be within the same project (share a common package.json root).');
            process.exit(1);
        }

        projectRoot = projectRoots.values().next().value!;

        if (opts.verbose) {
            if (targetPaths.length > 1) {
                console.log(`Processing ${targetPaths.length} paths`);
            }
            console.log(`Project root: ${projectRoot}`);
        }
    } else {
        // No paths provided - find project root from CWD and process entire project
        projectRoot = findProjectRoot(process.cwd());
        targetPaths = [projectRoot];

        if (opts.verbose) {
            console.log(`Processing entire project: ${projectRoot}`);
        }
    }

    // Discover source files from all specified paths
    if (!opts.verbose) {
        spinner.start('Discovering source files...');
    }
    const sourceFiles: string[] = [];
    for (const targetPath of targetPaths) {
        const stat = await fsp.stat(targetPath);
        if (stat.isDirectory()) {
            const files = await discoverFiles(targetPath, opts.verbose);
            sourceFiles.push(...files);
        } else if (stat.isFile() && isAcceptedFile(targetPath)) {
            sourceFiles.push(targetPath);
        }
    }
    // Remove duplicates (in case of overlapping paths)
    const uniqueSourceFiles = [...new Set(sourceFiles)];
    spinner.stop();

    if (uniqueSourceFiles.length === 0) {
        console.log('No source files found to process.');
        return;
    }

    if (opts.verbose) {
        console.log(`Found ${uniqueSourceFiles.length} source files`);
    }

    // Create validate-parsing recipe now that we know the project root
    if (isValidateParsing) {
        const validateRecipe = new ValidateParsingRecipe({projectRoot});
        // Set up reporting callback that coordinates with spinner
        validateRecipe.onReport = (message: string) => {
            if (!opts.verbose) {
                spinner.clear();
            }
            console.log(message);
            if (!opts.verbose) {
                spinner.resume();
            }
        };
        recipe = validateRecipe;
    }

    // Create streaming parser - files are parsed on-demand as they're consumed
    const totalFiles = uniqueSourceFiles.length;
    const sourceFileStream = parseFilesStreaming(uniqueSourceFiles, projectRoot, {
        verbose: opts.verbose
    });

    // Run the recipe with streaming output - for non-scanning recipes,
    // parsing and processing happen concurrently without collecting all files first
    const ctx = new ExecutionContext();
    let changeCount = 0;
    let processedCount = 0;

    // Progress callback for spinner updates during all phases (disabled in verbose mode)
    const onProgress: ProgressCallback | undefined = opts.verbose ? undefined : (phase, current, total, sourcePath) => {
        const totalStr = total > 0 ? total : totalFiles;
        const phaseLabel = phase.charAt(0).toUpperCase() + phase.slice(1);
        spinner.update(`${phaseLabel} [${current}/${totalStr}] ${sourcePath}`);
    };

    if (!opts.verbose) {
        spinner.start(`Processing ${totalFiles} files...`);
    }

    for await (const result of scheduleRunStreaming(recipe!, sourceFileStream, ctx, onProgress)) {
        processedCount++;
        const currentPath = result.after?.sourcePath ?? result.before?.sourcePath ?? '';

        const statusMsg = `Processing [${processedCount}/${totalFiles}] ${currentPath}`;

        // Skip unchanged files - first check object identity (fast path),
        // then check actual diff content for files where visitor returned new object
        if (result.before === result.after) {
            continue; // Definitely unchanged
        }

        if (opts.apply) {
            // Apply changes immediately
            if (result.after) {
                const filePath = path.join(projectRoot, result.after.sourcePath);
                const content = await TreePrinters.print(result.after);

                // Ensure directory exists
                await fsp.mkdir(path.dirname(filePath), {recursive: true});
                await fsp.writeFile(filePath, content);

                changeCount++;
                if (!opts.verbose) spinner.stop();
                const displayPath = toCwdRelative(result.after.sourcePath, projectRoot);
                if (result.before) {
                    console.log(`  Modified: ${displayPath}`);
                } else {
                    console.log(`  Created: ${displayPath}`);
                }
                if (!opts.verbose) spinner.start(statusMsg);
            } else if (result.before) {
                const filePath = path.join(projectRoot, result.before.sourcePath);
                await fsp.unlink(filePath);
                changeCount++;
                if (!opts.verbose) spinner.stop();
                console.log(`  Deleted: ${toCwdRelative(result.before.sourcePath, projectRoot)}`);
                if (!opts.verbose) spinner.start(statusMsg);
            }
        } else {
            // Dry-run mode: show diff or just list paths
            const diff = await result.diff();
            const hasChanges = diff.split('\n').some(line =>
                (line.startsWith('+') || line.startsWith('-')) &&
                !line.startsWith('+++') && !line.startsWith('---')
            );
            if (hasChanges) {
                changeCount++;
                if (!opts.verbose) spinner.stop();
                if (opts.list) {
                    // Just list the path
                    const displayPath = toCwdRelative(
                        result.after?.sourcePath ?? result.before?.sourcePath ?? '',
                        projectRoot
                    );
                    console.log(displayPath);
                } else {
                    // Show the diff with CWD-relative paths
                    console.log(colorizeDiff(transformDiffPaths(diff, projectRoot)));
                }
                if (!opts.verbose) spinner.start(statusMsg);
            }
        }
    }

    if (!opts.verbose) spinner.stop();

    // For validate-parsing recipe, check for errors and exit accordingly
    if (recipe instanceof ValidateParsingRecipe) {
        if (!recipe.hasErrors) {
            console.log('All files parsed successfully.');
        }
        process.exit(recipe.hasErrors ? 1 : 0);
    }

    if (changeCount === 0) {
        console.log('No changes to make.');
    } else if (opts.apply) {
        console.log(`\n${changeCount} file(s) changed.`);
    } else if (!opts.list) {
        console.log(`\n${changeCount} file(s) would be changed. Run with --apply to apply changes.`);
    }
}

main().catch((err) => {
    console.error('Fatal error:', err.message);
    process.exit(1);
});
