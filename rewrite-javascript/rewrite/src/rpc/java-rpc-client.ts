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
import * as readline from "node:readline";
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
    /**
     * Pre-built {@link RecipeMarketplace} for the TS-side {@link RewriteRpc} instance
     * only. Does NOT configure the Java side — for that, use {@link marketplaceCsv}.
     */
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

        const child = spawn(javaCmd, args);

        // Forward Java stderr line-by-line so stack traces surface in the test output.
        // `readline` handles partial-line buffering and flushes a non-newline-terminated
        // final line on `close` (a hand-rolled split would silently drop it).
        readline.createInterface({input: child.stderr, crlfDelay: Infinity})
            .on("line", line => {
                if (line.length > 0) process.stderr.write(`[Java RPC] ${line}\n`);
            });

        // If the JVM dies before we can talk to it, surface that immediately rather
        // than letting the first RPC request hang waiting for a response. Capture the
        // listener so we can detach it after the race resolves — otherwise the normal
        // exit during dispose() fires it, rejecting a Promise nobody is observing
        // (UnhandledPromiseRejection under Node 15+).
        let onEarlyExit!: (code: number | null, signal: NodeJS.Signals | null) => void;
        const earlyExit = new Promise<never>((_, reject) => {
            onEarlyExit = (code, signal) => {
                reject(new Error(
                    `Java RPC server exited before any request was sent ` +
                    `(code=${code}, signal=${signal}). Check the [Java RPC] stderr above.`
                ));
            };
            child.once("exit", onEarlyExit);
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
            child.kill("SIGKILL");
            throw e;
        } finally {
            child.removeListener("exit", onEarlyExit);
        }

        return new JavaRpcTestServer(child, rewriteRpc);
    }

    /**
     * Reset accumulated state on both the Java side and the TS-side
     * {@link RewriteRpc} caches so the next test starts clean.
     */
    async reset(): Promise<void> {
        await this.rpc.reset();
    }

    /**
     * End the JSON-RPC connection and wait for the Java process to exit. Falls back
     * to SIGKILL after a short grace period to ensure no orphan JVMs are left behind.
     */
    async dispose(): Promise<void> {
        // Attach the exit listener BEFORE signalling shutdown so we never miss the
        // event in a TOCTOU race between checking exitCode and registering the
        // listener. The Promise also resolves immediately if the process is
        // already gone — see the exitCode shortcut below.
        const exited = new Promise<void>((resolve) => {
            this.child.once("exit", resolve);
        });

        if (this.child.exitCode !== null) {
            return;
        }

        // End the connection before killing the process: this triggers an EOF on the
        // Java side's stdin reader, which lets it shut down cleanly. Killing first
        // can race with in-flight requests and leak the connection-close handler.
        try {
            this.rpc.end();
        } catch { /* ignore — already closed */ }

        const grace = setTimeout(() => {
            this.child.kill("SIGKILL");
        }, 5_000);
        try {
            await exited;
        } finally {
            clearTimeout(grace);
        }
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

    // From src/rpc/ (vitest source mode) or dist/rpc/ (compiled), the classpath
    // sits two levels up at the package root. The cwd fallback handles
    // consumer projects where this module is in node_modules but the file
    // lives next to the consumer's package root.
    const candidates = [
        path.resolve(__dirname, "..", "..", "test-classpath.txt"),
        path.resolve(process.cwd(), "test-classpath.txt"),
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
