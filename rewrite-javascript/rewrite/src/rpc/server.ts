#!/usr/bin/env node
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
import {Command} from 'commander';
import {dir} from 'tmp-promise';
import {DependencyWorkspace} from "../javascript/dependency-workspace";

// Include all languages you want this server to support.
import "../text";
import "../json";
import "../yaml";
import "../java";
import "../javascript";

// Not possible to set the stack size when executing from npx for security reasons
require('v8').setFlagsFromString('--stack-size=8000');

function initPyroscope(logger: rpc.Logger): any {
    // Strip trailing slashes: the SDK builds the ingest URL as `${serverAddress}/ingest`,
    // so a trailing slash produces `//ingest`, which the server normalizes via redirect —
    // and undici downgrades the redirected POST to a GET, silently dropping all profiles.
    const server = (process.env.PYROSCOPE_SERVER_ADDRESS || '').replace(/\/+$/, '');
    if (!server) {
        return undefined;
    }
    let Pyroscope: any;
    try {
        Pyroscope = require('@pyroscope/nodejs');
    } catch {
        logger.warn('PYROSCOPE_SERVER_ADDRESS set but @pyroscope/nodejs not installed; profiling disabled');
        return undefined;
    }
    const tags: Record<string, string> = {runtime: 'node'};
    for (const pair of (process.env.PYROSCOPE_TAGS || '').split(',')) {
        const eq = pair.indexOf('=');
        if (eq > 0) {
            tags[pair.slice(0, eq).trim()] = pair.slice(eq + 1).trim();
        }
    }
    Pyroscope.init({
        appName: process.env.PYROSCOPE_APPLICATION_NAME || 'modcli',
        serverAddress: server,
        tags,
        wall: {
            collectCpuTime: true,
        },
    });
    Pyroscope.start();
    return Pyroscope;
}

interface ProgramOptions {
    logFile?: string;
    metricsCsv?: string;
    traceRpcMessages?: boolean;
    batchSize?: number;
    recipeInstallDir?: string;
    profile?: boolean;
}

async function main() {
    const program = new Command();
    program
        .option('--port <number>', 'port number')
        .option('--log-file <log_path>', 'log file path')
        .option('--metrics-csv <metrics_csv_path>', 'metrics CSV output path')
        .option('--trace-rpc-messages', 'trace RPC messages at the protocol level')
        .option('--batch-size [size]', 'sets the batch size (default is 200)', s => parseInt(s, 10), 1000)
        .option('--recipe-install-dir <install_dir>', 'Recipe installation directory (default is a temporary directory)')
        .parse();

    const options = program.opts() as ProgramOptions;

    let recipeCleanup: (() => Promise<void>) | undefined;
    let recipeInstallDir: string;
    if (!options.recipeInstallDir) {
        const {path, cleanup} = await dir({unsafeCleanup: true});
        recipeCleanup = cleanup;
        recipeInstallDir = path;
    } else {
        recipeInstallDir = options.recipeInstallDir;
    }

    // Single graceful-shutdown path used by SIGINT, SIGTERM, and connection
    // close. The connection-close path is what catches parent SIGKILL: when
    // the host JVM is killed, our stdin closes, vscode-jsonrpc fires onClose,
    // and we exit. Without it, Pyroscope (or any other ref-holder) can keep
    // the event loop alive and orphan this process.
    let pyroscope: any;
    let shuttingDown = false;
    const shutdown = async () => {
        if (shuttingDown) return;
        shuttingDown = true;
        try {
            if (pyroscope) {
                try {
                    await pyroscope.stop();
                } catch (e: any) {
                    // best-effort flush; nothing to do if it fails during shutdown
                }
            }
            if (recipeCleanup) {
                await recipeCleanup();
            }
            DependencyWorkspace.cleanupOldWorkspaces();
        } finally {
            process.exit(0);
        }
    };

    process.on('SIGINT', shutdown);
    process.on('SIGTERM', shutdown);

    const log = options.logFile ? fs.createWriteStream(options.logFile, {flags: 'a'}) : undefined;
    const logger: rpc.Logger = {
        error: (msg: string) => log && log.write(`[js error] ${msg}\n`),
        warn: (msg: string) => log && log.write(`[js warn] ${msg}\n`),
        info: (msg: string) => log && log.write(`[js info] ${msg}\n`),
        // The RPC Tracer configured below itself writes to this "log" level for every message it sends or receives,
        // because the Tracer type has a log method on it that matches this signature.
        log: (msg: string) => log && options.traceRpcMessages && log.write(`[js trace] ${msg}\n`)
    };

    pyroscope = initPyroscope(logger);

    // Create the connection with the custom logger
    const connection = rpc.createMessageConnection(
        new rpc.StreamMessageReader(process.stdin),
        new rpc.StreamMessageWriter(process.stdout),
        logger
    );

    if (options.traceRpcMessages) {
        await connection.trace(rpc.Trace.Verbose, logger).catch((err: Error) => {
            // Handle any unexpected errors during trace configuration
            logger.error(`Failed to set trace: ${err}`);
        });
    } else {
        await connection.trace(rpc.Trace.Off, {} as rpc.Tracer);
    }

    connection.onError(err => {
        logger.error(`error: ${err}`);
    });

    connection.onClose(() => {
        logger.info(`connection closed`);
        void shutdown();
    })

    connection.onDispose(() => {
        logger.info(`connection disposed`);
    });

    // log uncaught exceptions
    process.on('uncaughtException', (error) => {
        logger.error('Fatal error:' + error.message);
        process.exit(8);
    });

    new RewriteRpc(connection, {
        batchSize: options.batchSize,
        logger: logger,
        metricsCsv: options.metricsCsv,
        recipeInstallDir: recipeInstallDir
    });
}

main().catch(console.error);
