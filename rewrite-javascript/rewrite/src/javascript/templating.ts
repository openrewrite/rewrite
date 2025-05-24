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
import {isExpressionStatement, JavaScriptVisitor, JS} from '.';
import {JavaScriptParser} from './parser';
import {JavaScriptPrinter} from './print';
import {Cursor, isScope, isTree, mapAsync, PrintOutputCapture, Tree} from '..';
import {emptySpace, Expression, isLiteral, J, rightPadded, Statement,} from '../java';
import {produce} from "immer";
import {isStatement} from "./parser-utils";
import {AutoformatVisitor} from "./format";

export type JavaCoordinates = {
    tree: Tree;
    loc: JavaCoordinates.Location;
    mode: JavaCoordinates.Mode;
};

export namespace JavaCoordinates {
    // FIXME need to come up with the equivalent of `Space.Location` support
    export type Location = 'EXPRESSION_PREFIX' | 'STATEMENT_PREFIX' | 'BLOCK_END';
    export enum Mode {
        Before,
        After,
        Replace,
    }
}

export class JavaScriptTemplate {
    private readonly _templateParser: JavaScriptTemplateParser;

    constructor(
        public readonly code: string,
        parser: JavaScriptParser = new JavaScriptParser(),
        public readonly onAfterVariableSubstitution?: (substituted: string) => void
    ) {
        this._templateParser = new JavaScriptTemplateParser(
            false,
            parser,
            onAfterVariableSubstitution
        );
    }

    async apply(scope: Cursor, coordinates: JavaCoordinates, ...parameters: any[]): Promise<J | undefined> {
        const substitutions = this.substitutions(parameters);
        const substituted = await substitutions.substitute();

        if (this.onAfterVariableSubstitution) {
            this.onAfterVariableSubstitution(substituted);
        }

        return new JavaScriptTemplateJavaScriptExtension(
            this._templateParser,
            substitutions,
            substituted,
            coordinates
        ).visit(scope.value as Tree, 0, scope.parent!);
    }

    substitutions(parameters: any[]): Substitutions {
        return new Substitutions(this.code, parameters);
    }
}

export class JavaScriptTemplateParser {
    constructor(
        public readonly contextSensitive: boolean,
        public readonly parser: JavaScriptParser,
        public readonly onAfterVariableSubstitution?: (substituted: string) => void
    ) {}

    async parseExpression(scope: Cursor, template: string, loc: JavaCoordinates.Location): Promise<J> {
        const parseResults = await this.parser.parse({text: template, sourcePath: 'template.ts'});
        const cu: JS.CompilationUnit = parseResults[0] as JS.CompilationUnit;

        const firstStatement = cu.statements[0].element;
        const j = isExpressionStatement(firstStatement)
            ? firstStatement.expression
            : firstStatement;

        return produce(j, draft => {
            draft.prefix = (scope.value as J).prefix;
        });
    }

    async parseBlockStatements(
        cursor: Cursor,
        template: string,
        loc: JavaCoordinates.Location,
        mode: JavaCoordinates.Mode
    ): Promise<J[]> {
        const parseResults = await this.parser.parse({text: template, sourcePath: 'template.ts'});
        const cu: JS.CompilationUnit = parseResults[0] as JS.CompilationUnit;
        return cu.statements.map(stmt => stmt.element);
    }
}

export class Substitutions {
    private static readonly PARAM_PATTERN = /__p(\d+)__/;

    constructor(
        public readonly code: string,
        public readonly parameters: (J | string)[]
    ) {}

    async substitute(): Promise<string> {
        let result = "";
        let pos = 0;
        let paramCount = 0;
        const namedIndex: Record<string, number> = {};

        while (true) {
            // Find next #{
            const start = this.code.indexOf('#{', pos);
            if (start === -1) {
                // No more parameters, add remaining code
                result += this.code.substring(pos);
                break;
            }

            // Add code before the #{
            result += this.code.substring(pos, start);

            // Find matching }
            let braceCount = 1;
            let i = start + 2; // Skip past #{
            while (i < this.code.length && braceCount > 0) {
                if (this.code[i] === '{') {
                    braceCount++;
                } else if (this.code[i] === '}') {
                    braceCount--;
                }
                i++;
            }

            if (braceCount > 0) {
                throw new Error("Unmatched { in template");
            }

            const pattern = this.code.substring(start + 2, i - 1).trim();
            const paramIndex = (!pattern || pattern.includes('any()'))
                ? paramCount
                : namedIndex[pattern];

            const sepIndex = pattern.indexOf(':');
            if (sepIndex >= 0) {
                namedIndex[pattern.substring(0, sepIndex)] = paramIndex;
            }

            // Replace the #{...} with parameter placeholder
            result += pattern
                ? `__p${paramIndex}__`
                : await this.substituteUntyped(paramIndex);

            if (!pattern || pattern.includes('any()')) {
                paramCount++;
            }

            pos = i;
        }

        return result;
    }

    private async substituteUntyped(index: number): Promise<string> {
        const param = this.parameters[index];
        if (isLiteral(param)) {
            const capture = new PrintOutputCapture();
            await new JavaScriptPrinter().visit(param, capture);
            return capture.out;
        }
        return String(param);
    }

