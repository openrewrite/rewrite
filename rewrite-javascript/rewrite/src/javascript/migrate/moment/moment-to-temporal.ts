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

import {Recipe} from "../../../recipe";
import {TreeVisitor} from "../../../visitor";
import {ExecutionContext} from "../../../execution";
import {JavaScriptVisitor, JS} from "../../index";
import {J, isIdentifier, Expression} from "../../../java";
import {capture, Template} from "../../templating/index";
import {maybeRemoveImport} from "../../remove-import";

const UNIT_MAP: Record<string, string> = {
    'years': 'years', 'y': 'years',
    'months': 'months', 'M': 'months',
    'weeks': 'weeks', 'w': 'weeks',
    'days': 'days', 'd': 'days',
    'hours': 'hours', 'h': 'hours',
    'minutes': 'minutes', 'm': 'minutes',
    'seconds': 'seconds', 's': 'seconds',
    'milliseconds': 'milliseconds', 'ms': 'milliseconds',
};

// Moment's startOf() zeroes time components; round-trip through PlainDate to achieve this
const STARTOF_MAP: Record<string, string> = {
    'day': '.toPlainDate().toPlainDateTime()',
    'month': '.with({day: 1}).toPlainDate().toPlainDateTime()',
    'year': '.with({month: 1, day: 1}).toPlainDate().toPlainDateTime()',
};

export class MomentToTemporal extends Recipe {
    name = "org.openrewrite.javascript.migrate.moment.moment-to-temporal";
    displayName = "Migrate Moment.js to Temporal API";
    description = "Replaces common Moment.js patterns with equivalent native Temporal API usage.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new MomentToTemporalVisitor();
    }
}

class MomentToTemporalVisitor extends JavaScriptVisitor<ExecutionContext> {
    private momentImportName: string | undefined;

    protected override async visitJsCompilationUnit(cu: JS.CompilationUnit, ctx: ExecutionContext): Promise<J | undefined> {
        this.momentImportName = undefined;

        for (const stmt of cu.statements) {
            const s = stmt.element ?? stmt;
            if (s.kind === JS.Kind.Import) {
                this.detectMomentImport(s as JS.Import);
            }
        }

        if (!this.momentImportName) {
            return cu;
        }

        const result = await super.visitJsCompilationUnit(cu, ctx);

        maybeRemoveImport(this, 'moment', 'default');

        return result;
    }

    private detectMomentImport(imp: JS.Import): void {
        const moduleSpecifier = imp.moduleSpecifier;
        if (!moduleSpecifier) return;

        const literal = moduleSpecifier.element;
        if (literal.kind !== J.Kind.Literal) return;
        if ((literal as J.Literal).value !== 'moment') return;

        const importClause = imp.importClause;
        if (!importClause?.name) return;

        const nameElem = importClause.name.element;
        if (isIdentifier(nameElem)) {
            this.momentImportName = nameElem.simpleName;
        }
    }

    protected override async visitFunctionCall(functionCall: JS.FunctionCall, ctx: ExecutionContext): Promise<J | undefined> {
        functionCall = await super.visitFunctionCall(functionCall, ctx) as JS.FunctionCall;
        if (!this.momentImportName) return functionCall;

        const fn = functionCall.function?.element;
        if (!fn || !isIdentifier(fn)) return functionCall;
        if (fn.simpleName !== this.momentImportName) return functionCall;

        return this.transformMomentCreation(functionCall.arguments.elements, functionCall);
    }

    protected override async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
        method = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;
        if (!this.momentImportName) return method;

        const methodName = (method.name as J.Identifier)?.simpleName;
        if (!methodName) return method;

        // moment() / moment(x) — parsed as MethodInvocation with no select
        if (!method.select && methodName === this.momentImportName) {
            const args = method.arguments.elements;
            const effectiveArgs = args.filter(a => a.element.kind !== J.Kind.Empty);
            return this.transformMomentCreation(effectiveArgs, method);
        }

        // moment.utc() / moment.utc(x)
        if (methodName === 'utc' && method.select) {
            const select = method.select.element;
            if (isIdentifier(select) && select.simpleName === this.momentImportName) {
                const args = method.arguments.elements;
                const effectiveArgs = args.filter(a => a.element.kind !== J.Kind.Empty);
                if (effectiveArgs.length === 0) {
                    return await Template.builder()
                        .code('Temporal.Now.instant()')
                        .build()
                        .apply(method, this.cursor);
                }
                if (effectiveArgs.length === 1) {
                    // Note: Temporal.Instant.from() requires an ISO 8601 string with offset/timezone;
                    // moment.utc(x) accepts more formats, so this may need manual adjustment
                    const arg = capture('arg');
                    return await Template.builder()
                        .code('Temporal.Instant.from(')
                        .param(arg)
                        .code(')')
                        .build()
                        .apply(method, this.cursor, {values: new Map([[arg, effectiveArgs[0].element]])});
                }
            }
        }

