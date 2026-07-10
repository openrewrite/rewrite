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
import {ExecutionContext} from "./execution";
import {OptionDescriptor} from "./recipe";

export const DATA_TABLE_STORE = Symbol.for("org.openrewrite.dataTables.store");
const COLUMNS_KEY = Symbol.for("org.openrewrite.dataTables.columns");

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

/**
 * Sanitize a value for a filename, byte-identical to the Java host's
 * {@code CsvDataTableStore.sanitize} so a shared data table resolves to the same file.
 */
export function sanitizeScope(scope: string): string {
    // 1. lowercase
    let s = scope.toLowerCase();
    // \p{L}\p{Nd} mirrors Java's Character.isLetterOrDigit, for cross-runtime parity
    s = s.replace(/[^\p{L}\p{Nd}]/gu, '-');
    s = s.replace(/-+/g, '-').replace(/^-|-$/g, '');
    if (s.length > 30) {
        s = s.substring(0, 30);
        const lastDash = s.lastIndexOf('-');
        if (lastDash > 0) {
            s = s.substring(0, lastDash);
        }
    }
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
            ctx.messages[DATA_TABLE_STORE] = new InMemoryDataTableStore();
        }
        const dataTableStore: DataTableStore = ctx.messages[DATA_TABLE_STORE];
        dataTableStore.insertRow(this, ctx, row);
    }
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
 * A DataTableStore that writes rows directly to CSV files as they are inserted.
 * Uses the data table's file-safe key for filenames.
 */
export class CsvDataTableStore implements DataTableStore {
    private readonly _rowCounts: { [key: string]: number } = {};
    private readonly _dataTables = new Map<string, DataTable<any>>();

    constructor(private readonly outputDir: string,
                private readonly prefixColumns: { [key: string]: string } = {},
                private readonly suffixColumns: { [key: string]: string } = {}) {
        fs.mkdirSync(outputDir, {recursive: true});
    }

    insertRow<Row>(dataTable: DataTable<Row>, _ctx: ExecutionContext, row: Row): void {
        const fileKey = CsvDataTableStore.fileKey(dataTable);
        const csvPath = path.join(this.outputDir, fileKey + '.csv');
        this._dataTables.set(fileKey, dataTable);

        const columns = dataTable.descriptor.columns;

        // Key the header on file existence, not in-memory state, so writers sharing one file
        // (Java host + RPC runtimes) emit it once; column names (not display names) match the Java writer.
        if (!fs.existsSync(csvPath)) {
            const header = [
                ...Object.keys(this.prefixColumns),
                ...columns.map(col => col.name),
                ...Object.keys(this.suffixColumns),
            ].map(escapeCsv).join(',');
            const comments = [
                `# @name ${dataTable.descriptor.name}`,
                `# @instanceName ${dataTable.instanceName}`,
                `# @group ${dataTable.group ?? ''}`,
            ].join('\n');
            fs.writeFileSync(csvPath, comments + '\n' + header + '\n');
        }

        const rowValues = [
            ...Object.values(this.prefixColumns),
            ...columns.map(col => (row as any)[col.name]),
            ...Object.values(this.suffixColumns),
        ].map(escapeCsv);
        fs.appendFileSync(csvPath, rowValues.join(',') + '\n');
        this._rowCounts[fileKey] = (this._rowCounts[fileKey] ?? 0) + 1;
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
        return Array.from(this._dataTables.keys());
    }

    /**
     * Filesystem-safe key for a data table; mirrors the Java host's
     * {@code CsvDataTableStore.fileKey} so a shared table resolves to the same filename.
     */
    static fileKey(dataTable: DataTable<any>): string {
        const name = dataTable.descriptor.name;
        const group = dataTable.group;
        if (group != null) {
            if (group === name) {
                return name;
            }
            return `${name}--${sanitizeScope(group)}`;
        }
        // Suffix only when instanceName was customized (differs from the display name).
        const instanceName = dataTable.instanceName;
        if (instanceName === dataTable.descriptor.displayName) {
            return name;
        }
        return `${name}--${sanitizeScope(instanceName)}`;
    }
}
