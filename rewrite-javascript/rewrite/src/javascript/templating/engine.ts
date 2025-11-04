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
import {Cursor, isTree} from '../..';
import {J} from '../../java';
import {JS} from '..';
import {produce} from 'immer';
import {TemplateCache, PlaceholderUtils} from './utils';
import {CaptureImpl, TemplateParamImpl, CaptureValue, CAPTURE_NAME_SYMBOL} from './capture';
import {PlaceholderReplacementVisitor} from './placeholder-replacement';
import {JavaCoordinates} from './template';

/**
 * Cache for compiled templates.
 */
const templateCache = new TemplateCache();

/**
 * Parameter specification for template generation.
 * Represents a placeholder in a template that will be replaced with a parameter value.
 */
export interface Parameter {
    /**
     * The value to substitute into the template.
     */
    value: any;
}

/**
 * Internal template engine - handles the core templating logic.
 * Not exported from index, so only visible within the templating module.
 */
export class TemplateEngine {
    /**
     * Applies a template with optional match results from pattern matching.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     * @param cursor The cursor pointing to the current location in the AST
     * @param coordinates The coordinates specifying where and how to insert the generated AST
     * @param values Map of capture names to values to replace the parameters with
     * @param wrappersMap Map of capture names to J.RightPadded wrappers (for preserving markers)
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     * @returns A Promise resolving to the generated AST node
     */
    static async applyTemplate(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        cursor: Cursor,
        coordinates: JavaCoordinates,
        values: Pick<Map<string, J>, 'get'> = new Map(),
        wrappersMap: Pick<Map<string, J.RightPadded<J> | J.RightPadded<J>[]>, 'get'> = new Map(),
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {}
    ): Promise<J | undefined> {
        // Build the template string with parameter placeholders
        const templateString = TemplateEngine.buildTemplateString(templateParts, parameters);

        // If the template string is empty, return undefined
        if (!templateString.trim()) {
            return undefined;
        }

        // Use cache to get or parse the compilation unit
        // For templates, we don't have captures, so use empty array
        const cu = await templateCache.getOrParse(
            templateString,
            [], // templates don't have captures in the cache key
            contextStatements,
            dependencies
        );

        // Check if there are any statements
        if (!cu.statements || cu.statements.length === 0) {
            throw new Error(`Failed to parse template code (no statements):\n${templateString}`);
        }

        // Skip context statements to get to the actual template code
        const templateStatementIndex = contextStatements.length;
        if (templateStatementIndex >= cu.statements.length) {
            return undefined;
        }

        // Extract the relevant part of the AST
        const firstStatement = cu.statements[templateStatementIndex].element;
        let extracted: J;

        // Check if this is a wrapped template (function __TEMPLATE__() { ... })
        if (firstStatement.kind === J.Kind.MethodDeclaration) {
            const func = firstStatement as J.MethodDeclaration;
            if (func.name.simpleName === '__TEMPLATE__' && func.body) {
                // __TEMPLATE__ wrapper indicates the original template was a block.
                // Always return the block to preserve the block structure.
                extracted = func.body;
            } else {
                // Not a __TEMPLATE__ wrapper
                extracted = firstStatement;
            }
        } else if (firstStatement.kind === JS.Kind.ExpressionStatement) {
            extracted = (firstStatement as JS.ExpressionStatement).expression;
        } else {
            extracted = firstStatement;
        }

        // Create a copy to avoid sharing cached AST instances
        const ast = produce(extracted, _ => {});

        // Create substitutions map for placeholders
        const substitutions = new Map<string, Parameter>();
        for (let i = 0; i < parameters.length; i++) {
            const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
            substitutions.set(placeholder, parameters[i]);
        }

        // Unsubstitute placeholders with actual parameter values and match results
        const visitor = new PlaceholderReplacementVisitor(substitutions, values, wrappersMap);
        const unsubstitutedAst = (await visitor.visit(ast, null))!;

        // Apply the template to the current AST
        return new TemplateApplier(cursor, coordinates, unsubstitutedAst).apply();
    }

    /**
     * Builds a template string with parameter placeholders.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     * @returns The template string
     */
    private static buildTemplateString(
        templateParts: TemplateStringsArray,
        parameters: Parameter[]
    ): string {
        let result = '';
        for (let i = 0; i < templateParts.length; i++) {
            result += templateParts[i];
            if (i < parameters.length) {
                const param = parameters[i].value;
                // Use a placeholder for Captures, TemplateParams, CaptureValues, Tree nodes, and Tree arrays
                // Inline everything else (strings, numbers, booleans) directly
                // Check for Capture (could be a Proxy, so check for symbol property)
                const isCapture = param instanceof CaptureImpl ||
                                (param && typeof param === 'object' && param[CAPTURE_NAME_SYMBOL]);
                const isTemplateParam = param instanceof TemplateParamImpl;
                const isCaptureValue = param instanceof CaptureValue;
                const isTreeArray = Array.isArray(param) && param.length > 0 && isTree(param[0]);
                if (isCapture || isTemplateParam || isCaptureValue || isTree(param) || isTreeArray) {
                    const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
                    result += placeholder;
                } else {
                    result += param;
                }
            }
        }

        // Detect if this is a block template that needs wrapping
        const trimmed = result.trim();
        if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
            result = `function __TEMPLATE__() ${result}`;
        }

        return result;
    }
}

/**
 * Helper class for applying a template to an AST.
 */
export class TemplateApplier {
    constructor(
        private readonly cursor: Cursor,
        private readonly coordinates: JavaCoordinates,
        private readonly ast: J
    ) {
    }

    /**
     * Applies the template to the current AST.
     *
     * @returns A Promise resolving to the modified AST
     */
    async apply(): Promise<J | undefined> {
        const {loc} = this.coordinates;

        // Apply the template based on the location and mode
        switch (loc || 'EXPRESSION_PREFIX') {
            case 'EXPRESSION_PREFIX':
                return this.applyToExpression();
            case 'STATEMENT_PREFIX':
                return this.applyToStatement();
            case 'BLOCK_END':
                return this.applyToBlock();
            default:
                throw new Error(`Unsupported location: ${loc}`);
        }
    }

    /**
     * Applies the template to an expression.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyToExpression(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return tree ? produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        }) : this.ast;
    }

    /**
     * Applies the template to a statement.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyToStatement(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        });
    }

    /**
     * Applies the template to a block.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyToBlock(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        });
    }
}
