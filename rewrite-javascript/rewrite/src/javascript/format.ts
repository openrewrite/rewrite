/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {JS} from "./tree";
import {ExecutionContext} from "../execution";
import {JavaScriptVisitor} from "./visitor";
import {Comment, J} from "../java";
import {Draft, produce} from "immer";
import {isTree} from "../tree";
import {SpacesStyle} from "./style";
import {produceAsync} from "../visitor";

export class AutoformatVisitor extends JavaScriptVisitor<ExecutionContext> {
    constructor(private style: SpacesStyle) {
        super();
    }

    protected async visitBinary(binary: J.Binary, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitBinary(binary, p) as J.Binary;
        let property = false;
        switch (ret.operator.element.valueOf()) {
            case J.Binary.Type.And:
            case J.Binary.Type.Or:
                property = this.style.aroundOperators.logical
                break;
            case J.Binary.Type.Equal:
            case J.Binary.Type.NotEqual:
                property = this.style.aroundOperators.equality
                break;
            case J.Binary.Type.LessThan:
            case J.Binary.Type.LessThanOrEqual:
            case J.Binary.Type.GreaterThan:
            case J.Binary.Type.GreaterThanOrEqual:
                property = this.style.aroundOperators.relational
                break;
            case J.Binary.Type.BitAnd:
            case J.Binary.Type.BitOr:
            case J.Binary.Type.BitXor:
                property = this.style.aroundOperators.bitwise
                break;
            case J.Binary.Type.Addition:
            case J.Binary.Type.Subtraction:
                property = this.style.aroundOperators.additive
                break;
            case J.Binary.Type.Multiplication:
            case J.Binary.Type.Division:
            case J.Binary.Type.Modulo:
                property = this.style.aroundOperators.multiplicative
                break;
            case J.Binary.Type.LeftShift:
            case J.Binary.Type.RightShift:
            case J.Binary.Type.UnsignedRightShift:
                property = this.style.aroundOperators.shift
                break;
            default:
                // TODO support arrowFunction, beforeUnaryNotAndNotNull, afterUnaryNotAndNotNull
                throw new Error("Unsupported operator type " + ret.operator.element.valueOf());
        }
        return produce(ret, draft => {
            draft.operator.before.whitespace = property ? " " : "";
            draft.right.prefix.whitespace = property ? " " : "";
        }) as J.Binary;
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitClassDeclaration(classDecl, p) as J.ClassDeclaration;
        // TODO
        // if (c.leadingAnnotations.length > 1) {
        //     c = {...c, leadingAnnotations: spaceBetweenAnnotations(c.leadingAnnotations)};
        // }

        return produce(ret, draft => {
            draft.body.prefix.whitespace = this.style.beforeLeftBrace.catchLeftBrace ? " " : "";
            // TODO if (classDecl.body.statements.length === 0) {
        }) as J.ClassDeclaration;

        // TODO
        // if (c.padding.typeParameters !== null) {
        //     c = {
        //         ...c,
        //         padding: {
        //             ...c.padding,
        //             typeParameters: spaceBefore(
        //                 c.padding.typeParameters,
        //                 style.typeParameters.beforeOpeningAngleBracket
        //             ),
        //         },
        //     };
        // }
        // if (c.padding.typeParameters !== null) {
        //     const spaceWithinAngleBrackets = style.within.angleBrackets;
        //     const typeParametersSize = c.padding.typeParameters.elements.length;
        //     c = {
        //         ...c,
        //         padding: {
        //             ...c.padding,
        //             typeParameters: {
        //                 ...c.padding.typeParameters,
        //                 padding: {
        //                     ...c.padding.typeParameters.padding,
        //                     elements: c.padding.typeParameters.padding.elements.map(
        //                         (elemContainer, index) => {
        //                             if (index === 0) {
        //                                 elemContainer = {
        //                                     ...elemContainer,
        //                                     element: spaceBefore(
        //                                         elemContainer.element,
        //                                         spaceWithinAngleBrackets
        //                                     ),
        //                                 };
        //                             } else {
        //                                 elemContainer = {
        //                                     ...elemContainer,
        //                                     element: spaceBefore(
        //                                         elemContainer.element,
        //                                         style.other.afterComma
        //                                     ),
        //                                 };
        //                             }
        //                             if (index === typeParametersSize - 1) {
        //                                 elemContainer = spaceAfter(
        //                                     elemContainer,
        //                                     spaceWithinAngleBrackets
        //                                 );
        //                             }
        //                             return elemContainer;
        //                         }
        //                     ),
        //                 },
        //             },
        //         },
        //     };
        // }
    }

