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
import * as fs from 'fs';
import * as path from 'path';
import {spawn} from 'child_process';
import {Recipe, RecipeRegistry} from '../recipe';
import {SourceFile} from '../tree';
import {ProjectParser} from '../javascript/project-parser';

// ANSI color codes
const colors = {
    red: '\x1b[31m',
    green: '\x1b[32m',
    cyan: '\x1b[36m',
    reset: '\x1b[0m',
};

/**
 * Colorize unified diff output for terminal display
 */
export function colorizeDiff(diff: string): string {
    // Check if stdout supports colors
    if (!process.stdout.isTTY) {
        return diff;
    }

    return diff
        .split('\n')
        .map(line => {
            if (line.startsWith('---') || line.startsWith('+++')) {
                return colors.cyan + line + colors.reset;
            } else if (line.startsWith('-')) {
                return colors.red + line + colors.reset;
            } else if (line.startsWith('+')) {
                return colors.green + line + colors.reset;
            } else if (line.startsWith('@@')) {
                return colors.cyan + line + colors.reset;
            }
            return line;
        })
        .join('\n');
}

export interface RecipeSpec {
    packageName: string;
    recipeName: string;
    isLocalPath: boolean;
}

/**
 * Parse a recipe specification in format "package:recipe" or "/path/to/module:recipe"
 *
 * Examples:
 *   @openrewrite/recipes-nodejs:replace-deprecated-slice
 *   some-package:my-recipe
 *   @scope/package:org.openrewrite.recipe.name
 *   /Users/dev/my-recipes:my-recipe
 *   ./local-recipes:my-recipe
 *   ../other-recipes:my-recipe
 */
export function parseRecipeSpec(arg: string): RecipeSpec | null {
    // Format: "package:recipe" where package can be scoped (@scope/package) or a local path
    const lastColonIndex = arg.lastIndexOf(':');
    if (lastColonIndex === -1) {
        return null;
    }

    const packageName = arg.substring(0, lastColonIndex);
    const recipeName = arg.substring(lastColonIndex + 1);

    if (!packageName || !recipeName) {
        return null;
    }

    // Check if this is a local path
    const isLocalPath = packageName.startsWith('/') ||
        packageName.startsWith('./') ||
        packageName.startsWith('../') ||
        /^[A-Za-z]:[\\/]/.test(packageName); // Windows absolute path

    return {packageName, recipeName, isLocalPath};
}

/**
 * Parse recipe options from command line format
 *
 * Options can be:
 *   - key=value pairs (e.g., "text=hello")
 *   - boolean flags (just the key name, implies true)
 *   - JSON values (e.g., "items=[1,2,3]")
 */
export function parseRecipeOptions(options: string[]): Record<string, any> {
    const result: Record<string, any> = {};

    for (const opt of options) {
        const eqIndex = opt.indexOf('=');
        if (eqIndex === -1) {
            // Boolean flag
            result[opt] = true;
        } else {
            const key = opt.substring(0, eqIndex);
            const value = opt.substring(eqIndex + 1);

            // Try to parse as JSON for complex types
            try {
                result[key] = JSON.parse(value);
            } catch {
                result[key] = value;
            }
        }
    }

    return result;
}

/**
 * Load recipes from a local directory path
 */
