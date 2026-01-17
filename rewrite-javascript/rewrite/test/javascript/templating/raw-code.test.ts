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
import {capture, JavaScriptVisitor, pattern, raw, rewrite, template, typescript, _} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('raw() function', () => {
    const spec = new RecipeSpec();

    describe('construction-time string interpolation', () => {
        test('splices raw code directly into template', () => {
            const methodName = "info";
            const msg = capture('msg');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    if ((method.select as unknown as J.Identifier | undefined)?.simpleName === 'console' &&
                        (method.name as J.Identifier).simpleName === 'log') {
                        return template`logger.${raw(methodName)}(${msg})`.apply(method, this.cursor, {
                            values: new Map([
                                ['msg', method.arguments.elements[0]]
                            ])
                        });
                    }
                    return method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'console.log("test message")',
                    'logger.info("test message")'
                )
            );
        });

        test('works with multiple raw() interpolations', () => {
            const obj = "console";
            const method = "log";
            const msg = capture('msg');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(invocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                    if ((invocation.select as unknown as J.Identifier | undefined)?.simpleName === 'logger' &&
                        (invocation.name as J.Identifier).simpleName === 'info') {
                        return template`${raw(obj)}.${raw(method)}(${msg})`.apply(invocation, this.cursor, {
                            values: new Map([
                                ['msg', invocation.arguments.elements[0]]
                            ])
                        });
                    }
                    return invocation;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'logger.info("hello")',
                    'console.log("hello")'
                )
            );
        });

        test('works with operators', () => {
            const operator = ">=";
            const value = capture('value');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    if (binary.operator.element === J.Binary.Type.Equal) {
                        return template`${value} ${raw(operator)} threshold`.apply(binary, this.cursor, {
                            values: new Map([
                                ['value', binary.left]
                            ])
                        });
                    }
                    return binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'count == threshold',
                    'count >= threshold'
                )
            );
        });
    });

    describe('integration with rewrite()', () => {
        test('works in rewrite rules', () => {
            const logLevel = "warn";
            const msg = _('msg');

            const rule = rewrite(() => ({
                before: pattern`console.log(${msg})`,
                after: template`logger.${raw(logLevel)}(${msg})`
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, method) || method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'console.log("warning")',
                    'logger.warn("warning")'
                )
            );
        });
    });

    describe('mixed with other parameters', () => {
        test('can mix raw() with capture()', () => {
            const prefix = "user";
            const value = capture('value');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    if (binary.operator.element === J.Binary.Type.LessThan &&
                        (binary.left as J.Identifier)?.simpleName === 'age') {
                        return template`${raw(prefix)}.age < ${value}`.apply(binary, this.cursor, {
                            values: new Map([
                                ['value', binary.right]
                            ])
                        });
                    }
                    return binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'age < 18',
                    'user.age < 18'
                )
            );
        });
    });

    describe('recipe option use case', () => {
        test('uses raw() with dynamic recipe configuration', () => {
            // Simulates a recipe with an option for the log level
            const logLevel = "debug";
            const msg = _('msg');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    if ((method.select as unknown as J.Identifier | undefined)?.simpleName === 'console' &&
                        (method.name as J.Identifier).simpleName === 'log') {
                        // Template is constructed with the dynamic log level from recipe option
                        return template`logger.${raw(logLevel)}(${msg})`.apply(method, this.cursor, {
                            values: {msg: method.arguments.elements[0]}
                        });
                    }
                    return method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'console.log("Debug info")',
                    'logger.debug("Debug info")'
                )
            );
        });
    });

    describe('raw() in patterns', () => {
        test('matches pattern with raw() method name', async () => {
            const methodName = "log";
            const msg = capture('msg');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    // Pattern with raw() for dynamic method name matching
                    const m = await pattern`console.${raw(methodName)}(${msg})`.match(method, this.cursor);
                    if (m) {
                        return template`logger.info(${msg})`.apply(method, this.cursor, {values: m});
                    }
                    return method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'console.log("test")',
                    'logger.info("test")'
                )
            );
        });

        test('combines raw() in both pattern and template', async () => {
            const oldMethod = "warn";
            const newMethod = "error";
            const msg = capture('msg');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    // Use raw() in pattern to match specific method
                    const m = await pattern`logger.${raw(oldMethod)}(${msg})`.match(method, this.cursor);
                    if (m) {
                        // Use raw() in template to replace with different method
                        return template`logger.${raw(newMethod)}(${msg})`.apply(method, this.cursor, {values: m});
                    }
                    return method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'logger.warn("warning message")',
                    'logger.error("warning message")'
                )
            );
        });

        test('works with rewrite() using raw() in pattern', () => {
            const operator = "==";
            const left = _('left');
            const right = _('right');

            const rule = rewrite(() => ({
                before: pattern`${left} ${raw(operator)} ${right}`,
                after: template`${left} === ${right}`
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'x == y',
                    'x === y'
                )
            );
        });

        test('multiple raw() in single pattern', async () => {
            const obj = "console";
            const method = "log";
            const msg = capture('msg');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(invocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                    // Pattern with multiple raw() calls
                    const m = await pattern`${raw(obj)}.${raw(method)}(${msg})`.match(invocation, this.cursor);
                    if (m) {
                        return template`logger.info(${msg})`.apply(invocation, this.cursor, {values: m});
                    }
                    return invocation;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'console.log("hello")',
                    'logger.info("hello")'
                )
            );
        });

        test('raw() with captures in complex pattern', async () => {
            const prefix = "user";
            const field = capture('field');
            const value = capture('value');

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    // Pattern matching property access with raw() prefix
                    const m = await pattern`${raw(prefix)}.${field} < ${value}`.match(binary, this.cursor);
                    if (m) {
                        return template`${raw(prefix)}.${field} >= ${value}`.apply(binary, this.cursor, {values: m});
                    }
                    return binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    'user.age < 18',
                    'user.age >= 18'
                )
            );
        });
    });
});
