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
import {ChildProcessWithoutNullStreams, spawn} from "child_process";
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import * as rpc from "vscode-jsonrpc/node";
import {RewriteRpc, RpcObjectData} from "../../src/rpc";
import {DEFAULT_BATCH_SIZE} from "../../src/rpc/rewrite-rpc";
import {chunkedJsonDecoder} from "../../src/rpc/message-decoder";
import {chunkedJsonEncoder} from "../../src/rpc/message-encoder";
import {ParseProject, ParseProjectResponseItem} from "../../src/rpc/request/parse-project";
import {JavaScriptVisitor, JS} from "../../src/javascript";
import {DependencyWorkspace} from "../../src/javascript/dependency-workspace";
import {J} from "../../src/java";
import {SourceFile} from "../../src/tree";

const serverJs = path.resolve(__dirname, "../../dist/rpc/server.js");

if (!fs.existsSync(serverJs)) {
    console.warn(`Skipping the server process tests: ${serverJs} has not been built.`);
}

// The server only exists once built, which `npm test` and the Gradle build both do before testing.
describe.skipIf(!fs.existsSync(serverJs))("the server process", () => {
    let server: ChildProcessWithoutNullStreams;
    let exited: Promise<number | null>;

    function start(...args: string[]): void {
        startWith([], ...args);
    }

    function startWith(nodeArgs: string[], ...args: string[]): void {
        server = spawn(process.execPath, [...nodeArgs, serverJs, ...args], {stdio: ["pipe", "pipe", "pipe"]});
        exited = new Promise(resolve => server.on("exit", resolve));
    }

    function connect(): rpc.MessageConnection {
        return rpc.createMessageConnection(
            new rpc.StreamMessageReader(server.stdout, {contentTypeDecoder: chunkedJsonDecoder}),
            new rpc.StreamMessageWriter(server.stdin, {contentTypeEncoder: chunkedJsonEncoder})
        );
    }

    function beforeExit<T>(response: Promise<T>): Promise<T> {
        return Promise.race([response, exited.then(code => Promise.reject(new Error(`The server exited with ${code}`)))]);
    }

    afterEach(() => {
        server.kill("SIGKILL");
    });

    test("attributes types through a call chain deeper than a default stack allows", async () => {
        start();
        // each function's return type is inferred from the one declared after it
        let text = "";
        for (let i = 1000; i > 0; i--) {
            text += `function f${i}() { return f${i - 1}(); }\n`;
        }
        text += "function f0() { return 0; }\n";

        const client = new RewriteRpc(rpc.createMessageConnection(
            new rpc.StreamMessageReader(server.stdout, {contentTypeDecoder: chunkedJsonDecoder}),
            new rpc.StreamMessageWriter(server.stdin, {contentTypeEncoder: chunkedJsonEncoder})
        ), {batchSize: 1000});

        const parsed = (await client.parse([{text, sourcePath: "chain.js"}], JS.Kind.CompilationUnit))[0];
        expect(parsed.kind).toEqual(JS.Kind.CompilationUnit);

        server.stdin.end();
        expect(await exited).toEqual(0);
    }, 60000);

    test("pages at the default batch size when started without --batch-size", async () => {
        start();
        const connection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(server.stdout, {contentTypeDecoder: chunkedJsonDecoder}),
            new rpc.StreamMessageWriter(server.stdin, {contentTypeEncoder: chunkedJsonEncoder})
        );
        const pages: RpcObjectData[][] = [];
        const sendRequest = connection.sendRequest.bind(connection);
        connection.sendRequest = (async (type: any, ...args: any[]) => {
            const response = await sendRequest(type, ...args);
            if (type.method === "GetObject") {
                pages.push(response as RpcObjectData[]);
            }
            return response;
        }) as typeof connection.sendRequest;
        const client = new RewriteRpc(connection, {batchSize: 1000});

        let text = "";
        for (let i = 0; i < 30; i++) {
            text += `const v${i} = ${i};\n`;
        }
        await client.parse([{text, sourcePath: "many.js"}], JS.Kind.CompilationUnit);

        // Too many messages for a page of 1000 and few enough for one page of DEFAULT_BATCH_SIZE,
        // so a single page proves the default applied rather than that the tree was small.
        expect(pages.length).toEqual(1);
        expect(pages[0].length).toBeGreaterThan(1000);
        expect(pages[0].length).toBeLessThanOrEqual(DEFAULT_BATCH_SIZE);
    }, 60000);

    test("shuts down when it is asked to terminate", async () => {
        start();
        // the server has to be listening for the signal before it is sent
        const client = new RewriteRpc(rpc.createMessageConnection(
            new rpc.StreamMessageReader(server.stdout, {contentTypeDecoder: chunkedJsonDecoder}),
            new rpc.StreamMessageWriter(server.stdin, {contentTypeEncoder: chunkedJsonEncoder})
        ), {batchSize: 1000});
        await client.parse([{text: "1", sourcePath: "one.js"}], JS.Kind.CompilationUnit);

        server.kill("SIGTERM");
        expect(await exited).toEqual(0);
    }, 60000);

    test("records one GetObject metrics row per transfer, however many pages it takes", async () => {
        const dir = fs.mkdtempSync(path.join(os.tmpdir(), "rpc-metrics-"));
        onTestFinished(() => fs.rmSync(dir, {recursive: true, force: true}));
        const csv = path.join(dir, "metrics.csv");
        start(`--metrics-csv=${csv}`, "--batch-size=10");
        const connection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(server.stdout, {contentTypeDecoder: chunkedJsonDecoder}),
            new rpc.StreamMessageWriter(server.stdin, {contentTypeEncoder: chunkedJsonEncoder})
        );
        let pages = 0;
        const sendRequest = connection.sendRequest.bind(connection);
        connection.sendRequest = ((type: any, ...args: any[]) => {
            if (type.method === "GetObject") {
                pages++;
            }
            return sendRequest(type, ...args);
        }) as typeof connection.sendRequest;
        const client = new RewriteRpc(connection, {batchSize: 1000});
        await client.parse([{text: "const a = 1;\n", sourcePath: "one.js"}], JS.Kind.CompilationUnit);

        // the row is written once the transfer ends, which may be after the client has its last page
        server.stdin.end();
        expect(await exited).toEqual(0);
        const targets = fs.readFileSync(csv, "utf8").split("\n")
            .filter(row => row.startsWith("GetObject,"))
            .map(row => row.split(",")[1]);
        expect(pages).toBeGreaterThan(1);
        expect(targets).toEqual(["one.js"]);
    }, 60000);

    test("parses a project file by file, so its trees together can outgrow the heap", async () => {
        const dir = fs.mkdtempSync(path.join(os.tmpdir(), "rpc-project-"));
        onTestFinished(() => fs.rmSync(dir, {recursive: true, force: true}));
        fs.writeFileSync(path.join(dir, "package.json"), JSON.stringify({name: "calls", version: "1.0.0"}));
        let text = "const list = [];\n";
        for (let i = 0; i < 3000; i++) {
            text += `list.push(String(${i}).padStart(4, '0'));\n`;
        }
        for (let i = 0; i < 30; i++) {
            fs.writeFileSync(path.join(dir, `calls${i}.js`), text);
        }
        // Room for the program and the trees of a file or two, not for those of all thirty
        startWith(["--max-old-space-size=832"]);
        const connection = connect();
        const client = new RewriteRpc(connection, {batchSize: 1000});

        const items = await beforeExit(connection.sendRequest(
            new rpc.RequestType<ParseProject, ParseProjectResponseItem[], Error>("ParseProject"), new ParseProject(dir)));
        const kinds: string[] = [];
        for (const item of items) {
            kinds.push((await beforeExit(client.getObject<SourceFile>(item.id, item.sourceFileType))).kind);
            // As the Java client does with each tree it receives
            await connection.sendNotification(new rpc.NotificationType<{ id: string }>("Evict"), {id: item.id});
            client.localObjects.delete(item.id);
            client.remoteObjects.delete(item.id);
        }
        expect(kinds.filter(kind => kind === JS.Kind.CompilationUnit)).toHaveLength(30);

        server.stdin.end();
        expect(await exited).toEqual(0);
    }, 120000);

    test("attributes a call whose signature TypeScript cannot print in bounded memory", async () => {
        const workspace = await DependencyWorkspace.getOrCreateWorkspace({dependencies: {typebox: "1.4.6"}});
        startWith(["--max-old-space-size=512"]);
        const client = new RewriteRpc(connect(), {batchSize: 1000});

        // Printing the resolved signature of this Record call instantiates typebox's types until the heap runs out
        const text = "import {type TSchema, IsRecord, Record, RecordKey, RecordValue} from 'typebox';\n" +
            "export function probe<T extends TSchema>(type: T) {\n" +
            "    return IsRecord(type) ? Record(RecordKey(type), RecordValue(type)) : type;\n" +
            "}\n";
        const [parsed] = await beforeExit(client.parse([{text, sourcePath: "probe.ts"}], JS.Kind.CompilationUnit, workspace));

        const called: string[] = [];
        await new class extends JavaScriptVisitor<void> {
            protected override async visitMethodInvocation(method: J.MethodInvocation, p: void): Promise<J | undefined> {
                if (method.methodType) {
                    called.push(method.methodType.name);
                }
                return super.visitMethodInvocation(method, p);
            }
        }().visit(parsed, undefined);
        expect(called).toContain("Record");

        server.stdin.end();
        expect(await exited).toEqual(0);
    }, 120000);
});
