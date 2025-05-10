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
import {describe} from "@jest/globals";
import {ParserSourceReader, readSourceSync} from "../src";


describe("parse source reader utility", () => {
    const sourceJson = {text: `  { "type": "object" }`, sourcePath: "source.json"};

    test("whitespace", () => {
        const reader = new ParserSourceReader(sourceJson);
        expect(reader.whitespace()).toEqual("  ");
        expect(reader.cursor).toEqual(2);
    })

    test("source before a token", () => {
        const reader = new ParserSourceReader(sourceJson);
        expect(reader.sourceBefore("{")).toEqual("  ");
        expect(reader.cursor).toEqual(3);
    });

    test("read in memory source file", () => {
        expect(readSourceSync(sourceJson)).toEqual(`  { "type": "object" }`);
    });
});
