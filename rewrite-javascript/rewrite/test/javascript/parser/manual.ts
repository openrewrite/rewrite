import {JavaScriptParser} from "../../../dist/src/javascript";
import {InMemoryExecutionContext, isParseError, ParseExceptionResult, ParserInput} from "../../../dist/src/core";
import fs from "fs";

const path = "/Users/knut/git/emilkowalski/vaul/src/index.tsx";

const parser = JavaScriptParser.builder().build();
const sourceFile = Array.from(parser.parseInputs([new ParserInput(path, null, false, () => fs.readFileSync(path))], null, new InMemoryExecutionContext()))[0];
if (isParseError(sourceFile)) {
    console.log(sourceFile.markers.findFirst(ParseExceptionResult)!.message);
}
