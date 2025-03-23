import {describe} from "@jest/globals";
import {createExecutionContext, ExecutionContext, ParserSourceReader, readSourceSync} from "../../main/javascript";
import {Volume} from "memfs";


describe("parse source reader utility", () => {
    const vol = Volume.fromJSON({"./source.json": `  { "type": "object" }`});
    const ctx: ExecutionContext = createExecutionContext();

    test("whitespace", () => {
        const reader = new ParserSourceReader("source.json", ctx);
        expect(reader.whitespace()).toEqual("  ");
        expect(reader.cursor).toEqual(2);
    })

    test("source before a token", () => {
        const reader = new ParserSourceReader("source.json", ctx);
        expect(reader.sourceBefore("{")).toEqual("  ");
        expect(reader.cursor).toEqual(3);
    });

    test("read in memory source file", () => {
        console.log(vol.toTree());
        expect(readSourceSync(ctx, "source.json")).toEqual(`  { "type": "object" }`);
    });
});
