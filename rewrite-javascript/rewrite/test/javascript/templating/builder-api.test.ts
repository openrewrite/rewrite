/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {capture, Capture, JavaScriptVisitor, Pattern, template, Template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('Builder API', () => {
    const spec = new RecipeSpec();

    describe('TemplateBuilder', () => {
        test('creates template with static code only', () => {
            const tmpl = Template.builder()
                .code('const x = ')
                .code('42')
                .build();

            expect(tmpl).toBeInstanceOf(Template);
        });

        test('creates template with code and parameters', () => {
            const value = capture('value');
            const tmpl = Template.builder()
                .code('const x = ')
                .param(value)
                .build();

            expect(tmpl).toBeInstanceOf(Template);
        });

        test('creates template equivalent to template literal', async () => {
            // Using builder - just replace the literal value
            const builderTmpl = Template.builder()
                .code(String(42))
                .build();

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                    if (literal.valueSource === '1') {
                        return builderTmpl.apply(this.cursor, literal);
                    }
                    return literal;
                }
            });

            return spec.rewriteRun(
                typescript('const a = 1', 'const a = 42'),
            );
        });

        test('handles conditional construction', async () => {
            const needsValidation = true;
            const value = capture('value');

            const builder = Template.builder().code('function validate(x) {');
            if (needsValidation) {
                builder.code(' if (typeof x !== "number") throw new Error("Invalid");');
            }
            builder.code(' return ')
                .param(value)
                .code('; }');

            const tmpl = builder.build();

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                    if (literal.valueSource === '1') {
                        const values = new Map([['value', literal]]);
                        return tmpl.apply(this.cursor, literal, values);
                    }
                    return literal;
                }
            });

            return spec.rewriteRun(
                typescript('const a = 1',
                    `
                      const a =
                          function validate(x) {
                              if (typeof x !== "number") throw new Error("Invalid");
                              return 1;
                          }
                      `
                ),
            );
        });

        test('handles loop-based construction', () => {
            const builder = Template.builder().code('sum(');
            const argCount = 3;
            for (let i = 0; i < argCount; i++) {
                if (i > 0) builder.code(', ');
                builder.param(capture(`arg${i}`));
            }
            builder.code(')');

            const tmpl = builder.build();
            expect(tmpl).toBeInstanceOf(Template);
        });

        test('handles multiple consecutive code calls', () => {
            const tmpl = Template.builder()
                .code('const ')
                .code('x')
                .code(' = ')
                .code('42')
                .build();

            expect(tmpl).toBeInstanceOf(Template);
        });

        test('handles multiple consecutive param calls', () => {
            const a = capture('a');
            const b = capture('b');
            const tmpl = Template.builder()
                .param(a)
                .param(b)
                .build();

            expect(tmpl).toBeInstanceOf(Template);
        });

        test('composition from fragments', async () => {
            function createWrapper(innerBody: string): Template {
                return Template.builder()
                    .code('function wrapper() { try { ')
                    .code(innerBody)
                    .code(' } catch (e) { console.error(e); } }')
                    .build();
            }

            const tmpl = createWrapper('return 42;');
            expect(tmpl).toBeInstanceOf(Template);

            // Verify the template generates the expected code
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                    if (literal.valueSource === '1') {
                        return tmpl.apply(this.cursor, literal);
                    }
                    return literal;
                }
            });

            return spec.rewriteRun(
                typescript(
                    'const x = 1',
                    `
                    const x =
                        function wrapper() {
                            try {
                                return 42;
                            } catch (e) {
                                console.error(e);
                            }
                        }`
                )
            );
        });
    });

    describe('PatternBuilder', () => {
        test('creates pattern with static code only', () => {
            const pat = Pattern.builder()
                .code('const x = ')
                .code('42')
                .build();

            expect(pat).toBeInstanceOf(Pattern);
        });

        test('creates pattern with code and captures', () => {
            const value = capture('value');
            const pat = Pattern.builder()
                .code('const x = ')
                .capture(value)
                .build();

            expect(pat).toBeInstanceOf(Pattern);
        });

        test('creates pattern equivalent to pattern literal', async () => {
            const left = capture('left');
            const right = capture('right');

            // Using builder
            const builderPat = Pattern.builder()
                .capture(left)
                .code('+')
                .capture(right)
                .build();

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    const match = await builderPat.match(binary);
                    if (match) {
                        return template`${match.get(right)!} + ${match.get(left)!}`.apply(this.cursor, binary, match);
                    }
                    return binary;
                }
            });

            return spec.rewriteRun(
                typescript('1 + 2', '2 + 1'),
            );
        });

        test('handles loop-based pattern generation', async () => {
            const builder = Pattern.builder().code('myFunction(');
            const argCount = 3;
            const captures: Capture<J.Literal>[] = [];
            for (let i = 0; i < argCount; i++) {
                if (i > 0) builder.code(', ');
                const cap = capture<J.Literal>();
                captures.push(cap);
                builder.capture(cap);
            }
            builder.code(')');

            const pat = builder.build();
            expect(pat).toBeInstanceOf(Pattern);

            // Verify the pattern matches and captures arguments correctly
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                    const match = await pat.match(methodInvocation);
                    if (match) {
                        // Verify all three captures were matched
                        const arg0 = match.get(captures[0]);
                        const arg1 = match.get(captures[1]);
                        const arg2 = match.get(captures[2]);

                        expect(arg0).toBeDefined();
                        expect(arg1).toBeDefined();
                        expect(arg2).toBeDefined();
                        expect(arg0?.value).toBe(1);
                        expect(arg1?.value).toBe(2);
                        expect(arg2?.value).toBe(3);

                        // Swap first and last arguments
                        return template`myFunction(${arg2!}, ${arg1!}, ${arg0!})`.apply(this.cursor, methodInvocation, match);
                    }
                    return methodInvocation;
                }
            });

            return spec.rewriteRun(
                typescript('myFunction(1, 2, 3)', 'myFunction(3, 2, 1)'),
            );
        });

        test('accepts string capture names', () => {
            const pat = Pattern.builder()
                .code('const ')
                .capture('varName')
                .code(' = ')
                .capture('value')
                .build();

            expect(pat).toBeInstanceOf(Pattern);
        });

        test('handles multiple consecutive code calls', () => {
            const pat = Pattern.builder()
                .code('const ')
                .code('x')
                .code(' = ')
                .code('42')
                .build();

            expect(pat).toBeInstanceOf(Pattern);
        });

        test('handles multiple consecutive capture calls', () => {
            const a = capture('a');
            const b = capture('b');
            const pat = Pattern.builder()
                .capture(a)
                .capture(b)
                .build();

            expect(pat).toBeInstanceOf(Pattern);
        });

        test('pattern from builder can be configured', () => {
            const value = capture('value');
            const pat = Pattern.builder()
                .code('const x = ')
                .capture(value)
                .build()
                .configure({
                    context: ['type MyType = number'],
                    lenientTypeMatching: true
                });

            expect(pat).toBeInstanceOf(Pattern);
            expect(pat.options.context).toEqual(['type MyType = number']);
            expect(pat.options.lenientTypeMatching).toBe(true);
        });
    });

    describe('Builder API Integration', () => {
        test('pattern and template builders work together', async () => {
            const left = capture('left');
            const right = capture('right');

            const pat = Pattern.builder()
                .capture(left)
                .code(' + ')
                .capture(right)
                .build();

            const tmpl = Template.builder()
                .param(right)
                .code(' + ')
                .param(left)
                .build();

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    const match = await pat.match(binary);
                    if (match) {
                        return tmpl.apply(this.cursor, binary, match);
                    }
                    return binary;
                }
            });

            return spec.rewriteRun(
                typescript('1 + 2', '2 + 1'),
            );
        });

        test('dynamic pattern generation based on configuration', async () => {
            // Simulate a configuration that determines the pattern
            const expectedArgs = ['first', 'second', 'third'];

            const builder = Pattern.builder().code('processArgs(');
            for (let i = 0; i < expectedArgs.length; i++) {
                if (i > 0) builder.code(', ');
                builder.capture(capture(expectedArgs[i]));
            }
            builder.code(')');

            const pat = builder.build();

            // Create corresponding template that reverses the arguments
            const tmplBuilder = Template.builder().code('processArgs(');
            for (let i = expectedArgs.length - 1; i >= 0; i--) {
                if (i < expectedArgs.length - 1) tmplBuilder.code(', ');
                tmplBuilder.param(capture(expectedArgs[i]));
            }
            tmplBuilder.code(')');

            const tmpl = tmplBuilder.build();

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(invocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                    const match = await pat.match(invocation);
                    if (match) {
                        return tmpl.apply(this.cursor, invocation, match);
                    }
                    return invocation;
                }
            });

            return spec.rewriteRun(
                typescript('processArgs(1, 2, 3)', 'processArgs(3, 2, 1)'),
            );
        });
    });
});
