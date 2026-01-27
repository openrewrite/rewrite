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
import {ExecutionContext} from "./execution";
import {OptionDescriptor} from "./recipe";

export const DATA_TABLE_STORE = Symbol.for("org.openrewrite.dataTables.store");
const COLUMNS_KEY = Symbol.for("org.openrewrite.dataTables.columns");

export function Column(descriptor: ColumnDescriptor) {
    return function (target: any, propertyKey: string) {
        // Ensure the constructor has columns storage.
        if (!target.constructor.hasOwnProperty(COLUMNS_KEY)) {
            Object.defineProperty(target.constructor, COLUMNS_KEY, {
                value: {},
                writable: true,
                configurable: true,
            });
        }

        // Register the option metadata under the property key.
        target.constructor[COLUMNS_KEY][propertyKey] = descriptor;
    }
}

export interface DataTableStore {
    insertRow<Row>(dataTable: DataTable<Row>, ctx: ExecutionContext, row: Row): void

    acceptRows(accept: boolean): void
}

export class InMemoryDataTableStore implements DataTableStore {
    private _acceptRows = false
    private readonly _dataTables: { [dataTable: string]: DataTable<any> } = {}
    private readonly _rows: { [dataTable: string]: any[] } = {}

    insertRow<Row>(dataTable: DataTable<Row>, _ctx: ExecutionContext, row: Row): void {
        if (this._acceptRows) {
            this._dataTables[dataTable.descriptor.name] = dataTable;
            if (!this._rows[dataTable.descriptor.name]) {
                this._rows[dataTable.descriptor.name] = [];
            }
            this._rows[dataTable.descriptor.name].push(row);
        }
    }

    acceptRows(accept: boolean): void {
        this._acceptRows = accept
    }
}

export class DataTable<Row> {
    private readonly _descriptor: DataTableDescriptor

    public constructor(name: string, displayName: string, description: string,
                       private rowConstructor: { [key: string | symbol]: any }) {
        this._descriptor = {
            name,
            displayName,
            description,
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
 * Quotes the value if it contains commas, quotes, or newlines.
 */
function escapeCsv(value: unknown): string {
    if (value === null || value === undefined) {
        return '""';
    }
    const str = String(value);
    // If the value contains comma, quote, or newline, wrap in quotes and escape internal quotes
    if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
        return '"' + str.replace(/"/g, '""') + '"';
    }
    return str;
}

/**
 * A DataTableStore that writes rows directly to CSV files as they are inserted.
 */
export class CsvDataTableStore implements DataTableStore {
    private _acceptRows = false;
    private readonly _initializedTables = new Set<string>();
    private readonly _rowCounts: { [dataTable: string]: number } = {};

    constructor(private readonly outputDir: string) {
        // Ensure output directory exists
        fs.mkdirSync(outputDir, {recursive: true});
    }

    insertRow<Row>(dataTable: DataTable<Row>, _ctx: ExecutionContext, row: Row): void {
        if (!this._acceptRows) {
            return;
        }

        const descriptor = dataTable.descriptor;
        const tableName = descriptor.name;
        const csvPath = path.join(this.outputDir, tableName + '.csv');

        // Write header row on first insert for this table
        if (!this._initializedTables.has(tableName)) {
            this._initializedTables.add(tableName);
            this._rowCounts[tableName] = 0;

            const columns = descriptor.columns;
            const headerRow = columns.map(col => escapeCsv(col.displayName)).join(',');

            fs.writeFileSync(csvPath, headerRow + '\n');
        }

        // Write the data row
        const columns = descriptor.columns;
        const rowValues = columns.map(col => {
            const value = (row as any)[col.name];
            return escapeCsv(value);
        });
        fs.appendFileSync(csvPath, rowValues.join(',') + '\n');
        this._rowCounts[tableName]++;
    }

    acceptRows(accept: boolean): void {
        this._acceptRows = accept;
    }

    /**
     * Get the number of rows written for each data table.
     */
    get rowCounts(): { [dataTable: string]: number } {
        return {...this._rowCounts};
    }

    /**
     * Get the names of all data tables that have been written to.
     */
    get tableNames(): string[] {
        return [...this._initializedTables];
    }
}
