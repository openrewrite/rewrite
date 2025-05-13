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
import {JS} from "./tree";
import {ExecutionContext} from "../execution";
import {JavaScriptVisitor} from "./visitor";
import {Comment, J, Statement} from "../java";
import {Draft, produce} from "immer";
import {Cursor, isTree, Tree} from "../tree";
import {SpacesStyle, styleFromSourceFile, StyleKind, WrappingAndBracesStyle} from "./style";
import {produceAsync} from "../visitor";

export class AutoformatVisitor extends JavaScriptVisitor<ExecutionContext> {
    async visit<R extends J>(tree: Tree, p: ExecutionContext, cursor?: Cursor): Promise<R | undefined> {
        const spacesStyle = styleFromSourceFile(StyleKind.SpacesStyle, tree) as SpacesStyle;
        const wrappingAndBracesStyle = styleFromSourceFile(StyleKind.WrappingAndBracesStyle, tree) as WrappingAndBracesStyle;
        let t: R | undefined = tree as R;
        // TODO possibly cursor.fork

        // TODO add it once we have the newlines visitor
        // t = t && await new MinimumViableSpacingVisitor().visit(t, p, cursor);
        t = t && await new WrappingAndBracesVisitor(wrappingAndBracesStyle).visit(t, p, cursor);
        t = t && await new SpacesVisitor(spacesStyle).visit(t, p, cursor);
        return t;
    }
}

export class SpacesVisitor extends JavaScriptVisitor<ExecutionContext> {
    constructor(private style: SpacesStyle) {
        super();
    }

    protected async visitArrayAccess(arrayAccess: J.ArrayAccess, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitArrayAccess(arrayAccess, p) as J.ArrayAccess;
        return produce(ret, draft => {
            draft.dimension.index.element.prefix.whitespace = this.style.within.arrayBrackets ? " " : "";
            draft.dimension.index.after.whitespace = this.style.within.arrayBrackets ? " " : "";
        });
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

        // TODO typeParameters - IntelliJ doesn't seem to provide a setting for angleBrackets spacing for Typescript (while it does for Java),
        // thus we either introduce our own setting or just enforce the natural spacing with no setting

        return produce(ret, draft => {
            draft.body.prefix.whitespace = this.style.beforeLeftBrace.catchLeftBrace ? " " : "";
            // TODO if (classDecl.body.statements.length === 0) {
        }) as J.ClassDeclaration;
    }

