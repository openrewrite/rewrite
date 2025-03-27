import {AfterRecipe, dedentAfter, SourceSpec} from "../test";
import {JsonParser} from "./parser";
import {JsonDocument, JsonKind} from "./tree";

export function json(before: string | undefined, after?: AfterRecipe): SourceSpec<JsonDocument> {
    return {
        kind: JsonKind.Document,
        before: before,
        after: dedentAfter(after),
        parser: () => new JsonParser()
    };
}
