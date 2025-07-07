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
import {Trace, Tracer} from "vscode-jsonrpc/node";
import {RewriteRpc} from "./rewrite-rpc";
import * as fs from "fs";
import {Command} from 'commander';
import {dir} from 'tmp-promise';

// Include all languages you want this server to support.
import "../text";
import "../json";
import "../java";
import "../javascript";
import {Writable} from "node:stream";

interface ProgramOptions {
    port?: number;
    logFile?: string;
    verbose?: boolean;
    batchSize?: number;
    traceGetObjectOutput?: boolean;
    traceGetObjectInput?: boolean;
    recipeInstallDir?: string;
}

async function main() {

    const program = new Command();
    program
        .option('--port <number>', 'port number')
        .option('--log-file <path>', 'log file path')
        .option('-v, --verbose', 'enable verbose output')
        .option('--batch-size [size]', 'sets the batch size (default is 200)', s => parseInt(s, 10), 200)
        .option('--trace-get-object-output', 'enable `GetObject` output tracing')
        .option('--trace-get-object-input', 'enable `GetObject` input tracing')
        .option('--recipe-install-dir', 'Recipe installation directory (default is a temporary directory)')
        .parse();

    const options = program.opts() as ProgramOptions;

    let recipeInstallDir: string;
    if (!options.recipeInstallDir) {
        let recipeCleanup: () => Promise<void>;

        async function setupRecipeDir() {
            const {path, cleanup} = await dir({unsafeCleanup: true});
            recipeCleanup = cleanup;
            return path;
        }

        // Register cleanup on exit
        process.on('SIGINT', async () => {
            if (recipeCleanup) {
                await recipeCleanup();
            }
            process.exit(0);
        });

        process.on('SIGTERM', async () => {
            if (recipeCleanup) {
                await recipeCleanup();
            }
            process.exit(0);
        });

        recipeInstallDir = await setupRecipeDir();
    } else {
        recipeInstallDir = options.recipeInstallDir;
    }

    const log: Writable | undefined = options.logFile ? fs.createWriteStream(options.logFile, {flags: 'a'}) :
        (options.port ? process.stdout : undefined);
    const logger: rpc.Logger = {
        error: (msg: string) => log && log.write(`[Error] ${msg}\n`),
        warn: (msg: string) => log && log.write(`[Warn] ${msg}\n`),
        info: (msg: string) => log && options.verbose && log.write(`[Info] ${msg}\n`),
        log: (msg: string) => log && options.verbose && log.write(`[Log] ${msg}\n`)
    };

    logger.log(`[js-rewrite-rpc] starting\n\n`);

    if (!options.port) {
        // Create the connection with the custom logger
        const connection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(process.stdin),
            new rpc.StreamMessageWriter(process.stdout),
            logger
        );

        if (options.verbose) {
            connection.trace(rpc.Trace.Verbose, logger).catch((err: Error) => {
                // Handle any unexpected errors during trace configuration
                logger.error(`Failed to set trace: ${err}\n`);
            });
        } else {
            connection.trace(Trace.Off, {} as Tracer);
        }

        connection.onError(err => {
            logger.error(`[js-rewrite-rpc] error: ${err}\n\n`);
        });

        connection.onClose(() => {
            logger.info(`[js-rewrite-rpc] connection closed\n\n`);
        })

        connection.onDispose(() => {
            logger.info(`[js-rewrite-rpc] connection disposed\n\n`);
        });

        new RewriteRpc(connection, {
            batchSize: options.batchSize,
            traceGetObjectInput: options.traceGetObjectInput ? log : undefined,
            traceGetObjectOutput: options.traceGetObjectOutput,
            recipeInstallDir: recipeInstallDir
        });
    } else {
        // Create a TCP server
        const server: net.Server = net.createServer((socket: net.Socket) => {
            logger.info(`[js-rewrite-rpc] new client connected: ${socket.remoteAddress}:${socket.remotePort}\n`);

            // Create the connection with the custom logger using the socket streams
            const connection: rpc.MessageConnection = rpc.createMessageConnection(
                new rpc.StreamMessageReader(socket),
                new rpc.StreamMessageWriter(socket),
                logger
            );

            if (options.verbose) {
                connection.trace(rpc.Trace.Verbose, logger).catch((err: Error) => {
                    // Handle any unexpected errors during trace configuration
                    logger.error(`Failed to set trace: ${err}\n`);
                });
            } else {
                connection.trace(Trace.Off, {} as Tracer);
            }

            connection.onError((err: [Error, rpc.Message | undefined, number | undefined]) => {
                logger.error(`[js-rewrite-rpc] error: ${err[0]}\n${err[0].stack}\n\n`);
            });

            connection.onClose(() => {
                logger.info(`[js-rewrite-rpc] connection closed\n\n`);
            });

            connection.onDispose(() => {
                logger.info(`[js-rewrite-rpc] connection disposed\n\n`);
            });

            socket.on('close', () => {
                logger.info(`[js-rewrite-rpc] socket closed: ${socket.remoteAddress}:${socket.remotePort}\n`);
            });

            socket.on('error', (err: Error) => {
                logger.error(`[js-rewrite-rpc] socket error: ${err.message}\n`);
            });

            // Initialize the RPC mechanism
            new RewriteRpc(connection, {
                batchSize: options.batchSize,
                traceGetObjectInput: options.traceGetObjectInput ? log : undefined,
                traceGetObjectOutput: options.traceGetObjectOutput,
                recipeInstallDir: recipeInstallDir
            });
        });

        // Handle server errors
        server.on('error', (err: Error) => {
            logger.error(`[js-rewrite-rpc] server error: ${err.message}\n`);
            process.exit(1);
        });

        // Start the server
        server.listen(options.port, '127.0.0.1', () => {
            logger.info(`[js-rewrite-rpc] server listening on 127.0.0.1:${options.port}\n`);
        });

        // Handle process termination
        process.on('SIGINT', () => {
            logger.info(`[js-rewrite-rpc] received SIGINT, shutting down\n`);
            server.close(() => {
                logger.info(`[js-rewrite-rpc] server closed\n`);
                process.exit(0);
            });
        });
    }

    // log uncaught exceptions
    process.on('uncaughtException', (error) => {
        logger.error('Fatal error:' + error.message);
        process.exit(1);
    });
}

main().catch(console.error);