    protected async visitForLoop(forLoop: J.ForLoop, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitForLoop(forLoop, p) as J.ForLoop;
        return produceAsync(ret, async draft => {
            draft.control.prefix.whitespace = this.style.beforeParentheses.forParentheses ? " " : "";
            draft.control.init = await Promise.all(draft.control.init.map(async (oneInit, index) => {
                if (oneInit.element.kind === JS.Kind.ScopedVariableDeclarations) {
                    const scopedVD = oneInit.element as Draft<JS.ScopedVariableDeclarations>;
                    if (scopedVD.scope != undefined) {
                        scopedVD.scope.before.whitespace = "";
                        scopedVD.variables[scopedVD.variables.length - 1].after.whitespace = "";
                    }
                }
                oneInit.after.whitespace = "";
                return await this.spaceBeforeRightPaddedElement(oneInit, index === 0 ? this.style.within.forParentheses : true);
            }));
            if (draft.control.condition) {
                draft.control.condition.element.prefix.whitespace = " ";
                draft.control.condition.after.whitespace = this.style.other.beforeForSemicolon ? " " : "";
            }
            draft.control.update = await Promise.all(draft.control.update.map(async (oneUpdate, index) => {
                oneUpdate.element.prefix.whitespace = " ";
                oneUpdate.after.whitespace = (index === draft.control.update.length - 1 ? this.style.within.forParentheses : this.style.other.beforeForSemicolon) ? " " : "";
                return oneUpdate;
            }));

            draft.body = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(ret.body, this.style.beforeLeftBrace.forLeftBrace), false);
        });
    }

    protected async visitIf(iff: J.If, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitIf(iff, p) as J.If;
        return produceAsync(ret, async draft => {
            draft.ifCondition = await this.spaceBefore(draft.ifCondition, this.style.beforeParentheses.ifParentheses);
            draft.ifCondition.tree = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(draft.ifCondition.tree, this.style.within.ifParentheses), this.style.within.ifParentheses);
            draft.thenPart = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(ret.thenPart, this.style.beforeLeftBrace.ifLeftBrace), false);
        });
    }

    protected async visitMethodDeclaration(methodDecl: J.MethodDeclaration, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitMethodDeclaration(methodDecl, p) as J.MethodDeclaration;
        return produceAsync(ret, async draft => {
            draft.body = ret.body && await this.spaceBefore(ret.body, this.style.beforeLeftBrace.functionLeftBrace);

            if (draft.parameters.elements.length > 0 && draft.parameters.elements[0].element.kind != J.Kind.Empty) {
                draft.parameters.elements = await Promise.all(draft.parameters.elements.map(async (param, index) => {
                    param = await this.spaceAfterRightPadded(param, index === draft.parameters.elements.length - 1 ? this.style.within.functionDeclarationParentheses : this.style.other.beforeComma);
                    param.element = await this.spaceBefore(param.element, index === 0 ? this.style.within.functionDeclarationParentheses : this.style.other.afterComma);
                    (param.element as Draft<J.VariableDeclarations>).variables[0].element.name.prefix.whitespace = "";
                    (param.element as Draft<J.VariableDeclarations>).variables[0].after.whitespace = "";
                    return param;
                }));
            } else if (draft.parameters.elements.length == 1) {
                draft.parameters.elements[0] = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(draft.parameters.elements[0], this.style.within.functionDeclarationParentheses), false);
            }
            draft.parameters = await this.spaceBeforeContainer(draft.parameters, this.style.beforeParentheses.functionDeclarationParentheses);

            // TODO typeParameters handling - see visitClassDeclaration
            // TODO
            // if (m.leadingAnnotations.length > 1) {
            //     m = m.withLeadingAnnotations(this.spaceBetweenAnnotations(m.leadingAnnotations));
            // }
        });
    }

    protected async visitMethodInvocation(methodInv: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitMethodInvocation(methodInv, p) as J.MethodInvocation;
        return produceAsync(ret, async draft => {
            if (draft.select) {
                draft.select = await this.spaceAfterRightPadded(draft.select, this.style.beforeParentheses.functionCallParentheses);
            }
            if (ret.arguments.elements.length > 0 && ret.arguments.elements[0].element.kind != J.Kind.Empty) {
                draft.arguments.elements = await Promise.all(draft.arguments.elements.map(async (arg, index) => {
                    arg = await this.spaceAfterRightPadded(arg, index === draft.arguments.elements.length - 1 ? this.style.within.functionCallParentheses : this.style.other.beforeComma);
                    arg.element = await this.spaceBefore(arg.element, index === 0 ? this.style.within.functionCallParentheses : this.style.other.afterComma);
                    return arg;
                }));
            } else if (ret.arguments.elements.length == 1) {
                draft.arguments.elements[0] = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(draft.arguments.elements[0], this.style.within.functionCallParentheses), false);
            }

            // TODO typeParameters handling - see visitClassDeclaration

            // TODO
            // m = m.getPadding().withArguments(spaceBefore(m.getPadding().getArguments(), style.getBeforeParentheses().getMethodCall()));
            // if (m.getArguments().isEmpty() || m.getArguments()[0] instanceof J.Empty) {
            //   ...
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

    protected async visitSwitch(switchNode: J.Switch, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitSwitch(switchNode, p) as J.Switch;
        return produceAsync(ret, async draft => {
            draft.selector = await this.spaceBefore(draft.selector, this.style.beforeParentheses.switchParentheses);
            draft.selector.tree = await this.spaceAfterRightPadded(
                await this.spaceBeforeRightPaddedElement(draft.selector.tree, this.style.within.switchParentheses),
                this.style.within.switchParentheses
            );
            draft.cases = await this.spaceBefore(draft.cases, this.style.beforeLeftBrace.switchLeftBrace);

            for (const case_ of draft.cases.statements) {
                if (case_.element.kind === J.Kind.Case) {
                    (case_.element as Draft<J.Case>).caseLabels.elements[0].after.whitespace = "";
                }
            }
        });
    }

    protected async visitTernary(ternary: J.Ternary, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitTernary(ternary, p) as J.Ternary;
        return produceAsync(ret, async draft => {
            draft.truePart = await this.spaceBeforeLeftPaddedElement(draft.truePart, this.style.ternaryOperator.beforeQuestionMark, this.style.ternaryOperator.afterQuestionMark);
            draft.falsePart = await this.spaceBeforeLeftPaddedElement(draft.falsePart, this.style.ternaryOperator.beforeColon, this.style.ternaryOperator.afterColon);
        });
    }

    protected async visitTry(try_: J.Try, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitTry(try_, p) as J.Try;
        return produceAsync(ret, async draft => {
            draft.body.prefix.whitespace = this.style.beforeLeftBrace.tryLeftBrace ? " " : "";
            draft.catches = await Promise.all(draft.catches.map(async (catch_, index) => {
                catch_ = await this.spaceBefore(catch_, this.style.beforeKeywords.catchKeyword);
                catch_.parameter.prefix.whitespace = this.style.beforeParentheses.catchParentheses ? " " : "";
                catch_.parameter.tree = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(catch_.parameter.tree, this.style.within.catchParentheses), this.style.within.catchParentheses);
                catch_.parameter.tree.element.variables[catch_.parameter.tree.element.variables.length - 1].after.whitespace = "";
                catch_.body.prefix.whitespace = this.style.beforeLeftBrace.catchLeftBrace ? " " : "";
                return catch_;
            }));
            draft.finally = draft.finally && await this.spaceBeforeLeftPaddedElement(draft.finally, this.style.beforeKeywords.finallyKeyword, this.style.beforeLeftBrace.finallyLeftBrace) as Draft<J.LeftPadded<J.Block>>;
        });
    }

    protected async visitTypeInfo(typeInfo: JS.TypeInfo, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitTypeInfo(typeInfo, p) as JS.TypeInfo;
        return produceAsync(ret, async draft => {
            draft.prefix.whitespace = this.style.other.beforeTypeReferenceColon ? " " : "";
            draft.typeIdentifier = await this.spaceBefore(draft.typeIdentifier, this.style.other.afterTypeReferenceColon);
        });
    }

    protected async visitUnary(unary: J.Unary, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitUnary(unary, p) as J.Unary;
        return produce(ret, draft => {
            const spacing = this.style.aroundOperators.unary;

            switch (draft.operator.element) {
                case J.Unary.Type.PreIncrement:
                case J.Unary.Type.PreDecrement:
                case J.Unary.Type.Negative:
                case J.Unary.Type.Positive:
                case J.Unary.Type.Not:
                case J.Unary.Type.Complement:
                    draft.expression.prefix.whitespace = spacing ? " " : "";
                    break;

                case J.Unary.Type.PostIncrement:
                case J.Unary.Type.PostDecrement:
                    // postfix: don't add space after operand
                    break;
            }
        });
    }
    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitVariable(variable, p) as J.VariableDeclarations.NamedVariable;
        return produceAsync(ret, async draft => {
            if (draft.initializer) {
                draft.initializer = await this.spaceBeforeLeftPaddedElement(draft.initializer, false, this.style.aroundOperators.assignment);
            }
        });
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitWhileLoop(whileLoop, p) as J.WhileLoop;
        return produceAsync(ret, async draft => {
            draft.body = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(ret.body, this.style.beforeLeftBrace.whileLeftBrace), false);
            draft.condition = await this.spaceBefore(draft.condition, this.style.beforeParentheses.whileParentheses);
            draft.condition.tree = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(draft.condition.tree, this.style.within.whileParentheses), this.style.within.whileParentheses);
        });
    }

    private async spaceAfterRightPadded<T extends J>(right: J.RightPadded<T>, spaceAfter: boolean): Promise<J.RightPadded<T>> {
        if (right.after.comments.length > 0) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            const comments = SpacesVisitor.spaceLastCommentSuffix(right.after.comments, spaceAfter);
            return {...right, after: {...right.after, comments}};
        }

        if (spaceAfter && SpacesVisitor.isNotSingleSpace(right.after.whitespace)) {
            return {...right, after: {...right.after, whitespace: " "}};
        } else if (!spaceAfter && SpacesVisitor.isOnlySpacesAndNotEmpty(right.after.whitespace)) {
            return {...right, after: {...right.after, whitespace: ""}};
        } else {
            return right;
        }
    }

    private async spaceBeforeLeftPaddedElement<T extends J>(left: J.LeftPadded<T>, spaceBeforePadding: boolean, spaceBeforeElement: boolean): Promise<J.LeftPadded<T>> {
        return produceAsync(left, async draft => {
            if (draft.before.comments.length == 0) {
                draft.before.whitespace = spaceBeforePadding ? " " : "";
            }
            draft.element = await this.spaceBefore(left.element, spaceBeforeElement) as Draft<T>;
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
        if (spaceBefore && SpacesVisitor.isNotSingleSpace(j.prefix.whitespace)) {
            return produce(j, draft => {
                draft.prefix.whitespace = " ";
            });
        } else if (!spaceBefore && SpacesVisitor.isOnlySpacesAndNotEmpty(j.prefix.whitespace)) {
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
                draft.before.comments = SpacesVisitor.spaceLastCommentSuffix(container.before.comments, spaceBefore);
            });
        }

        if (spaceBefore && SpacesVisitor.isNotSingleSpace(container.before.whitespace)) {
            return produce(container, draft => {
                draft.before.whitespace = " ";
            });
        } else if (!spaceBefore && SpacesVisitor.isOnlySpacesAndNotEmpty(container.before.whitespace)) {
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
            return produce(comment, draft => {
                draft.suffix = " ";
            });
        } else if (!spaceSuffix && this.isOnlySpacesAndNotEmpty(comment.suffix)) {
            return produce(comment, draft => {
                draft.suffix = "";
            });
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

export class WrappingAndBracesVisitor extends JavaScriptVisitor<ExecutionContext> {
    constructor(private readonly style: WrappingAndBracesStyle) {
        super();
    }

    public async visitStatement(statement: Statement, p: ExecutionContext): Promise<Statement> {
        const j = await super.visitStatement(statement, p) as Statement;
        const parent = this.cursor.parent?.value;
        if (parent?.kind === J.Kind.Block && j.kind !== J.Kind.EnumValueSet) {
            if (!j.prefix.whitespace.includes("\n")) {
                return produce(j, draft => {
                    draft.prefix.whitespace = "\n" + draft.prefix.whitespace;
                });
            }
        }
        return j;
    }

    protected async visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: ExecutionContext): Promise<J.VariableDeclarations> {
        const v = await super.visitVariableDeclarations(multiVariable, p) as J.VariableDeclarations;
        const parent = this.cursor.parent?.value;
        if (parent?.kind === J.Kind.Block) {
            return produce(v, draft => {
                draft.leadingAnnotations = this.withNewlines(draft.leadingAnnotations);
                if (draft.leadingAnnotations.length > 0) {
                    if (draft.modifiers.length > 0) {
                        draft.modifiers = this.withNewlineModifiers(draft.modifiers);
                    } else if (draft.typeExpression && !draft.typeExpression.prefix.whitespace.includes("\n")) {
                        draft.typeExpression.prefix.whitespace = "\n" + draft.typeExpression.prefix.whitespace;
                    }
                }
            });
        }
        return v;
    }

    protected async visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): Promise<J.MethodDeclaration> {
        const m = await super.visitMethodDeclaration(method, p) as J.MethodDeclaration;
        return produce(m, draft => {
            draft.leadingAnnotations = this.withNewlines(draft.leadingAnnotations);
            if (draft.leadingAnnotations.length > 0) {
                if (draft.modifiers.length > 0) {
                    draft.modifiers = this.withNewlineModifiers(draft.modifiers);
                } else if (draft.typeParameters && !draft.typeParameters.prefix.whitespace.includes("\n")) {
                    draft.typeParameters.prefix.whitespace = "\n" + draft.typeParameters.prefix.whitespace;
                } else if (draft.returnTypeExpression && !draft.returnTypeExpression.prefix.whitespace.includes("\n")) {
                    draft.returnTypeExpression.prefix.whitespace = "\n" + draft.returnTypeExpression.prefix.whitespace;
                } else if (!draft.name.prefix.whitespace.includes("\n")) {
                    draft.name.prefix.whitespace = "\n" + draft.name.prefix.whitespace;
                }
            }
        });
    }

    protected async visitElse(elsePart: J.If.Else, p: ExecutionContext): Promise<J.If.Else> {
        const e = await super.visitElse(elsePart, p) as J.If.Else;
        const hasBody = e.body.element.kind === J.Kind.Block || e.body.element.kind === J.Kind.If;

        return produce(e, draft => {
            if (hasBody) {
                const shouldHaveNewline = this.style.ifStatement.elseOnNewLine;
                const hasNewline = draft.prefix.whitespace.includes("\n");
                if (shouldHaveNewline && !hasNewline) {
                    draft.prefix.whitespace = "\n" + draft.prefix.whitespace;
                } else if (!shouldHaveNewline && hasNewline) {
                    draft.prefix.whitespace = "";
                }
            }
        });
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): Promise<J.ClassDeclaration> {
        const j = await super.visitClassDeclaration(classDecl, p) as J.ClassDeclaration;
        return produce(j, draft => {
            draft.leadingAnnotations = this.withNewlines(draft.leadingAnnotations);
            if (draft.leadingAnnotations.length > 0) {
                if (draft.modifiers.length > 0) {
                    draft.modifiers = this.withNewlineModifiers(draft.modifiers);
                } else {
                    const kind = draft.classKind;
                    if (!kind.prefix.whitespace.includes("\n")) {
                        kind.prefix.whitespace = "\n" + kind.prefix.whitespace;
                    }
                }
            }
        });
    }

    protected async visitBlock(block: J.Block, p: ExecutionContext): Promise<J.Block> {
        const b = await super.visitBlock(block, p) as J.Block;
        return produce(b, draft => {
            if (!draft.end.whitespace.includes("\n") && (draft.statements.length == 0 || !draft.statements[draft.statements.length - 1].after.whitespace.includes("\n"))) {
                draft.end = this.withNewlineSpace(draft.end);
            }
        });
    }

    protected async visitSwitch(aSwitch: J.Switch, p: ExecutionContext): Promise<J | undefined> {
        return super.visitSwitch(aSwitch, p);
    }

    private withNewlines<T extends { prefix: J.Space }>(list: T[]): T[] {
        return list.map((a, index) => {
            if (index > 0 && !a.prefix.whitespace.includes("\n")) {
                return produce(a, draft => {
                    draft.prefix.whitespace = "\n" + draft.prefix.whitespace;
                });
            }
            return a;
        });
    }

    private withNewlineSpace(space: J.Space): J.Space {
        if (space.comments.length === 0) {
            space = produce(space, draft => {
               draft.whitespace = "\n" + space.whitespace;
            });
        }
        // TODO clarify the situation with Comment.isMultiline and then add this case
        // else if (space.comments[space.comments.length - 1].isMultiline) {
        //     space = space.withComments(ListUtils.mapLast(space.comments, c => c.withSuffix("\n")));
        // }

        return space;
    }

    private withNewlineModifiers(modifiers: J.Modifier[]): J.Modifier[] {
        if (modifiers.length === 0) {
            return modifiers;
        }
        const first = modifiers[0];
        if (!first.prefix.whitespace.includes("\n")) {
            return [
                produce(first, draft => {
                    draft.prefix.whitespace = "\n" + draft.prefix.whitespace;
                }),
                ...modifiers.slice(1),
            ];
        }
        return modifiers;
    }
}


