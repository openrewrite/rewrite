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
import {Tree} from "../../tree";
import {detectIndent, getMemberKeyName, isJson, isObject, Json, JsonVisitor, rightPadded, space} from "../../json";
import {isDocuments, isYaml, Yaml} from "../../yaml";
import {isPlainText, PlainText} from "../../text";
import {
    allDependencyScopes,
    DependencyScope,
    findNodeResolutionResult,
    PackageManager,
    serializeNpmrcConfigs
} from "../node-resolution-result";
import {emptyMarkers, markupWarn} from "../../markers";
import {TreePrinters} from "../../print";
import {
    createDependencyRecipeAccumulator,
    createLockFileEditor,
    DependencyRecipeAccumulator,
    getAllLockFileNames,
    getLockFileName,
    parseLockFileContent,
    runInstallIfNeeded,
    runInstallInTempDir,
    storeInstallResult,
    updateNodeResolutionMarker
} from "../package-manager";
import {randomId} from "../../uuid";
import * as path from "path";

/**
 * Information about a project that needs updating
 */
interface ProjectUpdateInfo {
    /** Relative path to package.json (from source root) */
    packageJsonPath: string;
    /** Original package.json content */
    originalPackageJson: string;
    /** The scope where the dependency should be added */
    dependencyScope: DependencyScope;
    /** Version constraint to apply */
    newVersion: string;
    /** The package manager used by this project */
    packageManager: PackageManager;
    /** Config file contents extracted from the project (e.g., .npmrc) */
    configFiles?: Record<string, string>;
}

interface Accumulator extends DependencyRecipeAccumulator<ProjectUpdateInfo> {
    /** Original lock file content, keyed by lock file path */
    originalLockFiles: Map<string, string>;
}

/**
 * Adds a new dependency to package.json and updates the lock file.
 *
 * This recipe:
 * 1. Finds package.json files that don't already have the specified dependency
 * 2. Adds the dependency to the specified scope (defaults to 'dependencies')
 * 3. Runs the package manager to update the lock file
 * 4. Updates the NodeResolutionResult marker with new dependency info
 *
 * If the dependency already exists in any scope, no changes are made.
 * This matches the behavior of org.openrewrite.maven.AddDependency.
 */
export class AddDependency extends ScanningRecipe<Accumulator> {
    readonly name = "org.openrewrite.javascript.dependencies.add-dependency";
    readonly displayName = "Add npm dependency";
    readonly description = "Adds a new dependency to `package.json` and updates the lock file by running the package manager.";

    @Option({
        displayName: "Package name",
        description: "The name of the npm package to add (e.g., `lodash`, `@types/node`)",
        example: "lodash"
    })
    packageName!: string;

    @Option({
        displayName: "Version",
        description: "The version constraint to set (e.g., `^5.0.0`, `~2.1.0`, `3.0.0`)",
        example: "^5.0.0"
    })
    version!: string;

    @Option({
        displayName: "Scope",
        description: "The dependency scope: `dependencies`, `devDependencies`, `peerDependencies`, or `optionalDependencies`",
        example: "dependencies",
        required: false
    })
    scope?: DependencyScope;

    initialValue(_ctx: ExecutionContext): Accumulator {
        return {
            ...createDependencyRecipeAccumulator<ProjectUpdateInfo>(),
            originalLockFiles: new Map()
        };
    }

    private getTargetScope(): DependencyScope {
        return this.scope ?? 'dependencies';
    }