    protected async visitIf(iff: J.If, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitIf(iff, p) as J.If;
        return produceAsync(ret, async draft => {
            draft.ifCondition = await this.spaceBefore(draft.ifCondition, this.style.beforeParentheses.ifParentheses);
            draft.ifCondition.tree = await this.spaceAfter(await this.spaceBeforeRightPaddedElement(draft.ifCondition.tree, this.style.within.ifParentheses), this.style.within.ifParentheses);
            draft.thenPart = await this.spaceAfter(await this.spaceBeforeRightPaddedElement(ret.thenPart, this.style.beforeLeftBrace.ifLeftBrace), false);
        });
    }

    protected async visitMethodDeclaration(methodDecl: J.MethodDeclaration, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitMethodDeclaration(methodDecl, p) as J.MethodDeclaration;
        return produceAsync(ret, async draft => {
            draft.body = ret.body && await this.spaceBefore(ret.body, this.style.beforeLeftBrace.functionLeftBrace);
            draft.parameters = await this.spaceBeforeContainer(ret.parameters, this.style.beforeParentheses.functionDeclarationParentheses);
            // TODO
            // if (m.leadingAnnotations.length > 1) {
            //     m = m.withLeadingAnnotations(this.spaceBetweenAnnotations(m.leadingAnnotations));
            // }
            // if (m.parameters.length === 0 || m.parameters[0] instanceof J.Empty) {
            //     const useSpace = this.style.within.emptyMethodDeclarationParentheses;
            //     m = m.getPadding().withParameters(
            //         m.getPadding().parameters.getPadding().withElements(
            //             ListUtils.map(m.getPadding().parameters.getPadding().elements,
            //                 (param) => param.withElement(this.spaceBefore(param.element, useSpace))
            //             )
            //         )
            //     );
            // } else {
            //     const paramsSize = m.parameters.length;
            //     const useSpace = this.style.within.methodDeclarationParentheses;
            //     m = m.getPadding().withParameters(
            //         m.getPadding().parameters.getPadding().withElements(
            //             ListUtils.map(m.getPadding().parameters.getPadding().elements,
            //                 (index, param) => {
            //                     if (index === 0) {
            //                         param = param.withElement(this.spaceBefore(param.element, useSpace));
            //                     } else {
            //                         param = param.withElement(
            //                             this.spaceBefore(param.element, this.style.other.afterComma)
            //                         );
            //                     }
            //                     if (index === paramsSize - 1) {
            //                         param = this.spaceAfter(param, useSpace);
            //                     } else {
            //                         param = this.spaceAfter(param, this.style.other.beforeComma);
            //                     }
            //                     return param;
            //                 }
            //             )
            //         )
            //     );
            // }
            // if (m.annotations.typeParameters !== null) {
            //     const spaceWithinAngleBrackets = this.style.within.angleBrackets;
            //     const typeParametersSize = m.annotations.typeParameters.typeParameters.length;
            //     m = m.annotations.withTypeParameters(
            //         m.annotations.typeParameters.getPadding().withTypeParameters(
            //             ListUtils.map(m.annotations.typeParameters.getPadding().typeParameters,
            //                 (index, elemContainer) => {
            //                     if (index === 0) {
            //                         elemContainer = elemContainer.withElement(
            //                             this.spaceBefore(elemContainer.element, spaceWithinAngleBrackets)
            //                         );
            //                     } else {
            //                         elemContainer = elemContainer.withElement(
            //                             this.spaceBefore(elemContainer.element, this.style.other.afterComma)
            //                         );
            //                     }
            //                     if (index === typeParametersSize - 1) {
            //                         elemContainer = this.spaceAfter(elemContainer, spaceWithinAngleBrackets);
            //                     }
            //                     return elemContainer;
            //                 }
            //             )
            //         )
            //     );
            // }
        });
    }

