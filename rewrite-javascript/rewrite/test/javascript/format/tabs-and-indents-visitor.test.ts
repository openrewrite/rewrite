/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {IntelliJ, TabsAndIndentsStyle, TabsAndIndentsVisitor, tsx, typescript} from "../../../src/javascript";
import {Draft, produce} from "immer";
import {Style} from "../../../src";

type StyleCustomizer<T extends Style> = (draft: Draft<T>) => void;

function tabsAndIndents(customizer?: StyleCustomizer<TabsAndIndentsStyle>): TabsAndIndentsStyle {
    return customizer
        ? produce(IntelliJ.TypeScript.tabsAndIndents(), draft => customizer(draft))
        : IntelliJ.TypeScript.tabsAndIndents();
}

describe('TabsAndIndentsVisitor', () => {

    test('simple', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                class A {
                x = 3;
                m() {
                const z = 5;
                }
                }
                `,
                `
                class A {
                    x = 3;
                    m() {
                        const z = 5;
                    }
                }
                `)
            // @formatter:on
        )
    });

    test('indent', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                class Person {
                name: string;
                age: number;

                constructor(name: string, age: number) {
                this.name = name;
                this.age = age;
                }

                greet(): void {
                console.log("Hi, I'm " + this.name + " and I'm " + this.age + " years old.");
                }
                }

                const alice = new Person("Alice", 30);
                alice.greet();
                `,
                `
                class Person {
                    name: string;
                    age: number;

                    constructor(name: string, age: number) {
                        this.name = name;
                        this.age = age;
                    }

                    greet(): void {
                        console.log("Hi, I'm " + this.name + " and I'm " + this.age + " years old.");
                    }
                }

                const alice = new Person("Alice", 30);
                alice.greet();
                `)
            // @formatter:on
        )
    });

    test("not so simple", () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                    class K{
                    constructor  ( ){
                    }
                    m ( x :number  ,  y  :  number[] ) :number{
                    this.m( x, [1] );
                    return y[ 0 ];
                    }
                    s ( s: string ):number{
                    switch( s  ){
                    case "apple"  :
                    return 1;
                    }
                    return 0;
                    }
                    }
                    if(   1>0 ){
                    console.log  (   "four"   ,    "three"  ,    "six"   );
                    }
                    let i                                  =   1;
                    while(   i<4   ){
                    i++;
                    }
                    try{
                    throw new Error("test");
                    }catch(  error  ){
                    console.log("Error " + error);
                    }finally{
                    console.log("finally");
                    }
                    const isTypeScriptFun = i > 3?"yes":"hell yeah!";
                    for(   let j=1 ;j<=5 ;j++ ){
                    console.log(\`Number: \` + j);
                    }
                `,
                `
                    class K{
                        constructor  ( ){
                        }
                        m ( x :number  ,  y  :  number[] ) :number{
                            this.m( x, [1] );
                            return y[ 0 ];
                        }
                        s ( s: string ):number{
                            switch( s  ){
                                case "apple"  :
                                    return 1;
                            }
                            return 0;
                        }
                    }
                    if(   1>0 ){
                        console.log  (   "four"   ,    "three"  ,    "six"   );
                    }
                    let i                                  =   1;
                    while(   i<4   ){
                        i++;
                    }
                    try{
                        throw new Error("test");
                    }catch(  error  ){
                        console.log("Error " + error);
                    }finally{
                        console.log("finally");
                    }
                    const isTypeScriptFun = i > 3?"yes":"hell yeah!";
                    for(   let j=1 ;j<=5 ;j++ ){
                        console.log(\`Number: \` + j);
                    }
                `)
            // @formatter:on
        )
    });

    test('lambda', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`console.log(() => "a");`)
            // @formatter:on
        )
    });

    test("indent 5", () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents(draft => {
            draft.indentSize = 5;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                class A {
                x = 3;
                }
                `,
                `
                class A {
                     x = 3;
                }
                `)
            // @formatter:on
        )
    })

    test("type", () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                type Message = {
                [key: string]: any;
                }
                `,
                `
                type Message = {
                    [key: string]: any;
                }
                `)
            // @formatter:on
        )
    })

    test("multi-line callback", () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                [1, 2, 3].forEach( x => {
                console.log(x);
                });
                `,
                `
                [1, 2, 3].forEach( x => {
                    console.log(x);
                });
                `)
            // @formatter:on
        )
    })

    test("single-line callback with braces", () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`[1, 2, 3].forEach(x => {console.log(x)});`)
            // @formatter:on
        )
    })

    test("single-line callback without braces", () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`[1, 2, 3].forEach(x => console.log(x));`)
            // @formatter:on
        )
    })

    test("collapsed if/while", () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
               `
                if (504 == 436)
                console.log("That's practically true!");
                if (407 == 501)
                    console.log("Also true!");
                while (!areWeThereYet())
                 wait();
                if (116 == 119) console.log("Close, but false. No changes");
                function m(): void {
                    if (condition())
                        doSomething();
                     else {
                        doSomethingElse();
                    }
                }
                `,
                `
                if (504 == 436)
                    console.log("That's practically true!");
                if (407 == 501)
                    console.log("Also true!");
                while (!areWeThereYet())
                    wait();
                if (116 == 119) console.log("Close, but false. No changes");
                function m(): void {
                    if (condition())
                        doSomething();
                    else {
                        doSomethingElse();
                    }
                }
                `,
                )
            // @formatter:on
        )
    })

    test('unify indentation', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                const good = 136;
                 const great = 436;
                  const ideal = 504;
                `,
                `
                const good = 136;
                const great = 436;
                const ideal = 504;
                `)
            // @formatter:on
        )
    });

    test('nested if block inside function should get correct indentation', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                function example() {
                    if (true) {
                readFile();
                    }
                }
                `,
                `
                function example() {
                    if (true) {
                        readFile();
                    }
                }
                `)
            // @formatter:on
        )
    });

    test('class inside arrow function block should get correct indentation', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                describe('test', () => {
                class TestClass {
                name: string;
                }
                });
                `,
                `
                describe('test', () => {
                    class TestClass {
                        name: string;
                    }
                });
                `)
            // @formatter:on
        )
    });

    // TabsAndIndentsVisitor doesn't add newlines - it only normalizes existing indentation
    // Single-line empty blocks should remain unchanged
    test('empty arrow function body inside block should remain unchanged (no newlines to normalize)', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `function test() {
    const spy = jest.spyOn(console, 'error').mockImplementation(() => {});
}`
            )
            // @formatter:on
        )
    });

    test('object spread with callback should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `await spec.rewriteRun(
    npm(
        repo.path,
        {
            ...packageJson(\`{}\`), afterRecipe: async (doc) => {
                expect(marker).toBeDefined();
            }
        }
    )
);`
            )
            // @formatter:on
        )
    });

    test('anonymous class inside afterRecipe callback should preserve indentation', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `test("test", async () => spec.rewriteRun({
    ...typescript(\`code\`),
    afterRecipe: async (cu) => {
        await new class extends Visitor {
            protected async visitProperty(prop: Property): Promise<J | undefined> {
                expect(prop.name).toBe('foo');
                return prop;
            }
        }().visit(cu, undefined);
    }
}));`
            )
            // @formatter:on
        )
    });

    test('closing braces after method call should be indented correctly', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `test('name', () => {
    return spec.rewriteRun(
        typescript(\`code\`)
    )
    });`,
                `test('name', () => {
    return spec.rewriteRun(
        typescript(\`code\`)
    )
});`
            )
            // @formatter:on
        )
    })

    test('nested method calls with arrow function should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `await withDir(async (repo) => {
    await spec.rewriteRun(
        npm(
            repo.path,
            packageJson('before', 'after')
        )
    );
}, {unsafeCleanup: true});`
            )
            // @formatter:on
        )
    })

    test('array literal inside method call should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const result = parseRecipeOptions([
    'text=hello',
    'verbose',
    'count=5'
]);`
            )
            // @formatter:on
        )
    })

    test('spread operator in object literal should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `test("name", async () => spec.rewriteRun({
    //language=typescript
    ...typescript('before', 'after'),
}))`
            )
            // @formatter:on
        )
    })

    test('spread with afterRecipe callback should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `test("name", async () => spec.rewriteRun({
    //language=typescript
    ...typescript('before', 'after'),
    afterRecipe: async (cu) => {
        console.log(cu);
    }
}))`
            )
            // @formatter:on
        )
    })

    test('comment before closing paren should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `fn(
    arg1,
    arg2
    // trailing comment
)`
            )
            // @formatter:on
        )
    })

    test('inline comment after last argument should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `typescriptWithSpacesRemoved(
    'const response = await fetch("url");',
    'const response=await fetch("url");'
    // @formatter:on
)`
            )
            // @formatter:on
        )
    })

    test('arrow function with object literal and spread should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
            `
                test('plus', () =>
                    spec.rewriteRun({
                        //language=typescript
                        ...typescript(
                            '1 + 2'
                        ),
                        afterRecipe: (cu) => {
                            const binary = cu.statements[0].element.expression;
                            expect(binary.type).toBe(Type.Primitive.Double);
                        }
                    }));
                `
            )
            // @formatter:on
        )
    })

    test('describe with nested test and arrow functions should preserve indentation', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `describe('class mapping', () => {
    const spec = new RecipeSpec();

    test('empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('blabla')
        ));
});`
            )
            // @formatter:on
        )
    })

    test('indent initializer when on new line', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                    const x =
                    function () {
                        return 136;
                    };
                    `,
                `
                    const x =
                        function () {
                            return 136;
                        };
                    `
            )
            // @formatter:on
        )
    });

    test('TSX', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            tsx(
                `
                const ComplexComponent = function ComplexComponent({ ref, ...props }) {
                    const handleClick = () => {
                        console.log('clicked');
                    };

                    return (
                    <div ref={ref} onClick={handleClick}>
                    <h1>{props.title}</h1>
                    <p>{props.content}</p>
                    </div>
                    );
                };
                `,
                `
                const ComplexComponent = function ComplexComponent({ ref, ...props }) {
                    const handleClick = () => {
                        console.log('clicked');
                    };

                    return (
                        <div ref={ref} onClick={handleClick}>
                            <h1>{props.title}</h1>
                            <p>{props.content}</p>
                        </div>
                    );
                };
                `
            )
            // @formatter:on
        )
    })

    test('nested ternary should preserve continuation indent', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents()));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                const mappedType = isTupleType
                    ? elementTypes.length > 1
                        ? 'union'  // Tuples often have union element types
                        : 'tuple'
                    : 'array';
                `
            )
            // @formatter:on
        )
    })
});
