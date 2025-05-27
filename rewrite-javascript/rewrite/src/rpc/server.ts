// #!/usr/bin/env node
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
import * as rpc from "vscode-jsonrpc/node";
import {RewriteRpc} from "./rewrite-rpc";
import * as fs from "fs";
import {WriteStream} from "fs";

// Include all languages you want this server to support.
import "../text";
import "../json";
import "../java";
import "../javascript";

const log: WriteStream = fs.createWriteStream(`${process.cwd()}/rpc.js.log`, {flags: 'w'});
log.write(`[js-rewrite-rpc] starting\n\n`);

const logger: rpc.Logger = {
    error: (msg: string) => log.write(`[Error] ${msg}\n`),
    warn: (msg: string) => log.write(`[Warn] ${msg}\n`),
    info: (msg: string) => log.write(`[Info] ${msg}\n`),
    log: (msg: string) => log.write(`[Log] ${msg}\n`)
};

// Create the connection with the custom logger
const connection = rpc.createMessageConnection(
    new rpc.StreamMessageReader(process.stdin),
    new rpc.StreamMessageWriter(process.stdout),
    logger
);

connection.trace(rpc.Trace.Verbose, logger).catch(err => {
    // Handle any unexpected errors during trace configuration
    log.write(`Failed to set trace: ${err}`);
});

connection.onError(err => {
    log.write(`[js-rewrite-rpc] error: ${err}\n\n`);
});

connection.onClose(() => {
    log.write(`[js-rewrite-rpc] connection closed\n\n`);
})

connection.onDispose(() => {
    log.write(`[js-rewrite-rpc] connection disposed\n\n`);
});

new RewriteRpc(connection, {traceGetObjectInput: log, traceGetObjectOutput: true});
