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
import {PassThrough} from "node:stream";
import {MessageChannel, MessagePort} from "node:worker_threads";
import {newPageBuffers, PageBuffers, StdioRelay} from "../../src/rpc/worker-transport";

const OUT_STATE = 2;
const OUT_LEN = 3;
const FULL = 1;

/** Plays the worker's side of the page protocol by hand, so the test decides what order things arrive in. */
class Harness {
    readonly input = new PassThrough();
    readonly output = new PassThrough();
    readonly serves: { id: string }[] = [];
    readonly responses: { id: number, result?: unknown, error?: unknown }[] = [];
    private readonly control: Int32Array;
    private readonly workerPort: MessagePort;
    private readonly mainPort: MessagePort;
    private readonly buffers: PageBuffers = newPageBuffers();
    private received = "";

    constructor() {
        const channel = new MessageChannel();
        this.mainPort = channel.port1;
        this.workerPort = channel.port2;
        this.control = new Int32Array(this.buffers.control);
        new StdioRelay(this.input, this.output, new PassThrough(), this.mainPort, this.buffers);
        this.workerPort.on("message", (m: any) => {
            if (m.t === "serve") {
                this.serves.push(m.params);
            }
        });
        this.output.on("data", (chunk: Buffer) => {
            this.received += chunk.toString("utf8");
            for (; ;) {
                const header = /^Content-Length: (\d+)\r\n\r\n/.exec(this.received);
                if (!header || this.received.length < header[0].length + parseInt(header[1])) {
                    return;
                }
                const end = header[0].length + parseInt(header[1]);
                this.responses.push(JSON.parse(this.received.substring(header[0].length, end)));
                this.received = this.received.substring(end);
            }
        });
    }

    getObject(requestId: number, id: string): void {
        const body = JSON.stringify({jsonrpc: "2.0", id: requestId, method: "GetObject", params: {id}});
        this.input.write(`Content-Length: ${Buffer.byteLength(body)}\r\n\r\n${body}`);
    }

    sendPage(page: unknown[]): void {
        const text = JSON.stringify(page);
        Buffer.from(this.buffers.outbound, 0, text.length).write(text, 0, "utf8");
        this.control[OUT_LEN] = text.length;
        Atomics.store(this.control, OUT_STATE, FULL);
        this.workerPort.postMessage({t: "page"});
    }

    served(): void {
        this.workerPort.postMessage({t: "served"});
    }

    close(): void {
        this.mainPort.close();
        this.workerPort.close();
    }
}

async function until(condition: () => boolean, what: string): Promise<void> {
    const deadline = Date.now() + 2000;
    while (!condition()) {
        if (Date.now() > deadline) {
            throw new Error(`Timed out waiting for ${what}`);
        }
        await new Promise(resolve => setTimeout(resolve, 2));
    }
}

const endOfObject = [{state: "END_OF_OBJECT"}];

describe("StdioRelay", () => {
    let harness: Harness;

    beforeEach(() => {
        harness = new Harness();
    });

    afterEach(() => {
        harness.close();
    });

    // A peer holding several recipes asks for one execution context once per recipe, each as soon as the last answer
    // reached it, which can be before the worker's note that it finished serving reaches the relay.
    test("answers a request for the object it is just finishing serving", async () => {
        harness.getObject(1, "ctx");
        await until(() => harness.serves.length === 1, "the first serve");
        harness.sendPage(endOfObject);
        await until(() => harness.responses.length === 1, "the first response");

        harness.getObject(2, "ctx");
        await new Promise(resolve => setImmediate(resolve));
        harness.served();

        await until(() => harness.serves.length === 2, "the second serve");
        harness.sendPage(endOfObject);
        await until(() => harness.responses.length === 2, "the second response");
        expect(harness.responses[1].id).toBe(2);
    });

    test("serves what was queued behind that request afterward", async () => {
        harness.getObject(1, "ctx");
        await until(() => harness.serves.length === 1, "the first serve");
        harness.sendPage(endOfObject);
        await until(() => harness.responses.length === 1, "the first response");

        harness.getObject(2, "ctx");
        harness.getObject(3, "other");
        await new Promise(resolve => setImmediate(resolve));
        harness.served();

        await until(() => harness.serves.length === 2, "the second serve");
        expect(harness.serves[1].id).toBe("ctx");
        harness.sendPage(endOfObject);
        await until(() => harness.responses.length === 2, "the second response");
        harness.served();

        await until(() => harness.serves.length === 3, "the third serve");
        expect(harness.serves[2].id).toBe("other");
        harness.sendPage(endOfObject);
        await until(() => harness.responses.length === 3, "the third response");
        expect(harness.responses[2].id).toBe(3);
    });

    test("starts nothing more once the last request has been answered", async () => {
        harness.getObject(1, "ctx");
        await until(() => harness.serves.length === 1, "the first serve");
        harness.sendPage(endOfObject);
        await until(() => harness.responses.length === 1, "the first response");
        harness.served();
        await new Promise(resolve => setTimeout(resolve, 20));
        expect(harness.serves.length).toBe(1);
    });
});
