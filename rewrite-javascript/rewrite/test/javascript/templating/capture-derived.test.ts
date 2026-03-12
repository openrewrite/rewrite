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
import {capture, JavaScriptVisitor, pattern, raw, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('capture.derived', () => {
    test('derived capture with raw() output maps a captured value', () => {
        const UNIT_MAP: Record<string, string> = {
            'years': 'years',
            'months': 'months',
            'days': 'days',
        };

        const obj = capture('obj');
        const amount = capture('amount');
        const unit = capture({
            name: 'unit',
            constraint: (n: any) => n.kind === J.Kind.Literal && typeof n.value === 'string' && UNIT_MAP[n.value] !== undefined
        });
        const temporalUnit = capture.derived(unit, (node) => {
            const str = (node as J.Literal).value as string;
            return raw(UNIT_MAP[str]);
        });

        const rule = rewrite(() => ({
            before: pattern`${obj}.add(${amount}, ${unit})`,
            after: template`${obj}.add({${temporalUnit}: ${amount}})`,
        }));

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return await rule.tryOn(this.cursor, method) || method;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                'const result = date.add(5, "years")',
                'const result = date.add({years: 5})'
            ),
        );
    });

    test('derived capture with J node output', () => {
        const expr = capture('expr');
        const negated = capture.derived(expr, (node) => {
            return node as J;
        });

        const rule = rewrite(() => ({
            before: pattern`negate(${expr})`,
            after: template`${negated}`,
        }));

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return await rule.tryOn(this.cursor, method) || method;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                'const x = negate(foo)',
                'const x = foo'
            ),
        );
    });

    test('derived capture in before pattern throws', () => {
        const unit = capture('unit');
        const derived = capture.derived(unit, (node) => raw('mapped'));

        expect(() => {
            pattern`foo(${derived as any})`;
        }).toThrow();
    });

    test('derived capture in rewrite rule eliminates dynamic after', () => {
        // Uses different capture names to avoid pattern cache collision with other tests
        const UNIT_MAP: Record<string, string> = {
            'year': 'years',
            'month': 'months',
            'week': 'weeks',
            'day': 'days',
            'hour': 'hours',
            'minute': 'minutes',
            'second': 'seconds',
        };

        const target = capture('target');
        const val = capture('val');
        const unitArg = capture({
            name: 'unitArg',
            constraint: (n: any) => n.kind === J.Kind.Literal && typeof n.value === 'string' && UNIT_MAP[n.value] !== undefined
        });
        const temporalProp = capture.derived(unitArg, (node) => {
            const str = (node as J.Literal).value as string;
            return raw(UNIT_MAP[str]);
        });

        const rule = rewrite(() => ({
            before: pattern`${target}.add(${val}, ${unitArg})`,
            after: template`${target}.add({${temporalProp}: ${val}})`,
        }));

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return await rule.tryOn(this.cursor, method) || method;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                'const x = m.add(1, "month")',
                'const x = m.add({months: 1})'
            ),
        );
    });

    test('derived capture transforms different matched values correctly', () => {
        const SUFFIX_MAP: Record<string, string> = {
            'ms': 'milliseconds',
            's': 'seconds',
            'm': 'minutes',
        };

        const value = capture('value');
        const unit = capture({
            name: 'unit',
            constraint: (n: any) => n.kind === J.Kind.Literal && typeof n.value === 'string' && SUFFIX_MAP[n.value] !== undefined
        });
        const fullUnit = capture.derived(unit, (node) => {
            const str = (node as J.Literal).value as string;
            return raw(SUFFIX_MAP[str]);
        });

        const rule = rewrite(() => ({
            before: pattern`duration(${value}, ${unit})`,
            after: template`Temporal.Duration.from({${fullUnit}: ${value}})`,
        }));

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return await rule.tryOn(this.cursor, method) || method;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                'const t = duration(100, "ms")',
                'const t = Temporal.Duration.from({milliseconds: 100})'
            ),
        );
    });
});
