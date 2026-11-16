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
    createNodeResolutionResultMarker,
    findNodeResolutionResult,
    NodeResolutionResult,
    PackageJsonParser,
    RepositoryKind
} from "../../src/javascript";
import {Json} from "../../src/json";
import {ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";
import {withDir} from "tmp-promise";
import * as fs from "fs";
import * as path from "path";

describe("NodeResolutionResult repository", () => {

    const expressRepository = {type: "git", url: "git+https://github.com/expressjs/express.git"};

    async function parse(root: string, files: Record<string, object | string>, manifest = "package.json"): Promise<NodeResolutionResult> {
        for (const [rel, content] of Object.entries(files)) {
            fs.mkdirSync(path.join(root, path.dirname(rel)), {recursive: true});
            fs.writeFileSync(path.join(root, rel), typeof content === "string" ? content : JSON.stringify(content));
        }
        const parser = new PackageJsonParser({relativeTo: root});
        const results: Json.Document[] = [];
        for await (const result of parser.parse(path.join(root, manifest))) {
            results.push(result as Json.Document);
        }
        return findNodeResolutionResult(results[0])!;
    }

    function repositoryOf(marker: NodeResolutionResult, name: string) {
        return marker.resolvedDependencies.find(r => r.name === name)!.repository;
    }

    test("the project's repository object keeps its type, url and directory", () => {
        const marker = createNodeResolutionResultMarker("package.json", {
            name: "@babel/core",
            repository: {type: "git", url: "https://github.com/babel/babel.git", directory: "packages/babel-core"}
        });

        expect(marker.repository).toEqual({
            kind: RepositoryKind,
            type: "git",
            url: "https://github.com/babel/babel.git",
            directory: "packages/babel-core"
        });
    });

    test.each([
        "github:user/repo",
        "user/repo",
        "https://github.com/user/repo.git"
    ])("a repository shorthand %s is kept as the url exactly as written", shorthand => {
        const marker = createNodeResolutionResultMarker("package.json", {name: "app", repository: shorthand});

        expect(marker.repository).toEqual({kind: RepositoryKind, url: shorthand});
    });

    test.each([
        [undefined],
        [""],
        [{type: "git"}],
        [{type: "git", url: ""}],
        [{url: 42}],
        [["https://github.com/user/repo.git"]],
        [42]
    ])("a repository without a url %j is none", repository => {
        const marker = createNodeResolutionResultMarker("package.json", {name: "app", repository});

        expect(marker.repository).toBeUndefined();
    });

    test("a type or directory that is not a string is dropped", () => {
        const marker = createNodeResolutionResultMarker("package.json", {
            name: "app",
            repository: {type: 1, url: "https://github.com/user/repo.git", directory: {}}
        });

        expect(marker.repository).toEqual({kind: RepositoryKind, url: "https://github.com/user/repo.git"});
    });

    test("a resolved dependency takes the repository of its lock entry", () => {
        const marker = createNodeResolutionResultMarker(
            "package.json",
            {name: "app", dependencies: {express: "^5.0.0", ms: "^2.0.0"}},
            {
                packages: {
                    "": {},
                    "node_modules/express": {version: "5.1.0", repository: expressRepository},
                    "node_modules/ms": {version: "2.1.3"}
                }
            });

        expect(marker.dependencies[0].resolved!.repository).toEqual({kind: RepositoryKind, ...expressRepository});
        expect(marker.dependencies[1].resolved!.repository).toBeUndefined();
    });

    test("npm: read from the installed package.json, which package-lock.json does not carry", () =>
        withDir(async dir => {
            const marker = await parse(dir.path, {
                "package.json": {
                    name: "app", version: "1.0.0", repository: "github:acme/app",
                    dependencies: {express: "^5.0.0", debug: "^4.0.0", "not-installed": "^1.0.0", "@scope/pkg": "^1.0.0"}
                },
                "package-lock.json": {
                    name: "app", version: "1.0.0", lockfileVersion: 3,
                    packages: {
                        "": {name: "app", version: "1.0.0"},
                        "node_modules/express": {version: "5.1.0", dependencies: {debug: "^3.0.0"}},
                        "node_modules/express/node_modules/debug": {version: "3.2.7"},
                        "node_modules/debug": {version: "4.4.3"},
                        "node_modules/not-installed": {version: "1.0.0"},
                        "node_modules/@scope/pkg": {version: "1.0.0"}
                    }
                },
                "node_modules/express/package.json": {name: "express", version: "5.1.0", repository: expressRepository},
                "node_modules/express/node_modules/debug/package.json": {
                    name: "debug", version: "3.2.7", repository: "debug-js/debug"
                },
                "node_modules/debug/package.json": {name: "debug", version: "4.4.3"},
                "node_modules/@scope/pkg/package.json": {
                    name: "@scope/pkg", version: "1.0.0",
                    repository: {type: "git", url: "https://github.com/scope/mono.git", directory: "packages/pkg"}
                }
            });

            expect(marker.repository).toEqual({kind: RepositoryKind, url: "github:acme/app"});
            expect(repositoryOf(marker, "express")).toEqual({kind: RepositoryKind, ...expressRepository});
            expect(marker.resolvedDependencies.find(r => r.name === "debug" && r.version === "3.2.7")!.repository)
                .toEqual({kind: RepositoryKind, url: "debug-js/debug"});
            // installed, and declares none
            expect(marker.resolvedDependencies.find(r => r.name === "debug" && r.version === "4.4.3")!.repository)
                .toBeUndefined();
            expect(repositoryOf(marker, "not-installed")).toBeUndefined();
            expect(repositoryOf(marker, "@scope/pkg")).toEqual({
                kind: RepositoryKind, type: "git", url: "https://github.com/scope/mono.git", directory: "packages/pkg"
            });
        }, {unsafeCleanup: true}));

    test("npm workspace: a member reads the installed packages of the lock root", () =>
        withDir(async dir => {
            const marker = await parse(dir.path, {
                "package.json": {name: "root", version: "1.0.0", workspaces: ["packages/*"]},
                "packages/a/package.json": {name: "a", version: "1.0.0", dependencies: {express: "^5.0.0"}},
                "package-lock.json": {
                    name: "root", version: "1.0.0", lockfileVersion: 3,
                    packages: {
                        "": {name: "root", version: "1.0.0", workspaces: ["packages/*"]},
                        "node_modules/a": {resolved: "packages/a", link: true},
                        "packages/a": {version: "1.0.0", dependencies: {express: "^5.0.0"}},
                        "node_modules/express": {version: "5.1.0"}
                    }
                },
                "node_modules/express/package.json": {name: "express", version: "5.1.0", repository: expressRepository}
            }, "packages/a/package.json");

            expect(marker.dependencies[0].resolved!.repository).toEqual({kind: RepositoryKind, ...expressRepository});
        }, {unsafeCleanup: true}));

    test("bun: read from the installed package.json, which bun.lock does not carry", () =>
        withDir(async dir => {
            const marker = await parse(dir.path, {
                "package.json": {name: "app", version: "1.0.0", dependencies: {express: "^5.0.0", ms: "^2.0.0"}},
                "bun.lock": {
                    lockfileVersion: 1,
                    workspaces: {"": {name: "app", dependencies: {express: "^5.0.0", ms: "^2.0.0"}}},
                    packages: {
                        express: ["express@5.1.0", "", {}, "sha512-a"],
                        ms: ["ms@2.1.3", "", {}, "sha512-b"]
                    }
                },
                "node_modules/express/package.json": {name: "express", version: "5.1.0", repository: expressRepository}
            });

            expect(repositoryOf(marker, "express")).toEqual({kind: RepositoryKind, ...expressRepository});
            expect(repositoryOf(marker, "ms")).toBeUndefined();
        }, {unsafeCleanup: true}));

    test("pnpm: read from the .pnpm layout in the walk that reads its versions", () =>
        withDir(async dir => {
            const marker = await parse(dir.path, {
                "package.json": {name: "app", version: "1.0.0", dependencies: {express: "^5.0.0", "@scope/pkg": "^1.0.0"}},
                "pnpm-lock.yaml": "lockfileVersion: '9.0'\n",
                "node_modules/.pnpm/express@5.1.0/node_modules/express/package.json": {
                    name: "express", version: "5.1.0", repository: expressRepository
                },
                "node_modules/.pnpm/@scope+pkg@1.0.0_express@5.1.0/node_modules/@scope/pkg/package.json": {
                    name: "@scope/pkg", version: "1.0.0", repository: "github:scope/pkg"
                }
            });

            expect(repositoryOf(marker, "express")).toEqual({kind: RepositoryKind, ...expressRepository});
            expect(repositoryOf(marker, "@scope/pkg")).toEqual({kind: RepositoryKind, url: "github:scope/pkg"});
        }, {unsafeCleanup: true}));

    test.each([
        ["yarn classic", "# yarn lockfile v1\n\n\nexpress@^5.0.0:\n  version \"5.1.0\"\n"],
        ["yarn berry", "__metadata:\n  version: 8\n\n\"express@npm:^5.0.0\":\n  version: 5.1.0\n  resolution: \"express@npm:5.1.0\"\n"]
    ])("%s: read from node_modules in the walk that reads its versions", (_, lock) =>
        withDir(async dir => {
            const marker = await parse(dir.path, {
                "package.json": {name: "app", version: "1.0.0", dependencies: {express: "^5.0.0"}},
                "yarn.lock": lock,
                "node_modules/express/package.json": {name: "express", version: "5.1.0", repository: expressRepository}
            });

            expect(repositoryOf(marker, "express")).toEqual({kind: RepositoryKind, ...expressRepository});
        }, {unsafeCleanup: true}));

    test("yarn without node_modules: the lock file alone gives no repository", () =>
        withDir(async dir => {
            const marker = await parse(dir.path, {
                "package.json": {name: "app", version: "1.0.0", dependencies: {express: "^5.0.0"}},
                "yarn.lock": "# yarn lockfile v1\n\n\nexpress@^5.0.0:\n  version \"5.1.0\"\n"
            });

            expect(marker.resolvedDependencies).toHaveLength(1);
            expect(repositoryOf(marker, "express")).toBeUndefined();
        }, {unsafeCleanup: true}));

    test("the repositories survive an RPC round trip", async () => {
        const marker = createNodeResolutionResultMarker(
            "package.json",
            {
                name: "app",
                repository: {type: "git", url: "https://github.com/acme/mono.git", directory: "packages/app"},
                dependencies: {express: "^5.0.0", ms: "^2.0.0", debug: "^4.0.0"}
            },
            {
                packages: {
                    "": {},
                    "node_modules/express": {version: "5.1.0", repository: expressRepository},
                    "node_modules/ms": {version: "2.1.3"},
                    "node_modules/debug": {version: "4.4.3", repository: "debug-js/debug"}
                }
            });

        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        sq.send(marker, undefined);
        const rq = new RpcReceiveQueue(new Map(), undefined, [sq.finish()], undefined, false);
        const received = rq.receive<NodeResolutionResult>(undefined);

        expect(received.repository).toEqual({
            kind: RepositoryKind, type: "git", url: "https://github.com/acme/mono.git", directory: "packages/app"
        });
        expect(repositoryOf(received, "express")).toEqual({kind: RepositoryKind, ...expressRepository});
        expect(repositoryOf(received, "ms")).toBeUndefined();
        expect(repositoryOf(received, "debug")).toEqual({kind: RepositoryKind, url: "debug-js/debug"});
        // the field is last, so what follows the resolved dependencies is still read in step
        expect(received.name).toBe("app");
    });
});
