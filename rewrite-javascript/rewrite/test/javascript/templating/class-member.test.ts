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
import {capture, flattenBlock, JavaScriptVisitor, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

/** The name `member` declares, if it is a property. */
function declared(member: J): string | undefined {
    return member.kind === J.Kind.VariableDeclarations ?
        ((member as J.VariableDeclarations).variables[0].element.name as J.Identifier).simpleName : undefined;
}

describe("template replacing a class member", () => {
    const spec = new RecipeSpec();

    test("a template replacing a class member is a class member", () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitVariableDeclarations(declarations: J.VariableDeclarations, p: any): Promise<J | undefined> {
                return declared(declarations) === "context" && declarations.modifiers.length === 0 ?
                    await template`declare context: any;`.apply(declarations, this.cursor) : declarations;
            }
        });
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                class A extends Component {
                    context;
                }
            `,
            `
                class A extends Component {
                    declare context: any;
                }
            `
        ));
    });

    test("several members replacing a class member are a block that flattenBlock splices into the class body", () => {
        const context = capture('context');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitVariableDeclarations(declarations: J.VariableDeclarations, p: any): Promise<J | undefined> {
                const body = this.cursor.parentTree()!.value as J.Block;
                if (declared(declarations) !== "contextType" || body.statements.some(s => declared(s.element) === "context")) {
                    return declarations;
                }
                const members = await template`
                    static contextType = ${context};
                    declare context: React.ContextType<typeof ${context}>;
                `.apply(declarations, this.cursor, {values: {context: declarations.variables[0].element.initializer!.element}});
                return members?.kind === J.Kind.Block ? flattenBlock(this, members as J.Block) : members;
            }
        });
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                class A extends React.Component {
                    static contextType = ThemeContext;
                }
            `,
            // The members after the first keep the block's indentation, as flattenBlock leaves statements
            `
                class A extends React.Component {
                    static contextType = ThemeContext;
                        declare context: React.ContextType<typeof ThemeContext>;
                }
            `
        ));
    });
});
