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
import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";

describe('with mapping', () => {
    const spec = new RecipeSpec();

    // noinspection WithStatementJS
    test('with statement', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 with (0) {
                     console.log("aaa");
                 }
             `)
        ));

    // noinspection WithStatementJS
    test('with statement with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 /*a*/with /*b*/ (/*c*/0 /*d*/) /*e*/{/*f*/
                     console.log("aaa");
                     /*g*/}/*h*/
             `)
        ));

    // noinspection TypeScriptUnresolvedReference,WithStatementJS
    test('with statement with try-catch', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                with (ctx) try {
                    return eval("(" + str + ")")
                } catch (e) {
                }
            `)
        ));

    test('with statement with empty body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 with (0) {/*a*/}
             `)
        ));

    // noinspection WithStatementJS
    test('with statement with body without braces', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 with (0) 1;
             `)
        ));

    // noinspection TypeScriptUnresolvedReference
    test('with statement with await expr', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export {};
                // noinspection JSAnnotator
                with (await obj?.foo) {
                }
            `)
        ));

    test('with statement with empty expr and body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 with({/*a*/}) {/*b*/}
             `)
        ));

    test('with statement with multiline statement', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 with ([]) {
                     console.log("aaa");
                     console.log("bbb")
                 }
             `)
        ));

    // noinspection TypeScriptUnresolvedReference,WithStatementJS
    test('with statement with internal with statements', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 with (bindingContext) {
                     with (data || {}) {
                         with (options.templateRenderingVariablesInScope || {}) {
                             // Dummy [renderTemplate:...] syntax
                             result = templateText.replace(/\\[renderTemplate\\:(.*?)\\]/g, function (match, templateName) {
                                 return ko.renderTemplate(templateName, data, options);
                             });
 
                             var evalHandler = function (match, script) {
                                 try {
                                     var evalResult = eval(script);
                                     return (evalResult === null) || (evalResult === undefined) ? "" : evalResult.toString();
                                 } catch (ex) {
                                     throw new Error("Error evaluating script: [js: " + script + "]\\n\\nException: " + ex.toString());
                                 }
                             }
 
                             // Dummy [[js:...]] syntax (in case you need to use square brackets inside the expression)
                             result = result.replace(/\\[\\[js\\:([\\s\\S]*?)\\]\\]/g, evalHandler);
 
                             // Dummy [js:...] syntax
                             result = result.replace(/\\[js\\:([\\s\\S]*?)\\]/g, evalHandler);
                         }
                     }
                 }
             `)
        ));
});
