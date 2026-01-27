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
import {Parser, ParserInput, readSourceSync, Parsers} from "../parser";
import {PlainText} from "./tree";
import {randomId} from "../uuid";
import {emptyMarkers} from "../markers";

export class PlainTextParser extends Parser {
    async *parse(...sourcePaths: ParserInput[]): AsyncGenerator<PlainText> {
        for (const sourcePath of sourcePaths) {
            yield this.parseOne(sourcePath);
        }
    }

    override parseOne(input: ParserInput): PlainText {
        return {
            kind: PlainText.Kind.PlainText,
            id: randomId(),
            markers: emptyMarkers,
            sourcePath: this.relativePath(input),
            text: readSourceSync(input),
            snippets: []
        };
    }
}

Parsers.registerParser("plainText", PlainTextParser);
