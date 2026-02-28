// noinspection JSUnusedLocalSymbols,TypeScriptUnresolvedReference,TypeScriptMissingConfigOption,NpmUsedModulesInstalled,TypeScriptJSXUnresolvedComponent

import {describe} from "@jest/globals";
import {JavaScriptVisitor, JSX, npm, packageJson, tsx} from "../../../src/javascript";
import {RecipeSpec} from "../../../src/test";
import {J, Type} from "../../../src/java";
import {withDir} from "tmp-promise";

describe("jsx mapping", () => {
    const spec = new RecipeSpec();

    test("react component supertype", async () => {
        await withDir(async repo => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {
                        ...tsx(
                            `
                                import Select from 'react-select';
                                
                                const App = () => {
                                  return <Select options={[]} />;
                                };
                            `
                        ),
                        afterRecipe: async cu => {
                            await (new class extends JavaScriptVisitor<any> {
                                protected async visitJsxTag(tag: JSX.Tag, _: any): Promise<J | undefined> {
                                    const ident = tag.openName as J.Identifier;
                                    expect(Type.isClass(ident.type)).toBeTruthy();
                                    expect((ident.type as Type.Class).supertype?.fullyQualifiedName).toContain('Component');
                                    return tag;
                                }
                            }).visit(cu, 0);
                        }
                    },
                    //language=json
                    packageJson(
                        `
                          {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                              "react-select": "^2.4.4"
                            },
                            "devDependencies": {
                              "@types/react-select": "^2.0.0"
                            }
                          }
                        `
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("imported react functional component", async () => {
        await withDir(async repo => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {
                        ...tsx(
                            //language=tsx
                            `
                                import type { JSX } from 'react';
                                export const Button = (): JSX.Element => <button>Button</button>;
                            `
                        ),
                        path: 'components/Button.tsx',
                    },
                    {
                        ...tsx(
                            //language=tsx
                            `
                                import {Button} from './components/Button';
                                export {Button};
                            `
                        ),
                        afterRecipe: async cu => {
                            let foundButton = false;
                            await (new class extends JavaScriptVisitor<any> {
                                async visitIdentifier(identifier: J.Identifier, _: any): Promise<J | undefined> {
                                    if (identifier.simpleName === 'Button' && identifier.type) {
                                        foundButton = true;
                                        // Assert that imported Button has function type
                                        expect(Type.isFunctionType(identifier.type)).toBeTruthy();
                                        const funcType = identifier.type as Type.Class;
                                        expect(funcType.fullyQualifiedName).toBe('𝑓');

                                        // Check it has the correct structure
                                        expect(funcType.typeParameters.length).toBe(1); // R only (no params)
                                        const returnTypeParam = funcType.typeParameters[0] as Type.GenericTypeVariable;
                                        expect(returnTypeParam.name).toBe('R');
                                        expect(returnTypeParam.variance).toBe(Type.GenericTypeVariable.Variance.Covariant);

                                        // Check apply method
                                        expect(funcType.methods.length).toBe(1);
                                        const applyMethod = funcType.methods[0];
                                        expect(applyMethod.name).toBe('apply');
                                        expect(applyMethod.parameterTypes.length).toBe(0); // No parameters
                                    }
                                    return identifier;
                                }
                            }).visit(cu, 0);
                            expect(foundButton).toBeTruthy();
                        }
                    },
                    //language=json
                    packageJson(
                        `
                          {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                              "react": "^18.3.1",
                              "react-dom": "^18.3.1"
                            },
                            "devDependencies": {
                              "@types/react": "^18.3.5",
                              "@types/react-dom": "^18.3.0",
                              "typescript": "^5.6.3"
                            }
                          }
                        `
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

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
            tsx(`<Component<string, number> prop="value"/>`)
        )
    );

    test("jsx element with generic type arguments and children", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`
                <Container<ItemType>>
                    <Item/>
                </Container>
            `)
        )
    );

    test("jsx self-closing element with complex generic types", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<DataTable<{ id: number; name: string }> data={items}/>`)
        )
    );

    test("jsx element with nested generics", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<List<Array<Record<string, any>>> items={data}/>`)
        )
    );

    test("jsx element with union type generic", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<Component<string | number> props={data}/>`)
        )
    );

    test("jsx element with member expression and generics", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`<Components.Table<UserData> columns={columns}/>`)
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

    test("jsx with space after opening angle bracket", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`< ArrowUpRight className='h-3 w-3' />`)
        ));

    test("jsx with spread children", () =>
        spec.rewriteRun(
            //language=tsx
            tsx(`
                const groups = { a: [1, 2], b: [3, 4] };
                <div>
                    {...Object.entries(groups).map(([key, value]) => (
                        <span key={key}>
                            {...value.map(v => <i>{v}</i>)}
                        </span>
                    ))}
                </div>
            `)
        ));
});
