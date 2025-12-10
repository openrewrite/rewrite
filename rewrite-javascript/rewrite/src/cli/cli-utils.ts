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
import * as fsp from 'fs/promises';
import * as path from 'path';
import {spawn, spawnSync} from 'child_process';
import {Recipe, RecipeRegistry} from '../recipe';
import {SourceFile} from '../tree';
import {JavaScriptParser, PackageJsonParser} from '../javascript';
import {JsonParser} from '../json';

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
 * Discover source files in a project directory, respecting .gitignore
 */
export async function discoverFiles(projectRoot: string, verbose: boolean = false): Promise<string[]> {
    const files: string[] = [];

    if (verbose) {
        console.log(`Discovering files in ${projectRoot}...`);
    }

    // Get list of git-ignored files
    const ignoredFiles = new Set<string>();
    try {
        const result = spawnSync('git', ['ls-files', '--ignored', '--exclude-standard', '-o'], {
            cwd: projectRoot,
            encoding: 'utf8'
        });
        if (result.stdout) {
            for (const line of result.stdout.split('\n')) {
                if (line.trim()) {
                    ignoredFiles.add(path.join(projectRoot, line.trim()));
                }
            }
        }
    } catch {
        // Git not available or not a git repository
    }

    // Get tracked and untracked (but not ignored) files
    const trackedFiles = new Set<string>();
    try {
        // Get tracked files
        const tracked = spawnSync('git', ['ls-files'], {
            cwd: projectRoot,
            encoding: 'utf8'
        });
        if (tracked.stdout) {
            for (const line of tracked.stdout.split('\n')) {
                if (line.trim()) {
                    trackedFiles.add(path.join(projectRoot, line.trim()));
                }
            }
        }

        // Get untracked but not ignored files
        const untracked = spawnSync('git', ['ls-files', '--others', '--exclude-standard'], {
            cwd: projectRoot,
            encoding: 'utf8'
        });
        if (untracked.stdout) {
            for (const line of untracked.stdout.split('\n')) {
                if (line.trim()) {
                    trackedFiles.add(path.join(projectRoot, line.trim()));
                }
            }
        }
    } catch {
        // Not a git repository, fall back to recursive directory scan
        await walkDirectory(projectRoot, files, ignoredFiles, projectRoot);
        return files.filter(isAcceptedFile);
    }

    // Filter to accepted file types that exist on disk
    // (git ls-files returns deleted files that are still tracked)
    for (const file of trackedFiles) {
        if (!ignoredFiles.has(file) && isAcceptedFile(file) && fs.existsSync(file)) {
            files.push(file);
        }
    }

    return files;
}

/**
 * Walk a directory recursively, collecting files
 */
export async function walkDirectory(
    dir: string,
    files: string[],
    ignored: Set<string>,
    projectRoot: string
): Promise<void> {
    const entries = await fsp.readdir(dir, {withFileTypes: true});

    for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);

        // Skip hidden files and common ignore patterns
        if (entry.name.startsWith('.') || entry.name === 'node_modules' || entry.name === 'dist' ||
            entry.name === 'build' || entry.name === 'coverage') {
            continue;
        }

        if (ignored.has(fullPath)) {
            continue;
        }

        if (entry.isDirectory()) {
            await walkDirectory(fullPath, files, ignored, projectRoot);
        } else if (entry.isFile() && isAcceptedFile(fullPath)) {
            files.push(fullPath);
        }
    }
}

/**
 * Check if a file is accepted for parsing based on its extension
 */
export function isAcceptedFile(filePath: string): boolean {
    const ext = path.extname(filePath).toLowerCase();

    // JavaScript/TypeScript files
    if (['.js', '.jsx', '.ts', '.tsx', '.mjs', '.mts', '.cjs', '.cts'].includes(ext)) {
        return true;
    }

    // JSON files (including package.json which gets special parsing)
    if (ext === '.json') {
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
 */
export async function* parseFilesStreaming(
    filePaths: string[],
    projectRoot: string,
    options: ParseFilesOptions = {}
): AsyncGenerator<SourceFile, void, undefined> {
    const { verbose = false, onProgress } = options;
    const total = filePaths.length;
    let current = 0;

    // Group files by type
    const jsFiles: string[] = [];
    const packageJsonFiles: string[] = [];
    const jsonFiles: string[] = [];

    for (const filePath of filePaths) {
        const basename = path.basename(filePath);
        const ext = path.extname(filePath).toLowerCase();

        if (basename === 'package.json') {
            packageJsonFiles.push(filePath);
        } else if (['.js', '.jsx', '.ts', '.tsx', '.mjs', '.mts', '.cjs', '.cts'].includes(ext)) {
            jsFiles.push(filePath);
        } else if (ext === '.json') {
            jsonFiles.push(filePath);
        }
    }

    // Parse JavaScript/TypeScript files
    if (jsFiles.length > 0) {
        if (verbose) {
            console.log(`Parsing ${jsFiles.length} JavaScript/TypeScript files...`);
        }
        const jsParser = new JavaScriptParser({relativeTo: projectRoot});
        for await (const sf of jsParser.parse(...jsFiles)) {
            current++;
            onProgress?.(current, total, sf.sourcePath);
            yield sf;
        }
    }

    // Parse package.json files
    if (packageJsonFiles.length > 0) {
        if (verbose) {
            console.log(`Parsing ${packageJsonFiles.length} package.json files...`);
        }
        const pkgParser = new PackageJsonParser({relativeTo: projectRoot});
        for await (const sf of pkgParser.parse(...packageJsonFiles)) {
            current++;
            onProgress?.(current, total, sf.sourcePath);
            yield sf;
        }
    }

    // Parse other JSON files
    if (jsonFiles.length > 0) {
        if (verbose) {
            console.log(`Parsing ${jsonFiles.length} JSON files...`);
        }
        const jsonParser = new JsonParser({relativeTo: projectRoot});
        for await (const sf of jsonParser.parse(...jsonFiles)) {
            current++;
            onProgress?.(current, total, sf.sourcePath);
            yield sf;
        }
    }
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
