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
    private readonly _initializedTables = new Set<string>();
    private readonly _rowCounts: { [key: string]: number } = {};
    private readonly _dataTables = new Map<string, DataTable<any>>();

    constructor(private readonly outputDir: string) {
        fs.mkdirSync(outputDir, {recursive: true});
    }

    insertRow<Row>(dataTable: DataTable<Row>, _ctx: ExecutionContext, row: Row): void {
        const fileKey = CsvDataTableStore.fileKey(dataTable);
        const csvPath = path.join(this.outputDir, fileKey + '.csv');

        if (!this._initializedTables.has(fileKey)) {
            this._initializedTables.add(fileKey);
            this._rowCounts[fileKey] = 0;
            this._dataTables.set(fileKey, dataTable);

            const columns = dataTable.descriptor.columns;
            const headerRow = columns.map(col => escapeCsv(col.displayName)).join(',');
            // Write metadata comments + header
            const comments = [
                `# @name ${dataTable.descriptor.name}`,
                `# @instanceName ${dataTable.instanceName}`,
                `# @group ${dataTable.group ?? ''}`,
            ].join('\n');
            fs.writeFileSync(csvPath, comments + '\n' + headerRow + '\n');
        }

        const columns = dataTable.descriptor.columns;
        const rowValues = columns.map(col => {
            const value = (row as any)[col.name];
            return escapeCsv(value);
        });
        fs.appendFileSync(csvPath, rowValues.join(',') + '\n');
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
