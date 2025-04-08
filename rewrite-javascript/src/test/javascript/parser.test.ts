import {describe} from "@jest/globals";
import {ParserSourceReader, readSourceSync} from "../../main/javascript";


describe("parse source reader utility", () => {
    const sourceJson = {text: `  { "type": "object" }`, sourcePath: "source.json"};

    test("whitespace", () => {
        const reader = new ParserSourceReader(sourceJson);
        expect(reader.whitespace()).toEqual("  ");
        expect(reader.cursor).toEqual(2);
    })

    test("source before a token", () => {
        const reader = new ParserSourceReader(sourceJson);
        expect(reader.sourceBefore("{")).toEqual("  ");
        expect(reader.cursor).toEqual(3);
    });

    test("read in memory source file", () => {
        expect(readSourceSync(sourceJson)).toEqual(`  { "type": "object" }`);
    });
});
