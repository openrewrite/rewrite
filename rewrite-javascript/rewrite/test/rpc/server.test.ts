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
import * as path from "path";
import * as rpc from "vscode-jsonrpc/node";
import {RewriteRpc} from "../../src/rpc";
import {chunkedJsonDecoder} from "../../src/rpc/message-decoder";
import {chunkedJsonEncoder} from "../../src/rpc/message-encoder";
import {JS} from "../../src/javascript";

const serverJs = path.resolve(__dirname, "../../dist/rpc/server.js");

if (!fs.existsSync(serverJs)) {
    console.warn(`Skipping the server process tests: ${serverJs} has not been built.`);
}

// The server only exists once built, which `npm test` and the Gradle build both do before testing.
describe.skipIf(!fs.existsSync(serverJs))("the server process", () => {
    let server: ChildProcessWithoutNullStreams;
    let exited: Promise<number | null>;

    beforeEach(() => {
        server = spawn(process.execPath, [serverJs], {stdio: ["pipe", "pipe", "pipe"]});
        exited = new Promise(resolve => server.on("exit", resolve));
    });

    afterEach(() => {
        server.kill("SIGKILL");
    });

    test("attributes types through a call chain deeper than a default stack allows", async () => {
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

    test("shuts down when it is asked to terminate", async () => {
        // the server has to be listening for the signal before it is sent
        const client = new RewriteRpc(rpc.createMessageConnection(
            new rpc.StreamMessageReader(server.stdout, {contentTypeDecoder: chunkedJsonDecoder}),
            new rpc.StreamMessageWriter(server.stdin, {contentTypeEncoder: chunkedJsonEncoder})
        ), {batchSize: 1000});
        await client.parse([{text: "1", sourcePath: "one.js"}], JS.Kind.CompilationUnit);

        server.kill("SIGTERM");
        expect(await exited).toEqual(0);
    }, 60000);
});
