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
    private _rules: RewriteRule | undefined;
    private _rulesForName: string | undefined;

    private get rules(): RewriteRule {
        if (!this._rules || this._rulesForName !== this.momentImportName) {
            this._rules = this.buildRules();
            this._rulesForName = this.momentImportName;
        }
        return this._rules;
    }

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

    // Both visitFunctionCall and visitMethodInvocation try all rules — the parser
    // may represent moment() as either node type depending on context
    protected override async visitFunctionCall(functionCall: JS.FunctionCall, ctx: ExecutionContext): Promise<J | undefined> {
        functionCall = await super.visitFunctionCall(functionCall, ctx) as JS.FunctionCall;
        if (!this.momentImportName) return functionCall;
        return await this.rules.tryOn(this.cursor, functionCall) || functionCall;
    }

    protected override async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
        method = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;
        if (!this.momentImportName) return method;
        return await this.rules.tryOn(this.cursor, method) || method;
    }

    private buildRules(): RewriteRule {
        const name = this.momentImportName!;

        // moment() → Temporal.Now.plainDateTimeISO()
        const createNoArg = rewrite(() => ({
            before: pattern`${raw(name)}()`,
            after: template`Temporal.Now.plainDateTimeISO()`
        }));

        // moment(x) → Temporal.PlainDateTime.from(x)
        const createOneArg = rewrite(() => {
            const arg = capture('arg');
            return {
                before: pattern`${raw(name)}(${arg})`,
                after: template`Temporal.PlainDateTime.from(${arg})`
            };
        });

        // moment.utc() → Temporal.Now.instant()
        const utcNoArg = rewrite(() => ({
            before: pattern`${raw(name)}.utc()`,
            after: template`Temporal.Now.instant()`
        }));

        // moment.utc(x) → Temporal.Instant.from(x)
        const utcOneArg = rewrite(() => {
            const arg = capture('arg');
            return {
                before: pattern`${raw(name)}.utc(${arg})`,
                after: template`Temporal.Instant.from(${arg})`
            };
        });

        // .add(amount, unit) → .add({temporalUnit: amount})
        const add = this.addSubtractRule('add');
        const subtract = this.addSubtractRule('subtract');

        // .isBefore/.isAfter/.isSame → Temporal.PlainDateTime.compare(...) op 0
        const isBefore = this.comparisonRule('isBefore', '< 0');
        const isAfter = this.comparisonRule('isAfter', '> 0');
        const isSame = this.comparisonRule('isSame', '=== 0');

        // .clone() → just the receiver (Temporal is immutable)
        const clone = rewrite(() => {
            const obj = capture('obj');
            return {
                before: pattern`${obj}.clone()`,
                after: template`${obj}`
            };
        });

        // .startOf(unit) → temporal equivalent
        const startOf = this.startOfRule();

        return createNoArg
            .orElse(createOneArg)
            .orElse(utcNoArg)
            .orElse(utcOneArg)
            .orElse(add)
            .orElse(subtract)
            .orElse(isBefore)
            .orElse(isAfter)
            .orElse(isSame)
            .orElse(clone)
            .orElse(startOf);
    }

    private addSubtractRule(methodName: 'add' | 'subtract'): RewriteRule {
        return rewrite(() => {
            const obj = capture({type: 'Temporal.PlainDateTime'});
            const amount = capture();
            const unit = capture({
                constraint: (n: any) => isLiteral(n) && typeof n.value === 'string' && UNIT_MAP[n.value] !== undefined
            });
            return {
                before: pattern`${obj}.${raw(methodName)}(${amount}, ${unit})`.configure({
                    lenientTypeMatching: false
                }),
                after: (match) => {
                    const unitStr = (match.get(unit) as J.Literal).value as string;
                    return template`${obj}.${raw(methodName)}({${raw(UNIT_MAP[unitStr])}: ${amount}})`;
                }
            };
        });
    }

    private comparisonRule(methodName: string, operator: string): RewriteRule {
        return rewrite(() => {
            const obj = capture('obj');
            const other = capture('other');
            return {
                before: pattern`${obj}.${raw(methodName)}(${other})`,
                after: template`Temporal.PlainDateTime.compare(${obj}, ${other}) ${raw(operator)}`
            };
        });
    }

    private startOfRule(): RewriteRule {
        return rewrite(() => {
            const obj = capture('obj');
            const unit = capture({
                name: 'unit',
                constraint: (n: any) => isLiteral(n) && typeof n.value === 'string' && STARTOF_MAP[n.value] !== undefined
            });
            return {
                before: pattern`${obj}.startOf(${unit})`,
                after: (match) => {
                    const unitStr = (match.get('unit') as J.Literal).value as string;
                    return template`${obj}${raw(STARTOF_MAP[unitStr])}`;
                }
            };
        });
    }
}
