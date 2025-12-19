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
import * as path from 'path';
import * as fs from 'fs';
import * as fsp from 'fs/promises';
import * as os from 'os';
import {spawnSync} from 'child_process';
import {PrettierStyle} from './style';
import {randomId} from '../uuid';

/**
 * Cache of loaded Prettier modules by version.
 * This ensures we don't reload the same version multiple times.
 */
const prettierModuleCache: Map<string, typeof import('prettier')> = new Map();

/**
 * Gets the cache directory for a specific Prettier version.
 * Uses ~/.cache/openrewrite/prettier/<version>/
 */
function getPrettierCacheDir(version: string): string {
    const cacheBase = path.join(os.homedir(), '.cache', 'openrewrite', 'prettier');
    return path.join(cacheBase, version);
}

/**
 * Checks if a Prettier version is installed in the cache.
 */
function isPrettierCached(version: string): boolean {
    const cacheDir = getPrettierCacheDir(version);
    const prettierPath = path.join(cacheDir, 'node_modules', 'prettier', 'package.json');
    if (!fs.existsSync(prettierPath)) {
        return false;
    }
    // Verify the installed version matches
    try {
        const pkg = JSON.parse(fs.readFileSync(prettierPath, 'utf8'));
        return pkg.version === version;
    } catch {
        return false;
    }
}

/**
 * Installs a specific Prettier version to the cache directory.
 */
async function installPrettierToCache(version: string): Promise<void> {
    const cacheDir = getPrettierCacheDir(version);

    // Create directory structure
    await fsp.mkdir(cacheDir, {recursive: true});

    // Create minimal package.json
    const packageJson = {
        name: `prettier-cache-${version}`,
        version: '1.0.0',
        private: true,
        dependencies: {
            prettier: version
        }
    };
    await fsp.writeFile(
        path.join(cacheDir, 'package.json'),
        JSON.stringify(packageJson, null, 2)
    );

    // Run npm install
    const result = spawnSync('npm', ['install', '--silent'], {
        cwd: cacheDir,
        encoding: 'utf-8',
        stdio: ['pipe', 'pipe', 'pipe'],
        timeout: 120000 // 2 minutes
    });

    if (result.error) {
        throw new Error(`Failed to install Prettier ${version}: ${result.error.message}`);
    }
    if (result.status !== 0) {
        const stderr = result.stderr?.trim() || '';
        throw new Error(`Failed to install Prettier ${version}: npm exited with code ${result.status}${stderr ? '\n' + stderr : ''}`);
    }

    // Verify installation
    if (!isPrettierCached(version)) {
        throw new Error(`Prettier ${version} installation verification failed`);
    }
}

/**
 * Loads Prettier from the cache directory for a specific version.
 */
function loadPrettierFromCache(version: string): typeof import('prettier') {
    const cacheDir = getPrettierCacheDir(version);
    const prettierPath = path.join(cacheDir, 'node_modules', 'prettier');

    // Clear require cache for this path to ensure fresh load
    // (in case the version was reinstalled)
    const resolvedPath = require.resolve(prettierPath);
    delete require.cache[resolvedPath];

    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require(prettierPath);
}

/**
 * Result of detecting Prettier in a project.
 */
export interface PrettierDetectionResult {
    /**
     * Whether Prettier is available in the project.
     */
    available: boolean;
    /**
     * The Prettier version from the project's node_modules.
     */
    version?: string;
    /**
     * Reference to the bundled Prettier for resolving configs.
     * We use our bundled version to resolve configs (stable API),
     * but at formatting time, the project's version will be loaded dynamically.
     */
    bundledPrettier?: typeof import('prettier');
}

/**
 * Manages Prettier configuration detection and loading for a parse session.
 *
 * This class:
 * 1. Detects if Prettier is installed in the project
 * 2. Resolves Prettier config per-file (with overrides)
 * 3. Caches resolved configs for marker deduplication
 * 4. Creates PrettierStyle markers for source files
 *
 * The marker only stores the version and resolved config - at formatting time,
 * the correct version of Prettier will be loaded dynamically (like npx).
 */
export class PrettierConfigLoader {
    private detection?: PrettierDetectionResult;
    private configCache: Map<string, PrettierStyle> = new Map();

    /**
     * Creates a new PrettierConfigLoader for a project.
     *
     * @param projectRoot The root directory of the project (where package.json is)
     */
    constructor(private readonly projectRoot: string) {}

    /**
     * Detects Prettier in the project and loads our bundled version for config resolution.
     * Call this once at the start of parsing.
     */
    async detectPrettier(): Promise<PrettierDetectionResult> {
        if (this.detection) {
            return this.detection;
        }

        this.detection = { available: false };

        try {
            // Get the project's Prettier version from package.json or node_modules
            const version = this.getPrettierVersionFromProject();
            if (!version) {
                return this.detection;
            }

            // Load our bundled Prettier for config resolution
            // We use dynamic require to handle cases where Prettier isn't installed
            let bundledPrettier: typeof import('prettier');
            try {
                // eslint-disable-next-line @typescript-eslint/no-require-imports
                bundledPrettier = require('prettier');
            } catch (e) {
                // Our bundled Prettier isn't available
                console.warn('PrettierConfigLoader: Failed to load bundled Prettier:', e);
                return this.detection;
            }

            this.detection = {
                available: true,
                version,
                bundledPrettier
            };
        } catch {
            // Any error means Prettier isn't properly set up
            this.detection = { available: false };
        }

        return this.detection;
    }

