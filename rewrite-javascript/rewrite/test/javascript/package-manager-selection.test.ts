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
import {withDir} from "tmp-promise";
import * as fs from "fs";
import * as path from "path";
import {detectPackageManager, findNodeResolutionResult, findNodeRuntime, PackageJsonParser, PackageManager} from "../../src/javascript";
import {Json} from "../../src/json";
import {selectPackageManager} from "../../src/javascript/package-manager";
import {MarkersKind} from "../../src/markers";

function write(root: string, name: string, value: object | string): string {
    const file = path.join(root, name);
    fs.mkdirSync(path.dirname(file), {recursive: true});
    fs.writeFileSync(file, typeof value === "string" ? value : JSON.stringify(value));
    return file;
}

function npmLock(root: string): void {
    write(root, "package-lock.json", {name: "probe", lockfileVersion: 3,
        packages: {"": {name: "probe"}, "node_modules/ms": {version: "1.0.0"}}});
}

function bunLock(root: string): void {
    write(root, "bun.lock", {lockfileVersion: 1, workspaces: {"": {name: "probe"}},
        packages: {ms: ["ms@2.1.3", "", {}, ""]}});
}

async function parse(root: string, file = "package.json", text?: string): Promise<Json.Document> {
    const parser = new PackageJsonParser({relativeTo: root});
    const input = text === undefined ? path.join(root, file) : {sourcePath: path.join(root, file), text};
    const docs: Json.Document[] = [];
    for await (const doc of parser.parse(input)) docs.push(doc as Json.Document);
    return docs[0];
}

// Measure the executable actually selected; a declaration is not an observed version.
async function withFakeManagers<T>(root: string, fn: (runs: string) => Promise<T>): Promise<T> {
    const bin = path.join(root, "bin");
    const runs = path.join(root, "commands");
    for (const [name, version] of [["bun", "1.4.4"], ["npm", "11.19.1"]]) {
        const file = write(bin, name, `#!/bin/sh\necho ${name} >> "${runs}"\necho ${version}\n`);
        fs.chmodSync(file, 0o755);
        write(bin, name + ".cmd", `@echo off\r\necho ${name}>> "${runs}"\r\necho ${version}\r\n`);
    }
    const before = process.env.PATH;
    process.env.PATH = bin + path.delimiter + (before ?? "");
    try { return await fn(runs); } finally { process.env.PATH = before; }
}

const pkg = {name: "probe", version: "1.0.0", dependencies: {ms: "2.1.3"}};

