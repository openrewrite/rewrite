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
import * as crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';
import * as zlib from 'zlib';
import {ExecutionContext, RPC_SHARED_MESSAGE_PREFIX} from "./execution";
import {OptionDescriptor} from "./recipe";

export const DATA_TABLE_STORE = Symbol.for("org.openrewrite.dataTables.store");
const COLUMNS_KEY = Symbol.for("org.openrewrite.dataTables.columns");

/**
 * RPC-shared messages carrying the configuration for a {@link CsvDataTableStore}.
 * An orchestrating process (typically the JVM) sets these on its ExecutionContext;
 * they travel to this peer with the context, and {@link DataTable#insertRow}
 * materializes a local store from them on first use.
 *
 * Must stay in sync with `DataTableExecutionContextView` in Java.
 */
export const DATA_TABLE_STORE_OUTPUT_DIR = RPC_SHARED_MESSAGE_PREFIX + "dataTableOutputDir";
export const DATA_TABLE_STORE_FILE_EXTENSION = RPC_SHARED_MESSAGE_PREFIX + "dataTableFileExtension";
export const DATA_TABLE_STORE_PREFIX_COLUMNS = RPC_SHARED_MESSAGE_PREFIX + "dataTablePrefixColumns";
export const DATA_TABLE_STORE_SUFFIX_COLUMNS = RPC_SHARED_MESSAGE_PREFIX + "dataTableSuffixColumns";

export function Column(descriptor: ColumnDescriptor) {
    return function (target: any, propertyKey: string) {
        if (!target.constructor.hasOwnProperty(COLUMNS_KEY)) {
            Object.defineProperty(target.constructor, COLUMNS_KEY, {
                value: {},
                writable: true,
                configurable: true,
            });
        }
        target.constructor[COLUMNS_KEY][propertyKey] = descriptor;
    }
}

export interface DataTableStore {
    insertRow<Row>(dataTable: DataTable<Row>, ctx: ExecutionContext, row: Row): void

    getRows(dataTableName: string, scope: string): Iterable<any>

    getDataTables(): DataTable<any>[]
}

export class InMemoryDataTableStore implements DataTableStore {
    private readonly _buckets = new Map<string, { dataTable: DataTable<any>, rows: any[] }>()

    insertRow<Row>(dataTable: DataTable<Row>, _ctx: ExecutionContext, row: Row): void {
        const suffix = dataTable.group ?? dataTable.instanceName;
        const key = `${dataTable.descriptor.name}\0${suffix}`;
        let bucket = this._buckets.get(key);
        if (!bucket) {
            bucket = {dataTable, rows: []};
            this._buckets.set(key, bucket);
        }
        bucket.rows.push(row);
    }

    getRows(dataTableName: string, group?: string): any[] {
        if (group !== undefined) {
            const key = `${dataTableName}\0${group}`;
            const bucket = this._buckets.get(key);
            return bucket ? [...bucket.rows] : [];
        }
        // For ungrouped, find by name with no group
        for (const bucket of this._buckets.values()) {
            if (bucket.dataTable.descriptor.name === dataTableName && bucket.dataTable.group === undefined) {
                return [...bucket.rows];
            }
        }
        return [];
    }

    getDataTables(): DataTable<any>[] {
        return Array.from(this._buckets.values()).map(b => b.dataTable);
    }
}

function sha256Prefix(input: string, hexChars: number): string {
    const hash = crypto.createHash('sha256').update(input, 'utf8').digest('hex');
    return hash.substring(0, hexChars);
}

