import {AfterRecipe, dedentAfter, SourceSpec} from "../test";
import {memfs} from "memfs";
import {ExecutionContext} from "../execution";
import {PARSER_VOLUME} from "../parser";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import dedent from "dedent";
import {PlainTextParser} from ".";

export function text(
    before: string,
    after?: AfterRecipe,
    spec?: (spec: SourceSpec) => void
): SourceSpec {
    const sourcePath = `${SnowflakeId().generate()}.txt`;
    const vol = memfs().vol
    vol.mkdirSync(process.cwd(), {recursive: true});
    vol.writeFileSync(`${process.cwd()}/${sourcePath}`, dedent(before));
    const s: SourceSpec = {
        before: sourcePath,
        after: dedentAfter(after),
        parser: () => new PlainTextParser(),
        executionContext: new Map([[PARSER_VOLUME, vol]]) as ExecutionContext
    };
    if (spec) {
        spec(s);
    }
    return s;
}