export class MinimumViableSpacingVisitor extends JavaScriptVisitor<ExecutionContext> {
    constructor() {
        super();
    }

    override async visitSpace(space: J.Space, p: ExecutionContext): Promise<J.Space> {
        // Note - for some reason the original MinimumViableSpacingVisitor.java doesn't have it
        // and only has the logic in MinimumViableSpacingTest.defaults
        const ret = await super.visitSpace(space, p) as J.Space;
        return ret && produce(ret, draft => {
            draft.whitespace = "";
        });
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): Promise<J | undefined> {
        let c = await super.visitClassDeclaration(classDecl, p) as J.ClassDeclaration;
        let first = c.leadingAnnotations.length === 0;

        if (c.modifiers.length > 0) {
            if (!first && c.modifiers[0].prefix.whitespace === "") {
                c = produce(c, draft => {
                    draft.modifiers[0].prefix.whitespace = " ";
                });
            }
            c = produce(c, draft => {
                draft.modifiers = draft.modifiers.map((m, i) => i > 0 && m.prefix.whitespace === "" ?
                    {...m, prefix: {...m.prefix, whitespace: " "}} : m);
            });
            first = false;
        }

        if (c.classKind.prefix.whitespace === "" && !first) {
            c = produce(c, draft => {
                draft.classKind.prefix.whitespace = " ";
            });
            first = false;
        }

        c = produce(c, draft => {
            draft.name.prefix.whitespace = " ";
        });

        if (c.typeParameters && c.typeParameters.elements.length > 0 && c.typeParameters.before.whitespace === "" && !first) {
            c = produce(c, draft => {
                draft.typeParameters!.before.whitespace = " ";
            });
        }

        if (c.extends && c.extends.before.whitespace === "") {
            c = produce(c, draft => {
                draft.extends!.before.whitespace = " ";
            });
        }

        if (c.implements && c.implements.before.whitespace === "") {
            c = produce(c, draft => {
                draft.implements!.before.whitespace = " ";
                if (draft.implements != undefined && draft.implements.elements.length > 0) {
                    draft.implements.elements[0].element.prefix.whitespace = " ";
                }
            });
        }

        c = produce(c, draft => {
            draft.body.prefix.whitespace = "";
        });

        return c;
    }