export async function loadLocalRecipes(
    localPath: string,
    registry: RecipeRegistry,
    verbose: boolean = false
): Promise<void> {
    // Resolve the path
    const resolvedPath = path.resolve(localPath);

    if (!fs.existsSync(resolvedPath)) {
        throw new Error(`Local path does not exist: ${resolvedPath}`);
    }

    // Check if it's a directory or file
    const stat = fs.statSync(resolvedPath);
    let modulePath: string;

    if (stat.isDirectory()) {
        // Look for package.json to find the main entry point
        const packageJsonPath = path.join(resolvedPath, 'package.json');
        let mainFromPkg: string | undefined;
        if (fs.existsSync(packageJsonPath)) {
            const pkg = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
            mainFromPkg = pkg.main;
        }

        // Try the main field first, then common fallbacks
        const candidates = [
            mainFromPkg,
            'dist/index.js',
            'dist/src/index.js',
            'index.js',
            'src/index.ts'
        ].filter((c): c is string => !!c);

        const found = candidates.find(c => fs.existsSync(path.join(resolvedPath, c)));
        if (found) {
            modulePath = path.join(resolvedPath, found);
        } else {
            throw new Error(`Cannot find entry point in ${resolvedPath}. Tried: ${candidates.join(', ')}`);
        }
    } else {
        modulePath = resolvedPath;
    }

    if (!fs.existsSync(modulePath)) {
        throw new Error(`Module entry point not found: ${modulePath}. Did you run 'npm run build'?`);
    }

    if (verbose) {
        console.log(`Loading recipes from ${modulePath}...`);
    }

    const recipeModule = require(modulePath);

    if (typeof recipeModule.activate !== 'function') {
        throw new Error(`${localPath} does not export an 'activate' function`);
    }

    const activatePromise = recipeModule.activate(registry);
    if (activatePromise instanceof Promise) {
        await activatePromise;
    }

    if (verbose) {
        console.log(`Loaded ${registry.all.size} recipes`);
    }
}

/**
 * Install a recipe package from NPM and load its recipes into the registry
 */
export async function installRecipePackage(
    packageName: string,
    installDir: string,
    registry: RecipeRegistry,
    verbose: boolean = false
): Promise<void> {
    // Ensure directory exists
    if (!fs.existsSync(installDir)) {
        fs.mkdirSync(installDir, {recursive: true});
    }

    // Check if package.json exists, if not create one
    const packageJsonPath = path.join(installDir, 'package.json');
    if (!fs.existsSync(packageJsonPath)) {
        const packageJson = {
            name: 'openrewrite-recipes',
            version: '1.0.0',
            description: 'OpenRewrite recipe installation',
            private: true
        };
        fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2));
        if (verbose) {
            console.log('Created package.json for recipe dependencies');
        }
    }

    // Install the package
    if (verbose) {
        console.log(`Installing ${packageName}...`);
    }

    await new Promise<void>((resolve, reject) => {
        const child = spawn('npm', ['install', packageName, '--no-fund'], {
            cwd: installDir,
            stdio: verbose ? 'inherit' : 'pipe'
        });

        child.on('error', reject);
        child.on('close', (exitCode) => {
            if (exitCode === 0) {
                resolve();
            } else {
                reject(new Error(`npm install exited with code ${exitCode}`));
            }
        });
    });

    // Load the module and activate recipes
    const resolvedPath = require.resolve(path.join(installDir, 'node_modules', packageName));
    const recipeModule = require(resolvedPath);

    if (typeof recipeModule.activate !== 'function') {
        throw new Error(`${packageName} does not export an 'activate' function`);
    }

    const activatePromise = recipeModule.activate(registry);
    if (activatePromise instanceof Promise) {
        await activatePromise;
    }

    if (verbose) {
        console.log(`Loaded ${registry.all.size} recipes`);
    }
}

/**
 * Find a recipe by name in the registry
 *
 * Supports:
 *   - Exact match by FQN
 *   - Match by suffix (last segment of FQN)
 *   - Partial match (case-insensitive)
 *
 * Returns null if not found or if ambiguous (prints error message)
 */
export function findRecipe(
    registry: RecipeRegistry,
    recipeName: string,
    options: Record<string, any>
): Recipe | null {
    // Try exact match first
    const RecipeClass = registry.all.get(recipeName);
    if (RecipeClass) {
        return new RecipeClass(options);
    }

    // Try matching by suffix (last segment of FQN)
    const matches: string[] = [];
    for (const name of registry.all.keys()) {
        if (name.endsWith('.' + recipeName) || name.endsWith('-' + recipeName) ||
            name === recipeName || name.toLowerCase().endsWith(recipeName.toLowerCase())) {
            matches.push(name);
        }
    }

    if (matches.length === 0) {
        // Try partial match
        for (const name of registry.all.keys()) {
            if (name.toLowerCase().includes(recipeName.toLowerCase())) {
                matches.push(name);
            }
        }
    }

    if (matches.length === 0) {
        console.error(`Recipe not found: ${recipeName}`);
        console.error('\nAvailable recipes:');
        for (const name of [...registry.all.keys()].sort()) {
            console.error(`  ${name}`);
        }
        return null;
    }

    if (matches.length > 1) {
        console.error(`Ambiguous recipe name: ${recipeName}`);
        console.error('\nMatching recipes:');
        for (const name of matches.sort()) {
            console.error(`  ${name}`);
        }
        console.error('\nPlease use a more specific recipe name.');
        return null;
    }

    const MatchedRecipeClass = registry.all.get(matches[0])!;
    return new MatchedRecipeClass(options);
}

