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
import * as rpc from "vscode-jsonrpc/node";
import {MessagePort} from "node:worker_threads";
import {Readable, Writable} from "node:stream";
import {encodeFragmented} from "./message-encoder";
import {RpcObjectData, RpcObjectState} from "./queue";

// The RPC server runs on a worker thread whose codecs read and write object pages synchronously.
// The main thread owns stdio. It relays ordinary JSON-RPC messages and moves object pages through
// shared memory, one at a time, and the worker blocks until the page it needs has moved.

const IN_STATE = 0;
const IN_LEN = 1;
const OUT_STATE = 2;
const OUT_LEN = 3;

const EMPTY = 0;
const FULL = 1;
const ABORTED = 2;

const MAX_PAGE_BYTES = 1 << 30;

// Growable shared buffers are ES2024, newer than the lib this project compiles against
type GrowableSharedArrayBuffer = SharedArrayBuffer & { grow(length: number): void };
const GrowableSharedArrayBuffer = SharedArrayBuffer as unknown as
    new (length: number, options: { maxByteLength: number }) => GrowableSharedArrayBuffer;

export interface PageBuffers {
    control: SharedArrayBuffer;
    inbound: GrowableSharedArrayBuffer;
    outbound: GrowableSharedArrayBuffer;
}

export function newPageBuffers(): PageBuffers {
    return {
        control: new SharedArrayBuffer(4 * Int32Array.BYTES_PER_ELEMENT),
        inbound: new GrowableSharedArrayBuffer(1 << 20, {maxByteLength: MAX_PAGE_BYTES}),
        outbound: new GrowableSharedArrayBuffer(1 << 20, {maxByteLength: MAX_PAGE_BYTES}),
    };
}

function ensureCapacity(buffer: GrowableSharedArrayBuffer, length: number): void {
    if (buffer.byteLength < length) {
        buffer.grow(Math.min(MAX_PAGE_BYTES, Math.max(length, buffer.byteLength * 2)));
    }
}

export class PageAbortedError extends Error {
}

type ToMain =
    { t: "frame", bytes: Uint8Array } |
    { t: "pull", id: string, sourceFileType?: string } |
    { t: "page" } |
    { t: "serveError", message: string } |
    { t: "served" };

type ToWorker = { t: "serve", params: { id: string, sourceFileType?: string } };

function frame(body: Uint8Array): Buffer {
    const header = Buffer.from(`Content-Length: ${body.byteLength}\r\n\r\n`, "ascii");
    return Buffer.concat([header, body]);
}

/**
 * Hands each JSON-RPC message from the worker to the main thread as one whole frame, so a frame the
 * main thread writes for a page never lands between another frame's header and body.
 */
export class PortMessageWriter extends rpc.AbstractMessageWriter implements rpc.MessageWriter {
    constructor(private readonly port: MessagePort) {
        super();
    }

    write(msg: rpc.Message): Promise<void> {
        // A message longer than V8's ~512 MB string limit (a deep PrepareRecipe tree) is encoded in fragments
        let body: Buffer;
        try {
            body = Buffer.from(JSON.stringify(msg), "utf8");
        } catch (err) {
            if (!(err instanceof RangeError)) {
                return Promise.reject(err);
            }
            body = encodeFragmented(msg, "utf8");
        }
        const bytes = new Uint8Array(frame(body));
        this.port.postMessage({t: "frame", bytes} satisfies ToMain, [bytes.buffer]);
        return Promise.resolve();
    }

    end(): void {
    }
}

/**
 * The worker's side of the page exchange. It pulls the remote's pages and hands over its own,
 * blocking while the main thread moves them.
 */
export class WorkerPageChannel {
    private readonly control: Int32Array;
    private pullInFlight = false;

    constructor(private readonly port: MessagePort, private readonly buffers: PageBuffers) {
        this.control = new Int32Array(buffers.control);
    }

    /**
     * Pages of one transfer of `id`, each requested while the previous one is decoded.
     */
    pages(id: string, sourceFileType?: string): () => readonly RpcObjectData[] {
        let started = false;
        return () => {
            if (!started) {
                started = true;
                this.requestPage(id, sourceFileType);
            }
            const page = this.awaitPage();
            if (page.length > 0 && page[page.length - 1].state !== RpcObjectState.END_OF_OBJECT) {
                this.requestPage(id, sourceFileType);
            }
            return page;
        };
    }

    /**
     * Waits out a page requested ahead of a transfer that failed, so it cannot be taken for the
     * first page of the next one.
     */
    settle(): void {
        if (this.pullInFlight) {
            try {
                this.awaitPage();
            } catch {
                // the transfer already failed
            }
        }
    }

    private requestPage(id: string, sourceFileType?: string): void {
        this.pullInFlight = true;
        this.port.postMessage({t: "pull", id, sourceFileType} satisfies ToMain);
    }

