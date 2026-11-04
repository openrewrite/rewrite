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
import {
    createNodeRuntimeMarker,
    findNodeResolutionResult,
    findNodeRuntime,
    libc,
    NodeRuntime,
    NodeRuntimeKind,
    PackageJsonParser,
    PackageManager
} from "../../src/javascript";
import {Json} from "../../src/json";
import {ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";
import {getPlatformCommand} from "../../src/shell-utils";
import {withDir} from "tmp-promise";
import {spawnSync} from "child_process";
import * as fs from "fs";
import * as path from "path";

describe("NodeRuntime", () => {

    function write(root: string, file: string, content: string | object): string {
        const at = path.join(root, file);
        fs.mkdirSync(path.dirname(at), {recursive: true});
        fs.writeFileSync(at, typeof content === "string" ? content : JSON.stringify(content, null, 2));
        return at;
    }

    async function parse(parser: PackageJsonParser, ...files: string[]): Promise<Json.Document[]> {
        const docs: Json.Document[] = [];
        for await (const doc of parser.parse(...files)) {
            docs.push(doc as Json.Document);
        }
        return docs;
    }

    function lock(name: string): object {
        return {
            name, version: "1.0.0", lockfileVersion: 3, requires: true,
            packages: {"": {name, version: "1.0.0"}}
        };
    }

    // the parser's commands find their package manager on PATH: the test's own for the length of fn
    async function withPath<T>(dirs: string[], fn: () => Promise<T>): Promise<T> {
        const before = process.env.PATH;
        process.env.PATH = dirs.join(path.delimiter);
        try {
            return await fn();
        } finally {
            process.env.PATH = before;
        }
    }

    // an npm that answers `answer` to --version and notes each time it runs
    function fakeNpm(bin: string, answer: string, runs: string, exit = 0): void {
        fs.mkdirSync(bin, {recursive: true});
        const sh = path.join(bin, "npm");
        fs.writeFileSync(sh, `#!/bin/sh\necho run >> "${runs}"\necho "${answer}"\nexit ${exit}\n`);
        fs.chmodSync(sh, 0o755);
        fs.writeFileSync(path.join(bin, "npm.cmd"),
            `@echo off\r\necho run>> "${runs}"\r\necho ${answer}\r\nexit /b ${exit}\r\n`);
    }

    function runs(file: string): number {
        return fs.existsSync(file) ? fs.readFileSync(file, "utf-8").split("\n").filter(Boolean).length : 0;
    }

    test("the parser records the Node and platform it runs on", async () => {
        await withDir(async dir => {
            const pkg = write(dir.path, "package.json", {name: "a", version: "1.0.0"});

            const [doc] = await parse(new PackageJsonParser({relativeTo: dir.path}), pkg);

            const runtime = findNodeRuntime(doc)!;
            expect(runtime.kind).toBe(NodeRuntimeKind);
            expect(runtime.nodeVersion).toBe(process.version);
            expect(runtime.os).toBe(process.platform);
            expect(runtime.cpu).toBe(process.arch);
            expect(runtime.libc).toBe(libc(process.platform) ?? undefined);
            // without a lock the parser finds no package manager, and asks none for its version
            expect(runtime.packageManager).toBeUndefined();
            expect(runtime.packageManagerVersion).toBeUndefined();
        }, {unsafeCleanup: true});
    });

    test("with npm on PATH, an npm project records what npm --version answers in its root", async () => {
        await withDir(async dir => {
            const pkg = write(dir.path, "package.json", {name: "a", version: "1.0.0"});
            write(dir.path, "package-lock.json", lock("a"));

            const [doc] = await parse(new PackageJsonParser({relativeTo: dir.path}), pkg);

            const npm = spawnSync(getPlatformCommand("npm"), ["--version"], {
                cwd: dir.path, encoding: "utf-8", ...(process.platform === "win32" ? {shell: true} : {})
            });
            const runtime = findNodeRuntime(doc)!;
            expect(runtime.packageManager).toBe(PackageManager.Npm);
            expect(runtime.packageManagerVersion).toMatch(/^\d+\.\d+\.\d+/);
            expect(runtime.packageManagerVersion).toBe(npm.stdout.trim());
        }, {unsafeCleanup: true});
    });

    test("a package manager that is not installed leaves the version absent", async () => {
        await withDir(async dir => {
            const pkg = write(dir.path, "package.json", {name: "a", version: "1.0.0"});
            write(dir.path, "bun.lock", {lockfileVersion: 1, workspaces: {"": {name: "a"}}, packages: {}});
            const empty = path.join(dir.path, "bin");
            fs.mkdirSync(empty);

            const [doc] = await withPath([empty], () => parse(new PackageJsonParser({relativeTo: dir.path}), pkg));

            const runtime = findNodeRuntime(doc)!;
            expect(runtime.packageManager).toBe(PackageManager.Bun);
            expect(runtime.packageManagerVersion).toBeUndefined();
        }, {unsafeCleanup: true});
    });

    test("an answer that is no version, or a command that fails, leaves the version absent", async () => {
        await withDir(async dir => {
            const pkg = write(dir.path, "package.json", {name: "a", version: "1.0.0"});
            write(dir.path, "package-lock.json", lock("a"));
            const count = path.join(dir.path, "runs");

            fakeNpm(path.join(dir.path, "words"), "npm 11.17.0", count);
            const [words] = await withPath([path.join(dir.path, "words")],
                () => parse(new PackageJsonParser({relativeTo: dir.path}), pkg));
            fakeNpm(path.join(dir.path, "fails"), "11.17.0", count, 1);
            const [fails] = await withPath([path.join(dir.path, "fails")],
                () => parse(new PackageJsonParser({relativeTo: dir.path}), pkg));
            fakeNpm(path.join(dir.path, "prerelease"), "12.0.0-pre.1+build.5", count);
            const [prerelease] = await withPath([path.join(dir.path, "prerelease")],
                () => parse(new PackageJsonParser({relativeTo: dir.path}), pkg));

            expect(findNodeRuntime(words)!.packageManagerVersion).toBeUndefined();
            expect(findNodeRuntime(fails)!.packageManagerVersion).toBeUndefined();
            expect(findNodeRuntime(prerelease)!.packageManagerVersion).toBe("12.0.0-pre.1+build.5");
            expect(runs(count)).toBe(3);
        }, {unsafeCleanup: true});
    });

    test("a workspace's members share its root's NodeRuntime, asked once for the parser's lifetime", async () => {
        await withDir(async dir => {
            const root = write(dir.path, "package.json", {name: "root", private: true, workspaces: ["packages/*"]});
            write(dir.path, "package-lock.json", lock("root"));
            const a = write(dir.path, "packages/a/package.json", {name: "a", version: "1.0.0"});
            const b = write(dir.path, "packages/b/package.json", {name: "b", version: "1.0.0"});
            const bin = path.join(dir.path, "bin");
            const count = path.join(dir.path, "runs");
            fakeNpm(bin, "  10.9.2  ", count);

            const parser = new PackageJsonParser({relativeTo: dir.path});
            const docs = await withPath([bin], () => parse(parser, root, a, b));
            const again = await withPath([bin], () => parse(parser, a));

            const runtime = findNodeRuntime(docs[0])!;
            expect(runtime.packageManager).toBe(PackageManager.Npm);
            expect(runtime.packageManagerVersion).toBe("10.9.2");
            expect(findNodeRuntime(docs[1])).toBe(runtime);
            expect(findNodeRuntime(docs[2])).toBe(runtime);
            expect(findNodeRuntime(again[0])).toBe(runtime);
            expect(runs(count)).toBe(1);

            // another parser asks again
            await withPath([bin], () => parse(new PackageJsonParser({relativeTo: dir.path}), a));
            expect(runs(count)).toBe(2);
        }, {unsafeCleanup: true});
    });

    test("a package.json the parser gives no NodeResolutionResult still gets a NodeRuntime", async () => {
        await withDir(async dir => {
            const pkg = write(dir.path, "package.json", "null");
            write(dir.path, "package-lock.json", lock("a"));
            const warn = vi.spyOn(console, "warn").mockImplementation(() => {});

            const [doc] = await parse(new PackageJsonParser({relativeTo: dir.path}), pkg);

            expect(warn).toHaveBeenCalledWith(expect.stringContaining("Failed to create NodeResolutionResult marker"));
            expect(findNodeResolutionResult(doc)).toBeUndefined();
            const runtime = findNodeRuntime(doc)!;
            expect(runtime.nodeVersion).toBe(process.version);
            // the package manager is the one of the document's NodeResolutionResult, which it has none of
            expect(runtime.packageManager).toBeUndefined();
        }, {unsafeCleanup: true});
    });

    test("skipping dependency resolution runs no package manager", async () => {
        await withDir(async dir => {
            const pkg = write(dir.path, "package.json", {name: "a", version: "1.0.0"});
            write(dir.path, "package-lock.json", lock("a"));
            const bin = path.join(dir.path, "bin");
            const count = path.join(dir.path, "runs");
            fakeNpm(bin, "11.17.0", count);

            const [doc] = await withPath([bin],
                () => parse(new PackageJsonParser({relativeTo: dir.path, skipDependencyResolution: true}), pkg));

            const runtime = findNodeRuntime(doc)!;
            expect(runtime.packageManager).toBeUndefined();
            expect(runtime.packageManagerVersion).toBeUndefined();
            expect(runs(count)).toBe(0);
        }, {unsafeCleanup: true});
    });

    test("libc is told on Linux only", () => {
        expect(libc("darwin")).toBeUndefined();
        expect(libc("win32")).toBeUndefined();
        if (process.platform === "linux") {
            expect(["glibc", "musl", null]).toContain(libc("linux"));
        }
    });

    describe("RPC", () => {
        async function roundTrip(after: NodeRuntime, before?: NodeRuntime): Promise<NodeRuntime> {
            const batch = new RpcSendQueue(new ReferenceMap(), Json.Kind.Document, false).generate(after, before);
            return new RpcReceiveQueue(new Map(), Json.Kind.Document, [batch], undefined, false)
                .receive<NodeRuntime>(before);
        }

        test("every field survives the trip", async () => {
            const runtime: NodeRuntime = {
                ...createNodeRuntimeMarker(PackageManager.Pnpm, "10.18.3"),
                os: "linux", cpu: "x64", libc: "musl"
            };

            expect(await roundTrip(runtime)).toEqual(runtime);
        });

        test("absent fields stay absent, and a change is sent as one", async () => {
            const runtime = createNodeRuntimeMarker();
            const changed: NodeRuntime = {
                ...runtime, packageManager: PackageManager.Npm, packageManagerVersion: "11.17.0"
            };

            expect(await roundTrip(runtime)).toEqual(runtime);
            expect(await roundTrip(changed, runtime)).toEqual(changed);
        });
    });
});