    protected async visitMethodInvocation(methodInv: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitMethodInvocation(methodInv, p) as J.MethodInvocation;
        return produceAsync(ret, async draft => {
            if (draft.select) {
                draft.select = await this.spaceAfter(draft.select, this.style.beforeParentheses.functionCallParentheses);
            }
            if (ret.arguments.elements.length > 0 && ret.arguments.elements[0].element.kind != J.Kind.Empty) {
                // TODO all arguments, not only the first and last one
                draft.arguments.elements[0] = await this.spaceBeforeRightPaddedElement(draft.arguments.elements[0], this.style.within.functionCallParentheses);
                const lastArgIndex = draft.arguments.elements.length - 1;
                draft.arguments.elements[lastArgIndex] = await this.spaceAfter(draft.arguments.elements[lastArgIndex], this.style.within.functionCallParentheses);
            } else if (ret.arguments.elements.length == 1) {
                draft.arguments.elements[0] = await this.spaceAfter(await this.spaceBeforeRightPaddedElement(draft.arguments.elements[0], this.style.within.functionCallParentheses), false);
            }

            // TODO
            // m = m.getPadding().withArguments(spaceBefore(m.getPadding().getArguments(), style.getBeforeParentheses().getMethodCall()));
            // if (m.getArguments().isEmpty() || m.getArguments()[0] instanceof J.Empty) {
            //   ...
            // } else {
            //     const argsSize = m.getArguments().length;
            //     const useSpace = style.getWithin().getMethodCallParentheses();
            //     m = m.getPadding().withArguments(
            //         m.getPadding().getArguments().getPadding().withElements(
            //             ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
            //                 (index: number, arg: any) => {
            //                     if (index === 0) {
            //                         arg = arg.withElement(spaceBefore(arg.getElement(), useSpace));
            //                     } else {
            //                         arg = arg.withElement(
            //                             spaceBefore(arg.getElement(), style.getOther().getAfterComma())
            //                         );
            //                     }
            //                     if (index === argsSize - 1) {
            //                         arg = spaceAfter(arg, useSpace);
            //                     } else {
            //                         arg = spaceAfter(arg, style.getOther().getBeforeComma());
            //                     }
            //                     return arg;
            //                 }
            //             )
            //         )
            //     );
            // }
            // if (m.getPadding().getTypeParameters() !== null) {
            //     m = m.getPadding().withTypeParameters(
            //         spaceBefore(m.getPadding().getTypeParameters(),
            //             style.getTypeArguments().getBeforeOpeningAngleBracket())
            //     );
            //     m = m.withName(spaceBefore(m.getName(), style.getTypeArguments().getAfterClosingAngleBracket()));
            // }
            // if (m.getPadding().getTypeParameters() !== null) {
            //     m = m.getPadding().withTypeParameters(
            //         m.getPadding().getTypeParameters().getPadding().withElements(
            //             ListUtils.map(m.getPadding().getTypeParameters().getPadding().getElements(),
            //                 (index: number, elemContainer: any) => {
            //                     if (index !== 0) {
            //                         elemContainer = elemContainer.withElement(
            //                             spaceBefore(elemContainer.getElement(),
            //                                 style.getTypeArguments().getAfterComma())
            //                         );
            //                     }
            //                     return elemContainer;
            //                 }
            //             )
            //         )
            //     );
            // }
        });
    }

