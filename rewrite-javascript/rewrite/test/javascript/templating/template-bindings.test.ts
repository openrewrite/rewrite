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
import {capture, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {bindingContextStatement} from "../../../src/javascript/templating/bindings";

describe('templates that declare module bindings', () => {
    const spec = new RecipeSpec();

    /** Rewrites every `applyTheme(...)` through `tmpl`, resolving the template's bindings per file. */
    function recipeApplying(tmpl: ReturnType<typeof template>, arg: ReturnType<typeof capture>) {
        const rule = rewrite(() => ({before: pattern`applyTheme(${arg})`, after: tmpl}));
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                method = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return await rule.tryOn(this.cursor, method, {visitor: this}) || method;
            }
        });
    }

    test('the bound name is deconflicted and the template follows it', async () => {
        const arg = capture('arg');
        spec.recipe = recipeApplying(template`Theming.setTheme(${arg})`.configure({
            bindings: {Theming: {module: 'sap/ui/core/Theming', member: 'default'}}
        }), arg);

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `const Theming = 1;\napplyTheme('dark');`,
                `import Theming_1 from 'sap/ui/core/Theming';\n\nconst Theming = 1;\nTheming_1.setTheme('dark');`
            )
        );
    });

    test('an import already binding the module is reused, under the name it already has', async () => {
        const arg = capture('arg');
        spec.recipe = recipeApplying(template`Theming.setTheme(${arg})`.configure({
            bindings: {Theming: {module: 'sap/ui/core/Theming', member: 'default'}}
        }), arg);

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `import Th from 'sap/ui/core/Theming';\n\napplyTheme('dark');`,
                `import Th from 'sap/ui/core/Theming';\n\nTh.setTheme('dark');`
            )
        );
    });

    test('a template resolves every binding it declares', async () => {
        const arg = capture('arg');
        spec.recipe = recipeApplying(template`new Locale(Localization.getLanguageTag(${arg}))`.configure({
            bindings: {
                Locale: {module: 'sap/ui/core/Locale', member: 'default'},
                Localization: {module: 'sap/base/i18n/Localization', member: 'default'}
            }
        }), arg);

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `applyTheme('dark');`,
                `import Locale from 'sap/ui/core/Locale';\nimport Localization from 'sap/base/i18n/Localization';\n\nnew Locale(Localization.getLanguageTag('dark'));`
            )
        );
    });

    test('a name in a naming position is not a reference to the binding', async () => {
        const arg = capture('arg');
        spec.recipe = recipeApplying(template`Theming.setTheme(${arg}.Theming)`.configure({
            bindings: {Theming: {module: 'sap/ui/core/Theming', member: 'default'}}
        }), arg);

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `const Theming = 1;\napplyTheme(props);`,
                `import Theming_1 from 'sap/ui/core/Theming';\n\nconst Theming = 1;\nTheming_1.setTheme(props.Theming);`
            )
        );
    });

    test('a binding called on its own is a reference to it, not a name it gives something', async () => {
        const arg = capture('arg');
        spec.recipe = recipeApplying(template`merge(${arg}, {}).merge()`.configure({
            bindings: {merge: {module: 'sap/base/util/merge', member: 'default'}}
        }), arg);

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `const merge = 1;\napplyTheme('dark');`,
                `import merge_1 from 'sap/base/util/merge';\n\nconst merge = 1;\nmerge_1('dark', {}).merge();`
            )
        );
    });

    test('a rule that does not fire leaves the file\'s imports alone', async () => {
        const arg = capture('arg');
        spec.recipe = recipeApplying(template`Theming.setTheme(${arg})`.configure({
            bindings: {Theming: {module: 'sap/ui/core/Theming', member: 'default'}}
        }), arg);

        await spec.rewriteRun(
            //language=typescript
            typescript(`const x = Theming;\nsomethingElse('dark');`)
        );
    });

    test('a binding is parsed against an import only where the dependencies could resolve it', () => {
        expect(bindingContextStatement('Theming', {module: 'sap/ui/core/Theming', member: 'default'}, {}))
            .toBe('declare const Theming: any;');
        expect(bindingContextStatement('Theming', {module: 'sap/ui/core/Theming', member: 'default'}, {sap: '^1.0.0'}))
            .toBe("import Theming from 'sap/ui/core/Theming';");

        expect(bindingContextStatement('Props', {module: '@scope/pkg/props', member: 'Props', typeOnly: true}, {}))
            .toBe('type Props = any;');
        expect(bindingContextStatement('Props', {module: '@scope/pkg/props', member: 'Props', typeOnly: true}, {'@scope/pkg': '^1.0.0'}))
            .toBe("import type {Props} from '@scope/pkg/props';");
    });

    test('a declared binding left unresolved at apply is an error, not a silent wrong name', async () => {
        const arg = capture('arg');
        const tmpl = template`Theming.setTheme(${arg})`.configure({
            bindings: {Theming: {module: 'sap/ui/core/Theming', member: 'default'}}
        });
        const rule = rewrite(() => ({before: pattern`applyTheme(${arg})`, after: tmpl}));

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                method = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return await rule.tryOn(this.cursor, method) || method;
            }
        });

        await expect(spec.rewriteRun(
            //language=typescript
            typescript(`applyTheme('dark');`)
        )).rejects.toThrow(/Theming/);
    });
});
