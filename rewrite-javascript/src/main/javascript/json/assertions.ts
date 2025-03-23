import {AfterRecipe, dedentAfter, SourceSpec} from "../test";
import {JsonParser} from "./parser";
import {memfs} from "memfs";
import {createExecutionContext, ExecutionContext, PARSER_VOLUME} from "../";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import dedent from "dedent";
import {JsonDocument} from "./tree";

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

    let sourcePath = undefined;
    let executionContext = createExecutionContext();
    if (before) {
        const sourcePath = `${SnowflakeId().generate()}.json`;
        const vol = memfs().vol
        vol.mkdirSync(process.cwd(), {recursive: true});
        vol.writeFileSync(`${process.cwd()}/${sourcePath}`, dedent(before));
        executionContext[PARSER_VOLUME] = vol;
    }
    const s: SourceSpec<JsonDocument> = {
        before: sourcePath,
        after: dedentAfter(after),
        parser: () => new JsonParser(),
        executionContext: executionContext
    };
    if (spec) {
        spec(s);
    }
    return s;
}
