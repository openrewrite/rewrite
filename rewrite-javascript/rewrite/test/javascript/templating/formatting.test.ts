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
import {Autodetect, capture, javascript, JavaScriptVisitor, JS, pattern, rewrite, Template, template} from "../../../src/javascript";
import {J} from "../../../src/java";
import {create as produce} from "mutative";
import {replaceMarkerByKind} from "../../../src/markers";

/** Stands in for the style marker the project parser attaches to the compilation units it produces. */
async function withDetectedStyles(cu: JS.CompilationUnit): Promise<JS.CompilationUnit> {
    const detector = Autodetect.detector();
    await detector.sample(cu);
    const marker = detector.build();
    return produce(cu, draft => {
        draft.markers = replaceMarkerByKind(draft.markers, marker);
    });
}

describe('template formatting', () => {
    const spec = new RecipeSpec();

    test('generated code follows the styles of the file it lands in', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                if (m.name.simpleName !== 'extend') {
                    return m;
                }
                return Template.builder()
                    .code('merge({}, ')
                    .param(m.arguments.elements[2].element)
                    .code(')')
                    .build()
                    .apply(m, this.cursor);
            }
        });
        return spec.rewriteRun({
            //language=javascript
            ...javascript(
                `function f() {
\tvar o = jQuery.sap.extend(true, {}, {
\t\tname: "x",
\t\trun: function (a) {
\t\t\treturn a;
\t\t}
\t});
}
`,
                `function f() {
\tvar o = merge({}, {
\t\tname: "x",
\t\trun: function (a) {
\t\t\treturn a;
\t\t}
\t});
}
`),
            beforeRecipe: withDetectedStyles
        });
    });

    test('format: false anchors the template without fitting it or the recipe file\'s indentation', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                if (m.name.simpleName !== 'target') {
                    return m;
                }
                return template`
                    wrap(function () {
                        doThing();
                    })
                `.apply(m, this.cursor, {format: false});
            }
        });
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `function f() {
    if (x) {
        target();
    }
}
`,
                `function f() {
    if (x) {
        wrap(function () {
    doThing();
});
    }
}
`));
    });

    test('a rewrite rule passes format through to the template it applies', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitTernary(ternary: J.Ternary, p: any): Promise<J | undefined> {
                const visited = await super.visitTernary(ternary, p) as J.Ternary;
                const t = capture();
                return await rewrite(() => ({
                    before: pattern`${t} ? true : false`,
                    after: template`${t}`,
                    format: false
                })).tryOn(this.cursor, visited) || visited;
            }
        });
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const c = a <
  b ? true : false;
`,
                `const c = a <
  b;
`));
    });
});