        // .add(amount, unit) / .subtract(amount, unit)
        if ((methodName === 'add' || methodName === 'subtract') && method.select) {
            return this.transformAddSubtract(method, methodName);
        }

        // .isBefore(other) / .isAfter(other) / .isSame(other)
        if ((methodName === 'isBefore' || methodName === 'isAfter' || methodName === 'isSame') && method.select) {
            return this.transformComparison(method, methodName);
        }

        // .clone() — Temporal is immutable, just return the receiver
        if (methodName === 'clone' && method.select) {
            const cloneArgs = method.arguments.elements.filter(a => a.element.kind !== J.Kind.Empty);
            if (cloneArgs.length === 0) {
                const obj = capture('obj');
                return await Template.builder()
                    .param(obj)
                    .build()
                    .apply(method, this.cursor, {
                        values: new Map([[obj, method.select.element]])
                    });
            }
        }

        // .startOf(unit)
        if (methodName === 'startOf' && method.select) {
            return this.transformStartOf(method);
        }

        return method;
    }

    private async transformMomentCreation(
        args: J.RightPadded<Expression>[],
        node: J
    ): Promise<J | undefined> {
        if (args.length === 0) {
            return await Template.builder()
                .code('Temporal.Now.plainDateTimeISO()')
                .build()
                .apply(node, this.cursor);
        }

        if (args.length === 1) {
            const arg = capture('arg');
            return await Template.builder()
                .code('Temporal.PlainDateTime.from(')
                .param(arg)
                .code(')')
                .build()
                .apply(node, this.cursor, {values: new Map([[arg, args[0].element]])});
        }

        return node;
    }

    private async transformAddSubtract(
        method: J.MethodInvocation,
        methodName: 'add' | 'subtract'
    ): Promise<J | undefined> {
        const args = method.arguments.elements;
        if (args.length !== 2) return method;

        const unitArg = args[1].element;
        if (unitArg.kind !== J.Kind.Literal) return method;
        const unitStr = (unitArg as J.Literal).value;
        if (typeof unitStr !== 'string') return method;

        const temporalUnit = UNIT_MAP[unitStr];
        if (!temporalUnit) return method;

        const obj = capture('obj');
        const amount = capture('amount');

        return await Template.builder()
            .param(obj)
            .code(`.${methodName}({${temporalUnit}: `)
            .param(amount)
            .code('})')
            .build()
            .apply(method, this.cursor, {
                values: new Map([
                    [obj, method.select!.element],
                    [amount, args[0].element]
                ])
            });
    }

    private async transformComparison(
        method: J.MethodInvocation,
        methodName: 'isBefore' | 'isAfter' | 'isSame'
    ): Promise<J | undefined> {
        const args = method.arguments.elements;
        if (args.length !== 1) return method;

        const operator = methodName === 'isBefore' ? '< 0'
            : methodName === 'isAfter' ? '> 0'
            : '=== 0';

        const a = capture('a');
        const b = capture('b');

        return await Template.builder()
            .code('Temporal.PlainDateTime.compare(')
            .param(a)
            .code(', ')
            .param(b)
            .code(`) ${operator}`)
            .build()
            .apply(method, this.cursor, {
                values: new Map([
                    [a, method.select!.element],
                    [b, args[0].element]
                ])
            });
    }

    private async transformStartOf(method: J.MethodInvocation): Promise<J | undefined> {
        const args = method.arguments.elements;
        if (args.length !== 1) return method;

        const unitArg = args[0].element;
        if (unitArg.kind !== J.Kind.Literal) return method;
        const unitStr = (unitArg as J.Literal).value;
        if (typeof unitStr !== 'string') return method;

        const suffix = STARTOF_MAP[unitStr];
        if (!suffix) return method;

        const obj = capture('obj');

        return await Template.builder()
            .param(obj)
            .code(suffix)
            .build()
            .apply(method, this.cursor, {
                values: new Map([[obj, method.select!.element]])
            });
    }
}
