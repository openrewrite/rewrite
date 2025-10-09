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
import * as rpc from "vscode-jsonrpc/node";
import {Cursor, isSourceFile, SourceFile} from "../../tree";

const CSV_HEADER = 'request,target,durationMs,memoryUsedBytes,memoryMaxBytes';

/**
 * Extracts the sourcePath from a tree object, either directly from a SourceFile
 * or by finding the nearest SourceFile via a cursor.
 *
 * @param tree The tree object to extract sourcePath from
 * @param cursor Optional cursor to find the nearest SourceFile
 * @returns The sourcePath or empty string if not found
 */
export function extractSourcePath(tree: any, cursor?: Cursor): string {
    if (isSourceFile(tree)) {
        return (tree as SourceFile).sourcePath || '';
    }

    if (cursor) {
        const sourceFile = cursor.firstEnclosing(t => isSourceFile(t));
        if (sourceFile) {
            return (sourceFile as SourceFile).sourcePath || '';
        }
    }

    return '';
}

/**
 * Initializes the metrics CSV file with a header if it doesn't already exist or is empty.
 * If the file already contains the correct header, this function is a no-op.
 *
 * @param metricsCsv The path to the CSV file for recording metrics
 * @param logger Optional logger for warnings
 */
export function initializeMetricsCsv(metricsCsv?: string, logger?: rpc.Logger): void {
    if (!metricsCsv) {
        return;
    }

    try {
        // Check if file exists and has content
        if (fs.existsSync(metricsCsv)) {
            const content = fs.readFileSync(metricsCsv, 'utf8');
            const firstLine = content.split('\n')[0];

            // If file already has the correct header, skip initialization
            if (firstLine.trim() === CSV_HEADER) {
                return;
            }

            // File exists but has incorrect header - warn and reset
            if (firstLine.trim()) {
                logger?.warn(`Metrics CSV file ${metricsCsv} has incorrect header. Expected '${CSV_HEADER}' but found '${firstLine.trim()}'. Resetting file.`);
            }
        }

        // Write header to new or empty file (overwrites existing file with incorrect header)
        fs.writeFileSync(metricsCsv, CSV_HEADER + '\n');
    } catch (err) {
        console.error('Failed to initialize metrics CSV:', err);
    }
}

/**
 * Internal function to wrap a handler with metrics recording.
 */
async function wrapWithMetrics<R>(
    handler: () => Promise<R>,
    target: { target: string },
    request: string,
    metricsCsv?: string
): Promise<R> {
    if (!metricsCsv) {
        // No metrics recording requested, just execute the handler
        return handler();
    }

    const startTime = Date.now();

    try {
        const result = await handler();
        recordMetrics(metricsCsv, target.target, request, startTime);
        return result;
    } catch (error) {
        recordMetrics(metricsCsv, target.target, request, startTime);
        throw error;
    }
}

/**
 * Wraps an RPC request handler to record performance metrics to a CSV file.
 *
 * @param request The request type name (e.g., "Visit", "GetObject")
 * @param target A mutable object containing the target identifier for metrics
 * @param metricsCsv Optional path to the CSV file for recording metrics
 * @returns A function that wraps a request handler with metrics recording
 */
export function withMetrics<P, R>(
    request: string,
    target: { target: string },
    metricsCsv?: string
): (handler: (request: P) => Promise<R>) => (request: P) => Promise<R> {
    return (handler: (requestParam: P) => Promise<R>) => {
        return async (requestParam: P): Promise<R> => {
            return wrapWithMetrics(() => handler(requestParam), target, request, metricsCsv);
        };
    };
}

/**
 * Wraps an RPC request handler without parameters (RequestType0) to record performance metrics.
 *
 * @param request The request type name (e.g., "GetLanguages", "GetRecipes")
 * @param target A mutable object containing the target identifier for metrics
 * @param metricsCsv Optional path to the CSV file for recording metrics
 * @returns A function that wraps a request handler with metrics recording
 */
export function withMetrics0<R>(
    request: string,
    target: { target: string },
    metricsCsv?: string
): (handler: () => Promise<R>) => (token: any) => Promise<R> {
    return (handler: () => Promise<R>) => {
        return async (_: any): Promise<R> => {
            return wrapWithMetrics(handler, target, request, metricsCsv);
        };
    };
}

function recordMetrics(
    metricsCsv: string,
    target: string,
    request: string,
    startTime: number
): void {
    const endTime = Date.now();
    const memEnd = process.memoryUsage();
    const durationMs = endTime - startTime;

    const memoryUsedBytes = memEnd.heapUsed;
    const memoryMaxBytes = memEnd.heapTotal;

    const csvRow = `${request},${target},${durationMs},${memoryUsedBytes},${memoryMaxBytes}\n`;

    try {
        fs.appendFileSync(metricsCsv, csvRow);
    } catch (err) {
        console.error('Failed to write metrics to CSV:', err);
    }
}
