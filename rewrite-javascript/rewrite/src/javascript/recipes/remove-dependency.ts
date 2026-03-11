/*
 * Copyright 2026 the original author or authors.
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
import {getMemberKeyName, isJson, isObject, Json, JsonVisitor, space} from "../../json";
import {isDocuments, isYaml, Yaml} from "../../yaml";
import {isPlainText, PlainText} from "../../text";
import {
    allDependencyScopes,
    DependencyScope,
    findNodeResolutionResult,
    PackageManager,
    serializeNpmrcConfigs
} from "../node-resolution-result";
import {markupWarn} from "../../markers";
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
import * as path from "path";

interface ProjectUpdateInfo {
    packageJsonPath: string;
    originalPackageJson: string;
    dependencyScope: DependencyScope;
    packageManager: PackageManager;
    configFiles?: Record<string, string>;
}

interface Accumulator extends DependencyRecipeAccumulator<ProjectUpdateInfo> {
    originalLockFiles: Map<string, string>;
}

export class RemoveDependency extends ScanningRecipe<Accumulator> {
    readonly name = "org.openrewrite.javascript.dependencies.remove-dependency";
    readonly displayName = "Remove npm dependency";
    readonly description = "Removes a dependency from `package.json` and updates the lock file by running the package manager.";

    @Option({
        displayName: "Package name",
        description: "The name of the npm package to remove (e.g., `lodash`, `@types/node`)",
        example: "lodash"
    })
    packageName!: string;

    @Option({
        displayName: "Scope",
        description: "The dependency scope to remove from. If not specified, the dependency is removed from whichever scope it is found in.",
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

    async scanner(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;
        const LOCK_FILE_NAMES = getAllLockFileNames();

        return new class extends TreeVisitor<Tree, ExecutionContext> {
            protected async accept(tree: Tree, ctx: ExecutionContext): Promise<Tree | undefined> {
                if (isJson(tree) && tree.kind === Json.Kind.Document) {
                    return this.handleJsonDocument(tree as Json.Document, ctx);
                }
                if (isYaml(tree) && isDocuments(tree)) {
                    return this.handleYamlDocument(tree, ctx);
                }
                if (isPlainText(tree)) {
                    return this.handlePlainTextDocument(tree as PlainText, ctx);
                }
                return tree;
            }

            private async handleJsonDocument(doc: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                const basename = path.basename(doc.sourcePath);

                if (LOCK_FILE_NAMES.includes(basename)) {
                    acc.originalLockFiles.set(doc.sourcePath, await TreePrinters.print(doc));
                    return doc;
                }

                if (!doc.sourcePath.endsWith('package.json')) {
                    return doc;
                }

                const marker = findNodeResolutionResult(doc);
                if (!marker) {
                    return doc;
                }

                const scopesToCheck = recipe.scope ? [recipe.scope] : allDependencyScopes;
                let foundScope: DependencyScope | undefined;

                for (const scope of scopesToCheck) {
                    const deps = marker[scope];
                    if (deps?.some(d => d.name === recipe.packageName)) {
                        foundScope = scope;
                        break;
                    }
                }

                if (!foundScope) {
                    return doc;
                }

                const pm = marker.packageManager ?? PackageManager.Npm;

                const configFiles: Record<string, string> = {};
                const npmrcContent = serializeNpmrcConfigs(marker.npmrcConfigs);
                if (npmrcContent) {
                    configFiles['.npmrc'] = npmrcContent;
                }

                acc.projectsToUpdate.set(doc.sourcePath, {
                    packageJsonPath: doc.sourcePath,
                    originalPackageJson: await TreePrinters.print(doc),
                    dependencyScope: foundScope,
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

        const jsonEditor = new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
                const sourcePath = doc.sourcePath;

                if (sourcePath.endsWith('package.json')) {
                    const updateInfo = acc.projectsToUpdate.get(sourcePath);
                    if (!updateInfo) {
                        return doc;
                    }

                    const failureMessage = await runInstallIfNeeded(sourcePath, acc, () =>
                        recipe.runPackageManagerInstall(acc, updateInfo, ctx)
                    );
                    if (failureMessage) {
                        return markupWarn(
                            doc,
                            `Failed to remove ${recipe.packageName}`,
                            failureMessage
                        );
                    }

                    const visitor = new RemoveDependencyVisitor(
                        recipe.packageName,
                        updateInfo.dependencyScope
                    );
                    const modifiedDoc = await visitor.visit(doc, undefined) as Json.Document;

                    return updateNodeResolutionMarker(modifiedDoc, updateInfo, acc);
                }

                const lockFileName = path.basename(sourcePath);
                if (getAllLockFileNames().includes(lockFileName)) {
                    const updatedLockContent = acc.updatedLockFiles.get(sourcePath);
                    if (updatedLockContent) {
                        const parsed = await parseLockFileContent(updatedLockContent, sourcePath, lockFileName) as Json.Document;
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

        return createLockFileEditor(jsonEditor, acc);
    }

    private async runPackageManagerInstall(
        acc: Accumulator,
        updateInfo: ProjectUpdateInfo,
        _ctx: ExecutionContext
    ): Promise<void> {
        const modifiedPackageJson = this.createModifiedPackageJson(
            updateInfo.originalPackageJson,
            updateInfo.dependencyScope
        );

        const lockFileName = getLockFileName(updateInfo.packageManager);
        const packageJsonDir = path.dirname(updateInfo.packageJsonPath);
        const lockFilePath = packageJsonDir === '.'
            ? lockFileName
            : path.join(packageJsonDir, lockFileName);

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

    private createModifiedPackageJson(
        originalContent: string,
        scope: DependencyScope
    ): string {
        const packageJson = JSON.parse(originalContent);

        if (packageJson[scope] && packageJson[scope][this.packageName]) {
            delete packageJson[scope][this.packageName];

            if (Object.keys(packageJson[scope]).length === 0) {
                delete packageJson[scope];
            }
        }

        return JSON.stringify(packageJson, null, 2);
    }
}

class RemoveDependencyVisitor extends JsonVisitor<void> {
    private readonly packageName: string;
    private readonly targetScope: DependencyScope;

    constructor(packageName: string, targetScope: DependencyScope) {
        super();
        this.packageName = packageName;
        this.targetScope = targetScope;
    }

    protected async visitDocument(doc: Json.Document, p: void): Promise<Json | undefined> {
        if (!isObject(doc.value)) {
            return doc;
        }
        const rootObj = doc.value;
        const updatedRootMembers = this.processMembers(rootObj.members);
        if (updatedRootMembers === rootObj.members) {
            return doc;
        }
        return {
            ...doc,
            value: {
                ...rootObj,
                members: updatedRootMembers
            } as Json.Object
        };
    }

    private processMembers(members: Json.RightPadded<Json>[]): Json.RightPadded<Json>[] {
        let changed = false;
        const result: Json.RightPadded<Json>[] = [];

        for (const rp of members) {
            const member = rp.element as Json.Member;
            const keyName = getMemberKeyName(member);

            if (keyName === this.targetScope) {
                if (!isObject(member.value)) {
                    result.push(rp);
                    continue;
                }

                const scopeObj = member.value;
                const filtered = scopeObj.members.filter(depRp => {
                    const depMember = depRp.element as Json.Member;
                    return getMemberKeyName(depMember) !== this.packageName;
                });

                if (filtered.length === scopeObj.members.length) {
                    result.push(rp);
                    continue;
                }

                changed = true;

                if (filtered.length === 0) {
                    continue;
                }

                const lastOriginalAfter = scopeObj.members[scopeObj.members.length - 1].after;
                const updatedDeps = [...filtered];
                updatedDeps[updatedDeps.length - 1] = {
                    ...updatedDeps[updatedDeps.length - 1],
                    after: lastOriginalAfter
                };

                result.push({
                    ...rp,
                    element: {
                        ...member,
                        value: {
                            ...scopeObj,
                            members: updatedDeps
                        } as Json.Object
                    }
                } as Json.RightPadded<Json>);
            } else {
                result.push(rp);
            }
        }

        if (!changed) {
            return members;
        }

        if (result.length > 0 && result.length < members.length) {
            const originalLastAfter = members[members.length - 1].after;
            result[result.length - 1] = {
                ...result[result.length - 1],
                after: originalLastAfter
            };
        }

        return result;
    }
}
