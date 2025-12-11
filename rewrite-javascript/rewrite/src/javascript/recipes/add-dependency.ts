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
import {detectIndent, getMemberKeyName, isObject, Json, JsonParser, JsonVisitor, rightPadded, space} from "../../json";
import {
    allDependencyScopes,
    DependencyScope,
    findNodeResolutionResult,
    PackageManager
} from "../node-resolution-result";
import {emptyMarkers, markupWarn} from "../../markers";
import {TreePrinters} from "../../print";
import {
    createDependencyRecipeAccumulator,
    DependencyRecipeAccumulator,
    getUpdatedLockFileContent,
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
    /** Absolute path to the project directory */
    projectDir: string;
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
}

type Accumulator = DependencyRecipeAccumulator<ProjectUpdateInfo>;

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
        return createDependencyRecipeAccumulator();
    }

    private getTargetScope(): DependencyScope {
        return this.scope ?? 'dependencies';
    }

    async scanner(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                // Only process package.json files
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

                // Get the project directory and package manager
                const projectDir = path.dirname(path.resolve(doc.sourcePath));
                const pm = marker.packageManager ?? PackageManager.Npm;

                acc.projectsToUpdate.set(doc.sourcePath, {
                    projectDir,
                    packageJsonPath: doc.sourcePath,
                    originalPackageJson: await this.printDocument(doc),
                    dependencyScope: recipe.getTargetScope(),
                    newVersion: recipe.version,
                    packageManager: pm
                });

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

                // Handle lock files for all package managers
                const updatedLockContent = getUpdatedLockFileContent(sourcePath, acc);
                if (updatedLockContent) {
                    return await new JsonParser({}).parseOne({
                        text: updatedLockContent,
                        sourcePath: doc.sourcePath
                    }) as Json.Document;
                }

                return doc;
            }
        };
    }

    /**
     * Runs the package manager in a temporary directory to update the lock file.
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

        const result = await runInstallInTempDir(
            updateInfo.projectDir,
            updateInfo.packageManager,
            modifiedPackageJson
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
