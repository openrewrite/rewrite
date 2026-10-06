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
import {Cursor, isTree, Marker, Markers} from '../..';
import {Expression, J} from '../../java';
import {JS} from '..';
import {JavaScriptVisitor} from '../visitor';
import {create as produce} from 'mutative';
import {PlaceholderUtils} from './utils';
import {CaptureImpl, TemplateParamImpl, CaptureValue, CAPTURE_NAME_SYMBOL} from './capture';
import {enclosingTree, maybeParenthesize} from './precedence';
import {Parameter} from './types';
import {isExpression} from '../parser-utils';
import {randomId} from '../../uuid';

/**
 * Marks a value the template substituted, whose layout is the source's rather than the template's.
 * `ownPrefix` says whether the whitespace before it is the source's too.
 */
export class SubstitutedValue implements Marker {
    static readonly KIND = 'org.openrewrite.javascript.templating.SubstitutedValue';
    readonly kind = SubstitutedValue.KIND;
    readonly id = randomId();

    constructor(readonly ownPrefix: boolean) {
    }
}

/** Chooses a substituted value's prefix, and says whether it is the source's own. */
type MergePrefix = (sourcePrefix: J.Space, templatePrefix: J.Space) => [J.Space, boolean];

/**
 * Visitor that replaces placeholder nodes with actual parameter values.
 */
export class PlaceholderReplacementVisitor extends JavaScriptVisitor<any> {
    /** Whether any value was substituted, and so marked as a {@link SubstitutedValue}. */
    substituted = false;

    constructor(
        private readonly substitutions: Map<string, Parameter>,
        private readonly values: Pick<Map<string, J | J[]>, 'get'> = new Map(),
        private readonly wrappersMap: Pick<Map<string, J.RightPadded<J> | J.RightPadded<J>[]>, 'get'> = new Map()
    ) {
        super();
    }

    async visit<R extends J>(tree: J, p: any, parent?: Cursor): Promise<R | undefined> {
        // Check if this node is a placeholder
        // BUT: Don't handle `JS.BindingElement` here - let `visitBindingElement` preserve `propertyName`
        if (tree.kind !== JS.Kind.BindingElement && this.isPlaceholder(tree)) {
            const replacement = this.replacePlaceholder(tree);
            if (replacement !== tree) {
                // `this.cursor` is still the enclosing template node: `super.visit()` has not pushed this one
                return maybeParenthesize(enclosingTree(parent ?? this.cursor), tree.id, replacement, tree.markers) as R;
            }
        }

        // Continue with normal traversal
        return super.visit(tree, p, parent);
    }

    /**
     * Override visitBindingElement to preserve propertyName from template when replacing.
     * For example, in `{ ref: ${ref} }`, we want to preserve `ref:` when replacing ${ref}.
     */
    override async visitBindingElement(bindingElement: JS.BindingElement, p: any): Promise<J | undefined> {
        // Visit the name to potentially replace placeholders
        const visitedName = await this.visit(bindingElement.name, p);

        // If the name changed (placeholder was replaced), preserve the BindingElement structure
        // including the propertyName from the template
        if (visitedName !== bindingElement.name) {
            return produce(bindingElement, draft => {
                draft.name = visitedName as any;
                // propertyName is already set from the template and will be preserved by produce
            });
        }

        return bindingElement;
    }