    async scanner(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;
        const LOCK_FILE_NAMES = getAllLockFileNames();

        return new class extends TreeVisitor<Tree, ExecutionContext> {
            protected async accept(tree: Tree, ctx: ExecutionContext): Promise<Tree | undefined> {
                // Handle JSON documents (package.json and JSON lock files)
                if (isJson(tree) && tree.kind === Json.Kind.Document) {
                    return this.handleJsonDocument(tree as Json.Document, ctx);
                }

                // Handle YAML documents (pnpm-lock.yaml)
                if (isYaml(tree) && isDocuments(tree)) {
                    return this.handleYamlDocument(tree, ctx);
                }

                // Handle PlainText files (yarn.lock for Yarn Classic)
                if (isPlainText(tree)) {
                    return this.handlePlainTextDocument(tree as PlainText, ctx);
                }

                return tree;
            }

            private async handleJsonDocument(doc: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                const basename = path.basename(doc.sourcePath);

                // Capture JSON lock file content (package-lock.json, bun.lock)
                if (LOCK_FILE_NAMES.includes(basename)) {
                    acc.originalLockFiles.set(doc.sourcePath, await TreePrinters.print(doc));
                    return doc;
                }

                // Only process package.json files for dependency analysis
                if (!doc.sourcePath.endsWith('package.json')) {
                    return doc;
                }

                const marker = findNodeResolutionResult(doc);
                if (!marker) {
                    return doc;
                }

                // Check if dependency already exists in any scope
                for (const scope of allDependencyScopes) {
                    const deps = marker[scope];
                    if (deps?.some(d => d.name === recipe.packageName)) {
                        // Dependency already exists, don't add again
                        return doc;
                    }
                }

                const pm = marker.packageManager ?? PackageManager.Npm;

                // Serialize npmrc configs from marker using requested scopes
                const configFiles: Record<string, string> = {};
                const npmrcContent = serializeNpmrcConfigs(marker.npmrcConfigs);
                if (npmrcContent) {
                    configFiles['.npmrc'] = npmrcContent;
                }

                acc.projectsToUpdate.set(doc.sourcePath, {
                    packageJsonPath: doc.sourcePath,
                    originalPackageJson: await TreePrinters.print(doc),
                    dependencyScope: recipe.getTargetScope(),
                    newVersion: recipe.version,
                    packageManager: pm,
                    configFiles: Object.keys(configFiles).length > 0 ? configFiles : undefined
                });

                return doc;
            }

            private async handleYamlDocument(docs: Yaml.Documents, _ctx: ExecutionContext): Promise<Yaml.Documents | undefined> {
                const basename = path.basename(docs.sourcePath);
                if (LOCK_FILE_NAMES.includes(basename)) {
                    acc.originalLockFiles.set(docs.sourcePath, await TreePrinters.print(docs));
                }
                return docs;
            }

            private async handlePlainTextDocument(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                const basename = path.basename(text.sourcePath);
                if (LOCK_FILE_NAMES.includes(basename)) {
                    acc.originalLockFiles.set(text.sourcePath, await TreePrinters.print(text));
                }
                return text;
            }
        };
    }

