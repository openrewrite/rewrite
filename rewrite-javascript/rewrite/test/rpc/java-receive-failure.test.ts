/*
 * Copyright 2026 the original author or authors.
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
import {afterAll, afterEach, beforeAll, describe, expect, test, vi} from "vitest";
import {describeJavaRpc} from "../../src/test/java-rpc";
import {JavaRpcTestServer} from "../../src/rpc/java-rpc-client";
import {RpcReceiveQueue} from "../../src/rpc";
import {ExecutionContext} from "../../src/execution";
import {J} from "../../src/java";
import {JavaScriptParser, JavaScriptVisitor, JS} from "../../src/javascript";
import {TreePrinters} from "../../src/print";

const simplify = "org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor";

async function parse(statements: number): Promise<JS.CompilationUnit> {
    let text = "function f(b: boolean) {\n";
    for (let i = 0; i < statements; i++) {
        text += `    const c${i} = b || false;\n`;
    }
    return (await new JavaScriptParser().parse({text: text + "}", sourcePath: "f.ts"}).next()).value as JS.CompilationUnit;
}

function simplified(statements: number): string {
    let text = "function f(b: boolean) {\n";
    for (let i = 0; i < statements; i++) {
        text += `    const c${i} = b;\n`;
    }
    return text + "}";
}

describeJavaRpc("a transfer that could not be read to its end", () => {

    // one message a page leaves the sender in the middle of a transfer, a thousand lets it finish first
    describe.each([1, 1000])("with %d messages to a page from this side", batchSize => {
        let java: JavaRpcTestServer;

        beforeAll(async () => {
            java = await JavaRpcTestServer.start({batchSize});
        });

        afterAll(async () => {
            await java.dispose();
        });

        afterEach(() => {
            vi.restoreAllMocks();
        });

        test("is sent whole again after Java failed to receive it", async () => {
            const cu = await parse(1);
            const undecodable = await new class extends JavaScriptVisitor<number> {
                protected override async visitLiteral(literal: J.Literal, _p: number): Promise<J | undefined> {
                    return {...literal, kind: "org.openrewrite.java.tree.J$NoSuchTree"} as unknown as J;
                }
            }().visitDefined<JS.CompilationUnit>(cu, 0);

            await expect(java.rpc.visit(undecodable, simplify, new ExecutionContext())).rejects.toThrow();

            const after = await java.rpc.visit(cu, simplify, new ExecutionContext());
            expect(await TreePrinters.print(after as JS.CompilationUnit)).toEqual(simplified(1));
        });

        // Java pages a thousand messages at a time, so the larger file is one it is still sending
        test.each([1, 300])("is asked for whole again after this side failed to receive it (%d statements)", async statements => {
            const cu = await parse(statements);

            const receive = RpcReceiveQueue.prototype.receive;
            let untilFailure = 5;
            vi.spyOn(RpcReceiveQueue.prototype, "receive").mockImplementation(function (this: RpcReceiveQueue, before: any, onChange?: any) {
                if (untilFailure > 0 && --untilFailure === 0) {
                    return Promise.reject(new Error("cannot decode"));
                }
                return receive.call(this, before, onChange);
            } as any);

            await expect(java.rpc.visit(cu, simplify, new ExecutionContext())).rejects.toThrow("cannot decode");

            const after = await java.rpc.visit(cu, simplify, new ExecutionContext());
            expect(await TreePrinters.print(after as JS.CompilationUnit)).toEqual(simplified(statements));
        });
    });
});
