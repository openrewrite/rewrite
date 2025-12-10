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

import {PackageManager} from "./node-resolution-result";
import * as fs from "fs";
import * as fsp from "fs/promises";
import * as path from "path";
import * as os from "os";
import {spawnSync} from "child_process";

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
}

/**
 * Package manager configurations.
 */
const PACKAGE_MANAGER_CONFIGS: Record<PackageManager, PackageManagerConfig> = {
    [PackageManager.Npm]: {
        lockFile: 'package-lock.json',
        installLockOnlyCommand: ['npm', 'install', '--package-lock-only'],
        installCommand: ['npm', 'install'],
        listCommand: ['npm', 'list', '--json', '--all'],
    },
    [PackageManager.YarnClassic]: {
        lockFile: 'yarn.lock',
        // Yarn Classic doesn't have a lock-only mode
        installLockOnlyCommand: ['yarn', 'install', '--ignore-scripts'],
        installCommand: ['yarn', 'install'],
        listCommand: ['yarn', 'list', '--json'],
    },
    [PackageManager.YarnBerry]: {
        lockFile: 'yarn.lock',
        // Yarn Berry's mode skip-build skips post-install scripts
        installLockOnlyCommand: ['yarn', 'install', '--mode', 'skip-build'],
        installCommand: ['yarn', 'install'],
        listCommand: ['yarn', 'info', '--all', '--json'],
    },
    [PackageManager.Pnpm]: {
        lockFile: 'pnpm-lock.yaml',
        installLockOnlyCommand: ['pnpm', 'install', '--lockfile-only'],
        installCommand: ['pnpm', 'install'],
        listCommand: ['pnpm', 'list', '--json', '--depth=Infinity'],
    },
    [PackageManager.Bun]: {
        lockFile: 'bun.lock',
        // Bun doesn't have a lock-only mode, but is very fast anyway
        installLockOnlyCommand: ['bun', 'install', '--ignore-scripts'],
        installCommand: ['bun', 'install'],
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
 * Result of running a package manager command.
 */
export interface PackageManagerResult {
    success: boolean;
    stdout?: string;
    stderr?: string;
    error?: string;
}

/**
 * Options for running package manager install.
 */
export interface InstallOptions {
    /** Working directory */
    cwd: string;

    /** If true, only update lock file without installing to node_modules */
    lockOnly?: boolean;

    /** Timeout in milliseconds (default: 120000 = 2 minutes) */
    timeout?: number;

    /** Additional environment variables */
    env?: Record<string, string>;
}

/**
 * Detects the package manager used in a directory by checking for lock files.
 *
 * @param dir The directory to check
 * @returns The detected package manager, or undefined if none found
 */
export function detectPackageManager(dir: string): PackageManager | undefined {
    for (const config of LOCK_FILE_DETECTION) {
        const lockPath = path.join(dir, config.filename);
        if (fs.existsSync(lockPath)) {
            if (typeof config.packageManager === 'function') {
                try {
                    const content = fs.readFileSync(lockPath, 'utf-8');
                    return config.packageManager(content);
                } catch {
                    continue;
                }
            }
            return config.packageManager;
        }
    }
    return undefined;
}

/**
 * Gets the lock file detection configuration.
 * Returns the array of lock file configs in priority order.
 */
export function getLockFileDetectionConfig(): ReadonlyArray<LockFileDetectionConfig> {
    return LOCK_FILE_DETECTION;
}

/**
 * Gets the configuration for a package manager.
 */
export function getPackageManagerConfig(pm: PackageManager): PackageManagerConfig {
    return PACKAGE_MANAGER_CONFIGS[pm];
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
 * Checks if a file path is a lock file.
 */
export function isLockFile(filePath: string): boolean {
    const fileName = path.basename(filePath);
    return getAllLockFileNames().includes(fileName);
}

/**
 * Runs the package manager install command.
 *
 * @param pm The package manager to use
 * @param options Install options
 * @returns Result of the install command
 */
export function runInstall(pm: PackageManager, options: InstallOptions): PackageManagerResult {
    const config = PACKAGE_MANAGER_CONFIGS[pm];
    const command = options.lockOnly ? config.installLockOnlyCommand : config.installCommand;
    const [cmd, ...args] = command;

    try {
        const result = spawnSync(cmd, args, {
            cwd: options.cwd,
            encoding: 'utf-8',
            stdio: ['pipe', 'pipe', 'pipe'],
            timeout: options.timeout ?? 120000,
            env: options.env ? {...process.env, ...options.env} : process.env,
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
 * Options for adding/upgrading a package.
 */
export interface AddPackageOptions {
    /** Working directory */
    cwd: string;

    /** Package name to add/upgrade */
    packageName: string;

    /** Version constraint (e.g., "^5.0.0") */
    version: string;

    /** If true, only update lock file without installing to node_modules */
    lockOnly?: boolean;

    /** Timeout in milliseconds (default: 120000 = 2 minutes) */
    timeout?: number;

    /** Additional environment variables */
    env?: Record<string, string>;
}

/**
 * Runs a package manager command to add or upgrade a package.
 * This updates both package.json and the lock file.
 *
 * @param pm The package manager to use
 * @param options Add package options
 * @returns Result of the command
 */
export function runAddPackage(pm: PackageManager, options: AddPackageOptions): PackageManagerResult {
    const packageSpec = `${options.packageName}@${options.version}`;

    // Build command based on package manager
    let cmd: string;
    let args: string[];

    switch (pm) {
        case PackageManager.Npm:
            cmd = 'npm';
            args = ['install', packageSpec];
            if (options.lockOnly) {
                args.push('--package-lock-only');
            }
            break;
        case PackageManager.YarnClassic:
            cmd = 'yarn';
            args = ['add', packageSpec];
            if (options.lockOnly) {
                args.push('--ignore-scripts');
            }
            break;
        case PackageManager.YarnBerry:
            cmd = 'yarn';
            args = ['add', packageSpec];
            if (options.lockOnly) {
                args.push('--mode', 'skip-build');
            }
            break;
        case PackageManager.Pnpm:
            cmd = 'pnpm';
            args = ['add', packageSpec];
            if (options.lockOnly) {
                args.push('--lockfile-only');
            }
            break;
        case PackageManager.Bun:
            cmd = 'bun';
            args = ['add', packageSpec];
            if (options.lockOnly) {
                args.push('--ignore-scripts');
            }
            break;
    }

    try {
        const result = spawnSync(cmd, args, {
            cwd: options.cwd,
            encoding: 'utf-8',
            stdio: ['pipe', 'pipe', 'pipe'],
            timeout: options.timeout ?? 120000,
            env: options.env ? {...process.env, ...options.env} : process.env,
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

    const result = spawnSync(cmd, args, {
        cwd,
        encoding: 'utf-8',
        stdio: ['pipe', 'pipe', 'pipe'],
        timeout,
    });

    if (result.error || result.status !== 0) {
        return undefined;
    }

    return result.stdout;
}

/**
 * Checks if a package manager is available on the system.
 *
 * @param pm The package manager to check
 * @returns True if the package manager is available
 */
export function isPackageManagerAvailable(pm: PackageManager): boolean {
    const config = PACKAGE_MANAGER_CONFIGS[pm];
    const cmd = config.installCommand[0];

    try {
        const result = spawnSync(cmd, ['--version'], {
            encoding: 'utf-8',
            stdio: ['pipe', 'pipe', 'pipe'],
            timeout: 5000,
        });
        return result.status === 0;
    } catch {
        return false;
    }
}

/**
 * Gets a human-readable name for a package manager.
 */
export function getPackageManagerDisplayName(pm: PackageManager): string {
    switch (pm) {
        case PackageManager.Npm:
            return 'npm';
        case PackageManager.YarnClassic:
            return 'Yarn Classic';
        case PackageManager.YarnBerry:
            return 'Yarn Berry';
        case PackageManager.Pnpm:
            return 'pnpm';
        case PackageManager.Bun:
            return 'Bun';
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
 * Runs package manager install in a temporary directory.
 *
 * This function:
 * 1. Creates a temp directory
 * 2. Writes the provided package.json content
 * 3. Copies the existing lock file (if present)
 * 4. Copies config files (.npmrc, .yarnrc, etc.)
 * 5. Runs the package manager install (lock-only mode)
 * 6. Returns the updated lock file content
 * 7. Cleans up the temp directory
 *
 * @param projectDir The original project directory (for copying lock file and configs)
 * @param pm The package manager to use
 * @param modifiedPackageJson The modified package.json content to use
 * @param timeout Timeout in milliseconds (default: 120000 = 2 minutes)
 * @returns Result containing success status and lock file content or error
 */
export async function runInstallInTempDir(
    projectDir: string,
    pm: PackageManager,
    modifiedPackageJson: string,
    timeout: number = 120000
): Promise<TempInstallResult> {
    const lockFileName = getLockFileName(pm);
    const tempDir = await fsp.mkdtemp(path.join(os.tmpdir(), 'openrewrite-pm-'));

    try {
        // Write modified package.json to temp directory
        await fsp.writeFile(path.join(tempDir, 'package.json'), modifiedPackageJson);

        // Copy existing lock file if present
        const originalLockPath = path.join(projectDir, lockFileName);
        if (fs.existsSync(originalLockPath)) {
            await fsp.copyFile(originalLockPath, path.join(tempDir, lockFileName));
        }

        // Copy config files if present (for registry configuration and workspace setup)
        const configFiles = ['.npmrc', '.yarnrc', '.yarnrc.yml', '.pnpmfile.cjs', 'pnpm-workspace.yaml'];
        for (const configFile of configFiles) {
            const configPath = path.join(projectDir, configFile);
            if (fs.existsSync(configPath)) {
                await fsp.copyFile(configPath, path.join(tempDir, configFile));
            }
        }

        // Run package manager install
        const result = runInstall(pm, {
            cwd: tempDir,
            lockOnly: true,
            timeout
        });

        if (!result.success) {
            return {
                success: false,
                error: result.error || result.stderr || 'Unknown error'
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