export function sanitizeScope(scope: string): string {
    // 1. lowercase
    let s = scope.toLowerCase();
    // 2. replace non-alphanumeric with '-'
    s = s.replace(/[^a-z0-9]/g, '-');
    // 3. collapse consecutive '-', trim leading/trailing
    s = s.replace(/-+/g, '-').replace(/^-|-$/g, '');
    // 4. truncate to ~30 chars at word boundary
    if (s.length > 30) {
        s = s.substring(0, 30);
        const lastDash = s.lastIndexOf('-');
        if (lastDash > 0) {
            s = s.substring(0, lastDash);
        }
    }
    // 5. append 4-char hash
    const hash = sha256Prefix(scope, 4);
    return `${s}-${hash}`;
}

export class DataTable<Row> {
    private readonly _descriptor: DataTableDescriptor
    private _group?: string
    private _instanceName?: string

    public constructor(name: string, displayName: string, description: string,
                       private rowConstructor: { [key: string | symbol]: any }) {
        this._descriptor = {
            name: name,
            displayName: displayName,
            description: description,
            columns: []
        }
    }

    get descriptor(): DataTableDescriptor {
        const columnsRecord: Record<string, OptionDescriptor> = this.rowConstructor[COLUMNS_KEY] || {};
        return {
            ...this._descriptor,
            columns: Object.entries(columnsRecord).map(([name, descriptor]) =>
                ({name, ...descriptor})),
        }
    }

    get group(): string | undefined {
        return this._group;
    }

    set group(value: string) {
        this._group = value;
    }

    get instanceName(): string {
        return this._instanceName ?? this._descriptor.displayName;
    }

    set instanceName(value: string) {
        this._instanceName = value;
    }

    insertRow(ctx: ExecutionContext, row: Row): void {
        if (!ctx.messages[DATA_TABLE_STORE]) {
            ctx.messages[DATA_TABLE_STORE] = csvDataTableStoreFromConfig(ctx) ?? new InMemoryDataTableStore();
        }
        const dataTableStore: DataTableStore = ctx.messages[DATA_TABLE_STORE];
        dataTableStore.insertRow(this, ctx, row);
    }
}

/**
 * Materialize a {@link CsvDataTableStore} from the RPC-shared configuration
 * messages in the context, or undefined when no output directory is configured.
 * The store's files get a unique name suffix so that other processes writing
 * to the same directory (the orchestrator, other peers) never collide with it.
 */
function csvDataTableStoreFromConfig(ctx: ExecutionContext): CsvDataTableStore | undefined {
    const outputDir = ctx.messages[DATA_TABLE_STORE_OUTPUT_DIR];
    if (typeof outputDir !== 'string') {
        return undefined;
    }
    const fileExtension = ctx.messages[DATA_TABLE_STORE_FILE_EXTENSION] ?? '.csv';
    return new CsvDataTableStore(outputDir, {
        fileExtension: `-${crypto.randomBytes(4).toString('hex')}${fileExtension}`,
        prefixColumns: unflattenColumns(ctx.messages[DATA_TABLE_STORE_PREFIX_COLUMNS]),
        suffixColumns: unflattenColumns(ctx.messages[DATA_TABLE_STORE_SUFFIX_COLUMNS]),
    });
}

/**
 * Static columns travel over RPC as a flat list alternating name and value,
 * preserving column order while staying within the string/list-of-strings
 * restriction on shared message values.
 */
function unflattenColumns(flattened?: string[]): Record<string, string> {
    const columns: Record<string, string> = {};
    if (Array.isArray(flattened)) {
        for (let i = 0; i + 1 < flattened.length; i += 2) {
            columns[flattened[i]] = flattened[i + 1];
        }
    }
    return columns;
}

export interface DataTableDescriptor {
    name: string,
    displayName: string,
    description: string,
    columns: ({ name: string } & ColumnDescriptor)[]
}

export interface ColumnDescriptor {
    displayName: string,
    description: string
}

/**
 * Escape a value for CSV output following RFC 4180.
 */
function escapeCsv(value: unknown): string {
    if (value === null || value === undefined) {
        return '""';
    }
    const str = String(value);
    if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
        return '"' + str.replace(/"/g, '""') + '"';
    }
    return str;
}