    protected async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: ExecutionContext): Promise<J.RightPadded<T>> {
        const ret = await super.visitRightPadded(right, p);
        if (isTree(ret.element)) {
            switch (ret.element.kind) {
                case J.Kind.NamedVariable:
                    return produce(ret, draft => {
                        draft.after.whitespace = this.style.aroundOperators.assignment ? " " : "";
                    });
            }
        }
        return ret;
    }

    protected async visitTypeInfo(typeInfo: JS.TypeInfo, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitTypeInfo(typeInfo, p) as JS.TypeInfo;
        return produceAsync(ret, async draft => {
            draft.prefix.whitespace = this.style.other.beforeTypeReferenceColon ? " " : "";
            draft.typeIdentifier = await this.spaceBefore(draft.typeIdentifier, this.style.other.afterTypeReferenceColon);
        });
    }

    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitVariable(variable, p) as J.VariableDeclarations.NamedVariable;
        return produceAsync(ret, async draft => {
            draft.initializer = ret.initializer && await this.spaceBeforeLeftPaddedElement(ret.initializer, this.style.aroundOperators.assignment);
        });
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitWhileLoop(whileLoop, p) as J.WhileLoop;
        return produceAsync(ret, async draft => {
            draft.body = await this.spaceAfter(await this.spaceBeforeRightPaddedElement(ret.body, this.style.beforeLeftBrace.whileLeftBrace), false);
            draft.condition = await this.spaceBefore(draft.condition, this.style.beforeParentheses.whileParentheses);
            draft.condition.tree = await this.spaceAfter(await this.spaceBeforeRightPaddedElement(draft.condition.tree, this.style.within.whileParentheses), this.style.within.whileParentheses);
        });
    }

    private async spaceAfter<T extends J>(right: J.RightPadded<T>, spaceAfter: boolean): Promise<J.RightPadded<T>> {
        if (right.after.comments.length > 0) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            const comments = AutoformatVisitor.spaceLastCommentSuffix(right.after.comments, spaceAfter);
            return {...right, after: {...right.after, comments}};
        }

        if (spaceAfter && AutoformatVisitor.isNotSingleSpace(right.after.whitespace)) {
            return {...right, after: {...right.after, whitespace: " "}};
        } else if (!spaceAfter && AutoformatVisitor.isOnlySpacesAndNotEmpty(right.after.whitespace)) {
            return {...right, after: {...right.after, whitespace: ""}};
        } else {
            return right;
        }
    }

    private async spaceBeforeLeftPaddedElement<T extends J>(left: J.LeftPadded<T>, spaceBefore: boolean): Promise<J.LeftPadded<T>> {
        return produceAsync(left, async draft => {
            if (draft.before.comments.length == 0) {
                draft.before.whitespace = "";
            }
            draft.element = await this.spaceBefore(left.element, spaceBefore) as Draft<T>;
        });
    }

    private async spaceBeforeRightPaddedElement<T extends J>(right: J.RightPadded<T>, spaceBefore: boolean): Promise<J.RightPadded<T>> {
        return produceAsync(right, async draft => {
            draft.element = await this.spaceBefore(right.element, spaceBefore) as Draft<T>;
        });
    }

    private async spaceBefore<T extends J>(j: T, spaceBefore: boolean): Promise<T> {
        if (j.prefix.comments.length > 0) {
            return j;
        }
        if (spaceBefore && AutoformatVisitor.isNotSingleSpace(j.prefix.whitespace)) {
            return produce(j, draft => {
                draft.prefix.whitespace = " ";
            });
        } else if (!spaceBefore && AutoformatVisitor.isOnlySpacesAndNotEmpty(j.prefix.whitespace)) {
            return produce(j, draft => {
                draft.prefix.whitespace = "";
            });
        } else {
            return j;
        }
    }

    private async spaceBeforeContainer<T extends J>(container: J.Container<T>, spaceBefore: boolean): Promise<J.Container<T>> {
        if (container.before.comments.length > 0) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            return produce(container, draft => {
                draft.before.comments = AutoformatVisitor.spaceLastCommentSuffix(container.before.comments, spaceBefore);
            });
        }

        if (spaceBefore && AutoformatVisitor.isNotSingleSpace(container.before.whitespace)) {
            return produce(container, draft => {
                draft.before.whitespace = " ";
            });
        } else if (!spaceBefore && AutoformatVisitor.isOnlySpacesAndNotEmpty(container.before.whitespace)) {
            return produce(container, draft => {
                draft.before.whitespace = "";
            });
        } else {
            return container;
        }
    }

    private static spaceLastCommentSuffix(comments: Comment[], spaceSuffix: boolean): Comment[] {
        comments[comments.length - 1] = this.spaceSuffix(comments[comments.length - 1], spaceSuffix);
        return comments;
    }

    private static spaceSuffix(comment: Comment, spaceSuffix: boolean): Comment {
        if (spaceSuffix && this.isNotSingleSpace(comment.suffix)) {
            return {...comment, suffix: " "};
        } else if (!spaceSuffix && this.isOnlySpacesAndNotEmpty(comment.suffix)) {
            return {...comment, suffix: ""};
        } else {
            return comment;
        }
    }

    private static isOnlySpaces(str: string): boolean {
        return str.split('').every(char => char === ' ');
    }

    private static isOnlySpacesAndNotEmpty(s: string): boolean {
        return s !== "" && this.isOnlySpaces(s);
    }

    private static isNotSingleSpace(str: string): boolean {
        return this.isOnlySpaces(str) && str !== " ";
    }
}