/**
 * Discover source files in a project directory, respecting .gitignore.
 * Delegates to ProjectParser for file discovery.
 */
export async function discoverFiles(projectRoot: string, verbose: boolean = false): Promise<string[]> {
    const parser = new ProjectParser(projectRoot, {verbose});
    const discovered = await parser.discoverFiles();

    // Flatten all discovered files into a single array
    return [
        ...discovered.packageJsonFiles,
        ...discovered.lockFiles.json,
        ...discovered.lockFiles.yaml,
        ...discovered.lockFiles.text,
        ...discovered.jsFiles,
        ...discovered.jsonFiles,
        ...discovered.yamlFiles,
        ...discovered.textFiles
    ];
}

/**
 * Check if a file is accepted for parsing based on its extension.
 */
export function isAcceptedFile(filePath: string): boolean {
    const ext = path.extname(filePath).toLowerCase();
    const basename = path.basename(filePath);

    // JavaScript/TypeScript files
    if (['.js', '.jsx', '.ts', '.tsx', '.mjs', '.mts', '.cjs', '.cts'].includes(ext)) {
        return true;
    }

    // JSON files
    if (ext === '.json') {
        return true;
    }

    // YAML files
    if (['.yaml', '.yml'].includes(ext)) {
        return true;
    }

    // Lock files (yarn.lock has no extension)
    if (['yarn.lock', 'pnpm-lock.yaml', 'package-lock.json', 'bun.lock'].includes(basename)) {
        return true;
    }

    // Text config files
    if (['.prettierignore', '.gitignore', '.npmignore', '.eslintignore'].includes(basename)) {
        return true;
    }

    return false;
}

export type ProgressCallback = (current: number, total: number, filePath: string) => void;

export interface ParseFilesOptions {
    verbose?: boolean;
    onProgress?: ProgressCallback;
}

/**
 * Parse source files using appropriate parsers (streaming version).
 * Yields source files as they are parsed, allowing immediate processing.
 *
 * Uses ProjectParser with a file filter to parse only the specified files.
 * This handles Prettier detection, file classification, and appropriate parser selection.
 */
export async function* parseFilesStreaming(
    filePaths: string[],
    projectRoot: string,
    options: ParseFilesOptions = {}
): AsyncGenerator<SourceFile, void, undefined> {
    const {verbose = false, onProgress} = options;

    // Create a set for fast lookup
    const fileSet = new Set(filePaths.map(f => path.resolve(f)));
    let current = 0;
    const total = filePaths.length;

    const parser = new ProjectParser(projectRoot, {
        verbose,
        fileFilter: (absolutePath) => fileSet.has(absolutePath),
        onProgress: onProgress ? (phase, cur, tot, filePath) => {
            if (phase === "parsing" && filePath) {
                current++;
                onProgress(current, total, filePath);
            }
        } : undefined
    });

    yield* parser.parse();
}

/**
 * Parse source files using appropriate parsers.
 * Collects all parsed files into an array.
 */
export async function parseFiles(
    filePaths: string[],
    projectRoot: string,
    options: ParseFilesOptions = {}
): Promise<SourceFile[]> {
    const parsed: SourceFile[] = [];
    for await (const sf of parseFilesStreaming(filePaths, projectRoot, options)) {
        parsed.push(sf);
    }
    return parsed;
}
