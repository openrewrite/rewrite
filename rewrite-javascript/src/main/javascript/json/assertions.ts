import {AfterRecipe, dedentAfter, SourceSpec} from "../test";
import {JsonParser} from "./parser";
import {JsonDocument, JsonKind} from "./tree";

export function json(before: string): SourceSpec<JsonDocument>;
export function json(before: string | undefined, after: AfterRecipe): SourceSpec<JsonDocument>;
export function json(before: string | undefined, spec: (spec: SourceSpec<JsonDocument>) => void): SourceSpec<JsonDocument>;
export function json(before: string | undefined, after: AfterRecipe, spec: (spec: SourceSpec<JsonDocument>) => void): SourceSpec<JsonDocument>;
export function json(
    before: string | undefined,
    arg2?: AfterRecipe | ((spec: SourceSpec<JsonDocument>) => void),
    arg3?: (spec: SourceSpec<JsonDocument>) => void
): SourceSpec<JsonDocument> {
    let after: AfterRecipe | undefined;
    let spec: ((spec: SourceSpec<JsonDocument>) => void) | undefined;
    if (typeof arg2 === "function") {
        // Called with before and spec.
        spec = arg2;
    } else {
        // Called with before and after (or all three).
        after = arg2;
        spec = arg3;
    }
    const s: SourceSpec<JsonDocument> = {
        kind: JsonKind.Document,
        before: before,
        after: dedentAfter(after),
        parser: () => new JsonParser()
    };
    if (spec) {
        spec(s);
    }
    return s;
}