    async editorWithData(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        // Create JSON visitor that handles both package.json and JSON lock files
        const jsonEditor = new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
                const sourcePath = doc.sourcePath;

                // Handle package.json files
                if (sourcePath.endsWith('package.json')) {
                    const updateInfo = acc.projectsToUpdate.get(sourcePath);
                    if (!updateInfo) {
                        return doc; // This package.json doesn't need updating
                    }

                    // Run package manager install if needed, check for failure
                    const failureMessage = await runInstallIfNeeded(sourcePath, acc, () =>
                        recipe.runPackageManagerInstall(acc, updateInfo, ctx)
                    );
                    if (failureMessage) {
                        return markupWarn(
                            doc,
                            `Failed to add ${recipe.packageName} to ${recipe.version}`,
                            failureMessage
                        );
                    }

                    // Add the dependency to the JSON AST (preserves formatting)
                    const visitor = new AddDependencyVisitor(
                        recipe.packageName,
                        recipe.version,
                        updateInfo.dependencyScope
                    );
                    const modifiedDoc = await visitor.visit(doc, undefined) as Json.Document;

                    // Update the NodeResolutionResult marker
                    return updateNodeResolutionMarker(modifiedDoc, updateInfo, acc);
                }

                // Handle JSON lock files (package-lock.json, bun.lock)
                const lockFileName = path.basename(sourcePath);
                if (getAllLockFileNames().includes(lockFileName)) {
                    const updatedLockContent = acc.updatedLockFiles.get(sourcePath);
                    if (updatedLockContent) {
                        const parsed = await parseLockFileContent(updatedLockContent, sourcePath, lockFileName) as Json.Document;
                        // Preserve original ID for RPC compatibility
                        return {
                            ...doc,
                            value: parsed.value,
                            eof: parsed.eof
                        } as Json.Document;
                    }
                }

                return doc;
            }
        };

        // Return composite visitor that handles both JSON and YAML lock files
        return createLockFileEditor(jsonEditor, acc);
    }

    /**
     * Runs the package manager in a temporary directory to update the lock file.
     * All file contents are provided from in-memory sources (SourceFiles), not read from disk.
     */
    private async runPackageManagerInstall(
        acc: Accumulator,
        updateInfo: ProjectUpdateInfo,
        _ctx: ExecutionContext
    ): Promise<void> {
        // Create modified package.json with the new dependency
        const modifiedPackageJson = this.createModifiedPackageJson(
            updateInfo.originalPackageJson,
            updateInfo.dependencyScope
        );

        // Get the lock file path based on package manager
        const lockFileName = getLockFileName(updateInfo.packageManager);
        const packageJsonDir = path.dirname(updateInfo.packageJsonPath);
        const lockFilePath = packageJsonDir === '.'
            ? lockFileName
            : path.join(packageJsonDir, lockFileName);

        // Look up the original lock file content from captured SourceFiles
        const originalLockFileContent = acc.originalLockFiles.get(lockFilePath);

        const result = await runInstallInTempDir(
            updateInfo.packageManager,
            modifiedPackageJson,
            {
                originalLockFileContent,
                configFiles: updateInfo.configFiles
            }
        );

        storeInstallResult(result, acc, updateInfo, modifiedPackageJson);
    }

    /**
     * Creates a modified package.json with the new dependency added.
     */
    private createModifiedPackageJson(
        originalContent: string,
        scope: DependencyScope
    ): string {
        const packageJson = JSON.parse(originalContent);

        if (!packageJson[scope]) {
            packageJson[scope] = {};
        }

        packageJson[scope][this.packageName] = this.version;

        return JSON.stringify(packageJson, null, 2);
    }
}

/**
 * Visitor that adds a new dependency to a specific scope in package.json.
 * If the scope doesn't exist, it creates it.
 */
class AddDependencyVisitor extends JsonVisitor<void> {
    private readonly packageName: string;
    private readonly version: string;
    private readonly targetScope: DependencyScope;
    private scopeFound = false;
    private dependencyAdded = false;
    private baseIndent: string = '    '; // Will be detected from document

    constructor(packageName: string, version: string, targetScope: DependencyScope) {
        super();
        this.packageName = packageName;
        this.version = version;
        this.targetScope = targetScope;
    }

    protected async visitDocument(doc: Json.Document, p: void): Promise<Json | undefined> {
        // Detect indentation from the document
        this.baseIndent = detectIndent(doc);

        const result = await super.visitDocument(doc, p) as Json.Document;

        // If scope wasn't found, we need to add it to the document
        if (!this.scopeFound && !this.dependencyAdded) {
            return this.addScopeToDocument(result);
        }

        return result;
    }

    protected async visitMember(member: Json.Member, p: void): Promise<Json | undefined> {
        const keyName = getMemberKeyName(member);

        if (keyName === this.targetScope) {
            this.scopeFound = true;
            return this.addDependencyToScope(member);
        }

        return super.visitMember(member, p);
    }

