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

import {
    createNodeResolutionResultMarker,
    findNodeResolutionResult,
    PackageJsonContent,
    PackageLockContent,
    PackageManager
} from "./node-resolution-result";
import {replaceMarkerByKind} from "../markers";
import {Json, JsonParser, JsonVisitor} from "../json";
import {isDocuments, Yaml, YamlParser, YamlVisitor} from "../yaml";
import {PlainTextParser} from "../text";
import {SourceFile} from "../tree";
import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {getPlatformCommand} from "../shell-utils";
import * as fs from "fs";
import * as fsp from "fs/promises";
import * as path from "path";
import * as os from "os";
import {spawnSync} from "child_process";
import {isDeepStrictEqual} from "util";
import * as YAML from "yaml";
import * as semver from "semver";
import picomatch from "picomatch";

/**
 * Configuration for each package manager.
 */
interface PackageManagerConfig {
    /** The lock file name for this package manager */
    lockFile: string;

    /** Command to install dependencies and update lock file only (no node_modules) */
    installLockOnlyCommand: string[];

    /** Command to install dependencies fully */
    installCommand: string[];

    /** Command to list dependencies in JSON format (if supported) */
    listCommand?: string[];

    /** Command that prints the release of the package manager that runs where it is run */
    versionCommand: string[];
}

/**
 * Package manager configurations.
 */
const PACKAGE_MANAGER_CONFIGS: Record<PackageManager, PackageManagerConfig> = {
    [PackageManager.Npm]: {
        lockFile: 'package-lock.json',
        // --ignore-scripts prevents prepublish/prepare scripts from running in temp directory.
        // --legacy-peer-deps disables npm 7+'s strict peer-dep enforcement. The install here is
        // purely a validity check on the bumped package.json; the resulting node_modules is not
        // shipped to the user. Strict peer enforcement creates false negatives in common
        // ecosystem upgrades (e.g. Angular major bumps where third-party libs lag), which would
        // otherwise prevent the bump from being written even when it is correct.
        installLockOnlyCommand: ['npm', 'install', '--package-lock-only', '--ignore-scripts', '--legacy-peer-deps'],
        installCommand: ['npm', 'install', '--ignore-scripts', '--legacy-peer-deps'],
        listCommand: ['npm', 'list', '--json', '--all'],
        versionCommand: ['npm', '--version'],
    },
    [PackageManager.YarnClassic]: {
        lockFile: 'yarn.lock',
        // Yarn Classic doesn't have a lock-only mode; --ignore-scripts prevents lifecycle scripts
        installLockOnlyCommand: ['yarn', 'install', '--ignore-scripts'],
        installCommand: ['yarn', 'install', '--ignore-scripts'],
        listCommand: ['yarn', 'list', '--json'],
        versionCommand: ['yarn', '--version'],
    },
    [PackageManager.YarnBerry]: {
        lockFile: 'yarn.lock',
        // --mode skip-build skips post-install scripts in Yarn Berry
        installLockOnlyCommand: ['yarn', 'install', '--mode', 'skip-build'],
        installCommand: ['yarn', 'install', '--mode', 'skip-build'],
        listCommand: ['yarn', 'info', '--all', '--json'],
        versionCommand: ['yarn', '--version'],
    },
    [PackageManager.Pnpm]: {
        lockFile: 'pnpm-lock.yaml',
        // --ignore-scripts prevents lifecycle scripts from running in temp directory.
        // --no-strict-peer-dependencies disables pnpm's strict peer-dep enforcement (see the
        // `npm` entry above for rationale).
        installLockOnlyCommand: ['pnpm', 'install', '--lockfile-only', '--ignore-scripts', '--no-strict-peer-dependencies'],
        installCommand: ['pnpm', 'install', '--ignore-scripts', '--no-strict-peer-dependencies'],
        listCommand: ['pnpm', 'list', '--json', '--depth=Infinity'],
        versionCommand: ['pnpm', '--version'],
    },
    [PackageManager.Bun]: {
        lockFile: 'bun.lock',
        // Bun doesn't have a lock-only mode; --ignore-scripts prevents lifecycle scripts
        installLockOnlyCommand: ['bun', 'install', '--ignore-scripts'],
        installCommand: ['bun', 'install', '--ignore-scripts'],
        versionCommand: ['bun', '--version'],
    },
};

/**
 * Configuration for lock file detection.
 */
export interface LockFileDetectionConfig {
    /** The lock file name */
    filename: string;
    /** The package manager, or a function to detect it from file content */
    packageManager: PackageManager | ((content: string) => PackageManager);
    /** If true, prefer walking node_modules over parsing lock file (lock file may omit details) */
    preferNodeModules?: boolean;
}

/**
 * Lock file detection configuration with priority order.
 * Priority order determines which package manager is detected when multiple lock files exist.
 */