    /** A statement substituted into an expression statement's placeholder is the statement itself. */
    override async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, p: any): Promise<J | undefined> {
        const visited = await super.visitExpressionStatement(expressionStatement, p);
        if (visited?.kind !== JS.Kind.ExpressionStatement) {
            return visited;
        }
        const statement = visited as JS.ExpressionStatement;
        const expression: J = statement.expression;
        if (isExpression(expression)) {
            return statement;
        }
        return {...expression, prefix: this.concatPrefix(statement.prefix, expression.prefix)};
    }

    /**
     * A declaration substituted for the name of a declaration is the declaration itself: `(${p})` and
     * `(...${p})` with `p` bound to the parameter `props: Props` give `(props: Props)` and `(...props: Props)`.
     */
    override async visitVariableDeclarations(variableDeclarations: J.VariableDeclarations, p: any): Promise<J | undefined> {
        const visited = await super.visitVariableDeclarations(variableDeclarations, p);
        if (visited?.kind !== J.Kind.VariableDeclarations) {
            return visited;
        }
        const outer = visited as J.VariableDeclarations;
        if (outer.variables.length !== 1) {
            return outer;
        }
        const variable = outer.variables[0].element;
        const spread = variable.name.kind === JS.Kind.Spread ? variable.name as JS.Spread : undefined;
        const declared: J = spread ? spread.expression : variable.name;
        if (declared.kind !== J.Kind.VariableDeclarations) {
            return outer;
        }
        const inner = declared as J.VariableDeclarations;
        const prefix = this.concatPrefix(this.concatPrefix(outer.prefix, variable.prefix), inner.prefix);
        if (!spread) {
            return {...inner, prefix};
        } else if (inner.variables.length !== 1) {
            return outer;
        }
        const innerVariable = inner.variables[0];
        const spreadDeclaration: J.VariableDeclarations = {
            ...inner,
            prefix,
            variables: [{
                ...innerVariable,
                element: {...innerVariable.element, name: {...spread, expression: innerVariable.element.name as Expression} as JS.Spread}
            }]
        };
        return spreadDeclaration;
    }

    private concatPrefix(outer: J.Space, inner: J.Space): J.Space {
        if (outer.whitespace === '' && outer.comments.length === 0) {
            return inner;
        } else if (outer.comments.length === 0) {
            return {...inner, whitespace: outer.whitespace + inner.whitespace};
        }
        const last = outer.comments[outer.comments.length - 1];
        return {
            ...outer,
            comments: [...outer.comments.slice(0, -1), {...last, suffix: last.suffix + inner.whitespace}, ...inner.comments]
        };
    }

    /**
     * Override visitContainer to handle variadic expansion for containers.
     * This handles J.Container instances anywhere in the AST (method arguments, etc.).
     */
    override async visitContainer<T extends J>(container: J.Container<T>, p: any): Promise<J.Container<T>> {
        // Check if any elements are placeholders (possibly variadic)
        const hasPlaceholder = container.elements.some(elem => this.isPlaceholder(elem.element));

        if (!hasPlaceholder) {
            return super.visitContainer(container, p);
        }

        // A container's element layout is the author's; a block lays its statements out by indentation
        const newElements = await this.expandVariadicElements(container.elements, undefined, p, true);

        return produce(container, draft => {
            draft.elements = newElements as any;
        });
    }

    /**
     * Override visitRightPadded to handle single placeholder replacements.
     * The base implementation will visit the element, which triggers our visit() override
     * for placeholder detection and replacement.
     */
    override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: any): Promise<J.RightPadded<T> | undefined> {
        return super.visitRightPadded(right, p);
    }

    /**
     * Override visitBlock to handle variadic expansion in block statements.
     * Block.statements is J.RightPadded<Statement>[] (not a Container), so we need
     * array-level access for variadic expansion.
     */
    override async visitBlock(block: J.Block, p: any): Promise<J | undefined> {
        // An object literal's body is a block too, whose placeholders are shorthand properties
        const unwrapStatement = (element: J): J => {
            if (element.kind === JS.Kind.ExpressionStatement) {
                return (element as JS.ExpressionStatement).expression;
            }
            return PlaceholderUtils.shorthandPropertyName(element) ?? element;
        };

        if (!block.statements.some(stmt => this.isPlaceholder(unwrapStatement(stmt.element)))) {
            return super.visitBlock(block, p);
        }

        const newStatements = await this.expandVariadicElements(block.statements, unwrapStatement, p);

        return produce(block, draft => {
            draft.statements = newStatements;
        });
    }

    /**
     * Override visitJsCompilationUnit to handle variadic expansion in top-level statements.
     * CompilationUnit.statements is J.RightPadded<Statement>[] (not a Container), so we need
     * array-level access for variadic expansion.
     */
    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, p: any): Promise<J | undefined> {
        const hasPlaceholder = compilationUnit.statements.some(stmt => this.isPlaceholder(stmt.element));

        if (!hasPlaceholder) {
            return super.visitJsCompilationUnit(compilationUnit, p);
        }

        const newStatements = await this.expandVariadicElements(compilationUnit.statements, undefined, p);

        return produce(compilationUnit, draft => {
            draft.statements = newStatements;
        });
    }

    /**
     * Merges prefixes by preserving comments from the source element
     * while using whitespace from the template placeholder.
     *
     * @param sourcePrefix The prefix from the captured element (may contain comments)
     * @param templatePrefix The prefix from the template placeholder (defines whitespace)
     * @returns A merged prefix with source comments and template whitespace
     */
    private mergePrefix(sourcePrefix: J.Space, templatePrefix: J.Space): J.Space {
        // If source has no comments, just use template prefix
        if (sourcePrefix.comments.length === 0) {
            return templatePrefix;
        }

        // Preserve comments from source, use whitespace from template
        return {
            kind: J.Kind.Space,
            comments: sourcePrefix.comments,
            whitespace: templatePrefix.whitespace
        };
    }

    private readonly templatePrefix: MergePrefix = (source, template) => [this.mergePrefix(source, template), false];

    /** As `mergePrefix`, but a line break the source wrote and the template did not is the source's layout. */
    private readonly elementPrefix: MergePrefix = (source, template) =>
        source.whitespace.includes('\n') && !template.whitespace.includes('\n') ?
            [source, true] : this.templatePrefix(source, template);

    /** `value` substituted for `placeholder` under the prefix `merge` chooses, marked as a {@link SubstitutedValue}. */
    private substitute(value: J, placeholder: J, merge: MergePrefix): J {
        this.substituted = true;
        const [prefix, ownPrefix] = merge(value.prefix, placeholder.prefix);
        const markers = this.mergeMarkers(value.markers, placeholder.markers);
        return produce(value, draft => {
            draft.prefix = prefix;
            draft.markers = {...markers, markers: [...markers.markers, new SubstitutedValue(ownPrefix)]};
        });
    }

    /** As `mergePrefix`, for markers: everything the value was matched with, plus the kinds only the slot wrote. */
    private mergeMarkers(sourceMarkers: Markers, templateMarkers: Markers): Markers {
        const added = templateMarkers.markers.filter(
            template => !sourceMarkers.markers.some(source => source.kind === template.kind));
        return added.length === 0 ? sourceMarkers : {
            ...sourceMarkers,
            markers: [...sourceMarkers.markers, ...added]
        };
    }

    /**
     * Expands variadic placeholders in a list of elements.
     *
     * @param elements The list of wrapped elements to process
     * @param unwrapElement Optional function to unwrap the placeholder node from its container (e.g., ExpressionStatement)
     * @param p Context parameter for visitor
     * @returns Promise of new list with placeholders expanded
     */
    private async expandVariadicElements(
        elements: J.RightPadded<J>[],
        unwrapElement: (element: J) => J = (e) => e,
        p: any,
        ownLayout: boolean = false
    ): Promise<J.RightPadded<J>[]> {
        const newElements: J.RightPadded<J>[] = [];
        const merge = ownLayout ? this.elementPrefix : this.templatePrefix;
        // Past the first item, an item's prefix is the source's separator from the one before it
        const keep: MergePrefix = source => [source, ownLayout];

        for (const wrapped of elements) {
            const element = wrapped.element;
            const placeholderNode = unwrapElement(element);

            // Check if this element contains a placeholder
            if (this.isPlaceholder(placeholderNode)) {
                const placeholderText = this.getPlaceholderText(placeholderNode);
                if (placeholderText) {
                    const param = this.substitutions.get(placeholderText);
                    if (param) {
                        let arrayToExpand: J[] | J.RightPadded<J>[] | undefined = undefined;

                        // Check if it's a J.Container
                        const isContainer = param.value && typeof param.value === 'object' &&
                            param.value.kind === J.Kind.Container;
                        if (isContainer) {
                            // Extract elements from J.Container
                            arrayToExpand = param.value.elements as J.RightPadded<J>[];
                        }
                        // Check if it's a direct Tree[] array
                        else if (Array.isArray(param.value)) {
                            arrayToExpand = param.value as J[];
                        }
                        // Check if it's a CaptureValue
                        else if (param.value instanceof CaptureValue) {
                            const resolved = param.value.resolve(this.values);
                            if (Array.isArray(resolved)) {
                                arrayToExpand = resolved;
                            }
                        }
                        // Check if it's a direct variadic capture
                        else {
                            const isCapture = param.value instanceof CaptureImpl ||
                                (param.value && typeof param.value === 'object' && param.value[CAPTURE_NAME_SYMBOL]);
                            if (isCapture) {
                                const name = param.value[CAPTURE_NAME_SYMBOL] || param.value.name;
                                const capture = Array.from(this.substitutions.values())
                                    .map(p => p.value)
                                    .find(v => v instanceof CaptureImpl && v.getName() === name) as CaptureImpl | undefined;

                                if (capture?.isVariadic()) {
                                    // Prefer wrappers if available (to preserve markers like Semicolon)
                                    // Otherwise fall back to elements
                                    const wrappersArray = this.wrappersMap.get(name);
                                    if (Array.isArray(wrappersArray)) {
                                        arrayToExpand = wrappersArray;
                                    } else {
                                        const matchedArray = this.values.get(name);
                                        if (Array.isArray(matchedArray)) {
                                            arrayToExpand = matchedArray;
                                        }
                                    }
                                }
                            }
                        }

                        // Expand the array if we found one
                        if (arrayToExpand !== undefined) {
                            if (arrayToExpand.length > 0) {
                                for (let i = 0; i < arrayToExpand.length; i++) {
                                    const item = arrayToExpand[i];

                                    // Check if item is a JRightPadded wrapper or just an element
                                    // JRightPadded wrappers have 'element', 'after', and 'markers' properties
                                    // Also ensure the element field is not null
                                    const isWrapper = item && typeof item === 'object' && 'element' in item && 'after' in item && item.element != null;

                                    if (isWrapper) {
                                        // Item is a JRightPadded wrapper - use it directly to preserve markers
                                        const wrapper = item as J.RightPadded<J>;
                                        // Keep all other wrapper properties (including markers with Semicolon)
                                        newElements.push({
                                            ...wrapper,
                                            element: this.substitute(wrapper.element, element, i > 0 ? keep : merge)
                                        });
                                    } else if (item) {
                                        // Item is just an element (not a wrapper) - wrap it (backward compatibility)
                                        newElements.push({
                                            ...wrapped,
                                            element: this.substitute(item as J, element, i > 0 ? keep : merge)
                                        });
                                    }
                                }
                                continue; // Skip adding the placeholder itself
                            } else {
                                // Empty array - don't add any elements
                                continue;
                            }
                        }
                    }
                }
            }

            // Not a placeholder (or expansion failed) - process normally
            const slot = ownLayout && element.kind !== JS.Kind.BindingElement && this.isPlaceholder(element);
            const replacedElement = slot ?
                this.replaceElement(element, merge) :
                await this.visit(element, p);
            if (replacedElement) {
                // Check if the replacement came from a capture with a wrapper (to preserve markers)
                const placeholderNode = unwrapElement(element);
                const placeholderText = this.getPlaceholderText(placeholderNode);
                let wrapperToUse = wrapped;

                // A shorthand property keeps its own wrapper, since a captured one belongs to the name alone
                if (placeholderText && this.isPlaceholder(placeholderNode) && element.kind !== JS.Kind.PropertyAssignment) {
                    const param = this.substitutions.get(placeholderText);
                    if (param) {
                        const isCapture = param.value instanceof CaptureImpl ||
                            (param.value && typeof param.value === 'object' && param.value[CAPTURE_NAME_SYMBOL]);
                        if (isCapture) {
                            const name = param.value[CAPTURE_NAME_SYMBOL] || param.value.name;
                            const wrapper = this.wrappersMap.get(name);
                            // Use captured wrapper if available and not an array (non-variadic)
                            if (wrapper && !Array.isArray(wrapper)) {
                                wrapperToUse = wrapper as J.RightPadded<J>;
                            }
                        }
                    }
                }

                newElements.push(produce(wrapperToUse, draft => {
                    draft.element = replacedElement;
                }));
            }
        }

        return newElements;
    }

    /** As `visit` on a placeholder, with `merge` choosing the replacement's prefix. */
    private replaceElement(placeholder: J, merge: MergePrefix): J {
        const replacement = this.replacePlaceholder(placeholder, merge);
        return replacement === placeholder ? placeholder :
            maybeParenthesize(enclosingTree(this.cursor), placeholder.id, replacement, placeholder.markers);
    }

    /**
     * Checks if a node is a placeholder.
     *
     * @param node The node to check
     * @returns True if the node is a placeholder
     */
    private isPlaceholder(node: J): boolean {
        if (node.kind === J.Kind.Identifier) {
            const identifier = node as J.Identifier;
            return identifier.simpleName.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX);
        } else if (node.kind === J.Kind.Literal) {
            const literal = node as J.Literal;
            return literal.valueSource?.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX) || false;
        } else if (node.kind === JS.Kind.BindingElement) {
            // Check if the BindingElement's name is a placeholder
            const bindingElement = node as JS.BindingElement;
            return this.isPlaceholder(bindingElement.name);
        }
        return false;
    }

    /**
     * Replaces a placeholder node with the actual parameter value.
     *
     * @param placeholder The placeholder node
     * @param merge Chooses the replacement's prefix from its own and the placeholder's
     * @returns The replacement node or the original if not a placeholder
     */
    private replacePlaceholder(placeholder: J, merge: MergePrefix = this.templatePrefix): J {
        const placeholderText = this.getPlaceholderText(placeholder);

        if (!placeholderText || !placeholderText.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX)) {
            return placeholder;
        }

        // Find the corresponding parameter
        const param = this.substitutions.get(placeholderText);
        if (!param || param.value === undefined) {
            return placeholder;
        }

        // Check if the parameter value is a CaptureValue
        const isCaptureValue = param.value instanceof CaptureValue;

        if (isCaptureValue) {
            // Resolve the capture value to get the actual property value
            const propertyValue = param.value.resolve(this.values);

            if (propertyValue !== undefined) {
                // If the property value is already a J node, use it
                if (isTree(propertyValue)) {
                    const propValueAsJ = propertyValue as J;
                    return this.substitute(propValueAsJ, placeholder, merge);
                }
                // If it's a primitive value and placeholder is an identifier, update the simpleName
                if (typeof propertyValue === 'string' && placeholder.kind === J.Kind.Identifier) {
                    return produce(placeholder as J.Identifier, draft => {
                        draft.simpleName = propertyValue;
                    });
                }
                // If it's a primitive value and placeholder is a literal, update the value
                if (typeof propertyValue === 'string' && placeholder.kind === J.Kind.Literal) {
                    return produce(placeholder as J.Literal, draft => {
                        draft.value = propertyValue;
                        draft.valueSource = `"${propertyValue}"`;
                    });
                }
            }

            // If no match found or unhandled type, return placeholder unchanged
            return placeholder;
        }

        // Check if the parameter value is a Capture (could be a Proxy) or TemplateParam
        const isCapture = param.value instanceof CaptureImpl ||
            (param.value && typeof param.value === 'object' && param.value[CAPTURE_NAME_SYMBOL]);
        const isTemplateParam = param.value instanceof TemplateParamImpl;

        if (isCapture || isTemplateParam) {
            // Simple capture/template param (no property path for template params)
            const name = isTemplateParam ? param.value.name :
                (param.value[CAPTURE_NAME_SYMBOL] || param.value.name);
            const matchedNode = this.values.get(name);
            if (matchedNode && !Array.isArray(matchedNode)) {
                return this.substitute(matchedNode, placeholder, merge);
            }

            // If no match found, return placeholder unchanged
            return placeholder;
        }

        // Check if the parameter value is a J.RightPadded wrapper
        const isRightPadded = param.value && typeof param.value === 'object' &&
            param.value.kind === J.Kind.RightPadded && isTree(param.value.element);

        if (isRightPadded) {
            // Extract the element from the J.RightPadded wrapper
            const element = param.value.element as J;
            return this.substitute(element, placeholder, merge);
        }

        // Check if the parameter value is a J.Container
        const isContainer = param.value && typeof param.value === 'object' &&
            param.value.kind === J.Kind.Container;

        if (isContainer) {
            // J.Container should be handled by expandVariadicElements
            // For now, return placeholder - the expansion will happen at a higher level
            // This should not happen in normal usage, as containers are typically used in argument positions
            return placeholder;
        }

        // If the parameter value is an AST node, use it directly
        if (isTree(param.value)) {
            // Return the AST node, preserving comments from the source
            const value = param.value as J;
            return this.substitute(value, placeholder, merge);
        }

        return placeholder;
    }

    /**
     * Gets the placeholder text from a node.
     *
     * @param node The node to get placeholder text from
     * @returns The placeholder text or null
     */
    private getPlaceholderText(node: J): string | null {
        if (node.kind === J.Kind.Identifier) {
            return (node as J.Identifier).simpleName;
        } else if (node.kind === J.Kind.Literal) {
            return (node as J.Literal).valueSource || null;
        } else if (node.kind === JS.Kind.BindingElement) {
            // Extract placeholder text from the BindingElement's name
            const bindingElement = node as JS.BindingElement;
            return this.getPlaceholderText(bindingElement.name);
        }
        return null;
    }

}