describe("declared package manager selection", () => {
    test.each([
        ["npm@11.19.1", PackageManager.Npm],
        ["bun@1.2.2", PackageManager.Bun],
        ["pnpm@11.1.1", PackageManager.Pnpm],
        ["yarn@1.22.22", PackageManager.YarnClassic],
        ["yarn@4.12.0+sha512.test", PackageManager.YarnBerry]
    ])("%s wins over foreign lock files", async (declared, expected) => {
        await withDir(async dir => {
            write(dir.path, "package.json", {...pkg, packageManager: declared});
            npmLock(dir.path); bunLock(dir.path);
            write(dir.path, "pnpm-lock.yaml", "lockfileVersion: '9.0'\n");
            write(dir.path, "yarn.lock", expected === PackageManager.YarnClassic ?
                "# yarn lockfile v1\n" : "__metadata:\n  version: 8\n");
            expect(detectPackageManager(dir.path)).toBe(expected);
        }, {unsafeCleanup: true});
    });

    test.each([undefined, null, 42, "", "bun", "bun@invalid", "other@1.0.0"])(
        "an absent or unrecognized declaration %s retains lock detection", async declared => {
            await withDir(async dir => {
                write(dir.path, "package.json", {...pkg, packageManager: declared});
                npmLock(dir.path); bunLock(dir.path);
                expect(detectPackageManager(dir.path)).toBe(PackageManager.Npm);
            }, {unsafeCleanup: true});
        });

    test("the parser uses Bun's graph and measures Bun rather than copying its declared pin", async () => {
        await withDir(async dir => {
            write(dir.path, "package.json", {...pkg, packageManager: "bun@1.2.2"});
            npmLock(dir.path); bunLock(dir.path);
            await withFakeManagers(dir.path, async runs => {
                const doc = await parse(dir.path);
                expect(findNodeResolutionResult(doc)?.packageManager).toBe(PackageManager.Bun);
                expect(findNodeResolutionResult(doc)?.dependencies.find(d => d.name === "ms")?.resolved?.version).toBe("2.1.3");
                expect(findNodeRuntime(doc)?.packageManager).toBe(PackageManager.Bun);
                expect(findNodeRuntime(doc)?.packageManagerVersion).toBe("1.4.4");
                expect(fs.readFileSync(runs, "utf8").trim()).toBe("bun");
            });
        }, {unsafeCleanup: true});
    });

    test("workspace members inherit the root declaration while an explicit nested project can override it", async () => {
        await withDir(async dir => {
            write(dir.path, "package.json", {...pkg, packageManager: "bun@1.2.2", workspaces: ["packages/*"]});
            npmLock(dir.path); bunLock(dir.path);
            write(dir.path, "packages/a/package.json", pkg);
            npmLock(path.join(dir.path, "packages/a"));
            write(dir.path, "standalone/package.json", {...pkg, packageManager: "npm@11.19.1"});
            npmLock(path.join(dir.path, "standalone"));
            await withFakeManagers(dir.path, async () => {
                expect(findNodeResolutionResult(await parse(dir.path, "packages/a/package.json"))?.packageManager).toBe(PackageManager.Bun);
                expect(findNodeResolutionResult(await parse(dir.path, "standalone/package.json"))?.packageManager).toBe(PackageManager.Npm);
            });
        }, {unsafeCleanup: true});
    });

    test.each([
        ["bun@1.2.2", "package-lock.json", "bun.lock"],
        ["yarn@4.12.0", "yarn.lock", "YarnClassic"]
    ])("%s does not silently consume a missing or incompatible declared lock", async (declared, file, reason) => {
        await withDir(async dir => {
            write(dir.path, "package.json", {...pkg, packageManager: declared});
            if (file === "package-lock.json") npmLock(dir.path);
            else write(dir.path, file, "# yarn lockfile v1\n");
            expect(() => detectPackageManager(dir.path)).toThrow(reason);
            const warning = vi.spyOn(console, "warn").mockImplementation(() => {});
            try {
                const doc = await parse(dir.path);
                expect(findNodeResolutionResult(doc)).toBeUndefined();
                expect(findNodeRuntime(doc)?.packageManager).toBeUndefined();
                expect(doc.markers.markers.find(m => m.kind === MarkersKind.MarkupWarn))
                    .toMatchObject({message: expect.stringContaining(reason)});
            } finally { warning.mockRestore(); }
        }, {unsafeCleanup: true});
    });

    test("a corrected selection clears an earlier diagnostic on the same parser", async () => {
        await withDir(async dir => {
            const file = write(dir.path, "package.json", {...pkg, packageManager: "bun@1.2.2"});
            npmLock(dir.path);
            const parser = new PackageJsonParser({relativeTo: dir.path});
            const warning = vi.spyOn(console, "warn").mockImplementation(() => {});
            try {
                const first = await parser.parse(file).next();
                expect((first.value as Json.Document).markers.markers.some(m => m.kind === MarkersKind.MarkupWarn)).toBe(true);
                bunLock(dir.path);
                await withFakeManagers(dir.path, async () => {
                    const next = await parser.parse(file).next();
                    expect(findNodeResolutionResult(next.value as Json.Document)?.packageManager).toBe(PackageManager.Bun);
                    expect((next.value as Json.Document).markers.markers.some(m => m.kind === MarkersKind.MarkupWarn)).toBe(false);
                });
            } finally { warning.mockRestore(); }
        }, {unsafeCleanup: true});
    });

    test("the supplied manifest text takes precedence over older filesystem contents", async () => {
        await withDir(async dir => {
            write(dir.path, "package.json", {...pkg, packageManager: "npm@11.19.1"});
            npmLock(dir.path); bunLock(dir.path);
            await withFakeManagers(dir.path, async () => {
                const doc = await parse(dir.path, "package.json", JSON.stringify({...pkg, packageManager: "bun@1.2.2"}));
                expect(findNodeResolutionResult(doc)?.packageManager).toBe(PackageManager.Bun);
            });
        }, {unsafeCleanup: true});
    });

    test("a lockless declared project is recognized without borrowing a different manager", async () => {
        await withDir(async dir => {
            write(dir.path, "package.json", {...pkg, packageManager: "bun@1.2.2"});
            expect(detectPackageManager(dir.path)).toBe(PackageManager.Bun);
        }, {unsafeCleanup: true});
    });

    test("a declaration outside the parser root cannot select this project's manager", async () => {
        await withDir(async dir => {
            write(dir.path, "package.json", {...pkg, packageManager: "bun@1.2.2"});
            const root = path.join(dir.path, "independent");
            write(root, "package.json", pkg); npmLock(root);
            await withFakeManagers(dir.path, async () => {
                expect(findNodeResolutionResult(await parse(root))?.packageManager).toBe(PackageManager.Npm);
            });
        }, {unsafeCleanup: true});
    });
});

