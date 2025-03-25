import {ExecutionContext} from "./execution";
import {SourceFile} from "./tree";
import {readFileSync} from "node:fs";
import {Volume} from "memfs/lib/volume";

export const PARSER_VOLUME = Symbol("PARSER_VOLUME");

export abstract class Parser {
    abstract parse(ctx: ExecutionContext, relativeTo?: string, ...sourcePaths: string[]): SourceFile[]
}

/**
 * A source reader that can be used by parsers to incrementally parse a source, keeping
 * track of the position of the parsing.
 */
export class ParserSourceReader {
    readonly source: string
    cursor: number = 0;

    constructor(public readonly sourcePath: string, ctx: ExecutionContext) {
        this.source = readSourceSync(ctx, sourcePath)
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

export function readSourceSync(ctx: ExecutionContext, sourcePath: string) {
    const vol = ctx.get(PARSER_VOLUME) as Volume | undefined;
    return (vol?.readFileSync(sourcePath) ?? readFileSync(sourcePath)).toString();
}
