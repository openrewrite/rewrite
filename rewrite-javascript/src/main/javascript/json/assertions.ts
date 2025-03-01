import {AfterRecipe, dedentAfter, SourceSpec} from "../test";
import {JsonParser} from "./parser";
import {memfs} from "memfs";
import {ExecutionContext} from "../execution";
import {PARSER_VOLUME} from "../parser";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import dedent from "dedent";

export function json(
    before: string,
    after?: AfterRecipe,
    spec?: (spec: SourceSpec) => void
): SourceSpec {
    const sourcePath = `${SnowflakeId().generate()}.json`;
    const vol = memfs().vol
    vol.mkdirSync(process.cwd(), {recursive: true});
    vol.writeFileSync(`${process.cwd()}/${sourcePath}`, dedent(before));
    const s: SourceSpec = {
        before: sourcePath,
        after: dedentAfter(after),
        parser: () => new JsonParser(),
        executionContext: new Map([[PARSER_VOLUME, vol]]) as ExecutionContext
    };
    if (spec) {
        spec(s);
    }
    return s;
}
