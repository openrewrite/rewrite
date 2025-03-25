import {ReplacedText} from "./example-recipe";

describe("data tables", () => {

    test("data table descriptor", () => {
        const descriptor = ReplacedText.dataTable.descriptor;
        expect(descriptor).toEqual({
            name: "org.openrewrite.text.replaced-text",
            displayName: "Replaced text",
            description: "Text that was replaced.",
            columns: [
                {
                    name: "sourcePath",
                    displayName: "Source Path",
                    description: "The path of the file that was changed.",
                },
                {
                    name: "text",
                    displayName: "Text",
                    description: "The text that was replaced.",
                }
            ],
        });
    });
});
