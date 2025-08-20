// noinspection JSUnusedLocalSymbols

import {describe} from "@jest/globals";
import {tsx} from "../../../src/javascript";
import {RecipeSpec} from "../../../src/test";

describe("jsx mapping", () => {
    const spec = new RecipeSpec();

    // noinspection TypeScriptMissingConfigOption
    test("jsx with comments", () =>
        spec.rewriteRun(
            //language=jsx
            tsx("/*a*/<div /*b*/></div >/*d*/"),
        ));

    test("jsx namespace element", () => {
        spec.rewriteRun(
            //language=jsx
            tsx("<foo:bar />"),
        )
    });


    test("jsx member expression", () => {
        spec.rewriteRun(
            //language=jsx
            // noinspection JSXUnresolvedComponent
            tsx("<Foo.Bar />"),
        )
    });

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

    // noinspection HtmlUnknownAttribute
    test("jsx with namespaced attribute", () =>
        spec.rewriteRun(
            //language=jsx
            tsx("<div aria:foo=\"bar\"><span>Hello</span></div>")
        ));

    // noinspection HtmlUnknownTarget
    test("jsx with self-closing tag", () =>
        spec.rewriteRun(
            //language=jsx
            tsx("<img src=\"foo.png\" alt=\"Foo\" />")
        ));

    // noinspection TypeScriptMissingConfigOption,TypeScriptJSXUnresolvedComponent
    test("fragment shorthand", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`
                <>
                    <Item/>
                </>
            `)
        ));

    // noinspection TypeScriptMissingConfigOption
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

    test("jsx element with single generic type argument", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<SeeHowToRunGraphQL<DevCenterQueryVariables> />`)
        )
    );

    test("jsx element with multiple generic type arguments", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<Component<string, number> prop="value" />`)
        )
    );

    test("jsx element with generic type arguments and children", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`
                <Container<ItemType>>
                    <Item />
                </Container>
            `)
        )
    );

    test("jsx self-closing element with complex generic types", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<DataTable<{ id: number; name: string }> data={items} />`)
        )
    );

    test("jsx element with nested generics", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<List<Array<Record<string, any>>> items={data} />`)
        )
    );

    test("jsx element with union type generic", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<Component<string | number> props={data} />`)
        )
    );

    test("jsx element with member expression and generics", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<Components.Table<UserData> columns={columns} />`)
        )
    );

    test("nesting angle brackets with newline break", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(
         `
                <Ruud>
                    <MenuItemWithOverlay<DrawerProps>
                    >
                        <span>You didn't expect it, did you?</span>
                    </MenuItemWithOverlay>
                    <AnotherTag<Param>
                        slots={{
                            container: Drawer
                        }}
                    >
                    </AnotherTag>
                </Ruud>
                `
        )
    ));
});
