import {AfterRecipe, dedentAfter, SourceSpec} from "../test";
import {PlainText, PlainTextKind, PlainTextParser} from ".";

export function text(before: string | null, after?: AfterRecipe): SourceSpec<PlainText> {
    return {
        kind: PlainTextKind.PlainText,
        before: before,
        after: dedentAfter(after),
        parser: () => new PlainTextParser()
    };
}