    async unsubstitute(parsed: J): Promise<J | undefined> {
        return new UnsubstitutionVisitor(this.parameters).visit(parsed, 0);
    }

    async unsubstituteAll(parsed: J[]): Promise<J[]> {
        return mapAsync(parsed, j => this.unsubstitute(j));
    }
}

export class UnsubstitutionVisitor extends JavaScriptVisitor<unknown> {
    private static readonly PARAM_PATTERN = /__p(\d+)__/;

    constructor(public readonly parameters: (J | string)[]) {
        super();
    }

    override async visitIdentifier(identifier: J.Identifier, _p: unknown): Promise<J> {
        const match = identifier.simpleName.match(UnsubstitutionVisitor.PARAM_PATTERN);
        if (match) {
            const paramIndex = parseInt(match[1], 10);
            return produce(this.parameters[paramIndex] as J, draft => {
                draft.prefix = identifier.prefix;
            });
        }
        return identifier;
    }
}

export class JavaScriptTemplateJavaScriptExtension extends JavaScriptVisitor<unknown> {
    private readonly insertionPoint: Tree;
    private readonly loc: JavaCoordinates.Location;
    private readonly mode: JavaCoordinates.Mode;

    constructor(
        public readonly templateParser: JavaScriptTemplateParser,
        public readonly substitutions: Substitutions,
        public readonly substitutedTemplate: string,
        public readonly coordinates: JavaCoordinates
    ) {
        super();
        this.insertionPoint = coordinates.tree;
        this.loc = coordinates.loc;
        this.mode = coordinates.mode;
    }

    override async visitExpression(expression: Expression, p: unknown): Promise<J> {
        const isExpressionPrefix = this.loc === 'EXPRESSION_PREFIX';
        const isStatementPrefixForStatement = this.loc === 'STATEMENT_PREFIX' &&
            isStatement(expression);

        if ((isExpressionPrefix || isStatementPrefixForStatement) &&
            isScope(expression, this.insertionPoint)) {
            const parsed = await this.templateParser.parseExpression(
                this.cursor,
                this.substitutedTemplate,
                this.loc
            );
            return (await this.autoFormat(
                produce((await this.substitutions.unsubstitute(parsed))!, draft => {
                    draft.prefix = expression.prefix;
                }),
                p
            ))!;
        }

        return expression;
    }

    override async visitBlock(block: J.Block, p: unknown): Promise<J | undefined> {
        if (this.loc === 'BLOCK_END' && isScope(block, this.insertionPoint)) {
            const parsed = await this.templateParser.parseBlockStatements(
                new Cursor(this.insertionPoint, this.cursor),
                this.substitutedTemplate,
                this.loc,
                this.mode
            );

            const gen: Statement[] = (await this.substitutions.unsubstituteAll(parsed)) as Statement[];

            return gen.length > 0
                ? this.autoFormat(
                    produce(block, draft => {
                        draft.statements = [...draft.statements, ...gen.map(s => rightPadded(s, emptySpace))];
                    }),
                    p,
                    this.cursor.parent
                )
                : block;
        }

        if (this.loc === 'STATEMENT_PREFIX') {
            const newStatements: J.RightPadded<Statement>[] = [];
            for (const s of block.statements) {
                if (isScope(s.element, this.insertionPoint)) {
                    const replacements = await this.getReplacements(s.element) as Statement[];
                    newStatements.push(...(replacements.map(s2 => rightPadded(s2, emptySpace))));
                } else {
                    newStatements.push(s);
                }
            }

            return this.autoFormat(
                produce(block, draft => {
                    draft.statements = newStatements
                }),
                p,
                this.cursor.parent
            );
        }

        return await super.visitBlock(block, p);
    }

    override async visitStatement(statement: Statement, p: unknown): Promise<J> {
        return statement;
        // if (this.loc === Space.Location.STATEMENT_PREFIX && isScope(statement, this.insertionPoint)) {
        //   const parsed = this.templateParser.parseExpression(this.cursor, this.substitutedTemplate, this.loc);
        //   return this.autoFormat(this.substitutions.unsubstitute(parsed).withPrefix(expression.prefix), p);
        // }
        // return expression;
    }

    private async getReplacements(statement: Statement): Promise<J[]> {
        const parsed = await this.templateParser.parseBlockStatements(
            new Cursor(this.insertionPoint, this.cursor),
            this.substitutedTemplate,
            this.loc,
            this.mode
        );

        const gen = await this.substitutions.unsubstituteAll(parsed);
        const formatted = gen.map(s =>
            produce(s as Statement, draft => {
                draft.prefix = statement.prefix;
                draft.prefix.comments = [];
            })
        );

        switch (this.mode) {
            case JavaCoordinates.Mode.Replace:
                return formatted;
            case JavaCoordinates.Mode.Before:
                return [...formatted, statement];
            case JavaCoordinates.Mode.After:
                return [statement, ...formatted];
            default:
                throw new Error("Unknown mode: " + this.mode);
        }
    }

    private async autoFormat(j: J, p: unknown, cursor?: Cursor): Promise<J | undefined> {
        let visitor = new AutoformatVisitor();
        let parent = (cursor || this.cursor).parent!;
        while (!isTree(parent.value)) {
            parent = parent.parent!;
        }
        return await visitor.visit(j, p, parent);
    }
}