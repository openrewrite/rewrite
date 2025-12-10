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
import {ExecutionContext} from "./execution";
import {SourceFile} from "./tree";
import fs, {readFileSync} from "node:fs";
import {isAbsolute, relative} from "path";
import {ParseError, ParseErrorKind} from "./parse-error";
import {markers, MarkersKind, ParseExceptionResult} from "./markers";
import {randomId} from "./uuid";

export type SourcePath = string
export type ParserInput = SourcePath | { text: string, sourcePath: string }

export function parserInputRead(input: ParserInput): string {
    if (typeof input === "object") {
        return input.text;
    }
    return fs.readFileSync(input).toString()
}

export function parserInputFile(input: ParserInput): string {
    if (typeof input === "object") {
        return input.sourcePath;
    }
    return input;
}

export interface ParserOptions {
    ctx?: ExecutionContext;
    relativeTo?: string;
}

export abstract class Parser {
    constructor({
                    ctx = new ExecutionContext(),
                    relativeTo
                }: ParserOptions = {}) {
        this.ctx = ctx;
        this.relativeTo = relativeTo;
    }

    protected ctx: ExecutionContext;
    protected readonly relativeTo?: string;

    abstract parse(...sourcePaths: ParserInput[]): AsyncGenerator<SourceFile>

    protected relativePath(sourcePath: ParserInput): string {
        const path = typeof sourcePath === "string" ? sourcePath : sourcePath.sourcePath;
        return isAbsolute(path) && this.relativeTo ? relative(this.relativeTo, path) : path;
    }

    protected error(input: ParserInput, e: Error): ParseError {
        return {
            kind: ParseErrorKind,
            id: randomId(),
            markers: markers({
                kind: MarkersKind.ParseExceptionResult,
                id: randomId(),
                parserType: this.constructor.name,
                exceptionType: e.name,
                message: e.message + ':\n' + e.stack,
            } satisfies ParseExceptionResult as ParseExceptionResult),
            text: parserInputRead(input),
            sourcePath: this.relativePath(input),
        }
    }
}

/**
 * A source reader that can be used by parsers to incrementally parse a source, keeping
 * track of the position of the parsing.
 */
export class ParserSourceReader {
    readonly source: string
    cursor: number = 0;

    constructor(public readonly sourcePath: ParserInput) {
        this.source = readSourceSync(sourcePath)
    }

    whitespace(): string {
        function isWhitespace(char: string): boolean {
            const code = char.charCodeAt(0);
            return code === 32 || (code >= 9 && code <= 13);
        }

        const start = this.cursor;
        while (this.cursor < this.source.length && isWhitespace(this.source[this.cursor])) {
            this.cursor++;
        }
        return this.source.slice(start, this.cursor);
    }

    sourceBefore(token: string): string {
        const start = this.cursor;
        // increment cursor until we find the token
        while (this.cursor < this.source.length && !this.source.startsWith(token, this.cursor++)) {
        }
        return this.source.slice(start, this.cursor - token.length)
    }

    /**
     * Used in debugging parsers during development to see where the
     * cursor is in the source.
     */
    // noinspection JSUnusedGlobalSymbols
    get afterCursor(): string {
        return this.source.slice(this.cursor);
    }
}

export function readSourceSync(sourcePath: ParserInput) {
    if (typeof sourcePath === "string") {
        return readFileSync(sourcePath).toString();
    }
    return sourcePath.text;
}

type ParserConstructor<T extends Parser> = new (options?: ParserOptions) => T;

export type ParserType = "javascript" | "packageJson";

export class Parsers {
    private static registry = new Map<ParserType, ParserConstructor<Parser>>();

    static registerParser<T extends Parser>(
        name: ParserType,
        parserClass: ParserConstructor<T>
    ): void {
        Parsers.registry.set(name, parserClass as ParserConstructor<Parser>);
    }

    static createParser(name: ParserType, options?: ParserOptions): Parser {
        const ParserClass = Parsers.registry.get(name);
        if (!ParserClass) {
            throw new Error(`No parser registered with name: ${name}`);
        }
        return new ParserClass(options);
    }
}
