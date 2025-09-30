// noinspection JSUnusedLocalSymbols

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
import {RecipeSpec} from "../../src/test";
import {JavaScriptVisitor, JS, typescript} from "../../src/javascript";
import {J} from "../../src/java";


test("comments", () =>
    new RecipeSpec().rewriteRun({
        //language=typescript
        ...typescript(`
            /*1*/
            const /*2*/ x /*3*/ = /*4*/ 10;/*5*/
            /*6*/
            const y = 5 /*7*/; /*8*/
        `),
        afterRecipe: async (cu: JS.CompilationUnit) => {
            let commentCount = 0;
            const checkSpaces = new class extends JavaScriptVisitor<void> {
                public override async visitSpace(space: J.Space, p: void): Promise<J.Space> {
                    const ret = await super.visitSpace(space, p);
                    expect(ret.whitespace).not.toContain("/*");
                    commentCount += ret.comments.length;
                    return ret;
                }
            }
            await checkSpaces.visit(cu, undefined);
            expect(commentCount).toBe(8);
        }
    }));