    protected async visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): Promise<J | undefined> {
        let m = await super.visitMethodDeclaration(method, p) as J.MethodDeclaration;
        let first = m.leadingAnnotations.length === 0;

        if (m.modifiers.length > 0) {
            if (!first && m.modifiers[0].prefix.whitespace === "") {
                m = produce(m, draft => {
                    draft.modifiers[0].prefix.whitespace = " ";
                });
            }
            m = produce(m, draft => {
                draft.modifiers = draft.modifiers.map((mod, i) =>
                    i > 0 && mod.prefix.whitespace === "" ?
                        {...mod, prefix: {...mod.prefix, whitespace: " "}} : mod
                );
            });
            first = false;
        }

        if (m.typeParameters && m.typeParameters.typeParameters.length > 0 && m.typeParameters.prefix.whitespace === "" && !first) {
            m = produce(m, draft => {
                draft.typeParameters!.prefix.whitespace = " ";
            });
            first = false;
        }

        if (m.returnTypeExpression && m.returnTypeExpression.prefix.whitespace === "" && !first) {
            m = produce(m, draft => {
                if (m.returnTypeExpression!.kind === J.Kind.AnnotatedType) {
                    const ann = (m.returnTypeExpression as J.AnnotatedType).annotations;
                    (m.returnTypeExpression as Draft<J.AnnotatedType>).annotations = ann.map((a, i) =>
                        i === 0 ? {...a, prefix: {...a.prefix, whitespace: " "}} : a
                    );
                } else {
                    draft.returnTypeExpression!.prefix.whitespace = " ";
                }
            });
        }

        if (!first && m.name.prefix.whitespace === "") {
            m = produce(m, draft => {
                draft.name.prefix.whitespace = " ";
            });
        }

        if (m.throws && m.throws.before.whitespace === "") {
            m = produce(m, draft => {
                draft.throws!.before.whitespace = " ";
            });
        }

        return m;
    }

    protected async visitNewClass(newClass: J.NewClass, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitNewClass(newClass, p) as J.NewClass;
        return produce(ret, draft => {
            if (draft.class != undefined) {
                draft.class.prefix.whitespace = " ";
            }
        });
    }

    protected async visitReturn(returnNode: J.Return, p: ExecutionContext): Promise<J | undefined> {
        const r = await super.visitReturn(returnNode, p) as J.Return;
        if (r.expression && r.expression.prefix.whitespace === "" &&
            !r.markers.markers.find(m => m.id === "org.openrewrite.java.marker.ImplicitReturn")) {
            return produce(r, draft => {
                draft.expression!.prefix.whitespace = " ";
            });
        }
        return r;
    }

    protected async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, p: ExecutionContext): Promise<J | undefined> {
        const ret = await super.visitScopedVariableDeclarations(scopedVariableDeclarations, p) as JS.ScopedVariableDeclarations;
        return ret.scope && produce(ret, draft => {
            draft.variables[0].element.prefix.whitespace = " ";
        });
    }

    protected async visitVariableDeclarations(v: J.VariableDeclarations, p: ExecutionContext): Promise<J | undefined> {
        let ret = await super.visitVariableDeclarations(v, p) as J.VariableDeclarations;
        let first = ret.leadingAnnotations.length === 0;

        if (first && ret.modifiers.length > 0) {
            ret = produce(ret, draft => {
                draft.modifiers = draft.modifiers.map((m, i) =>
                    i > 0 && m.prefix.whitespace === "" ?
                        {...m, prefix: {...m.prefix, whitespace: " "}} : m
                );
            });
            first = false;
        }

        if (!first) {
            ret = produce(ret, draft => {
                draft.variables[0].element.prefix.whitespace = " ";
            });
        }

        return ret;
    }


    protected async visitCase(caseNode: J.Case, p: ExecutionContext): Promise<J | undefined> {
        const c = await super.visitCase(caseNode, p) as J.Case;

        if (c.guard && c.caseLabels.elements.length > 0 && c.caseLabels.elements[c.caseLabels.elements.length - 1].after.whitespace === "") {
            return produce(c, draft => {
                const last = draft.caseLabels.elements.length - 1;
                draft.caseLabels.elements[last].after.whitespace = " ";
            });
        }

        return c;
    }
}