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
import {CsvDataTableStore, DataTable, DATA_TABLE_STORE, ExecutionContext, sanitizeScope} from "../src";
import {SetDataTableStore} from "../src/rpc/request/set-data-table-store";

describe("data tables", () => {

    test("data table descriptor", () => {
        const descriptor = ReplacedText.dataTable.descriptor;
        expect(descriptor.name).toBe("org.openrewrite.text.replaced-text");
        expect(descriptor.displayName).toBe("Replaced text");
        expect(descriptor.description).toBe("Text that was replaced.");
        expect(descriptor.columns).toHaveLength(2);
        expect(descriptor.columns[0].name).toBe("sourcePath");
        expect(descriptor.columns[1].name).toBe("text");
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
        // Header row uses column names (matches the Java writer)
        expect(lines[3]).toBe('sourcePath,text');
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

    test("SetDataTableStore reconstructs a CSV store that writes prefix columns", () => {
        const store = SetDataTableStore.toDataTableStore({
            kind: "CSV",
            outputDir: tmpDir,
            prefixColumns: {repositoryOrigin: "github.com/acme/example"},
            suffixColumns: {},
        });
        expect(store).toBeInstanceOf(CsvDataTableStore);

        const ctx = new ExecutionContext();
        ctx.messages[DATA_TABLE_STORE] = store;
        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/foo.ts", "written"));

        const fileKey = CsvDataTableStore.fileKey(ReplacedText.dataTable);
        const csvPath = path.join(tmpDir, fileKey + ".csv");
        expect(fs.existsSync(csvPath)).toBe(true);
        const content = fs.readFileSync(csvPath, 'utf8');
        expect(content).toContain('repositoryOrigin');
        expect(content).toContain('github.com/acme/example,src/foo.ts,written');
    });

    test("SetDataTableStore NOOP does not write to disk", () => {
        const store = SetDataTableStore.toDataTableStore({kind: "NOOP"});
        expect(store).not.toBeInstanceOf(CsvDataTableStore);

        const ctx = new ExecutionContext();
        ctx.messages[DATA_TABLE_STORE] = store;
        ReplacedText.dataTable.insertRow(ctx, new ReplacedText("src/foo.ts", "dropped"));

        expect(fs.readdirSync(tmpDir).filter(f => f.endsWith(".csv"))).toHaveLength(0);
    });

    test("two stores share one file with a single header", () => {
        const a = new CsvDataTableStore(tmpDir, {repositoryOrigin: "github.com/acme/x"});
        const b = new CsvDataTableStore(tmpDir, {repositoryOrigin: "github.com/acme/x"});
        const ctxA = new ExecutionContext(); ctxA.messages[DATA_TABLE_STORE] = a;
        const ctxB = new ExecutionContext(); ctxB.messages[DATA_TABLE_STORE] = b;

        ReplacedText.dataTable.insertRow(ctxA, new ReplacedText("a.ts", "A"));
        ReplacedText.dataTable.insertRow(ctxB, new ReplacedText("b.ts", "B"));

        const fileKey = CsvDataTableStore.fileKey(ReplacedText.dataTable);
        const content = fs.readFileSync(path.join(tmpDir, fileKey + ".csv"), 'utf8');
        const headerLines = content.split('\n').filter(l => l.startsWith('repositoryOrigin,'));
        expect(headerLines).toHaveLength(1);
        expect(content).toContain('a.ts');
        expect(content).toContain('b.ts');
    });

    // fileKey must match the Java host byte-for-byte, or a shared table resolves to two files.
    describe("fileKey matches the Java host", () => {

        test("default table (no group, instanceName == displayName) -> bare FQN", () => {
            const dt = new DataTable("org.openrewrite.table.TextMatches", "Text matches", "Text matches.", {});
            expect(CsvDataTableStore.fileKey(dt)).toBe("org.openrewrite.table.TextMatches");
        });

        test("the ReplacedText default table -> bare FQN (no '--' suffix)", () => {
            const key = CsvDataTableStore.fileKey(ReplacedText.dataTable);
            expect(key).toBe("org.openrewrite.text.replaced-text");
            expect(key).not.toContain("--");
        });

        test("custom instanceName -> <name>--<sanitize(instanceName)>", () => {
            const dt = new DataTable("org.openrewrite.table.TextMatches", "Text matches", "Text matches.", {});
            dt.instanceName = "My custom instance";
            expect(CsvDataTableStore.fileKey(dt)).toBe("org.openrewrite.table.TextMatches--my-custom-instance-36f3");
            expect(CsvDataTableStore.fileKey(dt))
                .toBe(`org.openrewrite.table.TextMatches--${sanitizeScope("My custom instance")}`);
        });

        test("group -> <name>--<sanitize(group)>", () => {
            const dt = new DataTable("org.openrewrite.table.TextMatches", "Text matches", "Text matches.", {});
            dt.group = "acme.shared";
            expect(CsvDataTableStore.fileKey(dt)).toBe("org.openrewrite.table.TextMatches--acme-shared-eb95");
        });

        test("group equal to the name -> bare FQN", () => {
            const dt = new DataTable("org.openrewrite.table.TextMatches", "Text matches", "Text matches.", {});
            dt.group = "org.openrewrite.table.TextMatches";
            expect(CsvDataTableStore.fileKey(dt)).toBe("org.openrewrite.table.TextMatches");
        });

        test("long group -> truncated at last dash + hash", () => {
            const dt = new DataTable("org.openrewrite.table.TextMatches", "Text matches", "Text matches.", {});
            dt.group = "Group with a really really long descriptive name here";
            expect(CsvDataTableStore.fileKey(dt))
                .toBe("org.openrewrite.table.TextMatches--group-with-a-really-really-e49b");
        });
    });
});