const LOCK_FILE_DETECTION: ReadonlyArray<LockFileDetectionConfig> = [
    {filename: 'package-lock.json', packageManager: PackageManager.Npm},
    {filename: 'bun.lock', packageManager: PackageManager.Bun},
    {filename: 'pnpm-lock.yaml', packageManager: PackageManager.Pnpm, preferNodeModules: true},
    {
        filename: 'yarn.lock',
        packageManager: (content) =>
            content.includes('__metadata:') ? PackageManager.YarnBerry : PackageManager.YarnClassic,
        // yarn.lock omits transitive dependency details (engines/license), so prefer node_modules
        preferNodeModules: true
    },
];

/**
 * Lock file names that should be parsed as JSON/JSONC format.
 */
export const JSON_LOCK_FILE_NAMES = ['bun.lock', 'package-lock.json'] as const;

/**
 * Lock file names that should be parsed as YAML format.
 */
export const YAML_LOCK_FILE_NAMES = ['pnpm-lock.yaml'] as const;

/**
 * Lock file names that should be parsed as plain text (custom formats like yarn.lock v1).
 * Note: yarn.lock for Yarn Berry (v2+) is actually YAML format and should be parsed as such.
 * Use `getLockFileFormat` with content to determine the correct format for yarn.lock.
 */
export const TEXT_LOCK_FILE_NAMES = ['yarn.lock'] as const;

/**
 * Detects if a yarn.lock file is Yarn Berry (v2+) format based on content.
 * Yarn Berry lock files contain a `__metadata:` key which is not present in Classic.
 *
 * @param content The yarn.lock file content
 * @returns true if this is a Yarn Berry lock file (YAML format), false for Classic
 */
export function isYarnBerryLockFile(content: string): boolean {
    return content.includes('__metadata:');
}

/**
 * Result of running a package manager command.
 */
interface PackageManagerResult {
    success: boolean;
    stdout?: string;
    stderr?: string;
    error?: string;
}

/**
 * Options for running package manager install.
 */
interface InstallOptions {
    /** Working directory */
    cwd: string;

    /** If true, only update lock file without installing to node_modules */
    lockOnly?: boolean;

    /** Timeout in milliseconds (default: 120000 = 2 minutes) */
    timeout?: number;

    /** Additional environment variables */
    env?: Record<string, string>;
}

/** A conflicting declaration must not produce a resolution graph from another manager's lock. */
export class PackageManagerSelectionError extends Error {
}

export interface PackageManagerSelection {
    readonly packageManager: PackageManager;
    readonly root: string;
    readonly lockFile?: string;
}

function declaredPackageManager(manifest: unknown): PackageManager | undefined {
    if (!manifest || typeof manifest !== "object") return undefined;
    const value = (manifest as {packageManager?: unknown}).packageManager;
    if (typeof value !== "string") return undefined;
    const match = /^(npm|pnpm|bun|yarn)@(.+)$/.exec(value);
    const version = match && semver.valid(match[2].split("+", 1)[0]);
    if (!match || !version) return undefined;
    switch (match[1]) {
        case "npm": return PackageManager.Npm;
        case "pnpm": return PackageManager.Pnpm;
        case "bun": return PackageManager.Bun;
        case "yarn": return semver.major(version) === 1 ? PackageManager.YarnClassic : PackageManager.YarnBerry;
        default: return undefined;
    }
}

function isWorkspaceMember(root: string, member: string, manifest: unknown): boolean {
    let patterns: unknown = manifest && typeof manifest === "object" ?
        (manifest as {workspaces?: unknown}).workspaces : undefined;
    if (patterns && !Array.isArray(patterns) && typeof patterns === "object") {
        patterns = (patterns as {packages?: unknown}).packages;
    }
    if (!Array.isArray(patterns)) {
        try { patterns = YAML.parse(fs.readFileSync(path.join(root, "pnpm-workspace.yaml"), "utf8"))?.packages; }
        catch { return false; }
    }
    if (!Array.isArray(patterns) || !patterns.every(p => typeof p === "string")) return false;
    const relative = path.relative(root, member).split(path.sep).join("/");
    const include = patterns.filter(p => !p.startsWith("!"));
    const exclude = patterns.filter(p => p.startsWith("!")).map(p => p.slice(1));
    return include.length > 0 && picomatch(include)(relative) &&
        (exclude.length === 0 || !picomatch(exclude)(relative));
}

/**
 * Select a lock and its manager together. The nearest supported pinned declaration takes precedence over lock
 * filename order, including when a workspace member inherits it. An explicit parser boundary limits the walk.
 * The supplied manifest is the parser input at startDir, which may differ from the file on disk.
 */