    private awaitPage(): RpcObjectData[] {
        Atomics.wait(this.control, IN_STATE, EMPTY);
        const text = Buffer.from(this.buffers.inbound, 0, this.control[IN_LEN]).toString("utf8");
        Atomics.store(this.control, IN_STATE, EMPTY);
        this.pullInFlight = false;
        const response = JSON.parse(text);
        if (response.error) {
            throw new Error(response.error.message);
        }
        return response.result;
    }

    /**
     * Runs `serve` for each object the remote asks for. Its sink blocks until the main thread has
     * taken the previous page, and throws {@link PageAbortedError} once the remote aborts.
     */
    onServe(serve: (params: { id: string, sourceFileType?: string },
                    sink: (page: RpcObjectData[]) => void) => Promise<void>): void {
        this.port.on("message", (m: ToWorker) => {
            if (m.t !== "serve") {
                return;
            }
            serve(m.params, page => this.sendPage(page))
                .catch(e => {
                    if (!(e instanceof PageAbortedError)) {
                        this.port.postMessage({t: "serveError", message: String(e?.message ?? e)} satisfies ToMain);
                    }
                })
                .finally(() => {
                    try {
                        this.awaitTaken();
                    } catch {
                        // aborted after its last page
                    }
                    this.port.postMessage({t: "served"} satisfies ToMain);
                });
        });
    }

    private sendPage(page: RpcObjectData[]): void {
        this.awaitTaken();
        let text: string | Buffer;
        try {
            text = JSON.stringify(page);
        } catch (err) {
            if (!(err instanceof RangeError)) {
                throw err;
            }
            text = encodeFragmented(page, "utf8");
        }
        const length = typeof text === "string" ? Buffer.byteLength(text, "utf8") : text.byteLength;
        ensureCapacity(this.buffers.outbound, length);
        const view = Buffer.from(this.buffers.outbound, 0, length);
        if (typeof text === "string") {
            view.write(text, 0, "utf8");
        } else {
            text.copy(view);
        }
        this.control[OUT_LEN] = length;
        Atomics.store(this.control, OUT_STATE, FULL);
        this.port.postMessage({t: "page"} satisfies ToMain);
    }

    private awaitTaken(): void {
        Atomics.wait(this.control, OUT_STATE, FULL);
        if (Atomics.load(this.control, OUT_STATE) === ABORTED) {
            Atomics.store(this.control, OUT_STATE, EMPTY);
            throw new PageAbortedError("The remote aborted the transfer");
        }
    }
}

const HEADER_END = Buffer.from("\r\n\r\n", "ascii");
// How a response to one of the main thread's own page requests starts when the peer writes its id first
const PAGE_RESPONSE_PREFIX = Buffer.from(`{"jsonrpc":"2.0","id":"p`, "ascii");
// Requests and small responses are parsed whole to route them. Above this only the prefix is read.
const SMALL_MESSAGE = 4096;

interface Serving {
    objectId: string;
    // JSON of the ids of GetObject requests not yet answered
    waiting: string[];
    pageReady: boolean;
}

/**
 * The main thread's side. It frames stdin, forwards ordinary messages to the worker, answers the
 * remote's GetObject requests with the worker's pages and delivers the remote's pages to it.
 */
export class StdioRelay {
    private readonly control: Int32Array;
    private readonly pulls = new Set<string>();
    private nextPull = 0;
    private serving?: Serving;
    private readonly queuedServes: { id: string, params: { id: string, sourceFileType?: string } }[] = [];

    private pending: Buffer[] = [];
    private pendingLength = 0;
    private bodyLength = -1;

    constructor(input: Readable,
                private readonly output: Writable,
                private readonly worker: Writable,
                private readonly port: MessagePort,
                private readonly buffers: PageBuffers) {
        this.control = new Int32Array(buffers.control);
        input.on("data", (chunk: Buffer) => this.onData(chunk));
        input.on("end", () => worker.end());
        port.on("message", (m: ToMain) => this.onWorkerMessage(m));
    }

    private onData(chunk: Buffer): void {
        this.pending.push(chunk);
        this.pendingLength += chunk.byteLength;
        for (; ;) {
            if (this.bodyLength < 0) {
                const buffered = this.takeBuffered();
                const end = buffered.indexOf(HEADER_END);
                if (end < 0) {
                    this.keep(buffered);
                    return;
                }
                const header = buffered.toString("ascii", 0, end);
                const match = /Content-Length: *(\d+)/i.exec(header);
                if (!match) {
                    throw new Error(`No Content-Length in header: ${header}`);
                }
                this.bodyLength = parseInt(match[1]);
                this.keep(buffered.subarray(end + HEADER_END.byteLength));
            }
            if (this.pendingLength < this.bodyLength) {
                return;
            }
            const buffered = this.takeBuffered();
            const body = buffered.subarray(0, this.bodyLength);
            this.keep(buffered.subarray(this.bodyLength));
            this.bodyLength = -1;
            this.onMessage(body);
        }
    }

