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

import {Option, ScanningRecipe} from "../../recipe";
import {ExecutionContext} from "../../execution";
import {TreeVisitor} from "../../visitor";
import {Json, JsonParser, JsonVisitor} from "../../json";
import {
    createNodeResolutionResultMarker,
    findNodeResolutionResult,
    PackageJsonContent,
    PackageLockContent,
    readNpmrcConfigs
} from "../node-resolution-result";
import * as fs from "fs";
import * as fsp from "fs/promises";
import * as path from "path";
import * as os from "os";
import {execSync} from "child_process";
import {replaceMarker} from "../../markers";
import {TreePrinters} from "../../print";

/**
 * Represents a dependency scope in package.json
 */
type DependencyScope = 'dependencies' | 'devDependencies' | 'peerDependencies' | 'optionalDependencies';

/**
 * Information about a project that needs updating
 */
interface ProjectUpdateInfo {
    /** Absolute path to the project directory */
    projectDir: string;
    /** Relative path to package.json (from source root) */
    packageJsonPath: string;
    /** Original package.json content */
    originalPackageJson: string;
    /** The scope where the dependency was found */
    dependencyScope: DependencyScope;
    /** Current version constraint */
    currentVersion: string;
    /** New version constraint to apply */
    newVersion: string;
}

/**
 * Accumulator for tracking state across scanning and editing phases
 */
interface Accumulator {
    /** Projects that need updating: packageJsonPath -> update info */
    projectsToUpdate: Map<string, ProjectUpdateInfo>;

    /** After running package manager, store the updated lock file content */
    updatedLockFiles: Map<string, string>;

    /** Updated package.json content (after npm install may have modified it) */
    updatedPackageJsons: Map<string, string>;

    /** Track which projects have been processed (npm install has run) */
    processedProjects: Set<string>;
}

/**
 * Upgrades the version of a dependency in package.json and updates the lock file.
 *
 * This recipe:
 * 1. Finds package.json files containing the specified dependency
 * 2. Updates the version constraint to the new version
 * 3. Runs npm install to update package-lock.json
 * 4. Updates the NodeResolutionResult marker with new dependency info
 */
export class UpgradeDependencyVersion extends ScanningRecipe<Accumulator> {
    readonly name = "org.openrewrite.javascript.dependencies.UpgradeDependencyVersion";
    readonly displayName = "Upgrade npm dependency version";
    readonly description = "Upgrades the version of a dependency in package.json and updates the lock file by running the package manager.";

    @Option({
        displayName: "Package name",
        description: "The name of the npm package to upgrade (e.g., 'lodash', '@types/node')",
        example: "lodash"
    })
    packageName!: string;

    @Option({
        displayName: "New version",
        description: "The new version constraint to set (e.g., '^5.0.0', '~2.1.0', '3.0.0')",
        example: "^5.0.0"
    })
    newVersion!: string;

    initialValue(_ctx: ExecutionContext): Accumulator {
        return {
            projectsToUpdate: new Map(),
            updatedLockFiles: new Map(),
            updatedPackageJsons: new Map(),
            processedProjects: new Set()
        };
    }

    async scanner(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
                // Only process package.json files
                if (!doc.sourcePath.endsWith('package.json')) {
                    return doc;
                }

                const marker = findNodeResolutionResult(doc);
                if (!marker) {
                    return doc;
                }

                // Check each dependency scope for the target package
                const scopes: DependencyScope[] = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'];

                for (const scope of scopes) {
                    const deps = marker[scope];
                    const dep = deps?.find(d => d.name === recipe.packageName);

                    if (dep) {
                        const currentVersion = dep.versionConstraint;

                        // Check if version needs updating
                        if (currentVersion !== recipe.newVersion) {
                            // Get the project directory from the marker path
                            const projectDir = path.dirname(path.resolve(doc.sourcePath));

                            acc.projectsToUpdate.set(doc.sourcePath, {
                                projectDir,
                                packageJsonPath: doc.sourcePath,
                                originalPackageJson: await this.printDocument(doc),
                                dependencyScope: scope,
                                currentVersion,
                                newVersion: recipe.newVersion
                            });
                        }
                        break; // Found the dependency, no need to check other scopes
                    }
                }

                return doc;
            }