export function selectPackageManager(startDir: string, rootDir?: string, manifest?: unknown): PackageManagerSelection | undefined {
    const dirs: string[] = [];
    const stop = rootDir === undefined ? undefined : path.resolve(rootDir);
    for (let dir = path.resolve(startDir); ; dir = path.dirname(dir)) {
        dirs.push(dir);
        if (dir === stop || path.dirname(dir) === dir) break;
    }
    const manifests = new Map<string, unknown>();
    for (const dir of dirs) {
        if (dir === dirs[0] && manifest !== undefined) manifests.set(dir, manifest);
        else {
            try { manifests.set(dir, JSON.parse(fs.readFileSync(path.join(dir, "package.json"), "utf8"))); }
            catch { /* A lock can still identify a project without a readable manifest. */ }
        }
    }
    let declared: {manager: PackageManager, root: string} | undefined;
    for (const dir of dirs) {
        const pkg = manifests.get(dir);
        const manager = declaredPackageManager(pkg);
        if (manager !== undefined && (dir === dirs[0] || isWorkspaceMember(dir, dirs[0], pkg))) {
            declared = {manager, root: dir};
            break;
        }
    }
    if (declared) {
        // A nested declaration defines its own project unless a compatible workspace actually contains it.
        let boundary = dirs.indexOf(declared.root);
        for (let i = boundary + 1; i < dirs.length; i++) {
            const pkg = manifests.get(dirs[i]);
            const manager = declaredPackageManager(pkg);
            if (manager !== undefined && manager !== declared.manager) break;
            if (isWorkspaceMember(dirs[i], declared.root, pkg)) boundary = i;
        }
        const projectDirs = dirs.slice(0, boundary + 1);
        const filename = getLockFileName(declared.manager);
        for (const dir of projectDirs) {
            const lockFile = path.join(dir, filename);
            if (!fs.existsSync(lockFile)) continue;
            if (filename === "yarn.lock") {
                const actual = isYarnBerryLockFile(fs.readFileSync(lockFile, "utf8")) ?
                    PackageManager.YarnBerry : PackageManager.YarnClassic;
                if (actual !== declared.manager) {
                    throw new PackageManagerSelectionError(
                        `packageManager selects ${declared.manager}, but ${lockFile} contains ${actual}`);
                }
            }
            return {packageManager: declared.manager, root: dir, lockFile};
        }
        const foreign = projectDirs.flatMap(dir => LOCK_FILE_DETECTION
            .map(config => path.join(dir, config.filename)).filter(file => fs.existsSync(file)));
        if (foreign.length) {
            throw new PackageManagerSelectionError(
                `packageManager selects ${declared.manager}, but ${filename} is missing; other lock files: ${foreign.join(", ")}`);
        }
        return {packageManager: declared.manager, root: declared.root};
    }
    // Without a recognized declaration, retain the existing nearest-lock/filename precedence.
    for (const dir of dirs) {
        for (const config of LOCK_FILE_DETECTION) {
            const lockFile = path.join(dir, config.filename);
            if (!fs.existsSync(lockFile)) continue;
            if (typeof config.packageManager !== "function") {
                return {packageManager: config.packageManager, root: dir, lockFile};
            }
            try {
                return {packageManager: config.packageManager(fs.readFileSync(lockFile, "utf8")), root: dir, lockFile};
            } catch {
                return {packageManager: PackageManager.YarnClassic, root: dir, lockFile};
            }
        }
    }
    return undefined;
}

/** Detect the manager selected by the declaration and locks in this directory. */
export function detectPackageManager(dir: string): PackageManager | undefined {
    return selectPackageManager(dir, dir)?.packageManager;
}

/**
 * Gets the lock file detection configuration.
 * Returns the array of lock file configs in priority order.
 */
export function getLockFileDetectionConfig(): ReadonlyArray<LockFileDetectionConfig> {
    return LOCK_FILE_DETECTION;
}

/**
 * Gets the lock file name for a package manager.
 */
export function getLockFileName(pm: PackageManager): string {
    return PACKAGE_MANAGER_CONFIGS[pm].lockFile;
}

/**
 * Gets all supported lock file names.
 */
export function getAllLockFileNames(): string[] {
    return LOCK_FILE_DETECTION.map(c => c.filename);
}

/**
 * Runs the package manager install command.
 */
function runInstall(pm: PackageManager, options: InstallOptions): PackageManagerResult {
    const config = PACKAGE_MANAGER_CONFIGS[pm];
    const command = options.lockOnly ? config.installLockOnlyCommand : config.installCommand;
    const [cmd, ...args] = command;

    try {
        const result = spawnSync(getPlatformCommand(cmd), args, {
            cwd: options.cwd,
            encoding: 'utf-8',
            stdio: ['pipe', 'pipe', 'pipe'],
            timeout: options.timeout ?? 120000,
            env: options.env ? {...process.env, ...options.env} : process.env,
            ...(os.platform() === 'win32' ? { shell: true } : {})
        });

        if (result.error) {
            return {
                success: false,
                error: result.error.message,
                stderr: result.stderr,
            };
        }

        if (result.status !== 0) {
            return {
                success: false,
                stdout: result.stdout,
                stderr: result.stderr,
                error: `Command exited with code ${result.status}`,
            };
        }

        return {
            success: true,
            stdout: result.stdout,
            stderr: result.stderr,
        };
    } catch (error: any) {
        return {
            success: false,
            error: error.message,
        };
    }
}