    /**
     * Gets the Prettier version from the project's package.json or node_modules.
     * Scans upward from projectRoot to support monorepo structures where Prettier
     * is installed at the repository root.
     */
    private getPrettierVersionFromProject(): string | undefined {
        // Scan upward from projectRoot to find Prettier
        // This handles monorepos where Prettier is at the root but files are in subdirectories
        let dir = this.projectRoot;

        while (true) {
            // First, check for prettier in node_modules (actual installation)
            const prettierPackageJson = path.join(dir, 'node_modules', 'prettier', 'package.json');
            if (fs.existsSync(prettierPackageJson)) {
                try {
                    const pkg = JSON.parse(fs.readFileSync(prettierPackageJson, 'utf8'));
                    return pkg.version;
                } catch {
                    // Corrupted package.json, continue scanning
                }
            }

            // Check package.json for prettier dependency (might be a range like ^3.0.0)
            const packageJsonPath = path.join(dir, 'package.json');
            if (fs.existsSync(packageJsonPath)) {
                try {
                    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
                    const deps = {
                        ...packageJson.dependencies,
                        ...packageJson.devDependencies
                    };
                    if (deps.prettier) {
                        // Found prettier as a dependency, but node_modules wasn't found
                        // This might mean dependencies aren't installed yet
                        // Continue scanning upward in case there's a parent with node_modules
                    }
                } catch {
                    // package.json parse error, continue
                }
            }

            // Move up to parent directory
            const parent = path.dirname(dir);
            if (parent === dir) {
                // Reached filesystem root
                break;
            }
            dir = parent;
        }

        return undefined;
    }

    /**
     * Gets or creates a PrettierStyle marker for the given source file.
     * Returns undefined if Prettier is not available or no config applies to this file.
     *
     * @param filePath Absolute path to the source file
     */
    async getConfigMarker(filePath: string): Promise<PrettierStyle | undefined> {
        if (!this.detection?.available || !this.detection.bundledPrettier) {
            return undefined;
        }

        try {
            // Resolve path against projectRoot if not absolute
            const absolutePath = path.isAbsolute(filePath)
                ? filePath
                : path.join(this.projectRoot, filePath);

            // Resolve config for this specific file (applies overrides)
            // If no config file exists, use empty config (Prettier defaults)
            const config = await this.detection.bundledPrettier.resolveConfig(absolutePath) ?? {};

            // Create a cache key from the resolved config + version
            const configKey = JSON.stringify({ config, version: this.detection.version });

            // Check cache for existing marker with same config
            let marker = this.configCache.get(configKey);
            if (marker) {
                return marker;
            }

            // Create new PrettierStyle instance
            marker = new PrettierStyle(randomId(), config, this.detection.version);

            // Cache and return
            this.configCache.set(configKey, marker);
            return marker;
        } catch (e) {
            // Config resolution failed for this file
            console.warn(`PrettierConfigLoader: Failed to resolve config for ${filePath}:`, e);
            return undefined;
        }
    }

    /**
     * Clears the config cache. Call this between parse batches if needed.
     */
    clearCache(): void {
        this.configCache.clear();
    }
}

/**
 * Dynamically loads a specific version of Prettier for formatting.
 *
 * This function will attempt to load Prettier in this order:
 * 1. From the in-memory cache (if already loaded)
 * 2. From the current working directory's node_modules (if version matches)
 * 3. From the cached npm project at ~/.cache/openrewrite/prettier/<version>/
 * 4. Install to cache and load from there
 *
 * @param version The Prettier version to load (e.g., "3.4.2")
 * @returns The loaded Prettier module
 */
export async function loadPrettierVersion(version: string): Promise<typeof import('prettier')> {
    // Check in-memory cache first
    const cached = prettierModuleCache.get(version);
    if (cached) {
        return cached;
    }

    // Try to load from local node_modules if version matches
    try {
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const localPrettier = require('prettier');
        if (localPrettier.version === version) {
            prettierModuleCache.set(version, localPrettier);
            return localPrettier;
        }
    } catch {
        // Local prettier not available
    }

    // Check if version is cached on disk
    if (isPrettierCached(version)) {
        const prettier = loadPrettierFromCache(version);
        prettierModuleCache.set(version, prettier);
        return prettier;
    }

    // Install to cache and load
    await installPrettierToCache(version);
    const prettier = loadPrettierFromCache(version);
    prettierModuleCache.set(version, prettier);
    return prettier;
}
