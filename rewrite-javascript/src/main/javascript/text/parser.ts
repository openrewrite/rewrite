import {Parser, ParserInput, readSourceSync} from "../parser";
import {PlainText, PlainTextKind} from "./tree";
import {randomId} from "../uuid";
import {emptyMarkers} from "../markers";

export class PlainTextParser extends Parser<PlainText> {
    async parse(...sourcePaths: ParserInput[]): Promise<PlainText[]> {
        return sourcePaths.map(sourcePath => ({
            kind: PlainTextKind.PlainText,
            id: randomId(),
            markers: emptyMarkers,
            sourcePath: this.relativePath(sourcePath),
            text: readSourceSync(sourcePath),
            snippets: []
        }))
    }
}
