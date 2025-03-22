import {ExecutionContext} from "../execution";
import {Parser, readSourceSync} from "../parser";
import {PlainText, PlainTextKind} from "./tree";
import {randomId} from "../uuid";
import {emptyMarkers} from "../markers";
import {relative} from "path";

export class PlainTextParser extends Parser {

    parse(ctx: ExecutionContext, relativeTo?: string, ...sourcePaths: string[]): PlainText[] {
        return sourcePaths.map(sourcePath => ({
            kind: PlainTextKind.PlainText,
            id: randomId(),
            markers: emptyMarkers,
            sourcePath: relative(relativeTo || "", sourcePath),
            text: readSourceSync(ctx, sourcePath),
            snippets: []
        }))
    }

}
