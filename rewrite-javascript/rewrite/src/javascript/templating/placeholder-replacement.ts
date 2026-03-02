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
import {getPaddedElement, isRightPadded, J, Statement} from '../../java';
import {JS} from '..';
import {JavaScriptVisitor} from '../visitor';
import {create as produce} from 'mutative';
import {PlaceholderUtils} from './utils';
import {CaptureImpl, TemplateParamImpl, CaptureValue, CAPTURE_NAME_SYMBOL} from './capture';
import {Parameter} from './types';

/**
 * Visitor that replaces placeholder nodes with actual parameter values.
 */
export class PlaceholderReplacementVisitor extends JavaScriptVisitor<any> {
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
                return replacement as R;
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

    /**
     * Override visitContainer to handle variadic expansion for containers.
     * This handles J.Container instances anywhere in the AST (method arguments, etc.).
     */
    override async visitContainer<T extends J>(container: J.Container<T>, p: any): Promise<J.Container<T>> {
        // Check if any elements are placeholders (possibly variadic)
        // For tree types, the padded value IS the element (intersection type)
        const hasPlaceholder = container.elements.some(elem => this.isPlaceholder(getPaddedElement(elem as J.RightPadded<J>)));

        if (!hasPlaceholder) {
            return super.visitContainer(container, p);
        }

        // Expand variadic placeholders in the container's elements
        const newElements = await this.expandVariadicElements(container.elements, undefined, p);

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
        const hasPlaceholder = block.statements.some(stmt => {
            if (stmt.kind === JS.Kind.ExpressionStatement) {
                const exprStmt = stmt as Statement as JS.ExpressionStatement;
                return this.isPlaceholder(exprStmt.expression);
            }
            return this.isPlaceholder(stmt);
        });

        if (!hasPlaceholder) {
            return super.visitBlock(block, p);
        }

        // Unwrap function to extract placeholder from ExpressionStatement
        const unwrapStatement = (element: J): J => {
            if (element.kind === JS.Kind.ExpressionStatement) {
                return (element as JS.ExpressionStatement).expression;
            }
            return element;
        };

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
        const hasPlaceholder = compilationUnit.statements.some(stmt => this.isPlaceholder(stmt));

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
        p: any
    ): Promise<J.RightPadded<J>[]> {
        const newElements: J.RightPadded<J>[] = [];

        for (const wrapped of elements) {
            // For tree types, the padded value IS the element (intersection type)
            const element = getPaddedElement(wrapped);
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
                                    // For tree types with intersection types, wrappers have 'padding' property
                                    const isWrapper = item && typeof item === 'object' && 'padding' in item && !('element' in item);

                                    if (isWrapper) {
                                        // Item is a JRightPadded wrapper - use it directly to preserve markers
                                        // For tree types, the wrapper IS the element with padding mixed in
                                        newElements.push(produce(item as J.RightPadded<J>, draft => {
                                            if (i === 0 && draft.prefix) {
                                                // Merge the placeholder's prefix with the first item's prefix
                                                // For tree types, draft IS the element with prefix directly on it
                                                (draft as any).prefix = this.mergePrefix((draft as any).prefix, element.prefix);
                                            }
                                            // Keep all other wrapper properties (including padding.markers with Semicolon)
                                        }));
                                    } else if (item) {
                                        // Item is just an element (not a wrapper) - wrap it (backward compatibility)
                                        const elem = item as J;
                                        // For tree types, spread elem and add padding properties
                                        const newPadded = produce(elem, itemDraft => {
                                            if (i === 0) {
                                                (itemDraft as any).prefix = this.mergePrefix(elem.prefix, element.prefix);
                                            }
                                            // For i > 0, prefix is already correct, no changes needed
                                        });
                                        newElements.push({
                                            ...newPadded,
                                            padding: wrapped.padding
                                        } as J.RightPadded<J>);
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
            const replacedElement = await this.visit(element, p);
            if (replacedElement) {
                // Check if the replacement came from a capture with a wrapper (to preserve markers)
                const placeholderNode = unwrapElement(element);
                const placeholderText = this.getPlaceholderText(placeholderNode);
                let wrapperToUse = wrapped;

                if (placeholderText && this.isPlaceholder(placeholderNode)) {
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

                // For tree types, merge replacedElement with padding
                newElements.push({
                    ...replacedElement,
                    padding: wrapperToUse.padding
                } as J.RightPadded<J>);
            }
        }

        return newElements;
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
     * @returns The replacement node or the original if not a placeholder
     */
    private replacePlaceholder(placeholder: J): J {
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
                    return produce(propValueAsJ, draft => {
                        draft.markers = placeholder.markers;
                        draft.prefix = this.mergePrefix(propValueAsJ.prefix, placeholder.prefix);
                    });
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
                return produce(matchedNode, draft => {
                    draft.markers = placeholder.markers;
                    draft.prefix = this.mergePrefix(matchedNode.prefix, placeholder.prefix);
                });
            }

            // If no match found, return placeholder unchanged
            return placeholder;
        }

        if (isRightPadded(param.value)) {
            // For intersection types, the padded value IS the element (with padding mixed in)
            const element = param.value as J;
            return produce(element, draft => {
                draft.markers = placeholder.markers;
                draft.prefix = this.mergePrefix(element.prefix, placeholder.prefix);
            });
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
            return produce(param.value as J, draft => {
                draft.markers = placeholder.markers;
                draft.prefix = this.mergePrefix(param.value.prefix, placeholder.prefix);
            });
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
