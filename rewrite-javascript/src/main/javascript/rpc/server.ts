// #!/usr/bin/env node
import * as rpc from "vscode-jsonrpc/node";
import {RewriteRpc} from "./rewrite-rpc";
import {PassThrough} from "node:stream";
import * as fs from "fs";
import {WriteStream} from "fs";

const logFile: WriteStream = fs.createWriteStream(`${process.cwd()}/server.log`, {flags: 'a'});
logFile.write(`[server] starting\n\n`);

const inputLogger = new PassThrough();
process.stdin.pipe(inputLogger);
inputLogger.on('data', chunk => {
    logFile.write(`[server] ⇦ received: '${chunk.toString()}'\n\n`);
    logFile.write(`[server] ⇦ received: [${Array.from(chunk).join(", ")}]\n\n`);
});

const outputLogger = new PassThrough();
process.stdout.pipe(outputLogger);
outputLogger.on('data', chunk => {
    logFile.write(`[server] ⇨ sent: '${chunk.toString()}'\n\n`);
});

const logger: rpc.Logger = {
    error: (msg: string) => logFile.write(`[Error] ${msg}\n`),
    warn: (msg: string) => logFile.write(`[Warn] ${msg}\n`),
    info: (msg: string) => logFile.write(`[Info] ${msg}\n`),
    log: (msg: string) => logFile.write(`[Log] ${msg}\n`)
};

// Create the connection with the custom logger
const connection = rpc.createMessageConnection(
    new rpc.StreamMessageReader(inputLogger, 'utf-8'),
    new rpc.StreamMessageWriter(outputLogger, 'utf-8'),
    logger
);

connection.trace(rpc.Trace.Verbose, logger).catch(err => {
    // Handle any unexpected errors during trace configuration
    logFile.write(`Failed to set trace: ${err}`);
});

connection.onError(err => {
    logFile.write(`[server] error: ${err}\n\n`);
});

connection.onClose(() => {
    logFile.write(`[server] connection closed\n\n`);
})

connection.onDispose(() => {
    logFile.write(`[server] connection disposed\n\n`);
});

new RewriteRpc(connection);
