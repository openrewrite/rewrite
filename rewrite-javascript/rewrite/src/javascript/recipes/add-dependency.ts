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
import {Json, JsonVisitor} from "../../json";
import {findNodeResolutionResult, PackageManager} from "../node-resolution-result";
import {getAllLockFileNames} from "../package-manager";
import {markers as createMarkers} from "../../markers";
import {randomId} from "../../uuid";
import * as path from "path";
import {
    createDependencyAccumulator,
    createWarningDocument,
    DependencyAccumulator,
    DependencyScope,
    getMemberKeyName,
    parseUpdatedLockFile,
    printDocument,
    runPackageManagerInstall,
    updateNodeResolutionMarker
} from "./dependency-utils";

export class AddDependency extends ScanningRecipe<DependencyAccumulator> {
    readonly name = "org.openrewrite.javascript.dependencies.add-dependency";
    readonly displayName = "Add npm dependency";
    readonly description = "Adds a new dependency to `package.json` and updates the lock file by running the package manager.";

    @Option({
        displayName: "Package name",
        description: "The name of the npm package to add (e.g., 'lodash', '@types/node')",
        example: "lodash"
    })
    packageName!: string;

    @Option({
        displayName: "Version",
        description: "The version constraint to set (e.g., '^5.0.0', '~2.1.0', '3.0.0')",
        example: "^5.0.0"
    })
    version!: string;

    @Option({
        displayName: "Scope",
        description: "The dependency scope: 'dependencies', 'devDependencies', 'peerDependencies', or 'optionalDependencies'",
        example: "dependencies",
        required: false
    })
    scope?: DependencyScope;

    initialValue(_ctx: ExecutionContext): DependencyAccumulator {
        return createDependencyAccumulator();
    }

    private getTargetScope(): DependencyScope {
        return this.scope ?? 'dependencies';
    }

    async scanner(acc: DependencyAccumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                if (!doc.sourcePath.endsWith('package.json')) {
                    return doc;
                }

                const marker = findNodeResolutionResult(doc);
                if (!marker) {
                    return doc;
                }

                // Check if dependency already exists in any scope
                const scopes: DependencyScope[] = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'];
                for (const scope of scopes) {
                    const deps = marker[scope];
                    if (deps?.some(d => d.name === recipe.packageName)) {
                        return doc;
                    }
                }

                const projectDir = path.dirname(path.resolve(doc.sourcePath));
                const pm = marker.packageManager ?? PackageManager.Npm;

                acc.projectsToUpdate.set(doc.sourcePath, {
                    projectDir,
                    packageJsonPath: doc.sourcePath,
                    originalPackageJson: await printDocument(doc),
                    dependencyScope: recipe.getTargetScope(),
                    newVersion: recipe.version,
                    packageName: recipe.packageName,
                    packageManager: pm,
                    skipInstall: false
                });

                return doc;
            }
        };
    }

    async editorWithData(acc: DependencyAccumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                const sourcePath = doc.sourcePath;

                if (sourcePath.endsWith('package.json')) {
                    const updateInfo = acc.projectsToUpdate.get(sourcePath);
                    if (!updateInfo) {
                        return doc;
                    }

                    if (!acc.processedProjects.has(sourcePath)) {
                        await runPackageManagerInstall(acc, updateInfo, (original) =>
                            recipe.createModifiedPackageJson(original, updateInfo.dependencyScope)
                        );
                        acc.processedProjects.add(sourcePath);
                    }

                    const failureMessage = acc.failedProjects.get(sourcePath);
                    if (failureMessage) {
                        return createWarningDocument(doc, 'add', recipe.packageName, recipe.version, failureMessage);
                    }

                    const visitor = new AddDependencyVisitor(
                        recipe.packageName,
                        recipe.version,
                        updateInfo.dependencyScope
                    );
                    const modifiedDoc = await visitor.visit(doc, undefined) as Json.Document;

                    return updateNodeResolutionMarker(modifiedDoc, updateInfo, acc, findNodeResolutionResult);
                }

                for (const lockFileName of getAllLockFileNames()) {
                    if (sourcePath.endsWith(lockFileName)) {
                        const packageJsonPath = sourcePath.replace(lockFileName, 'package.json');
                        const updateInfo = acc.projectsToUpdate.get(packageJsonPath);

                        if (updateInfo && acc.updatedLockFiles.has(sourcePath)) {
                            const updatedContent = acc.updatedLockFiles.get(sourcePath)!;
                            return parseUpdatedLockFile(doc, updatedContent);
                        }
                        break;
                    }
                }

                return doc;
            }
        };
    }

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

