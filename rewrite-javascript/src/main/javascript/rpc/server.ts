// #!/usr/bin/env node
import * as rpc from "vscode-jsonrpc/node";
import {RewriteRpc} from "./rewrite-rpc";
import * as fs from "fs";
import {WriteStream} from "fs";

const log: WriteStream = fs.createWriteStream(`${process.cwd()}/server.log`, {flags: 'a'});
log.write(`[server] starting\n\n`);

// const inprocess.stdin.on('data', (chunk) => {
//   log.write(`[server] ⇦ received: '${chunk.toString()}'\n\n`);
// })

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
    log.write(`[server] error: ${err}\n\n`);
});

connection.onClose(() => {
    log.write(`[server] connection closed\n\n`);
})

connection.onDispose(() => {
    log.write(`[server] connection disposed\n\n`);
});

new RewriteRpc(connection);
