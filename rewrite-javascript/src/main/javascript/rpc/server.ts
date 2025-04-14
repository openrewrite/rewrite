// #!/usr/bin/env node
import * as rpc from "vscode-jsonrpc/node";
import {RewriteRpc} from "./rewrite-rpc";
import {PassThrough} from "node:stream";
import * as fs from "fs";
import {WriteStream} from "fs";

const logFile: WriteStream = fs.createWriteStream(`${process.cwd()}/server.log`, {flags: 'a'});
const inputLogger = new PassThrough();
process.stdin.pipe(inputLogger);
inputLogger.on('data', chunk => {
    logFile.write(`[server] ⇦ received: '${chunk.toString()}'\n`);
});

const outputLogger = new PassThrough();
process.stdout.pipe(outputLogger);
outputLogger.on('data', chunk => {
    logFile.write(`[server] ⇨ sent: '${chunk.toString()}'\n`);
});

new RewriteRpc(rpc.createMessageConnection(
    new rpc.StreamMessageReader(inputLogger),
    new rpc.StreamMessageWriter(outputLogger)
));
