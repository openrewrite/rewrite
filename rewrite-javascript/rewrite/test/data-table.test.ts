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

import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import {ReplacedText} from "../fixtures/replaced-text";
import {CsvDataTableStore, DATA_TABLE_STORE, ExecutionContext} from "../src";

describe("data tables", () => {

    test("data table descriptor", () => {
        const descriptor = ReplacedText.dataTable.descriptor;
        expect(descriptor).toEqual({
            name: "org.openrewrite.text.replaced-text",
            displayName: "Replaced text",
            description: "Text that was replaced.",
            columns: [
                {name: "sourcePath", displayName: "Source Path", description: "Source path of the file"},
                {name: "text", displayName: "Text", description: "The replaced text"}
            ]
        });
    });

    let tmpDir: string;

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'csv-data-table-test-'));
    });

    afterEach(() => {
        fs.rmSync(tmpDir, {recursive: true, force: true});
    });

    test("writes CSV file with comments, header and data rows", () => {
        const store = new CsvDataTableStore(tmpDir);
        const ctx = new ExecutionContext();
        ctx.messages[DATA_TABLE_STORE] = store;

        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/foo.ts", "old text"));
        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/bar.ts", "another"));

        const fileKey = CsvDataTableStore.fileKey(ReplacedText.dataTable);
        const csvPath = path.join(tmpDir, fileKey + ".csv");
        expect(fs.existsSync(csvPath)).toBe(true);

        const content = fs.readFileSync(csvPath, 'utf8');
        const lines = content.split('\n');

        // Comment rows
        expect(lines[0]).toMatch(/^# @name /);
        expect(lines[1]).toMatch(/^# @instanceName /);
        expect(lines[2]).toMatch(/^# @group/);
        // Header row
        expect(lines[3]).toBe('Source Path,Text');
        // Data rows
        expect(lines[4]).toBe('src/foo.ts,old text');
        expect(lines[5]).toBe('src/bar.ts,another');
    });

    test("escapes values containing commas", () => {
        const store = new CsvDataTableStore(tmpDir);
        const ctx = new ExecutionContext();
        ctx.messages[DATA_TABLE_STORE] = store;

        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/file.ts", "hello, world"));

        const fileKey = CsvDataTableStore.fileKey(ReplacedText.dataTable);
        const csvPath = path.join(tmpDir, fileKey + ".csv");
        const content = fs.readFileSync(csvPath, 'utf8');
        const lines = content.split('\n');

        // Data row is after 3 comment lines + 1 header line
        expect(lines[4]).toBe('src/file.ts,"hello, world"');
    });

    test("escapes values containing quotes", () => {
        const store = new CsvDataTableStore(tmpDir);
        const ctx = new ExecutionContext();
        ctx.messages[DATA_TABLE_STORE] = store;

        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/file.ts", 'say "hello"'));

        const fileKey = CsvDataTableStore.fileKey(ReplacedText.dataTable);
        const csvPath = path.join(tmpDir, fileKey + ".csv");
        const content = fs.readFileSync(csvPath, 'utf8');
        const lines = content.split('\n');

        expect(lines[4]).toBe('src/file.ts,"say ""hello"""');
    });

    test("escapes values containing newlines", () => {
        const store = new CsvDataTableStore(tmpDir);
        const ctx = new ExecutionContext();
        ctx.messages[DATA_TABLE_STORE] = store;

        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/file.ts", "line1\nline2"));

        const fileKey = CsvDataTableStore.fileKey(ReplacedText.dataTable);
        const csvPath = path.join(tmpDir, fileKey + ".csv");
        const content = fs.readFileSync(csvPath, 'utf8');

        expect(content).toContain('src/file.ts,"line1\nline2"');
    });

    test("always writes rows (no acceptRows gating)", () => {
        const store = new CsvDataTableStore(tmpDir);
        const ctx = new ExecutionContext();
        ctx.messages[DATA_TABLE_STORE] = store;

        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/foo.ts", "written"));

        const fileKey = CsvDataTableStore.fileKey(ReplacedText.dataTable);
        const csvPath = path.join(tmpDir, fileKey + ".csv");
        expect(fs.existsSync(csvPath)).toBe(true);
    });

    test("tracks row counts and table keys", () => {
        const store = new CsvDataTableStore(tmpDir);
        const ctx = new ExecutionContext();
        ctx.messages[DATA_TABLE_STORE] = store;

        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/foo.ts", "text1"));
        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/bar.ts", "text2"));
        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/baz.ts", "text3"));

        const fileKey = CsvDataTableStore.fileKey(ReplacedText.dataTable);
        expect(store.tableKeys).toEqual([fileKey]);
        expect(store.rowCounts).toEqual({[fileKey]: 3});
    });

    test("creates output directory if it does not exist", () => {
        const nestedDir = path.join(tmpDir, 'nested', 'output', 'dir');
        expect(fs.existsSync(nestedDir)).toBe(false);

        const store = new CsvDataTableStore(nestedDir);
        expect(fs.existsSync(nestedDir)).toBe(true);
    });
});