            private async printDocument(doc: Json.Document): Promise<string> {
                return TreePrinters.print(doc);
            }
        };
    }

    async editorWithData(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
                const sourcePath = doc.sourcePath;

                // Handle package.json files
                if (sourcePath.endsWith('package.json')) {
                    const updateInfo = acc.projectsToUpdate.get(sourcePath);
                    if (!updateInfo) {
                        return doc; // This package.json doesn't need updating
                    }

                    // Run npm install if we haven't processed this project yet
                    if (!acc.processedProjects.has(sourcePath)) {
                        await recipe.runPackageManagerInstall(acc, updateInfo, ctx);
                        acc.processedProjects.add(sourcePath);
                    }

                    // Modify the dependency version in the JSON tree
                    const modifiedDoc = await this.updateDependencyVersion(doc, updateInfo);

                    // Update the NodeResolutionResult marker
                    const updatedDoc = await recipe.updateMarker(modifiedDoc, updateInfo, acc);

                    return updatedDoc;
                }

                // Handle package-lock.json files
                if (sourcePath.endsWith('package-lock.json')) {
                    // Find the corresponding package.json path
                    const packageJsonPath = sourcePath.replace('package-lock.json', 'package.json');
                    const updateInfo = acc.projectsToUpdate.get(packageJsonPath);

                    if (updateInfo && acc.updatedLockFiles.has(sourcePath)) {
                        // Parse the updated lock file content and return it
                        const updatedContent = acc.updatedLockFiles.get(sourcePath)!;
                        return this.parseUpdatedLockFile(doc, updatedContent);
                    }
                }

                return doc;
            }

            /**
             * Updates the dependency version in the JSON tree.
             */
            private async updateDependencyVersion(
                doc: Json.Document,
                updateInfo: ProjectUpdateInfo
            ): Promise<Json.Document> {
                const visitor = new UpdateVersionVisitor(
                    recipe.packageName,
                    updateInfo.newVersion,
                    updateInfo.dependencyScope
                );
                return await visitor.visit(doc, undefined) as Json.Document;
            }

            /**
             * Parses updated lock file content and creates a new document.
             */
            private async parseUpdatedLockFile(
                originalDoc: Json.Document,
                updatedContent: string
            ): Promise<Json.Document> {
                // Parse the updated content using JsonParser
                const parser = new JsonParser({});
                const parsed: Json.Document[] = [];

                for await (const sf of parser.parse({text: updatedContent, sourcePath: originalDoc.sourcePath})) {
                    parsed.push(sf as Json.Document);
                }

                if (parsed.length > 0) {
                    // Preserve the original source path and markers (except NodeResolutionResult)
                    return {
                        ...parsed[0],
                        sourcePath: originalDoc.sourcePath,
                        markers: originalDoc.markers
                    };
                }

                return originalDoc;
            }
        };
    }

    /**
     * Runs npm install in a temporary directory to update the lock file.
     */
    private async runPackageManagerInstall(
        acc: Accumulator,
        updateInfo: ProjectUpdateInfo,
        _ctx: ExecutionContext
    ): Promise<void> {
        // Create temp directory
        const tempDir = await fsp.mkdtemp(path.join(os.tmpdir(), 'openrewrite-npm-'));

        try {
            // Create modified package.json content
            const modifiedPackageJson = this.createModifiedPackageJson(
                updateInfo.originalPackageJson,
                updateInfo.dependencyScope,
                updateInfo.newVersion
            );

            // Write package.json to temp directory
            await fsp.writeFile(path.join(tempDir, 'package.json'), modifiedPackageJson);

            // Copy existing lock file if present
            const originalLockPath = path.join(updateInfo.projectDir, 'package-lock.json');
            if (fs.existsSync(originalLockPath)) {
                await fsp.copyFile(originalLockPath, path.join(tempDir, 'package-lock.json'));
            }

            // Copy .npmrc if present (for registry configuration)
            const npmrcPath = path.join(updateInfo.projectDir, '.npmrc');
            if (fs.existsSync(npmrcPath)) {
                await fsp.copyFile(npmrcPath, path.join(tempDir, '.npmrc'));
            }

            // Store the modified package.json content (do this before npm install in case it fails)
            acc.updatedPackageJsons.set(updateInfo.packageJsonPath, modifiedPackageJson);

            // Run npm install
            try {
                execSync('npm install --package-lock-only', {
                    cwd: tempDir,
                    stdio: 'pipe',
                    timeout: 120000 // 2 minute timeout
                });

                // Read back the updated lock file
                const updatedLockPath = path.join(tempDir, 'package-lock.json');
                if (fs.existsSync(updatedLockPath)) {
                    const updatedLockContent = await fsp.readFile(updatedLockPath, 'utf-8');
                    const lockFilePath = updateInfo.packageJsonPath.replace('package.json', 'package-lock.json');
                    acc.updatedLockFiles.set(lockFilePath, updatedLockContent);
                }
            } catch (error: any) {
                console.warn(`npm install failed: ${error.message}`);
                // Continue without lock file update - package.json is already stored
            }

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
     * Creates a modified package.json with the updated dependency version.
     */
    private createModifiedPackageJson(
        originalContent: string,
        scope: DependencyScope,
        newVersion: string
    ): string {
        const packageJson = JSON.parse(originalContent);

        if (packageJson[scope] && packageJson[scope][this.packageName]) {
            packageJson[scope][this.packageName] = newVersion;
        }

        // Preserve original formatting as much as possible
        return JSON.stringify(packageJson, null, 2);
    }

    /**
     * Updates the NodeResolutionResult marker with new dependency information.
     */
    private async updateMarker(
        doc: Json.Document,
        updateInfo: ProjectUpdateInfo,
        acc: Accumulator
    ): Promise<Json.Document> {
        const existingMarker = findNodeResolutionResult(doc);
        if (!existingMarker) {
            return doc;
        }

        // Parse the updated package.json and lock file to create new marker
        const updatedPackageJson = acc.updatedPackageJsons.get(updateInfo.packageJsonPath);
        const updatedLockFile = acc.updatedLockFiles.get(
            updateInfo.packageJsonPath.replace('package.json', 'package-lock.json')
        );

        let packageJsonContent: PackageJsonContent;
        let lockContent: PackageLockContent | undefined;

        try {
            packageJsonContent = JSON.parse(updatedPackageJson || updateInfo.originalPackageJson);
        } catch {
            return doc; // Failed to parse, keep original marker
        }

        if (updatedLockFile) {
            try {
                lockContent = JSON.parse(updatedLockFile);
            } catch {
                // Continue without lock file content
            }
        }

        // Read npmrc configs from the project directory
        const npmrcConfigs = await readNpmrcConfigs(updateInfo.projectDir);

        // Create new marker
        const newMarker = createNodeResolutionResultMarker(
            existingMarker.path,
            packageJsonContent,
            lockContent,
            existingMarker.workspacePackagePaths,
            existingMarker.packageManager,
            npmrcConfigs.length > 0 ? npmrcConfigs : undefined
        );

        // Replace the marker in the document
        return {
            ...doc,
            markers: replaceMarker(doc.markers, existingMarker, newMarker)
        };
    }
}

/**
 * Visitor that updates the version of a specific dependency in a specific scope.
 */
class UpdateVersionVisitor extends JsonVisitor<void> {
    private readonly packageName: string;
    private readonly newVersion: string;
    private readonly targetScope: DependencyScope;
    private inTargetScope = false;

    constructor(packageName: string, newVersion: string, targetScope: DependencyScope) {
        super();
        this.packageName = packageName;
        this.newVersion = newVersion;
        this.targetScope = targetScope;
    }

    protected async visitMember(member: Json.Member, p: void): Promise<Json | undefined> {
        // Check if we're entering the target scope
        const keyName = this.getMemberKeyName(member);

        if (keyName === this.targetScope) {
            // We're entering the dependencies scope
            this.inTargetScope = true;
            const result = await super.visitMember(member, p);
            this.inTargetScope = false;
            return result;
        }

        // Check if this is the dependency we're looking for
        if (this.inTargetScope && keyName === this.packageName) {
            // Update the version value
            return this.updateVersion(member);
        }

        return super.visitMember(member, p);
    }

    private getMemberKeyName(member: Json.Member): string | undefined {
        const key = member.key.element;
        if (key.kind === Json.Kind.Literal) {
            // Remove quotes from string literal
            const source = (key as Json.Literal).source;
            if (source.startsWith('"') && source.endsWith('"')) {
                return source.slice(1, -1);
            }
            return source;
        } else if (key.kind === Json.Kind.Identifier) {
            return (key as Json.Identifier).name;
        }
        return undefined;
    }

    private updateVersion(member: Json.Member): Json.Member {
        const value = member.value;

        if (value.kind !== Json.Kind.Literal) {
            return member; // Not a literal value, can't update
        }

        const literal = value as Json.Literal;

        // Create new literal with updated version
        const newLiteral: Json.Literal = {
            ...literal,
            source: `"${this.newVersion}"`,
            value: this.newVersion
        };

        return {
            ...member,
            value: newLiteral
        };
    }
}
