import {AfterRecipe, dedentAfter, SourceSpec} from "../test";
import {PlainText, PlainTextKind, PlainTextParser} from ".";

export function text(before: string): SourceSpec<PlainText>;
export function text(before: string | undefined, after: AfterRecipe): SourceSpec<PlainText>;
export function text(before: string | undefined, spec: (spec: SourceSpec<PlainText>) => void): SourceSpec<PlainText>;
export function text(before: string | undefined, after: AfterRecipe, spec: (spec: SourceSpec<PlainText>) => void): SourceSpec<PlainText>;
export function text(
    before: string | undefined,
    arg2?: AfterRecipe | ((spec: SourceSpec<PlainText>) => void),
    arg3?: (spec: SourceSpec<PlainText>) => void
): SourceSpec<PlainText> {
    let after: AfterRecipe | undefined;
    let spec: ((spec: SourceSpec<PlainText>) => void) | undefined;
    if (typeof arg2 === "function") {
        // Called with before and spec.
        spec = arg2;
    } else {
        // Called with before and after (or all three).
        after = arg2;
        spec = arg3;
    }
    const s: SourceSpec<PlainText> = {
        kind: PlainTextKind.PlainText,
        before: before,
        after: dedentAfter(after),
        parser: () => new PlainTextParser()
    };
    if (spec) {
        spec(s);
    }
    return s;
}