function space(whitespace: string): Json.Space {
    return {
        kind: Json.Kind.Space,
        comments: [],
        whitespace
    };
}

function emptyMarkers() {
    return createMarkers();
}

function rightPadded<T extends Json>(element: T, after: Json.Space): Json.RightPadded<T> {
    return {
        kind: Json.Kind.RightPadded,
        element,
        after,
        markers: emptyMarkers()
    };
}

class AddDependencyVisitor extends JsonVisitor<void> {
    private readonly packageName: string;
    private readonly version: string;
    private readonly targetScope: DependencyScope;
    private scopeFound = false;
    private dependencyAdded = false;

    constructor(packageName: string, version: string, targetScope: DependencyScope) {
        super();
        this.packageName = packageName;
        this.version = version;
        this.targetScope = targetScope;
    }

    protected async visitDocument(doc: Json.Document, p: void): Promise<Json | undefined> {
        const result = await super.visitDocument(doc, p) as Json.Document;

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

    private addDependencyToScope(scopeMember: Json.Member): Json.Member {
        const value = scopeMember.value;

        if (value.kind !== Json.Kind.Object) {
            return scopeMember;
        }

        const obj = value as Json.Object;
        const members = [...(obj.members || [])];

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
                ...obj,
                members
            } as Json.Object
        };
    }

    private createDependencyMemberWithAfter(afterWhitespace: string): Json.RightPadded<Json.Member> {
        const keyLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space('\n        '),
            markers: emptyMarkers(),
            source: `"${this.packageName}"`,
            value: this.packageName
        };

        const valueLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space(' '),
            markers: emptyMarkers(),
            source: `"${this.version}"`,
            value: this.version
        };

        const member: Json.Member = {
            kind: Json.Kind.Member,
            id: randomId(),
            prefix: space(''),
            markers: emptyMarkers(),
            key: rightPadded(keyLiteral, space('')),
            value: valueLiteral
        };

        return rightPadded(member, space(afterWhitespace));
    }

    private addScopeToDocument(doc: Json.Document): Json.Document {
        const docValue = doc.value;

        if (docValue.kind !== Json.Kind.Object) {
            return doc;
        }

        const obj = docValue as Json.Object;
        const members = [...(obj.members || [])];

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
                ...obj,
                members
            } as Json.Object
        };
    }

    private createScopeMemberWithAfter(afterWhitespace: string): Json.RightPadded<Json.Member> {
        const keyLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space('\n    '),
            markers: emptyMarkers(),
            source: `"${this.targetScope}"`,
            value: this.targetScope
        };

        const depKeyLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space('\n        '),
            markers: emptyMarkers(),
            source: `"${this.packageName}"`,
            value: this.packageName
        };

        const depValueLiteral: Json.Literal = {
            kind: Json.Kind.Literal,
            id: randomId(),
            prefix: space(' '),
            markers: emptyMarkers(),
            source: `"${this.version}"`,
            value: this.version
        };

        const depMember: Json.Member = {
            kind: Json.Kind.Member,
            id: randomId(),
            prefix: space(''),
            markers: emptyMarkers(),
            key: rightPadded(depKeyLiteral, space('')),
            value: depValueLiteral
        };

        const scopeObject: Json.Object = {
            kind: Json.Kind.Object,
            id: randomId(),
            prefix: space(' '),
            markers: emptyMarkers(),
            members: [rightPadded(depMember, space('\n    '))]
        };

        const scopeMemberNode: Json.Member = {
            kind: Json.Kind.Member,
            id: randomId(),
            prefix: space(''),
            markers: emptyMarkers(),
            key: rightPadded(keyLiteral, space('')),
            value: scopeObject
        };

        return rightPadded(scopeMemberNode, space(afterWhitespace));
    }
}
