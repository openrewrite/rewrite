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
import * as fs from "fs";
import * as path from "path";
import {JavaScriptParser, JS} from "../../src/javascript";

export const moduleDir = path.resolve(__dirname, "../..");

export interface Source {
    cu: JS.CompilationUnit;
    text: string;
}

/**
 * Parses the JavaScript and TypeScript files under a directory for the Java printer parity tests.
 */
export async function parse(dir: string): Promise<Source[]> {
    const files = (fs.readdirSync(dir, {recursive: true}) as string[])
        .filter(file => /\.(ts|tsx|js|jsx|mjs)$/.test(file))
        .sort();
    expect(files).not.toHaveLength(0);

    const texts = files.map(file => fs.readFileSync(path.join(dir, file), "utf8"));
    const sources: Source[] = [];
    for await (const parsed of new JavaScriptParser({relativeTo: dir})
        .parse(...files.map((file, i) => ({text: texts[i], sourcePath: file})))) {
        expect(parsed.kind, files[sources.length]).toEqual(JS.Kind.CompilationUnit);
        sources.push({cu: parsed as JS.CompilationUnit, text: texts[sources.length]});
    }
    return sources;
}
