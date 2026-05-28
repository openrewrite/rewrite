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
import * as fs from "node:fs";
import * as path from "node:path";
import {ChildProcessWithoutNullStreams, spawn} from "node:child_process";
import * as rpc from "vscode-jsonrpc/node";
import {RewriteRpc} from "./rewrite-rpc";
import {RecipeMarketplace} from "../marketplace";

export interface JavaRpcOptions {
    /** Explicit classpath override. Falls back to env var, then test-classpath.txt. */
    classpath?: string;
    /** JAVA_HOME to use. Falls back to process.env.JAVA_HOME, then `java` on PATH. */
    javaHome?: string;
    /** Optional logger forwarded to the underlying RewriteRpc instance. */
    logger?: rpc.Logger;
    /** Optional path to a marketplace CSV — passed via `--marketplace=` to the Java process. */
    marketplaceCsv?: string;
    /** Enable RPC tracing on the Java side (passed as `--trace`). */
    trace?: boolean;
    /** Pre-built marketplace passed into the TS-side RewriteRpc. */
    marketplace?: RecipeMarketplace;
}

/**
 * Test-only wrapper around a spawned `org.openrewrite.maven.rpc.JavaRewriteRpc` process,
 * exposing a {@link RewriteRpc} client wired to its stdio over JSON-RPC.
 *
 * Typical use:
 * ```
 * const server = await JavaRpcTestServer.start();
 * try {
 *     const recipe = await server.rpc.prepareRecipe("org.openrewrite.text.FindAndReplace", {...});
 *     const result = await server.rpc.visit(parsed, recipe.editVisitor, ctx);
 * } finally {
 *     await server.dispose();
 * }
 * ```
 */
export class JavaRpcTestServer {
    private constructor(
        private readonly child: ChildProcessWithoutNullStreams,
        readonly rpc: RewriteRpc,
    ) {
    }

    /** Spawn the Java RPC server and connect a {@link RewriteRpc} client to its stdio. */
    static async start(opts: JavaRpcOptions = {}): Promise<JavaRpcTestServer> {
        const classpath = opts.classpath ?? findTestClasspath();
        if (!classpath) {
            throw new Error(
                "Java RPC test classpath not configured. " +
                "Run `./gradlew :rewrite-javascript:generateTestClasspath`, " +
                "or set REWRITE_JAVASCRIPT_CLASSPATH."
            );
        }

        const javaCmd = resolveJavaCommand(opts.javaHome);
        const args = ["-cp", classpath, "org.openrewrite.maven.rpc.JavaRewriteRpc"];
        if (opts.marketplaceCsv) args.push(`--marketplace=${opts.marketplaceCsv}`);
        if (opts.trace) args.push("--trace");

        const child = spawn(javaCmd, args, {stdio: ["pipe", "pipe", "pipe"]});

        // Forward Java stderr line-by-line so stack traces surface in the test output.
        child.stderr.setEncoding("utf8");
        let stderrTail = "";
        child.stderr.on("data", (chunk: string) => {
            stderrTail = (stderrTail + chunk);
            const lines = stderrTail.split(/\r?\n/);
            stderrTail = lines.pop() ?? "";
            for (const line of lines) {
                if (line.length > 0) process.stderr.write(`[Java RPC] ${line}\n`);
            }
        });

        // If the JVM dies before we can talk to it, surface that immediately rather
        // than letting the first RPC request hang waiting for a response.
        const earlyExit = new Promise<never>((_, reject) => {
            const onExit = (code: number | null, signal: NodeJS.Signals | null) => {
                reject(new Error(
                    `Java RPC server exited before any request was sent ` +
                    `(code=${code}, signal=${signal}). Check the [Java RPC] stderr above.`
                ));
            };
            child.once("exit", onExit);
        });

        const connection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(child.stdout),
            new rpc.StreamMessageWriter(child.stdin),
            opts.logger,
        );

        // RewriteRpc's constructor calls connection.listen() — no need to call it ourselves.
        const rewriteRpc = new RewriteRpc(connection, {
            marketplace: opts.marketplace,
            logger: opts.logger,
        });

        // Hand back control once we know either (a) the JVM is up and the connection is
        // listening, or (b) the JVM died early. A trivial round-trip (GetLanguages) does both.
        try {
            await Promise.race([
                rewriteRpc.languages(),
                earlyExit,
            ]);
        } catch (e) {
            try {
                connection.end();
            } catch { /* ignore */ }
            child.kill("SIGKILL");
            throw e;
        }

        return new JavaRpcTestServer(child, rewriteRpc);
    }

    /** Reset accumulated state on the Java side so the next test starts clean. */
    async reset(): Promise<void> {
        await this.rpc.connection.sendRequest(
            new rpc.RequestType0<boolean, Error>("Reset"),
        );
    }

    /**
     * End the JSON-RPC connection and wait for the Java process to exit. Falls back
     * to SIGKILL after a short grace period to ensure no orphan JVMs are left behind.
     */
    async dispose(): Promise<void> {
        // End the connection before killing the process: this triggers an EOF on the
        // Java side's stdin reader, which lets it shut down cleanly. Killing first
        // can race with in-flight requests and leak the connection-close handler.
        try {
            this.rpc.end();
        } catch { /* ignore — already closed */ }

        if (this.child.exitCode !== null || this.child.killed) {
            return;
        }
        await new Promise<void>((resolve) => {
            const grace = setTimeout(() => {
                this.child.kill("SIGKILL");
            }, 5_000);
            this.child.once("exit", () => {
                clearTimeout(grace);
                resolve();
            });
        });
    }
}

/**
 * Find the classpath for spawning the Java RPC server.
 *
 * Resolution order:
 *  1. `REWRITE_JAVASCRIPT_CLASSPATH` environment variable.
 *  2. `test-classpath.txt` written by the `:rewrite-javascript:generateTestClasspath`
 *     Gradle task. Walks up from this source file (works both when running directly
 *     via vitest and when running from `dist/`).
 *
 * @returns the classpath string, or `undefined` if no source is configured.
 */
export function findTestClasspath(): string | undefined {
    const env = process.env.REWRITE_JAVASCRIPT_CLASSPATH;
    if (env && env.length > 0) {
        return env;
    }

    // src/rpc/java-rpc-client.ts        → ../../test-classpath.txt
    // dist/rpc/java-rpc-client.js       → ../../test-classpath.txt
    const candidates = [
        path.resolve(__dirname, "..", "..", "test-classpath.txt"),
        path.resolve(__dirname, "..", "..", "..", "test-classpath.txt"),
        path.resolve(process.cwd(), "test-classpath.txt"),
        path.resolve(process.cwd(), "rewrite", "test-classpath.txt"),
    ];
    for (const candidate of candidates) {
        if (fs.existsSync(candidate)) {
            return fs.readFileSync(candidate, "utf8").trim();
        }
    }
    return undefined;
}

function resolveJavaCommand(javaHome?: string): string {
    const home = javaHome ?? process.env.JAVA_HOME;
    if (home) {
        const candidate = path.join(home, "bin", process.platform === "win32" ? "java.exe" : "java");
        if (fs.existsSync(candidate)) {
            return candidate;
        }
    }
    return "java";
}