/**
 * Options for {@link CsvDataTableStore}.
 */
export interface CsvDataTableStoreOptions {
    /**
     * File extension including dot (default ".csv"). An extension ending in
     * ".gz" GZIP-compresses the output; each write appends a complete GZIP
     * member, producing a valid multi-member archive at all times.
     */
    fileExtension?: string;
    /** Static columns prepended to each row, in insertion order. */
    prefixColumns?: Record<string, string>;
    /** Static columns appended to each row, in insertion order. */
    suffixColumns?: Record<string, string>;
}

/**
 * A DataTableStore that writes rows directly to CSV files as they are inserted.
 * Uses the data table's file-safe key for filenames.
 */
export class CsvDataTableStore implements DataTableStore {
    private readonly _initializedTables = new Set<string>();
    private readonly _rowCounts: { [key: string]: number } = {};
    private readonly _dataTables = new Map<string, DataTable<any>>();
    private readonly _fileExtension: string;
    private readonly _prefixColumns: Record<string, string>;
    private readonly _suffixColumns: Record<string, string>;

    constructor(private readonly outputDir: string, options?: CsvDataTableStoreOptions) {
        this._fileExtension = options?.fileExtension ?? '.csv';
        this._prefixColumns = options?.prefixColumns ?? {};
        this._suffixColumns = options?.suffixColumns ?? {};
        fs.mkdirSync(outputDir, {recursive: true});
    }

    private write(csvPath: string, text: string, append: boolean): void {
        const data = this._fileExtension.endsWith('.gz') ? zlib.gzipSync(Buffer.from(text, 'utf8')) : text;
        if (append) {
            fs.appendFileSync(csvPath, data);
        } else {
            fs.writeFileSync(csvPath, data);
        }
    }

    insertRow<Row>(dataTable: DataTable<Row>, _ctx: ExecutionContext, row: Row): void {
        const fileKey = CsvDataTableStore.fileKey(dataTable);
        const csvPath = path.join(this.outputDir, fileKey + this._fileExtension);

        if (!this._initializedTables.has(fileKey)) {
            this._initializedTables.add(fileKey);
            this._rowCounts[fileKey] = 0;
            this._dataTables.set(fileKey, dataTable);

            const columns = dataTable.descriptor.columns;
            const headerRow = [
                ...Object.keys(this._prefixColumns),
                ...columns.map(col => col.displayName),
                ...Object.keys(this._suffixColumns)
            ].map(escapeCsv).join(',');
            // Write metadata comments + header
            const comments = [
                `# @name ${dataTable.descriptor.name}`,
                `# @instanceName ${dataTable.instanceName}`,
                `# @group ${dataTable.group ?? ''}`,
            ].join('\n');
            this.write(csvPath, comments + '\n' + headerRow + '\n', false);
        }

        const columns = dataTable.descriptor.columns;
        const rowValues = [
            ...Object.values(this._prefixColumns),
            ...columns.map(col => (row as any)[col.name]),
            ...Object.values(this._suffixColumns)
        ].map(escapeCsv);
        this.write(csvPath, rowValues.join(',') + '\n', true);
        this._rowCounts[fileKey]++;
    }

    getRows(_dataTableName: string, _group?: string): any[] {
        // CSV store writes to disk; reading back is not supported
        return [];
    }

    getDataTables(): DataTable<any>[] {
        return Array.from(this._dataTables.values());
    }

    get rowCounts(): { [key: string]: number } {
        return {...this._rowCounts};
    }

    get tableKeys(): string[] {
        return [...this._initializedTables];
    }

    static fileKey(dataTable: DataTable<any>): string {
        const suffix = dataTable.group ?? dataTable.instanceName;
        if (suffix === dataTable.descriptor.name) {
            return dataTable.descriptor.name;
        }
        return `${dataTable.descriptor.name}--${sanitizeScope(suffix)}`;
    }
}
