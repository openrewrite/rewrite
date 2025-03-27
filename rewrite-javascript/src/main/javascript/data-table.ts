import {ExecutionContext} from "./execution";
import {OptionDescriptor, Recipe} from "./recipe";

const DATA_TABLES_KEY = "org.openrewrite.dataTables";
const COLUMNS_KEY = Symbol("org.openrewrite.dataTables.columns");

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

export class DataTable<Row> {
    private readonly _descriptor: DataTableDescriptor

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

    insertRow(ctx: ExecutionContext, row: Row): void {
        if (!ctx[DATA_TABLES_KEY]) {
            ctx[DATA_TABLES_KEY] = {};
        }
        if (!ctx[DATA_TABLES_KEY][this._descriptor.name]) {
            ctx[DATA_TABLES_KEY][this._descriptor.name] = [];
        }
        ctx[DATA_TABLES_KEY][this._descriptor.name].push(row);
    }
}

export function getRowsByDataTableName(ctx: ExecutionContext): [string, any[]][] {
    if (!(DATA_TABLES_KEY in ctx)) {
        return [];
    }
    return Object.entries(ctx[DATA_TABLES_KEY]);
}

export function getRows<Row>(dataTableName: string, ctx: ExecutionContext): Row[] {
    return ctx[DATA_TABLES_KEY]?.[dataTableName] ?? [];
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
