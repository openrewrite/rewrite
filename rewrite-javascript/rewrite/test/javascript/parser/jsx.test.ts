// noinspection JSUnusedLocalSymbols

import {describe} from "@jest/globals";
import {tsx} from "../../../src/javascript";
import {RecipeSpec} from "../../../src/test";

describe("jsx mapping", () => {
    const spec = new RecipeSpec();

    test("jsx with comments", () =>
        spec.rewriteRun(
            //language=jsx
            tsx("/*a*/<div /*b*/></div>/*d*/"),
        ));

    test("jsx text", () => {
        spec.rewriteRun(
            //language=jsx
            tsx("<div>hello world</div>"),
        )
    });

    test("jsx with attributes", () =>
        spec.rewriteRun(
            //language=jsx
            tsx("<div className=\"foo\" id=\"bar\" />")
        ));

    test("jsx with children", () =>
        spec.rewriteRun(
            //language=jsx
            tsx("<div><span>Hello</span></div>")
        ));

    test("jsx with attributes and children", () =>
        spec.rewriteRun(
            //language=jsx
            tsx("<div className=\"foo\"><span>Hello</span></div>")
        ));

    // noinspection HtmlUnknownTarget
    test("jsx with self-closing tag", () =>
        spec.rewriteRun(
            //language=jsx
            tsx("<img src=\"foo.png\" alt=\"Foo\" />")
        ));

    test("fragment shorthand", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`
                <>
                    <Item/>
                </>
            `)
        ));

    test("jsx with nested elements", () =>
        spec.rewriteRun(
            //language=jsx
            tsx(`
                const items = ["Item 1", "Item 2", "Item 3"];
                const list = (
                    <ul>
                        {items.map((item, index) => (
                            <li key={index}>{item}</li>
                        ))}
                    </ul>
                );
            `)
        )
    );
});
