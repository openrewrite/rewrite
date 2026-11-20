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
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import {JavaScriptParser} from "../../../src/javascript";
import {ParseErrorKind} from "../../../src/parse-error";
import {TreePrinters} from "../../../src/print";
import {SourceFile} from "../../../src/tree";

describe('source file encoding', () => {
    const source = `/**\r\n * Prometheus rules file\r\n */\r\nexport interface PrometheusRules {\r\n  groups?: string[];\r\n}\r\n`;
    let dir: string;

    beforeAll(() => {
        dir = fs.mkdtempSync(path.join(os.tmpdir(), "encoding-test-"));
    });

    afterAll(() => {
        fs.rmSync(dir, {recursive: true, force: true});
    });

    function utf16be(text: string): Buffer {
        return Buffer.from(Buffer.from(text, "utf16le").swap16());
    }

    async function parseBytes(name: string, bytes: Buffer): Promise<SourceFile> {
        const file = path.join(dir, name);
        fs.writeFileSync(file, bytes);
        const parsed: SourceFile[] = [];
        for await (const sf of new JavaScriptParser({relativeTo: dir}).parse(file)) {
            parsed.push(sf);
        }
        expect(parsed).toHaveLength(1);
        return parsed[0];
    }

    const cases: [string, () => Buffer][] = [
        ["UTF-16LE with BOM", () => Buffer.concat([Buffer.from([0xFF, 0xFE]), Buffer.from(source, "utf16le")])],
        ["UTF-16BE with BOM", () => Buffer.concat([Buffer.from([0xFE, 0xFF]), utf16be(source)])],
        ["UTF-8 with BOM", () => Buffer.concat([Buffer.from([0xEF, 0xBB, 0xBF]), Buffer.from(source, "utf8")])],
        ["UTF-8 without BOM", () => Buffer.from(source, "utf8")],
    ];

    test.each(cases)('%s file parses and round-trips', async (name, bytes) => {
        const sf = await parseBytes(`a-${name.replace(/\W+/g, "-")}.ts`, bytes());
        expect(sf.kind).not.toBe(ParseErrorKind);
        const printed = await TreePrinters.print(sf);
        expect(printed.replace(/^﻿/, "")).toBe(source);
        expect(printed.startsWith("﻿")).toBe(!name.includes("without"));
    });
});
