import {AfterRecipe, dedentAfter, SourceSpec} from "../test";
import {memfs} from "memfs";
import {createExecutionContext, PARSER_VOLUME} from "../";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import dedent from "dedent";
import {PlainText, PlainTextParser} from ".";

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

    let sourcePath = undefined;
    let executionContext = createExecutionContext();
    if (before) {
        const sourcePath = `${SnowflakeId().generate()}.txt`;
        const vol = memfs().vol
        vol.mkdirSync(process.cwd(), {recursive: true});
        vol.writeFileSync(`${process.cwd()}/${sourcePath}`, dedent(before));
        executionContext[PARSER_VOLUME] = vol;
    }
    const s: SourceSpec<PlainText> = {
        before: sourcePath,
        after: dedentAfter(after),
        parser: () => new PlainTextParser(),
        executionContext: executionContext
    };
    if (spec) {
        spec(s);
    }
    return s;
}