    /**
     * Adds the dependency to an existing scope object.
     */
    private addDependencyToScope(scopeMember: Json.Member): Json.Member {
        const value = scopeMember.value;

        if (!isObject(value)) {
            return scopeMember;
        }

        const members = [...(value.members || [])];

        // Determine the closing whitespace (goes before closing brace after last element)
        let closingWhitespace = '\n    ';
        if (members.length > 0) {
            const lastMember = members[members.length - 1];
            closingWhitespace = lastMember.after.whitespace;
            // Update the last member's after to be empty (comma will be added by printer)
            members[members.length - 1] = {
                ...lastMember,
                after: space('')
            };
        }

        const newMember = this.createDependencyMemberWithAfter(closingWhitespace);
        this.dependencyAdded = true;

        members.push(newMember);

        return {
            ...scopeMember,
            value: {
                ...value,
                members
            } as Json.Object
        };
    }

    /**
     * Creates a new dependency member node.
     */
    private createDependencyMemberWithAfter(afterWhitespace: string): Json.RightPadded<Json.Member> {
        // Dependencies inside a scope are indented twice (e.g., 8 spaces if base is 4)
        const depIndent = this.baseIndent + this.baseIndent;

        const keyLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space('\n' + depIndent),
            markers: emptyMarkers,
            source: `"${this.packageName}"`,
            value: this.packageName
        };

        const valueLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space(' '),
            markers: emptyMarkers,
            source: `"${this.version}"`,
            value: this.version
        };

        const member: Json.Member = {
            kind: Json.Kind.Member,
            id: randomId(),
            prefix: space(''),
            markers: emptyMarkers,
            key: rightPadded(keyLiteral, space('')),
            value: valueLiteral
        };

        return rightPadded(member, space(afterWhitespace));
    }

    /**
     * Adds a new scope section to the document when the target scope doesn't exist.
     */
    private addScopeToDocument(doc: Json.Document): Json.Document {
        const docValue = doc.value;

        if (!isObject(docValue)) {
            return doc;
        }

        const members = [...(docValue.members || [])];

        // Get the trailing whitespace from the last member
        let closingWhitespace = '\n';
        if (members.length > 0) {
            const lastMember = members[members.length - 1];
            closingWhitespace = lastMember.after.whitespace;
            // Update the last member's after to be empty (comma will be added by printer)
            members[members.length - 1] = {
                ...lastMember,
                after: space('')
            };
        }

        const scopeMember = this.createScopeMemberWithAfter(closingWhitespace);
        this.dependencyAdded = true;

        members.push(scopeMember);

        return {
            ...doc,
            value: {
                ...docValue,
                members
            } as Json.Object
        };
    }

    /**
     * Creates a new scope member with the dependency inside.
     */
    private createScopeMemberWithAfter(afterWhitespace: string): Json.RightPadded<Json.Member> {
        // Dependencies inside a scope are indented twice (e.g., 8 spaces if base is 4)
        const depIndent = this.baseIndent + this.baseIndent;

        const keyLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space('\n' + this.baseIndent),
            markers: emptyMarkers,
            source: `"${this.targetScope}"`,
            value: this.targetScope
        };

        const depKeyLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space('\n' + depIndent),
            markers: emptyMarkers,
            source: `"${this.packageName}"`,
            value: this.packageName
        };

        const depValueLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space(' '),
            markers: emptyMarkers,
            source: `"${this.version}"`,
            value: this.version
        };

        const depMember: Json.Member = {
            kind: Json.Kind.Member,
            id: randomId(),
            prefix: space(''),
            markers: emptyMarkers,
            key: rightPadded(depKeyLiteral, space('')),
            value: depValueLiteral
        };

        const scopeObject: Json.Object = {
            kind: Json.Kind.Object,
            id: randomId(),
            prefix: space(' '),
            markers: emptyMarkers,
            members: [rightPadded(depMember, space('\n' + this.baseIndent))]
        };

        const scopeMemberNode: Json.Member = {
            kind: Json.Kind.Member,
            id: randomId(),
            prefix: space(''),
            markers: emptyMarkers,
            key: rightPadded(keyLiteral, space('')),
            value: scopeObject
        };

        return rightPadded(scopeMemberNode, space(afterWhitespace));
    }
}
