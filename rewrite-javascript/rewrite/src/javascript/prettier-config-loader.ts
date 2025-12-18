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
import {PrettierStyle} from './style';
import {randomId} from '../uuid';

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
     */
    private getPrettierVersionFromProject(): string | undefined {
        // First, check project's package.json for prettier dependency
        const packageJsonPath = path.join(this.projectRoot, 'package.json');
        if (fs.existsSync(packageJsonPath)) {
            try {
                const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
                const deps = {
                    ...packageJson.dependencies,
                    ...packageJson.devDependencies
                };
                if (deps.prettier) {
                    // Try to get exact version from node_modules
                    const installedVersion = this.getInstalledPrettierVersion();
                    if (installedVersion) {
                        return installedVersion;
                    }
                    // Fall back to declared version (might be a range)
                    return deps.prettier;
                }
            } catch {
                // package.json parse error
            }
        }

        // Check if prettier is installed in node_modules even without being in package.json
        return this.getInstalledPrettierVersion();
    }

    /**
     * Gets the exact Prettier version from node_modules.
     */
    private getInstalledPrettierVersion(): string | undefined {
        try {
            const prettierPackageJson = path.join(
                this.projectRoot,
                'node_modules',
                'prettier',
                'package.json'
            );
            if (fs.existsSync(prettierPackageJson)) {
                const pkg = JSON.parse(fs.readFileSync(prettierPackageJson, 'utf8'));
                return pkg.version;
            }
        } catch {
            // Not installed or can't read
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
            const config = await this.detection.bundledPrettier.resolveConfig(absolutePath);

            if (!config) {
                // No Prettier config found for this file
                return undefined;
            }

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
 * 1. From the current working directory's node_modules (if version matches)
 * 2. Using dynamic import with version specifier (npx-like behavior)
 *
 * @param version The Prettier version to load (e.g., "3.4.2")
 * @returns The loaded Prettier module
 */
export async function loadPrettierVersion(version: string): Promise<typeof import('prettier')> {
    // First, try to load from local node_modules if available and version matches
    try {
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const localPrettier = require('prettier');
        // Check if version matches
        if (localPrettier.version === version) {
            return localPrettier;
        }
    } catch {
        // Local prettier not available
    }

    // For now, fall back to whatever prettier is available
    // TODO: Implement npx-like dynamic version loading
    // This would involve:
    // 1. Check if version is available in a cache directory
    // 2. If not, download and install to cache
    // 3. Load from cache
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('prettier');
}
