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
import * as net from 'net';
import * as rpc from "vscode-jsonrpc/node";
import {RewriteRpc} from "./rewrite-rpc";
import * as fs from "fs";
import {WriteStream} from "fs";
import {Command} from 'commander';

// Include all languages you want this server to support.
import "../text";
import "../json";
import "../java";
import "../javascript";

interface ProgramOptions {
    port?: number;
    logFile?: string;
    verbose?: boolean;
    batchSize?: number;
    traceGetObjectOutput?: boolean;
    traceGetObjectInput?: boolean;
}

const program = new Command();
program
    .option('--port <number>', 'port number')
    .option('--log-file <path>', 'log file path')
    .option('-v, --verbose', 'enable verbose output')
    .option('--batch-size [size]', 'sets the batch size (default is 100)', s => parseInt(s, 10), 100)
    .option('--trace-get-object-output', 'enable `GetObject` output tracing')
    .option('--trace-get-object-input', 'enable `GetObject` input tracing')
    .parse();

const options = program.opts() as ProgramOptions;

const log: WriteStream = fs.createWriteStream(options.logFile ?? `${process.cwd()}/rpc.js.log`, {flags: 'w'});
log.write(`[js-rewrite-rpc] starting\n\n`);

const logger: rpc.Logger = {
    error: (msg: string) => log.write(`[Error] ${msg}\n`),
    warn: (msg: string) => log.write(`[Warn] ${msg}\n`),
    info: (msg: string) => options.verbose && log.write(`[Info] ${msg}\n`),
    log: (msg: string) => options.verbose && log.write(`[Log] ${msg}\n`)
};

if (!options.port) {
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

    new RewriteRpc(connection, {
        batchSize: options.batchSize,
        traceGetObjectInput: options.traceGetObjectInput ? log : undefined,
        traceGetObjectOutput: options.traceGetObjectOutput,
    });
} else {
// Create a TCP server
    const server: net.Server = net.createServer((socket: net.Socket) => {
        log.write(`[js-rewrite-rpc] new client connected: ${socket.remoteAddress}:${socket.remotePort}\n`);

        // Create the connection with the custom logger using the socket streams
        const connection: rpc.MessageConnection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(socket),
            new rpc.StreamMessageWriter(socket),
            logger
        );

        connection.trace(rpc.Trace.Verbose, logger).catch((err: Error) => {
            // Handle any unexpected errors during trace configuration
            log.write(`Failed to set trace: ${err}\n`);
        });

        connection.onError((err: [Error, rpc.Message | undefined, number | undefined]) => {
            log.write(`[js-rewrite-rpc] error: ${err[0]}\n\n`);
        });

        connection.onClose(() => {
            log.write(`[js-rewrite-rpc] connection closed\n\n`);
        });

        connection.onDispose(() => {
            log.write(`[js-rewrite-rpc] connection disposed\n\n`);
        });

        socket.on('close', () => {
            log.write(`[js-rewrite-rpc] socket closed: ${socket.remoteAddress}:${socket.remotePort}\n`);
        });

        socket.on('error', (err: Error) => {
            log.write(`[js-rewrite-rpc] socket error: ${err.message}\n`);
        });

        // Initialize the RPC mechanism
        new RewriteRpc(connection, {
            batchSize: options.batchSize,
            traceGetObjectInput: options.traceGetObjectInput ? log : undefined,
            traceGetObjectOutput: options.traceGetObjectOutput,
        });
    });

// Handle server errors
    server.on('error', (err: Error) => {
        log.write(`[js-rewrite-rpc] server error: ${err.message}\n`);
        process.exit(1);
    });

// Start the server
    server.listen(options.port, '127.0.0.1', () => {
        log.write(`[js-rewrite-rpc] server listening on 127.0.0.1:${options.port}\n`);
    });

// Handle process termination
    process.on('SIGINT', () => {
        log.write(`[js-rewrite-rpc] received SIGINT, shutting down\n`);
        server.close(() => {
            log.write(`[js-rewrite-rpc] server closed\n`);
            process.exit(0);
        });
    });
}
