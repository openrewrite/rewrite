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
import {J, isIdentifier} from "../../../java";
import {capture, Pattern, Template, rewrite, RewriteRule} from "../../templating/index";
import {maybeRemoveImport} from "../../remove-import";

const DATE_FNS_FUNCTIONS = new Set([
    'addDays', 'addMonths', 'addYears',
    'subDays', 'subMonths', 'subYears',
    'isAfter', 'isBefore', 'isEqual',
    'differenceInDays', 'differenceInHours', 'differenceInMinutes',
    'startOfDay', 'startOfMonth', 'startOfYear',
    'endOfDay', 'endOfMonth', 'endOfYear',
]);

export class DateFnsToTemporal extends Recipe {
    name = "org.openrewrite.javascript.migrate.date-fns.date-fns-to-temporal";
    displayName = "Migrate date-fns to Temporal API";
    description = "Replaces common date-fns function calls with equivalent native Temporal API usage.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new DateFnsToTemporalVisitor();
    }
}

class DateFnsToTemporalVisitor extends JavaScriptVisitor<ExecutionContext> {
    private importedDateFnsFunctions = new Set<string>();
    private aliasToOriginal = new Map<string, string>();

    protected override async visitJsCompilationUnit(cu: JS.CompilationUnit, ctx: ExecutionContext): Promise<J | undefined> {
        this.importedDateFnsFunctions = new Set();
        this.aliasToOriginal = new Map();

        for (const stmt of cu.statements) {
            const s = stmt.element ?? stmt;
            if (s.kind === JS.Kind.Import) {
                this.collectDateFnsImports(s as JS.Import);
            }
        }

        if (this.importedDateFnsFunctions.size === 0) {
            return cu;
        }

        const result = await super.visitJsCompilationUnit(cu, ctx);

        for (const fn of this.importedDateFnsFunctions) {
            const originalName = this.aliasToOriginal.get(fn) ?? fn;
            maybeRemoveImport(this, 'date-fns', originalName);
        }

        return result;
    }

    private collectDateFnsImports(imp: JS.Import): void {
        const moduleSpecifier = imp.moduleSpecifier;
        if (!moduleSpecifier) return;

        const literal = moduleSpecifier.element;
        if (literal.kind !== J.Kind.Literal) return;
        if ((literal as J.Literal).value !== 'date-fns') return;

        const importClause = imp.importClause;
        if (!importClause?.namedBindings) return;
        if (importClause.namedBindings.kind !== JS.Kind.NamedImports) return;

        const namedImports = importClause.namedBindings as JS.NamedImports;
        for (const elem of namedImports.elements.elements) {
            const specifier = elem.element;
            const specifierNode = specifier.specifier;

            // Simple import: import { addDays } from 'date-fns'
            if (isIdentifier(specifierNode) && DATE_FNS_FUNCTIONS.has(specifierNode.simpleName)) {
                this.importedDateFnsFunctions.add(specifierNode.simpleName);
                continue;
            }

            // Aliased import: import { addDays as plusDays } from 'date-fns'
            if (specifierNode.kind === JS.Kind.Alias) {
                const alias = specifierNode as JS.Alias;
                const imported = alias.propertyName.element;
                const local = alias.alias;
                if (isIdentifier(imported) && isIdentifier(local) && DATE_FNS_FUNCTIONS.has(imported.simpleName)) {
                    this.importedDateFnsFunctions.add(local.simpleName);
                    this.aliasToOriginal.set(local.simpleName, imported.simpleName);
                }
            }
        }
    }

    protected override async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
        method = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;
        if (this.importedDateFnsFunctions.size === 0) return method;

        const name = (method.name as J.Identifier)?.simpleName;
        if (!name || !this.importedDateFnsFunctions.has(name)) return method;

        const originalName = this.aliasToOriginal.get(name) ?? name;
        const rule = this.getRuleForFunction(name, originalName);
        if (!rule) return method;

