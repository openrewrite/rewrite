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
import "../java";
import "../javascript";

// Not possible to set the stack size when executing from npx for security reasons
require('v8').setFlagsFromString('--stack-size=8000');

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
        .option('--batch-size [size]', 'sets the batch size (default is 200)', s => parseInt(s, 10), 200)
        .option('--recipe-install-dir <install_dir>', 'Recipe installation directory (default is a temporary directory)')
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
            // Clean up old dependency workspaces (older than 24 hours)
            DependencyWorkspace.cleanupOldWorkspaces();
            process.exit(0);
        });

        process.on('SIGTERM', async () => {
            if (recipeCleanup) {
                await recipeCleanup();
            }
            // Clean up old dependency workspaces (older than 24 hours)
            DependencyWorkspace.cleanupOldWorkspaces();
            process.exit(0);
        });

        recipeInstallDir = await setupRecipeDir();
    } else {
        recipeInstallDir = options.recipeInstallDir;
    }

    const log = options.logFile ? fs.createWriteStream(options.logFile, {flags: 'a'}) : undefined;
    const logger: rpc.Logger = {
        error: (msg: string) => log && log.write(`[js error] ${msg}\n`),
        warn: (msg: string) => log && log.write(`[js warn] ${msg}\n`),
        info: (msg: string) => log && log.write(`[js info] ${msg}\n`),
        // The RPC Tracer configured below itself writes to this "log" level for every message it sends or receives,
        // because the Tracer type has a log method on it that matches this signature.
        log: (msg: string) => log && options.traceRpcMessages && log.write(`[js trace] ${msg}\n`)
    };

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