test('an explicit nested project does not borrow an outer foreign lock', async () => {
 await withDir(async d => {
  fs.writeFileSync(path.join(d.path,'package.json'),JSON.stringify({name:'outer',packageManager:'bun@1.2.2'}));
  fs.writeFileSync(path.join(d.path,'bun.lock'),'{}');
  fs.writeFileSync(path.join(d.path,'package-lock.json'),'{}');
  const nested=path.join(d.path,'standalone');fs.mkdirSync(nested);
  fs.writeFileSync(path.join(nested,'package.json'),JSON.stringify({name:'nested',packageManager:'npm@11.19.1'}));
  const selected=selectPackageManager(nested,d.path);
  expect(selected).toEqual({packageManager:PackageManager.Npm,root:nested});
 },{unsafeCleanup:true});
});

test('an unrelated standalone nested npm project keeps its own lock',async()=>{
 await withDir(async d=>{
  fs.writeFileSync(path.join(d.path,'package.json'),JSON.stringify({name:'outer',packageManager:'bun@1.2.2',workspaces:['packages/*']}));
  fs.writeFileSync(path.join(d.path,'bun.lock'),'{}');
  const nested=path.join(d.path,'standalone');fs.mkdirSync(nested);
  fs.writeFileSync(path.join(nested,'package.json'),JSON.stringify({name:'nested'}));
  const lockFile=path.join(nested,'package-lock.json');fs.writeFileSync(lockFile,'{}');
  const selected=selectPackageManager(nested,d.path);
  expect(selected).toEqual({packageManager:PackageManager.Npm,root:nested,lockFile});
 },{unsafeCleanup:true});
});

test.each(["array", "object", "pnpm"])("%s workspace membership permits a compatible member declaration to share its root lock", async style => {
    await withDir(async dir => {
        write(dir.path, "package.json", {...pkg, packageManager: "bun@1.2.2",
            ...(style === "array" ? {workspaces: ["packages/*", "!packages/excluded"]} :
                style === "object" ? {workspaces: {packages: ["packages/*", "!packages/excluded"]}} : {})});
        if (style === "pnpm") write(dir.path, "pnpm-workspace.yaml", "packages:\n  - packages/*\n  - '!packages/excluded'\n");
        bunLock(dir.path);
        const member = path.join(dir.path, "packages/a");
        write(member, "package.json", {...pkg, packageManager: "bun@1.2.2"});
        expect(selectPackageManager(member, dir.path)).toEqual({packageManager: PackageManager.Bun,
            root: dir.path, lockFile: path.join(dir.path, "bun.lock")});
        const excluded = path.join(dir.path, "packages/excluded");
        write(excluded, "package.json", pkg);
        npmLock(excluded);
        expect(selectPackageManager(excluded, dir.path)?.packageManager).toBe(PackageManager.Npm);
    }, {unsafeCleanup: true});
});