/**
 * Runs a package manager list command to get dependency information.
 *
 * @param pm The package manager to use
 * @param cwd Working directory
 * @param timeout Timeout in milliseconds
 * @returns The JSON output, or undefined if failed
 */
export function runList(pm: PackageManager, cwd: string, timeout: number = 30000): string | undefined {
    const config = PACKAGE_MANAGER_CONFIGS[pm];
    if (!config.listCommand) {
        return undefined;
    }

    const [cmd, ...args] = config.listCommand;

    const result = spawnSync(getPlatformCommand(cmd), args, {
        cwd,
        encoding: 'utf-8',
        stdio: ['pipe', 'pipe', 'pipe'],
        timeout,
        ...(os.platform() === 'win32' ? { shell: true } : {})
    });

    if (result.error || result.status !== 0) {
        return undefined;
    }

    return result.stdout;
}

/**
 * A release as a package manager's `--version` prints it: `major.minor.patch`, with a prerelease or build or both.
 */
const RELEASE = /^\d+\.\d+\.\d+(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;

/**
 * What the package manager's `--version` answers, run in `cwd`: in the project's root directory, the release that
 * would install the project on this machine, since Yarn and pnpm decide there which release runs (`yarnPath`,
 * corepack, pnpm's own switching).
 *
 * It runs as the parser's other package manager commands do, with `COREPACK_ENABLE_DOWNLOAD_PROMPT=0`, no input and
 * a short timeout, so it never prompts and never throws.
 *
 * npm is asked once per process where `npm` on PATH is npm's own `npm-cli.js` ({@link npmEntryPoint}). That prints
 * the version of the package it belongs to, whatever the project configures, so its answer is the same in every root.
 *
 * @param pm The package manager to ask
 * @param cwd The project's root directory
 * @param timeout Timeout in milliseconds
 * @returns The trimmed answer, or undefined when the package manager is not installed, fails, or answers something
 *          that is no version
 */
export function runVersion(pm: PackageManager, cwd: string, timeout: number = 10000): string | undefined {
    const entryPoint = pm === PackageManager.Npm ? npmEntryPoint(cwd) : undefined;
    const known = entryPoint === undefined ? undefined : npmVersions.get(entryPoint);
    if (known !== undefined) {
        return known;
    }
    const answer = spawnVersion(pm, cwd, timeout);
    // a failure, such as a timeout on a busy machine, is asked again
    if (entryPoint !== undefined && answer !== undefined) {
        npmVersions.set(entryPoint, answer);
    }
    return answer;
}

const npmVersions = new Map<string, string>();

const NPM_CLI = path.sep + path.join('npm', 'bin', 'npm-cli.js');

/**
 * The `npm-cli.js` that `npm` on PATH runs in `cwd`, with its change time, so that an npm upgraded in place is asked
 * again. Installers may keep the archive's modification time, but writing the file moves its change time. A relative
 * directory on PATH is resolved against `cwd`, as the spawn resolves it. Undefined where `npm` is something else, such
 * as a shim that picks a release per project or a Windows `.cmd` script.
 */
function npmEntryPoint(cwd: string): string | undefined {
    if (os.platform() === 'win32' || process.env.PATH === undefined) {
        return undefined;
    }
    for (const dir of process.env.PATH.split(path.delimiter)) {
        const candidate = path.resolve(cwd, dir, 'npm');
        try {
            if (!fs.statSync(candidate).isFile()) {
                continue;
            }
            fs.accessSync(candidate, fs.constants.X_OK);
        } catch {
            continue;
        }
        try {
            const real = fs.realpathSync(candidate);
            return real.endsWith(NPM_CLI) ? `${real}\0${fs.statSync(real).ctimeMs}` : undefined;
        } catch {
            return undefined;
        }
    }
    return undefined;
}

function spawnVersion(pm: PackageManager, cwd: string, timeout: number): string | undefined {
    const [cmd, ...args] = PACKAGE_MANAGER_CONFIGS[pm].versionCommand;
    try {
        const result = spawnSync(getPlatformCommand(cmd), args, {
            cwd,
            encoding: 'utf-8',
            stdio: ['ignore', 'pipe', 'pipe'],
            timeout,
            env: {...process.env, COREPACK_ENABLE_DOWNLOAD_PROMPT: '0'},
            ...(os.platform() === 'win32' ? { shell: true } : {})
        });
        if (result.error || result.status !== 0 || typeof result.stdout !== 'string') {
            return undefined;
        }
        const answer = result.stdout.trim();
        return RELEASE.test(answer) ? answer : undefined;
    } catch {
        return undefined;
    }
}

/**
 * Result of running install in a temporary directory.
 */
export interface TempInstallResult {
    /** Whether the install succeeded */
    success: boolean;
    /** The updated lock file content (if successful and lock file exists) */
    lockFileContent?: string;
    /** Error message (if failed) */
    error?: string;
}

/**
 * Generic accumulator for dependency recipes that run package manager operations.
 * Used by scanning recipes to track state across scanning and editing phases.
 *
 * @typeParam T The recipe-specific project update info type
 */
export interface DependencyRecipeAccumulator<T> {
    /** Projects that need updating: packageJsonPath -> update info */
    projectsToUpdate: Map<string, T>;

    /** After running package manager, store the updated lock file content */
    updatedLockFiles: Map<string, string>;

    /** Updated package.json content (after npm install may have modified it) */
    updatedPackageJsons: Map<string, string>;

    /** Track which projects have been processed (npm install has run) */
    processedProjects: Set<string>;

    /** Track projects where npm install failed: packageJsonPath -> error message */
    failedProjects: Map<string, string>;
}

/**
 * Creates a new empty accumulator for dependency recipes.
 */
export function createDependencyRecipeAccumulator<T>(): DependencyRecipeAccumulator<T> {
    return {
        projectsToUpdate: new Map(),
        updatedLockFiles: new Map(),
        updatedPackageJsons: new Map(),
        processedProjects: new Set(),
        failedProjects: new Map()
    };
}

/**
 * Checks if a source path is a lock file and returns the updated content if available.
 * This is a helper for dependency recipes that need to update lock files.
 *
 * @param sourcePath The source path to check
 * @param acc The recipe accumulator containing updated lock file content
 * @returns The updated lock file content if this is a lock file that was updated, undefined otherwise
 */
export function getUpdatedLockFileContent<T>(
    sourcePath: string,
    acc: DependencyRecipeAccumulator<T>
): string | undefined {
    for (const lockFileName of getAllLockFileNames()) {
        if (sourcePath.endsWith(lockFileName)) {
            // Find the corresponding package.json path
            const packageJsonPath = sourcePath.replace(lockFileName, 'package.json');
            const updateInfo = acc.projectsToUpdate.get(packageJsonPath);

            if (updateInfo && acc.updatedLockFiles.has(sourcePath)) {
                return acc.updatedLockFiles.get(sourcePath);
            }
            break;
        }
    }
    return undefined;
}

/**
 * Determines the appropriate parser for a lock file based on its filename and optionally content.
 *
 * For yarn.lock files, the format depends on the Yarn version:
 * - Yarn Classic (v1): Custom plain text format
 * - Yarn Berry (v2+): YAML format
 *
 * If content is provided for yarn.lock, it will be used to detect the format.
 * Otherwise, defaults to 'text' (Yarn Classic).
 *
 * @param lockFileName The lock file name (e.g., "pnpm-lock.yaml", "package-lock.json", "yarn.lock")
 * @param content Optional file content (used for yarn.lock format detection)
 * @returns 'yaml' for YAML lock files, 'json' for JSON lock files, 'text' for plain text
 */
export function getLockFileFormat(lockFileName: string, content?: string): 'yaml' | 'json' | 'text' {
    if ((YAML_LOCK_FILE_NAMES as readonly string[]).includes(lockFileName)) {
        return 'yaml';
    }
    if (lockFileName === 'yarn.lock') {
        // Yarn Berry (v2+) uses YAML format, Classic uses custom text format
        if (content && isYarnBerryLockFile(content)) {
            return 'yaml';
        }
        return 'text';
    }
    if ((TEXT_LOCK_FILE_NAMES as readonly string[]).includes(lockFileName)) {
        return 'text';
    }
    // package-lock.json, bun.lock
    return 'json';
}

/**
 * Re-parses updated lock file content using the appropriate parser.
 * This is used by dependency recipes to create the updated lock file SourceFile.
 *
 * For yarn.lock files, the content is used to detect whether it's Yarn Berry (YAML)
 * or Yarn Classic (plain text) format.
 *
 * @param content The updated lock file content
 * @param sourcePath The source path of the lock file
 * @param lockFileName The lock file name (e.g., "pnpm-lock.yaml", "yarn.lock")
 * @returns The parsed SourceFile (Json.Document, Yaml.Documents, or PlainText)
 */
export async function parseLockFileContent(
    content: string,
    sourcePath: string,
    lockFileName: string
): Promise<SourceFile> {
    // Pass content to getLockFileFormat for yarn.lock detection
    const format = getLockFileFormat(lockFileName, content);

    switch (format) {
        case 'yaml': {
            const parser = new YamlParser({});
            return await parser.parseOne({text: content, sourcePath}) as Yaml.Documents;
        }
        case 'text': {
            const parser = new PlainTextParser({});
            return await parser.parseOne({text: content, sourcePath});
        }
        case 'json':
        default: {
            const parser = new JsonParser({});
            return await parser.parseOne({text: content, sourcePath}) as Json.Document;
        }
    }
}

/**
 * Base interface for project update info used by dependency recipes.
 * Recipes extend this with additional fields specific to their needs.
 */
export interface BaseProjectUpdateInfo {
    /** Relative path to package.json (from source root) */
    packageJsonPath: string;
    /** The package manager used by this project */
    packageManager: PackageManager;
    /** Selected lock path relative to the source root, when it is shared by workspace members. */
    lockFilePath?: string;
}

/**
 * Stores the result of a package manager install into the accumulator.
 * This handles the common pattern of storing updated lock files and tracking failures.
 *
 * @param result The result from runInstallInTempDir
 * @param acc The recipe accumulator
 * @param updateInfo The project update info (must have packageJsonPath and packageManager)
 * @param modifiedPackageJson The modified package.json content that was used for install
 */
export function storeInstallResult<T extends BaseProjectUpdateInfo>(
    result: TempInstallResult,
    acc: DependencyRecipeAccumulator<T>,
    updateInfo: T,
    modifiedPackageJson: string
): void {
    if (result.success) {
        acc.updatedPackageJsons.set(updateInfo.packageJsonPath, modifiedPackageJson);

        if (result.lockFileContent) {
            const lockFileName = getLockFileName(updateInfo.packageManager);
            const lockFilePath = updateInfo.packageJsonPath.replace('package.json', lockFileName);
            acc.updatedLockFiles.set(lockFilePath, result.lockFileContent);
        }
    } else {
        acc.failedProjects.set(updateInfo.packageJsonPath, result.error || 'Unknown error');
    }
}

/**
 * Runs the package manager install for a project if it hasn't been processed yet.
 * Updates the accumulator's processedProjects set after running.
 *
 * @param sourcePath The source path (package.json path) being processed
 * @param acc The recipe accumulator
 * @param runInstall Function that performs the actual install (recipe-specific)
 * @returns The failure message if install failed, undefined otherwise
 */
export async function runInstallIfNeeded<T>(
    sourcePath: string,
    acc: DependencyRecipeAccumulator<T>,
    runInstall: () => Promise<void>
): Promise<string | undefined> {
    if (!acc.processedProjects.has(sourcePath)) {
        await runInstall();
        acc.processedProjects.add(sourcePath);
    }
    return acc.failedProjects.get(sourcePath);
}

/**
 * Updates the NodeResolutionResult marker on a JSON document after a package manager operation.
 * This recreates the marker based on the updated package.json and lock file content.
 *
 * @param doc The JSON document containing the marker
 * @param updateInfo Project update info with paths and package manager
 * @param acc The recipe accumulator containing updated content
 * @returns The document with the updated marker, or unchanged if no existing marker
 */
export async function updateNodeResolutionMarker<T extends BaseProjectUpdateInfo>(
    doc: Json.Document,
    updateInfo: T & { originalPackageJson: string },
    acc: DependencyRecipeAccumulator<T>
): Promise<Json.Document> {
    const existingMarker = findNodeResolutionResult(doc);
    if (!existingMarker) {
        return doc;
    }

    // Parse the updated package.json and lock file to create new marker
    const updatedPackageJson = acc.updatedPackageJsons.get(updateInfo.packageJsonPath);
    const lockFileName = getLockFileName(updateInfo.packageManager);
    // A shared lock root must be supplied by the caller that selected it. Merely
    // finding an ancestor lock in the accumulator cannot rule out an independent
    // (unchanged) lock beside this manifest.
    const manifestDir = path.posix.dirname(updateInfo.packageJsonPath.replace(/\\/g, '/'));
    const lockPath = updateInfo.lockFilePath?.replace(/\\/g, '/') ?? path.posix.join(manifestDir, lockFileName);
    const lockDir = path.posix.dirname(lockPath);
    const updatedLockFile = acc.updatedLockFiles.get(lockPath);

    let packageJsonContent: PackageJsonContent;
    let lockContent: PackageLockContent | undefined;

    try {
        packageJsonContent = JSON.parse(updatedPackageJson || updateInfo.originalPackageJson);
    } catch {
        return doc; // Failed to parse, keep original marker
    }

    if (updatedLockFile) {
        try {
            // Parse lock file based on format
            if (updateInfo.packageManager === PackageManager.Pnpm) {
                // pnpm-lock.yaml is YAML format
                lockContent = YAML.parse(updatedLockFile);
            } else if (updateInfo.packageManager === PackageManager.YarnClassic ||
                       updateInfo.packageManager === PackageManager.YarnBerry) {
                // yarn.lock has a custom format - skip parsing here
                // The marker will still be updated with package.json info
                lockContent = undefined;
            } else {
                // npm (package-lock.json) and bun (bun.lock) use JSON
                lockContent = JSON.parse(updatedLockFile);
            }
        } catch {
            // Continue without lock file content
        }
    }

    // Create new marker, preserving existing npmrc configs from the parser
    // (recipes don't have filesystem access to re-read them)
    const newMarker = createNodeResolutionResultMarker(
        existingMarker.path,
        packageJsonContent,
        lockContent,
        existingMarker.workspacePackagePaths,
        existingMarker.packageManager,
        existingMarker.npmrcConfigs,
        updatedLockFile === undefined ? "" : path.posix.relative(lockDir, manifestDir)
    );

    // If the new marker is structurally identical to the existing one (ignoring the
    // auto-generated id), return doc unchanged to avoid minting a fresh AST identity
    // for a no-op marker update. This prevents the empty-diff guard from firing on
    // recipes that don't modify package.json (e.g. lock-file-only fix strategies).
    const {id: _existingId, ...existingRest} = existingMarker;
    const {id: _newId, ...newRest} = newMarker;
    if (isDeepStrictEqual(existingRest, newRest)) {
        return doc;
    }

    // Replace the marker in the document
    return {
        ...doc,
        markers: replaceMarkerByKind(doc.markers, newMarker)
    };
}

/**
 * Options for running install in a temporary directory.
 */
export interface TempInstallOptions {
    /** Timeout in milliseconds (default: 120000 = 2 minutes) */
    timeout?: number;
    /**
     * If true, only update the lock file without installing node_modules.
     * If false, perform a full install which creates node_modules in the temp dir.
     * Default: true (lock-only is faster and sufficient for most cases)
     */
    lockOnly?: boolean;
    /**
     * Original lock file content to use. If provided, this content will be written
     * to the temp directory instead of copying from projectDir.
     * This allows recipes to work with in-memory SourceFiles without filesystem access.
     */
    originalLockFileContent?: string;
    /**
     * Config file contents to use. Keys are filenames (e.g., '.npmrc'), values are content.
     * If provided, these will be written to the temp directory instead of copying from projectDir.
     */
    configFiles?: Record<string, string>;
}

/**
 * Options for running install in a temporary directory with workspace support.
 */
export interface WorkspaceTempInstallOptions extends TempInstallOptions {
    /**
     * Workspace package.json files. Keys are relative paths from the project root
     * (e.g., "packages/foo/package.json"), values are the package.json content.
     * The root package.json should have a "workspaces" field pointing to these packages.
     */
    workspacePackages?: Record<string, string>;
}

/**
 * Internal implementation for running package manager install in a temporary directory.
 * Supports both simple projects and workspaces.
 */
async function runInstallInTempDirCore(
    pm: PackageManager,
    rootPackageJson: string,
    options: WorkspaceTempInstallOptions = {}
): Promise<TempInstallResult> {
    const {
        timeout = 120000,
        lockOnly = true,
        originalLockFileContent,
        configFiles: configFileContents,
        workspacePackages
    } = options;
    const lockFileName = getLockFileName(pm);
    const tempDir = await fsp.mkdtemp(path.join(os.tmpdir(), 'openrewrite-pm-'));

    try {
        // Write root package.json to temp directory
        await fsp.writeFile(path.join(tempDir, 'package.json'), rootPackageJson);

        // Write workspace package.json files (creating subdirectories as needed)
        if (workspacePackages) {
            for (const [relativePath, content] of Object.entries(workspacePackages)) {
                const fullPath = path.join(tempDir, relativePath);
                await fsp.mkdir(path.dirname(fullPath), {recursive: true});
                await fsp.writeFile(fullPath, content);
            }
        }

        // Write lock file if provided
        if (originalLockFileContent !== undefined) {
            await fsp.writeFile(path.join(tempDir, lockFileName), originalLockFileContent);
        }

        // Write config files if provided
        if (configFileContents) {
            for (const [configFile, content] of Object.entries(configFileContents)) {
                await fsp.writeFile(path.join(tempDir, configFile), content);
            }
        }

        // Run package manager install
        const result = runInstall(pm, {
            cwd: tempDir,
            lockOnly,
            timeout
        });

        if (!result.success) {
            // Combine error message with stderr for more useful diagnostics
            const errorParts: string[] = [];
            if (result.error) {
                errorParts.push(result.error);
            }
            if (result.stderr) {
                // Trim and limit stderr to avoid excessively long error messages
                const stderr = result.stderr.trim();
                if (stderr) {
                    errorParts.push(stderr.length > 2000 ? stderr.slice(0, 2000) + '...' : stderr);
                }
            }
            return {
                success: false,
                error: errorParts.length > 0 ? errorParts.join('\n\n') : 'Unknown error'
            };
        }

        // Read back the updated lock file
        const updatedLockPath = path.join(tempDir, lockFileName);
        let lockFileContent: string | undefined;
        if (fs.existsSync(updatedLockPath)) {
            lockFileContent = await fsp.readFile(updatedLockPath, 'utf-8');
        }

        return {
            success: true,
            lockFileContent
        };

    } finally {
        // Cleanup temp directory
        try {
            await fsp.rm(tempDir, {recursive: true, force: true});
        } catch {
            // Ignore cleanup errors
        }
    }
}

/**
 * Runs package manager install in a temporary directory.
 *
 * This function:
 * 1. Creates a temp directory
 * 2. Writes the provided package.json content
 * 3. Writes the lock file content (if provided)
 * 4. Writes config files (if provided)
 * 5. Runs the package manager install
 * 6. Returns the updated lock file content
 * 7. Cleans up the temp directory
 *
 * @param pm The package manager to use
 * @param modifiedPackageJson The modified package.json content to use
 * @param options Optional settings for timeout, lock-only mode, and file contents
 * @returns Result containing success status and lock file content or error
 */
export async function runInstallInTempDir(
    pm: PackageManager,
    modifiedPackageJson: string,
    options: TempInstallOptions = {}
): Promise<TempInstallResult> {
    return runInstallInTempDirCore(pm, modifiedPackageJson, options);
}

/**
 * Runs package manager install in a temporary directory with workspace support.
 *
 * This function:
 * 1. Creates a temp directory
 * 2. Writes the root package.json content
 * 3. Writes workspace package.json files (creating subdirectories as needed)
 * 4. Writes the lock file content (if provided)
 * 5. Writes config files (if provided)
 * 6. Runs the package manager install at the root
 * 7. Returns the updated lock file content
 * 8. Cleans up the temp directory
 *
 * @param pm The package manager to use
 * @param rootPackageJson The root package.json content (should contain "workspaces" field)
 * @param options Optional settings including workspace packages, timeout, lock-only mode, and file contents
 * @returns Result containing success status and lock file content or error
 */
export async function runWorkspaceInstallInTempDir(
    pm: PackageManager,
    rootPackageJson: string,
    options: WorkspaceTempInstallOptions = {}
): Promise<TempInstallResult> {
    return runInstallInTempDirCore(pm, rootPackageJson, options);
}

/**
 * Creates a lock file visitor that handles updating YAML lock files (pnpm-lock.yaml).
 * This is a reusable component for dependency recipes.
 *
 * @param acc The recipe accumulator containing updated lock file content
 * @returns A YamlVisitor that updates YAML lock files
 */
export function createYamlLockFileVisitor<T>(
    acc: DependencyRecipeAccumulator<T>
): YamlVisitor<ExecutionContext> {
    return new class extends YamlVisitor<ExecutionContext> {
        protected async visitDocuments(docs: Yaml.Documents, _ctx: ExecutionContext): Promise<Yaml | undefined> {
            const sourcePath = docs.sourcePath;
            const updatedLockContent = getUpdatedLockFileContent(sourcePath, acc);
            if (updatedLockContent) {
                const lockFileName = path.basename(sourcePath);
                const parsed = await parseLockFileContent(updatedLockContent, sourcePath, lockFileName) as Yaml.Documents;
                // Preserve original ID for RPC compatibility
                return {
                    ...docs,
                    documents: parsed.documents,
                    suffix: parsed.suffix
                } as Yaml.Documents;
            }
            return docs;
        }
    };
}

/**
 * Creates a composite visitor that delegates to the appropriate editor based on tree type.
 * This handles both JSON (package-lock.json, bun.lock) and YAML (pnpm-lock.yaml) lock files.
 *
 * @param jsonEditor The JSON visitor for handling JSON files
 * @param acc The recipe accumulator for YAML lock file handling
 * @returns A TreeVisitor that handles both JSON and YAML files
 */
export function createLockFileEditor<T>(
    jsonEditor: JsonVisitor<ExecutionContext>,
    acc: DependencyRecipeAccumulator<T>
): TreeVisitor<any, ExecutionContext> {
    const yamlEditor = createYamlLockFileVisitor(acc);

    return new class extends TreeVisitor<any, ExecutionContext> {
        async visit(tree: any, ctx: ExecutionContext): Promise<any> {
            if (isDocuments(tree)) {
                return yamlEditor.visit(tree, ctx);
            } else if (tree && tree.kind === Json.Kind.Document) {
                return jsonEditor.visit(tree, ctx);
            }
            return tree;
        }
    };
}