        return await rule.tryOn(this.cursor, method) || method;
    }

    protected override async visitFunctionCall(functionCall: JS.FunctionCall, ctx: ExecutionContext): Promise<J | undefined> {
        functionCall = await super.visitFunctionCall(functionCall, ctx) as JS.FunctionCall;
        if (this.importedDateFnsFunctions.size === 0) return functionCall;

        const fn = functionCall.function?.element;
        if (!fn || !isIdentifier(fn)) return functionCall;

        const name = fn.simpleName;
        if (!this.importedDateFnsFunctions.has(name)) return functionCall;

        const originalName = this.aliasToOriginal.get(name) ?? name;
        const rule = this.getRuleForFunction(name, originalName);
        if (!rule) return functionCall;

        return await rule.tryOn(this.cursor, functionCall) || functionCall;
    }

    private _ruleCache = new Map<string, RewriteRule>();

    private getRuleForFunction(localName: string, originalName: string): RewriteRule | undefined {
        if (this._ruleCache.has(localName)) return this._ruleCache.get(localName)!;
        const rule = this.buildRule(localName, originalName);
        if (rule) this._ruleCache.set(localName, rule);
        return rule;
    }

    private buildRule(localName: string, originalName: string): RewriteRule | undefined {
        switch (originalName) {
            case 'addDays': return this.addSubRule(localName, 'days', 'add');
            case 'addMonths': return this.addSubRule(localName, 'months', 'add');
            case 'addYears': return this.addSubRule(localName, 'years', 'add');
            case 'subDays': return this.addSubRule(localName, 'days', 'subtract');
            case 'subMonths': return this.addSubRule(localName, 'months', 'subtract');
            case 'subYears': return this.addSubRule(localName, 'years', 'subtract');

            case 'isAfter': return this.compareRule(localName, '> 0');
            case 'isBefore': return this.compareRule(localName, '< 0');
            case 'isEqual': return this.compareRule(localName, '=== 0');

            case 'differenceInDays': return this.differenceRule(localName, 'day', 'days');
            case 'differenceInHours': return this.differenceRule(localName, 'hour', 'hours');
            case 'differenceInMinutes': return this.differenceRule(localName, 'minute', 'minutes');

            case 'startOfDay': return this.singleArgRule(localName, 'Temporal.PlainDate.from(', ').toPlainDateTime()');
            case 'startOfMonth': return this.singleArgRule(localName, 'Temporal.PlainDate.from(', ').with({day: 1}).toPlainDateTime()');
            case 'startOfYear': return this.singleArgRule(localName, 'Temporal.PlainDate.from(', ').with({month: 1, day: 1}).toPlainDateTime()');
            case 'endOfDay': return this.singleArgRule(localName, 'Temporal.PlainDate.from(', ').toPlainDateTime({hour: 23, minute: 59, second: 59, millisecond: 999})');
            case 'endOfMonth': return this.endOfMonthRule(localName);
            case 'endOfYear': return this.singleArgRule(localName, 'Temporal.PlainDate.from(', ').with({month: 12, day: 31}).toPlainDateTime({hour: 23, minute: 59, second: 59, millisecond: 999})');

            default: return undefined;
        }
    }

    private addSubRule(fnName: string, unit: string, method: 'add' | 'subtract'): RewriteRule {
        const date = capture('date');
        const amount = capture('amount');

        const pat = Pattern.builder()
            .code(`${fnName}(`)
            .capture(date)
            .code(', ')
            .capture(amount)
            .code(')')
            .build();

        const tmpl = Template.builder()
            .code(`Temporal.PlainDate.from(`)
            .param(date)
            .code(`).${method}({ ${unit}: `)
            .param(amount)
            .code(' })')
            .build();

        return rewrite(() => ({ before: pat, after: tmpl }));
    }

    private compareRule(fnName: string, operator: string): RewriteRule {
        const a = capture('a');
        const b = capture('b');

        const pat = Pattern.builder()
            .code(`${fnName}(`)
            .capture(a)
            .code(', ')
            .capture(b)
            .code(')')
            .build();

        const tmpl = Template.builder()
            .code('Temporal.PlainDateTime.compare(')
            .param(a)
            .code(', ')
            .param(b)
            .code(`) ${operator}`)
            .build();

        return rewrite(() => ({ before: pat, after: tmpl }));
    }

    private differenceRule(fnName: string, largestUnit: string, field: string): RewriteRule {
        const a = capture('a');
        const b = capture('b');

        const pat = Pattern.builder()
            .code(`${fnName}(`)
            .capture(a)
            .code(', ')
            .capture(b)
            .code(')')
            .build();

        // date-fns differenceIn*(a, b) computes a - b, so use a.since(b)
        const tmpl = Template.builder()
            .param(a)
            .code(`.since(`)
            .param(b)
            .code(`, {largestUnit: "${largestUnit}"}).${field}`)
            .build();

        return rewrite(() => ({ before: pat, after: tmpl }));
    }

    private singleArgRule(fnName: string, prefix: string, suffix: string): RewriteRule {
        const date = capture('date');

        const pat = Pattern.builder()
            .code(`${fnName}(`)
            .capture(date)
            .code(')')
            .build();

        const tmpl = Template.builder()
            .code(prefix)
            .param(date)
            .code(suffix)
            .build();

        return rewrite(() => ({ before: pat, after: tmpl }));
    }

    private endOfMonthRule(fnName: string = 'endOfMonth'): RewriteRule {
        const date = capture('date');

        const pat = Pattern.builder()
            .code(`${fnName}(`)
            .capture(date)
            .code(')')
            .build();

        // Use IIFE to avoid referencing .daysInMonth on the unconverted input
        const tmpl = Template.builder()
            .code('((d) => d.with({day: d.daysInMonth}).toPlainDateTime({hour: 23, minute: 59, second: 59, millisecond: 999}))(Temporal.PlainDate.from(')
            .param(date)
            .code('))')
            .build();

        return rewrite(() => ({ before: pat, after: tmpl }));
    }
}
