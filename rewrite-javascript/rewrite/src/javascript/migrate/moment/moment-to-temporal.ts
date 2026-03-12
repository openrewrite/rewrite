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
import {J, isIdentifier, isLiteral} from "../../../java";
import {capture, pattern, template, raw, rewrite, RewriteRule} from "../../templating/index";
import {maybeRemoveImport} from "../../remove-import";

const UNIT_MAP: Record<string, string> = {
    'years': 'years', 'year': 'years', 'y': 'years',
    'months': 'months', 'month': 'months', 'M': 'months',
    'weeks': 'weeks', 'week': 'weeks', 'w': 'weeks',
    'days': 'days', 'day': 'days', 'd': 'days',
    'hours': 'hours', 'hour': 'hours', 'h': 'hours',
    'minutes': 'minutes', 'minute': 'minutes', 'm': 'minutes',
    'seconds': 'seconds', 'second': 'seconds', 's': 'seconds',
    'milliseconds': 'milliseconds', 'millisecond': 'milliseconds', 'ms': 'milliseconds',
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
    private rules: RewriteRule | undefined;

    protected override async visitJsCompilationUnit(cu: JS.CompilationUnit, ctx: ExecutionContext): Promise<J | undefined> {
        this.momentImportName = undefined;
        this.rules = undefined;

        for (const stmt of cu.statements) {
            const s = stmt.element ?? stmt;
            if (s.kind === JS.Kind.Import) {
                this.detectMomentImport(s as JS.Import);
            }
        }

        if (!this.momentImportName) {
            return cu;
        }

        this.rules = this.buildRules();

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
        if (!this.momentImportName || !this.rules) return functionCall;
        return await this.rules.tryOn(this.cursor, functionCall) || functionCall;
    }

    protected override async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
        method = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;
        if (!this.momentImportName || !this.rules) return method;
        return await this.rules.tryOn(this.cursor, method) || method;
    }

    private buildRules(): RewriteRule {
        return this.createNoArgRule()
            .orElse(this.createOneArgRule())
            .orElse(this.utcNoArgRule())
            .orElse(this.utcOneArgRule())
            .orElse(this.addSubtractRule('add'))
            .orElse(this.addSubtractRule('subtract'))
            .orElse(this.comparisonRule('isBefore', '< 0'))
            .orElse(this.comparisonRule('isAfter', '> 0'))
            .orElse(this.comparisonRule('isSame', '=== 0'))
            .orElse(this.cloneRule())
            .orElse(this.startOfRule());
    }

    // moment() -> Temporal.Now.plainDateTimeISO()
    private createNoArgRule(): RewriteRule {
        return rewrite(() => ({
            before: pattern`${raw(this.momentImportName!)}()`,
            after: template`Temporal.Now.plainDateTimeISO()`
        }));
    }

    // moment(x) -> Temporal.PlainDateTime.from(x)
    private createOneArgRule(): RewriteRule {
        const arg = capture();
        return rewrite(() => ({
            before: pattern`${raw(this.momentImportName!)}(${arg})`,
            after: template`Temporal.PlainDateTime.from(${arg})`
        }));
    }

    // moment.utc() -> Temporal.Now.instant()
    private utcNoArgRule(): RewriteRule {
        return rewrite(() => ({
            before: pattern`${raw(this.momentImportName!)}.utc()`,
            after: template`Temporal.Now.instant()`
        }));
    }

    // moment.utc(x) -> Temporal.Instant.from(x)
    private utcOneArgRule(): RewriteRule {
        const arg = capture();
        return rewrite(() => ({
            before: pattern`${raw(this.momentImportName!)}.utc(${arg})`,
            after: template`Temporal.Instant.from(${arg})`
        }));
    }

    // .add(amount, unit) / .subtract(amount, unit)
    private addSubtractRule(methodName: 'add' | 'subtract'): RewriteRule {
        return rewrite(() => {
            const obj = capture();
            const amount = capture();
            const unit = capture({
                constraint: (n: any) => isLiteral(n) && typeof n.value === 'string' && UNIT_MAP[n.value] !== undefined
            });
            return {
                before: pattern`${obj}.${raw(methodName)}(${amount}, ${unit})`,
                after: (match) => {
                    const unitStr = (match.get(unit) as J.Literal).value as string;
                    return template`${obj}.${raw(methodName)}({${raw(UNIT_MAP[unitStr])}: ${amount}})`;
                }
            };
        });
    }

    // .isBefore(other) / .isAfter(other) / .isSame(other)
    private comparisonRule(methodName: string, operator: string): RewriteRule {
        const a = capture();
        const b = capture();
        return rewrite(() => ({
            before: pattern`${a}.${raw(methodName)}(${b})`,
            after: template`Temporal.PlainDateTime.compare(${a}, ${b}) ${raw(operator)}`
        }));
    }

    // .clone() — Temporal is immutable, just return the receiver
    private cloneRule(): RewriteRule {
        const obj = capture();
        return rewrite(() => ({
            before: pattern`${obj}.clone()`,
            after: template`${obj}`
        }));
    }

    // .startOf(unit) — needs dynamic after for STARTOF_MAP lookup
    private startOfRule(): RewriteRule {
        return rewrite(() => {
            const obj = capture();
            const unit = capture({
                constraint: (n: any) => isLiteral(n) && typeof n.value === 'string' && STARTOF_MAP[n.value] !== undefined
            });
            return {
                before: pattern`${obj}.startOf(${unit})`,
                after: (match) => {
                    const unitStr = (match.get(unit) as J.Literal).value as string;
                    return template`${obj}${raw(STARTOF_MAP[unitStr])}`;
                }
            };
        });
    }
}
