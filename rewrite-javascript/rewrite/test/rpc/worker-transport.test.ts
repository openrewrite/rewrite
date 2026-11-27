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
import * as fs from "fs";
import * as path from "path";
import {PassThrough} from "node:stream";
import {MessageChannel, Worker} from "node:worker_threads";

// A worker thread cannot load the TypeScript sources, so both sides come from the build
const transport = path.resolve(__dirname, "../../dist/rpc/worker-transport.js");

if (!fs.existsSync(transport)) {
    console.warn(`Skipping the worker transport tests: ${transport} has not been built.`);
}

interface Response {
    id: number;
    result?: unknown[];
    error?: { message: string };
}

// A page the worker pulls from the remote
interface Pull {
    id: string;
    method: "GetObject";
    params: { id: string };
}

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

describe.skipIf(!fs.existsSync(transport))("the worker transport", () => {
    let worker: Worker | undefined;
    let stdin: PassThrough | undefined;

    afterEach(async () => {
        stdin?.end();
        await worker?.terminate();
        stdin = worker = undefined;
    });

    /**
     * The returned functions play the remote. `serve` is the source of the function the worker
     * thread serves each GetObject with, and can pull the remote's pages through `channel`.
     * `pull` answers each page the worker pulls. `wake` wakes the worker's wait on one side of the
     * exchange without any change to it, as a notify of an earlier change can arrive late.
     */
    function start(serve: string, pull?: (params: { id: string }) => unknown[] | Promise<unknown[]>) {
        const {StdioRelay, newPageBuffers} = require(transport) as typeof import("../../src/rpc/worker-transport");
        const {port1, port2} = new MessageChannel();
        const buffers = newPageBuffers();
        worker = new Worker(`
            const {workerData} = require("node:worker_threads");
            const {WorkerPageChannel} = require(workerData.transport);
            const channel = new WorkerPageChannel(workerData.port, workerData.buffers);
            channel.onServe(${serve});
        `, {eval: true, workerData: {transport, port: port2, buffers}, transferList: [port2]});
        const input = stdin = new PassThrough();
        const stdout = new PassThrough();
        new StdioRelay(input, stdout, new PassThrough(), port1, buffers);

        const write = (message: object): void => {
            const body = Buffer.from(JSON.stringify(message));
            input.write(Buffer.concat([Buffer.from(`Content-Length: ${body.length}\r\n\r\n`), body]));
        };

        const answer = new Map<number, (response: Response) => void>();
        let received = Buffer.alloc(0);
        stdout.on("data", (chunk: Buffer) => {
            received = Buffer.concat([received, chunk]);
            for (; ;) {
                const end = received.indexOf("\r\n\r\n");
                if (end < 0) {
                    return;
                }
                const length = Number(/Content-Length: *(\d+)/.exec(received.toString("ascii", 0, end))![1]);
                if (received.length < end + 4 + length) {
                    return;
                }
                const message: Response | Pull = JSON.parse(received.toString("utf8", end + 4, end + 4 + length));
                received = received.subarray(end + 4 + length);
                if ("method" in message) {
                    const {id, params} = message;
                    Promise.resolve(pull!(params)).then(result => write({jsonrpc: "2.0", id, result}));
                } else {
                    answer.get(message.id)!(message);
                }
            }
        });

        let nextId = 0;
        const send = (method: string, params: object): void => write({jsonrpc: "2.0", id: nextId++, method, params});
        const control = new Int32Array(buffers.control);
        return {
            send,
            // the indices of the inbound and outbound states in the control array
            wake: (side: "inbound" | "outbound") => Atomics.notify(control, side === "inbound" ? 0 : 2),
            request(method: string, params: object): Promise<Response> {
                const id = nextId;
                // registered first, since the relay answers within the write when the answer is ready
                const response = new Promise<Response>((resolve, reject) => {
                    const unanswered = setTimeout(() => reject(new Error(`${method} ${id} was never answered`)), 5000);
                    answer.set(id, r => {
                        clearTimeout(unanswered);
                        resolve(r);
                    });
                });
                send(method, params);
                return response;
            }
        };
    }

    test("a GetObject of the object just served starts a new transfer before the serve is over", async () => {
        const remote = start(`async (params, sink) => {
            sink([{state: "END_OF_OBJECT"}]);
            await new Promise(resolve => setTimeout(resolve, 50));
        }`);

        await remote.request("GetObject", {id: "ctx"});

        expect(await remote.request("GetObject", {id: "ctx"})).toMatchObject({result: [{state: "END_OF_OBJECT"}]});
    });

    test("a serve that fails answers the page requested after the failure with the error", async () => {
        const remote = start(`async (params, sink) => {
            sink([{state: "ADD"}]);
            throw new Error("cannot send the rest");
        }`);

        await remote.request("GetObject", {id: "tree"});
        await sleep(50);

        expect(await remote.request("GetObject", {id: "tree"}))
            .toMatchObject({error: {message: "cannot send the rest"}});
    });

    test("an abort after the last page leaves the next transfer alone", async () => {
        const remote = start(`async (params, sink) => sink([{state: "END_OF_OBJECT"}])`);

        await remote.request("GetObject", {id: "tree"});
        // Blocks this thread, so the worker reports the serve over before the relay reads the abort
        Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 50);
        remote.send("AbortGetObject", {id: "tree"});

        expect(await remote.request("GetObject", {id: "other"})).toMatchObject({result: [{state: "END_OF_OBJECT"}]});
    });

    test("an abort while the worker writes a page ends the serve", async () => {
        const remote = start(`async (params, sink) => {
            if (params.id === "tree") {
                sink([{state: "ADD"}]);
                sink([{state: "ADD"}]);
                sink([{
                    toJSON() {
                        Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 100);
                        return {state: "ADD"};
                    }
                }]);
            }
            sink([{state: "END_OF_OBJECT"}]);
        }`);

        await remote.request("GetObject", {id: "tree"});
        await remote.request("GetObject", {id: "tree"});
        // while the worker writes the third page
        remote.send("AbortGetObject", {id: "tree"});

        expect(await remote.request("GetObject", {id: "other"})).toMatchObject({result: [{state: "END_OF_OBJECT"}]});
    });

    test("pages larger than the buffer they move through reach the remote whole", async () => {
        const remote = start(`async (params, sink) => {
            sink([{state: "ADD", value: "a".repeat(3 << 20)}]);
            sink([{state: "ADD", value: "b".repeat(9 << 20)}]);
            sink([{state: "END_OF_OBJECT"}]);
        }`);

        expect(await remote.request("GetObject", {id: "tree"})).toMatchObject({result: [{value: "a".repeat(3 << 20)}]});
        expect(await remote.request("GetObject", {id: "tree"})).toMatchObject({result: [{value: "b".repeat(9 << 20)}]});
        // read from the buffer the second page moved to
        expect(await remote.request("GetObject", {id: "tree"})).toMatchObject({result: [{state: "END_OF_OBJECT"}]});
    });

    test("remote pages larger than the buffer they move through reach the worker whole", async () => {
        const pages = [
            [{state: "ADD", value: "a".repeat(3 << 20)}],
            [{state: "ADD", value: "b".repeat(9 << 20)}],
            [{state: "END_OF_OBJECT", value: "c"}],
        ];
        const remote = start(`async (params, sink) => {
            const next = channel.pages("remote-tree");
            const values = [next(), next(), next()].map(page => page[0].value);
            sink([{state: "END_OF_OBJECT", value: values.map(value => value[0] + value.length)}]);
        }`, () => pages.shift()!);

        expect(await remote.request("GetObject", {id: "tree"}))
            .toMatchObject({result: [{value: [`a${3 << 20}`, `b${9 << 20}`, "c1"]}]});
    });

    test("a worker woken before its page is taken does not write over it", async () => {
        const remote = start(`async (params, sink) => {
            sink([{state: "ADD", value: "first"}]);
            sink([{state: "ADD", value: "second"}]);
            sink([{state: "END_OF_OBJECT", value: "third"}]);
        }`);

        await remote.request("GetObject", {id: "tree"});
        // the worker has handed over the second page and waits for it to be taken
        await sleep(50);
        remote.wake("outbound");
        await sleep(50);

        expect(await remote.request("GetObject", {id: "tree"})).toMatchObject({result: [{value: "second"}]});
        expect(await remote.request("GetObject", {id: "tree"})).toMatchObject({result: [{value: "third"}]});
    });

    test("a worker woken before the remote's page arrives keeps waiting for it", async () => {
        let release!: () => void;
        const released = new Promise<void>(resolve => release = resolve);
        const remote = start(`async (params, sink) => {
            sink([{state: "END_OF_OBJECT", value: channel.pages("remote-tree")()[0].value}]);
        }`, () => released.then(() => [{state: "END_OF_OBJECT", value: "whole"}]));

        const response = remote.request("GetObject", {id: "tree"});
        await sleep(50);
        remote.wake("inbound");
        await sleep(50);
        release();

        expect(await response).toMatchObject({result: [{value: "whole"}]});
    });

    test("a serve that fails after its last page lets the next transfer start", async () => {
        const remote = start(`async (params, sink) => {
            sink([{state: "END_OF_OBJECT"}]);
            throw new Error("after the last page");
        }`);

        await remote.request("GetObject", {id: "tree"});

        expect(await remote.request("GetObject", {id: "other"})).toMatchObject({result: [{state: "END_OF_OBJECT"}]});
    });
});
