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

import {Json, JsonParser, JsonVisitor} from "../../json";
import {
    createNodeResolutionResultMarker,
    PackageJsonContent,
    PackageLockContent,
    PackageManager,
    readNpmrcConfigs
} from "../node-resolution-result";
import {getAllLockFileNames, getLockFileName, runInstall} from "../package-manager";
import {markupWarn, replaceMarkerByKind} from "../../markers";
import {TreePrinters} from "../../print";
import * as fs from "fs";
import * as fsp from "fs/promises";
import * as path from "path";
import * as os from "os";

export type DependencyScope = 'dependencies' | 'devDependencies' | 'peerDependencies' | 'optionalDependencies';

export interface ProjectUpdateInfo {
    projectDir: string;
    packageJsonPath: string;
    originalPackageJson: string;
    dependencyScope: DependencyScope;
    currentVersion?: string;
    newVersion: string;
    packageName: string;
    packageManager: PackageManager;
    skipInstall: boolean;
}

export interface DependencyAccumulator {
    projectsToUpdate: Map<string, ProjectUpdateInfo>;
    updatedLockFiles: Map<string, string>;
    updatedPackageJsons: Map<string, string>;
    processedProjects: Set<string>;
    failedProjects: Map<string, string>;
}

export function createDependencyAccumulator(): DependencyAccumulator {
    return {
        projectsToUpdate: new Map(),
        updatedLockFiles: new Map(),
        updatedPackageJsons: new Map(),
        processedProjects: new Set(),
        failedProjects: new Map()
    };
}

export async function printDocument(doc: Json.Document): Promise<string> {
    return TreePrinters.print(doc);
}

export async function runPackageManagerInstall(
    acc: DependencyAccumulator,
    updateInfo: ProjectUpdateInfo,
    modifyPackageJson: (original: string) => string
): Promise<void> {
    const pm = updateInfo.packageManager;
    const lockFileName = getLockFileName(pm);
    const tempDir = await fsp.mkdtemp(path.join(os.tmpdir(), 'openrewrite-pm-'));

    try {
        const modifiedPackageJson = modifyPackageJson(updateInfo.originalPackageJson);
        await fsp.writeFile(path.join(tempDir, 'package.json'), modifiedPackageJson);

        const originalLockPath = path.join(updateInfo.projectDir, lockFileName);
        if (fs.existsSync(originalLockPath)) {
            await fsp.copyFile(originalLockPath, path.join(tempDir, lockFileName));
        }

        const configFiles = ['.npmrc', '.yarnrc', '.yarnrc.yml', '.pnpmfile.cjs', 'pnpm-workspace.yaml'];
        for (const configFile of configFiles) {
            const configPath = path.join(updateInfo.projectDir, configFile);
            if (fs.existsSync(configPath)) {
                await fsp.copyFile(configPath, path.join(tempDir, configFile));
            }
        }

        const result = runInstall(pm, {
            cwd: tempDir,
            lockOnly: true,
            timeout: 120000
        });

        if (result.success) {
            acc.updatedPackageJsons.set(updateInfo.packageJsonPath, modifiedPackageJson);

            const updatedLockPath = path.join(tempDir, lockFileName);
            if (fs.existsSync(updatedLockPath)) {
                const updatedLockContent = await fsp.readFile(updatedLockPath, 'utf-8');
                const lockFilePath = updateInfo.packageJsonPath.replace('package.json', lockFileName);
                acc.updatedLockFiles.set(lockFilePath, updatedLockContent);
            }
        } else {
            const errorMessage = result.error || result.stderr || 'Unknown error';
            acc.failedProjects.set(updateInfo.packageJsonPath, errorMessage);
        }
    } finally {
        try {
            await fsp.rm(tempDir, {recursive: true, force: true});
        } catch {
            // Ignore cleanup errors
        }
    }
}

export async function updateNodeResolutionMarker(
    doc: Json.Document,
    updateInfo: ProjectUpdateInfo,
    acc: DependencyAccumulator,
    findMarker: (doc: Json.Document) => any
): Promise<Json.Document> {
    const existingMarker = findMarker(doc);
    if (!existingMarker) {
        return doc;
    }

    const updatedPackageJson = acc.updatedPackageJsons.get(updateInfo.packageJsonPath);
    const lockFileName = getLockFileName(updateInfo.packageManager);
    const updatedLockFile = acc.updatedLockFiles.get(
        updateInfo.packageJsonPath.replace('package.json', lockFileName)
    );

    let packageJsonContent: PackageJsonContent;
    let lockContent: PackageLockContent | undefined;

    try {
        packageJsonContent = JSON.parse(updatedPackageJson || updateInfo.originalPackageJson);
    } catch {
        return doc;
    }

    if (updatedLockFile) {
        try {
            lockContent = JSON.parse(updatedLockFile);
        } catch {
            // Continue without lock file content
        }
    }

    const npmrcConfigs = await readNpmrcConfigs(updateInfo.projectDir);

    const newMarker = createNodeResolutionResultMarker(
        existingMarker.path,
        packageJsonContent,
        lockContent,
        existingMarker.workspacePackagePaths,
        existingMarker.packageManager,
        npmrcConfigs.length > 0 ? npmrcConfigs : undefined
    );

    return {
        ...doc,
        markers: replaceMarkerByKind(doc.markers, newMarker)
    };
}

export async function parseUpdatedLockFile(
    originalDoc: Json.Document,
    updatedContent: string
): Promise<Json.Document> {
    const parser = new JsonParser({});
    const parsed: Json.Document[] = [];

    for await (const sf of parser.parse({text: updatedContent, sourcePath: originalDoc.sourcePath})) {
        parsed.push(sf as Json.Document);
    }

    if (parsed.length > 0) {
        return {
            ...parsed[0],
            sourcePath: originalDoc.sourcePath,
            markers: originalDoc.markers
        };
    }

    return originalDoc;
}

export function getMemberKeyName(member: Json.Member): string | undefined {
    const key = member.key.element;
    if (key.kind === Json.Kind.Literal) {
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

export function createWarningDocument(
    doc: Json.Document,
    operation: string,
    packageName: string,
    version: string,
    message: string
): Json.Document {
    return markupWarn(doc, `Failed to ${operation} ${packageName} to ${version}`, message);
}

export function handleLockFileUpdate(
    sourcePath: string,
    acc: DependencyAccumulator
): { packageJsonPath: string; updateInfo: ProjectUpdateInfo | undefined; updatedContent: string | undefined } | undefined {
    for (const lockFileName of getAllLockFileNames()) {
        if (sourcePath.endsWith(lockFileName)) {
            const packageJsonPath = sourcePath.replace(lockFileName, 'package.json');
            const updateInfo = acc.projectsToUpdate.get(packageJsonPath);

            return {
                packageJsonPath,
                updateInfo,
                updatedContent: acc.updatedLockFiles.get(sourcePath)
            };
        }
    }
    return undefined;
}

export class UpdateVersionVisitor extends JsonVisitor<void> {
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
        const keyName = getMemberKeyName(member);

        if (keyName === this.targetScope) {
            this.inTargetScope = true;
            const result = await super.visitMember(member, p);
            this.inTargetScope = false;
            return result;
        }

        if (this.inTargetScope && keyName === this.packageName) {
            return this.updateVersion(member);
        }

        return super.visitMember(member, p);
    }

    private updateVersion(member: Json.Member): Json.Member {
        const value = member.value;

        if (value.kind !== Json.Kind.Literal) {
            return member;
        }

        const literal = value as Json.Literal;

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
