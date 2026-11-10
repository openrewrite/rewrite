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
import * as fs from "fs/promises";
import * as path from "path";
import {withDir} from "tmp-promise";
import {MessageConnection} from "vscode-jsonrpc/node";
import {ProjectParser} from "../../src/javascript/project-parser";
import {ParseProject, ParseProjectResponseItem} from "../../src/rpc/request/parse-project";
import {SourceFile} from "../../src/tree";
import {StyleKind} from "../../src/javascript/style";

async function parseRpc(projectPath: string): Promise<SourceFile[]> {
    let handler!: (request: ParseProject) => Promise<ParseProjectResponseItem[]>;
    const localObjects = new Map<string, (id: string) => Promise<SourceFile>>();
    ParseProject.handle({
        onRequest: (_type: unknown, callback: typeof handler) => {
            handler = callback;
        }
    } as unknown as MessageConnection, localObjects);
    const items = await handler(new ParseProject(projectPath));
    const files: SourceFile[] = [];
    for (const item of items) {
        files.push(await localObjects.get(item.id)!(item.id));
    }
    return files;
}

describe('Project formatter selection', () => {
    test.each(['streaming', 'rpc'])('%s uses Autodetect alongside configured Prettier files', async mode => {
        // given
        await withDir(async repo => {
            const files = {
                'package.json': '{"name":"test-project"}',
                'node_modules/prettier/package.json': '{"name":"prettier","version":"3.0.0"}',
                'src/app.ts': "export const key = 'key';\n",
                'formatted/.prettierrc': '{}',
                'formatted/app.ts': 'export const key = "key";\n'
            };
            for (const [relativePath, content] of Object.entries(files)) {
                const filePath = path.join(repo.path, relativePath);
                await fs.mkdir(path.dirname(filePath), {recursive: true});
                await fs.writeFile(filePath, content);
            }

            // when
            const parsed: SourceFile[] = [];
            if (mode === 'rpc') {
                parsed.push(...await parseRpc(repo.path));
            } else {
                for await (const file of new ProjectParser(repo.path, {useGit: false}).parse()) {
                    parsed.push(file);
                }
            }

            // then
            const unconfigured = parsed.find(file => file.sourcePath === 'src/app.ts')!;
            const configured = parsed.find(file => file.sourcePath === 'formatted/app.ts')!;
            expect(unconfigured.markers.markers.map(marker => marker.kind)).toContain(StyleKind.Autodetect);
            expect(unconfigured.markers.markers.map(marker => marker.kind)).not.toContain(StyleKind.PrettierStyle);
            expect(configured.markers.markers.map(marker => marker.kind)).toContain(StyleKind.PrettierStyle);
            expect(configured.markers.markers.map(marker => marker.kind)).not.toContain(StyleKind.Autodetect);
        }, {unsafeCleanup: true});
    });
});
