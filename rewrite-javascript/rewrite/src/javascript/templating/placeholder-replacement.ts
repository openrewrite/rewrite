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
import {JavaScriptVisitor} from '../visitor';
import {produce} from 'immer';
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
            const element = wrapped.element;
            const placeholderNode = unwrapElement(element);

            // Check if this element contains a placeholder
            if (this.isPlaceholder(placeholderNode)) {
                const placeholderText = this.getPlaceholderText(placeholderNode);
                if (placeholderText) {
                    const param = this.substitutions.get(placeholderText);
                    if (param) {
                        let arrayToExpand: J[] | J.RightPadded<J>[] | undefined = undefined;

                        // Check if it's a direct Tree[] array
                        if (Array.isArray(param.value)) {
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
                                        newElements.push(produce(item, draft => {
                                            if (i === 0 && draft.element) {
                                                // Merge the placeholder's prefix with the first item's prefix
                                                // Modify prefix directly without nested produce to avoid immer issues
                                                draft.element.prefix = this.mergePrefix(draft.element.prefix, element.prefix);
                                            }
                                            // Keep all other wrapper properties (including markers with Semicolon)
                                        }));
                                    } else if (item) {
                                        // Item is just an element (not a wrapper) - wrap it (backward compatibility)
                                        const elem = item as J;
                                        newElements.push(produce(wrapped, draft => {
                                            draft.element = produce(elem, itemDraft => {
                                                if (i === 0) {
                                                    itemDraft.prefix = this.mergePrefix(elem.prefix, element.prefix);
                                                }
                                                // For i > 0, prefix is already correct, no changes needed
                                            });
                                        }));
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

                newElements.push(produce(wrapperToUse, draft => {
                    draft.element = replacedElement;
                }));
            }
        }

        return newElements;
    }

    async visit<R extends J>(tree: J, p: any, parent?: Cursor): Promise<R | undefined> {
        // Check if this node is a placeholder
        if (this.isPlaceholder(tree)) {
            const replacement = this.replacePlaceholder(tree);
            if (replacement !== tree) {
                return replacement as R;
            }
        }

        // Continue with normal traversal
        return super.visit(tree, p, parent);
    }

    override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
        // Check if any arguments are placeholders (possibly variadic)
        const hasPlaceholderInArgs = method.arguments.elements.some(arg => this.isPlaceholder(arg.element));
        // Check if the select (the object being called on) is a placeholder
        const hasPlaceholderInSelect = method.select && this.isPlaceholder(method.select.element);

        if (!hasPlaceholderInArgs && !hasPlaceholderInSelect) {
            return super.visitMethodInvocation(method, p);
        }

        let newArguments = method.arguments.elements;
        if (hasPlaceholderInArgs) {
            newArguments = await this.expandVariadicElements(method.arguments.elements, undefined, p);
        }

        let newSelect = method.select;
        if (hasPlaceholderInSelect && method.select) {
            const visitedSelect = await this.visit(method.select.element, p);
            if (visitedSelect) {
                newSelect = produce(method.select, draft => {
                    draft.element = visitedSelect;
                });
            }
        }

        return produce(method, draft => {
            draft.arguments.elements = newArguments;
            if (newSelect !== method.select) {
                draft.select = newSelect;
            }
        });
    }

    override async visitBlock(block: J.Block, p: any): Promise<J | undefined> {
        // Check if any statements are placeholders (possibly variadic)
        const hasPlaceholder = block.statements.some(stmt => {
            const stmtElement = stmt.element;
            // Check if it's an ExpressionStatement containing a placeholder
            if (stmtElement.kind === JS.Kind.ExpressionStatement) {
                const exprStmt = stmtElement as JS.ExpressionStatement;
                return this.isPlaceholder(exprStmt.expression);
            }
            return this.isPlaceholder(stmtElement);
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

    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, p: any): Promise<J | undefined> {
        // Check if any statements are placeholders (possibly variadic)
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
        }
        return null;
    }

}
