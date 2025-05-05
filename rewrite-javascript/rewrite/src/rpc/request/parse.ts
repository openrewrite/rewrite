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
import {ExecutionContext} from "../../execution";
import {UUID} from "node:crypto";
import {Parser, ParserInput, Parsers} from "../../parser";

export class Parse {
    constructor(private readonly parser: string, private readonly inputs: {
        text?: string,
        path: string
    }[], private readonly relativeTo?: string) {
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, any>): void {
        connection.onRequest(new rpc.RequestType<Parse, UUID[], Error>("Parse"), async (request) => {
            let parser: Parser | undefined = Parsers.createParser(request.parser, new ExecutionContext(), request.relativeTo);

            if (parser) {
                const sourcePaths: ParserInput[] = request.inputs.map(i => i.text === undefined ? i.path! : {
                    text: i.text!,
                    sourcePath: i.path
                });
                const parsed = await parser.parse(...sourcePaths);
                return parsed.map(g => {
                    localObjects.set(g.id.toString(), g);
                    return g.id;
                })
            }
            return [];
        });
    }
}