    private takeBuffered(): Buffer {
        return this.pending.length === 1 ? this.pending[0] : Buffer.concat(this.pending, this.pendingLength);
    }

    private keep(rest: Buffer): void {
        this.pending = rest.byteLength > 0 ? [rest] : [];
        this.pendingLength = rest.byteLength;
    }

    private onMessage(body: Buffer): void {
        if (body.byteLength <= SMALL_MESSAGE) {
            const message = JSON.parse(body.toString("utf8"));
            if (message.method === "GetObject") {
                this.onGetObject(JSON.stringify(message.id), message.params);
                return;
            }
            if (message.method === "AbortGetObject" && this.serving?.objectId === message.params?.id) {
                // the worker's traversal stops at its next page, then the abort reaches its handler
                Atomics.store(this.control, OUT_STATE, ABORTED);
                Atomics.notify(this.control, OUT_STATE);
            } else if (message.method === undefined && typeof message.id === "string" && this.pulls.delete(message.id)) {
                this.deliverPage(body);
                return;
            }
        } else if (this.pulls.size > 0) {
            const id = this.pageResponseId(body);
            if (id !== undefined && this.pulls.delete(id)) {
                this.deliverPage(body);
                return;
            }
        }
        this.worker.write(frame(body));
    }

    private pageResponseId(body: Buffer): string | undefined {
        if (body.compare(PAGE_RESPONSE_PREFIX, 0, PAGE_RESPONSE_PREFIX.byteLength, 0, PAGE_RESPONSE_PREFIX.byteLength) === 0) {
            const end = body.indexOf(0x22, PAGE_RESPONSE_PREFIX.byteLength);
            return "p" + body.toString("ascii", PAGE_RESPONSE_PREFIX.byteLength, end);
        }
        // A peer that writes the id after the result is read the slow way
        const id = JSON.parse(body.toString("utf8")).id;
        return typeof id === "string" ? id : undefined;
    }

    private deliverPage(body: Buffer): void {
        if (Atomics.load(this.control, IN_STATE) !== EMPTY) {
            throw new Error("A page arrived before the worker took the previous one");
        }
        ensureCapacity(this.buffers.inbound, body.byteLength);
        body.copy(Buffer.from(this.buffers.inbound, 0, body.byteLength));
        this.control[IN_LEN] = body.byteLength;
        Atomics.store(this.control, IN_STATE, FULL);
        Atomics.notify(this.control, IN_STATE);
    }

    private onGetObject(id: string, params: { id: string, sourceFileType?: string }): void {
        if (this.serving?.objectId === params.id) {
            this.serving.waiting.push(id);
            this.respondWithPage();
        } else if (this.serving) {
            this.queuedServes.push({id, params});
        } else {
            this.startServe(id, params);
        }
    }

    private startServe(id: string, params: { id: string, sourceFileType?: string }): void {
        this.serving = {objectId: params.id, waiting: [id], pageReady: false};
        this.port.postMessage({t: "serve", params} satisfies ToWorker);
    }

    private respondWithPage(): void {
        const serving = this.serving!;
        if (!serving.pageReady || serving.waiting.length === 0) {
            return;
        }
        const prefix = Buffer.from(`{"jsonrpc":"2.0","id":${serving.waiting.shift()},"result":`, "utf8");
        const page = Buffer.from(this.buffers.outbound, 0, this.control[OUT_LEN]);
        // copied out before the worker may overwrite the buffer with its next page
        const body = Buffer.concat([prefix, page, Buffer.from("}")]);
        serving.pageReady = false;
        Atomics.store(this.control, OUT_STATE, EMPTY);
        Atomics.notify(this.control, OUT_STATE);
        this.output.write(frame(body));
    }

    private onWorkerMessage(m: ToMain): void {
        switch (m.t) {
            case "frame":
                this.output.write(m.bytes);
                break;
            case "pull": {
                const id = `p${this.nextPull++}`;
                this.pulls.add(id);
                this.output.write(frame(Buffer.from(JSON.stringify({
                    jsonrpc: "2.0",
                    id,
                    method: "GetObject",
                    params: {id: m.id, sourceFileType: m.sourceFileType}
                }), "utf8")));
                break;
            }
            case "page":
                this.serving!.pageReady = true;
                this.respondWithPage();
                break;
            case "serveError": {
                const id = this.serving!.waiting.shift();
                if (id !== undefined) {
                    this.output.write(frame(Buffer.from(
                        `{"jsonrpc":"2.0","id":${id},"error":${JSON.stringify({code: -32603, message: m.message})}}`, "utf8")));
                }
                break;
            }
            case "served": {
                this.serving = undefined;
                const next = this.queuedServes.shift();
                if (next) {
                    this.startServe(next.id, next.params);
                }
                break;
            }
        }
    }
}
