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
import {JavaScriptVisitor} from "./visitor";
import {Comment, J, lastWhitespace, replaceLastWhitespace, Statement} from "../java";
import {Draft, produce} from "immer";
import {Cursor, isScope, Tree} from "../tree";
import {BlankLinesStyle, getStyle, SpacesStyle, StyleKind, TabsAndIndentsStyle, WrappingAndBracesStyle} from "./style";
import {NamedStyles} from "../style";
import {produceAsync} from "../visitor";
import {findMarker} from "../markers";
import {Generator} from "./markers";
import {TabsAndIndentsVisitor} from "./tabs-and-indents-visitor";

export {TabsAndIndentsVisitor} from "./tabs-and-indents-visitor";

export const maybeAutoFormat = async <J2 extends J, P>(before: J2, after: J2, p: P, stopAfter?: J, parent?: Cursor): Promise<J2> => {
    if (before !== after) {
        return autoFormat(after, p, stopAfter, parent);
    }
    return after;
}

export const autoFormat = async <J2 extends J, P>(j: J2, p: P, stopAfter?: J, parent?: Cursor, styles?: NamedStyles[]): Promise<J2> =>
    (await new AutoformatVisitor(stopAfter, styles).visit(j, p, parent) as J2);

/**
 * Formats JavaScript/TypeScript code using a comprehensive set of formatting rules.
 *
 * Style resolution order (first match wins):
 * 1. Styles passed to the constructor
 * 2. Styles from source file markers (NamedStyles)
 * 3. IntelliJ defaults
 */
export class AutoformatVisitor<P> extends JavaScriptVisitor<P> {
    private readonly styles?: NamedStyles[];

    constructor(private stopAfter?: Tree, styles?: NamedStyles[]) {
        super();
        this.styles = styles;
    }

    async visit<R extends J>(tree: Tree, p: P, cursor?: Cursor): Promise<R | undefined> {
        const visitors = [
            new NormalizeWhitespaceVisitor(this.stopAfter),
            new MinimumViableSpacingVisitor(this.stopAfter),
            new BlankLinesVisitor(getStyle(StyleKind.BlankLinesStyle, tree, this.styles) as BlankLinesStyle, this.stopAfter),
            new WrappingAndBracesVisitor(getStyle(StyleKind.WrappingAndBracesStyle, tree, this.styles) as WrappingAndBracesStyle, this.stopAfter),
            new SpacesVisitor(getStyle(StyleKind.SpacesStyle, tree, this.styles) as SpacesStyle, this.stopAfter),
            new TabsAndIndentsVisitor(getStyle(StyleKind.TabsAndIndentsStyle, tree, this.styles) as TabsAndIndentsStyle, this.stopAfter),
        ]

        let t: R | undefined = tree as R;
        for (const visitor of visitors) {
            t = await visitor.visit(t, p, cursor);
            if (t === undefined) {
                return undefined;
            }
        }

        return t;
    }
}

export class NormalizeWhitespaceVisitor<P> extends JavaScriptVisitor<P> {
    // called NormalizeFormat in Java
    // Ensures that whitespace is on the outermost AST element possible

    constructor(private stopAfter?: Tree) {
        super();
    }

    override async visit<R extends J>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        if (this.cursor?.getNearestMessage("stop") != null) {
            return tree as R;
        }
        return super.visit(tree, p, parent);
    }

    override async postVisit(tree: J, p: P): Promise<J | undefined> {
        if (this.stopAfter != null && isScope(this.stopAfter, tree)) {
            this.cursor?.root.messages.set("stop", true);
        }
        return super.postVisit(tree, p);
    }
}

export class SpacesVisitor<P> extends JavaScriptVisitor<P> {
    constructor(private style: SpacesStyle, private stopAfter?: Tree) {
        super();
    }

    override async visit<R extends J>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        if (this.cursor?.getNearestMessage("stop") != null) {
            return tree as R;
        }
        return super.visit(tree, p, parent);
    }

    override async postVisit(tree: J, p: P): Promise<J | undefined> {
        if (this.stopAfter != null && isScope(this.stopAfter, tree)) {
            this.cursor?.root.messages.set("stop", true);
        }
        return super.postVisit(tree, p);
    }

    protected async visitAlias(alias: JS.Alias, p: P): Promise<J | undefined> {
        const ret = await super.visitAlias(alias, p) as JS.Alias;
        return produce(ret, draft => {
            draft.propertyName.after.whitespace = " ";
        });
    }

    protected async visitArrayAccess(arrayAccess: J.ArrayAccess, p: P): Promise<J | undefined> {
        const ret = await super.visitArrayAccess(arrayAccess, p) as J.ArrayAccess;
        return produce(ret, draft => {
            draft.dimension.index.element.prefix.whitespace = this.style.within.arrayBrackets ? " " : "";
            draft.dimension.index.after.whitespace = this.style.within.arrayBrackets ? " " : "";
        });
    }

    protected async visitAssignment(assignment: J.Assignment, p: P): Promise<J | undefined> {
        const ret = await super.visitAssignment(assignment, p) as J.Assignment;
        return produce(ret, draft => {
            // Preserve newlines - only modify if no newlines present
            if (!draft.assignment.before.whitespace.includes("\n")) {
                draft.assignment.before.whitespace = this.style.aroundOperators.assignment ? " " : "";
            }
            if (!draft.assignment.element.prefix.whitespace.includes("\n")) {
                draft.assignment.element.prefix.whitespace = this.style.aroundOperators.assignment ? " " : "";
            }
        });
    }

    protected async visitBinary(binary: J.Binary, p: P): Promise<J | undefined> {
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
                throw new Error("Unsupported operator type " + ret.operator.element.valueOf());
        }
        return produce(ret, draft => {
            // Preserve newlines - only modify if no newlines present
            if (!draft.operator.before.whitespace.includes("\n")) {
                draft.operator.before.whitespace = property ? " " : "";
            }
            if (!draft.right.prefix.whitespace.includes("\n")) {
                draft.right.prefix.whitespace = property ? " " : "";
            }
        }) as J.Binary;
    }

    protected async visitCase(aCase: J.Case, p: P): Promise<J | undefined> {
        const ret = await super.visitCase(aCase, p) as J.Case;
        return ret && produce(ret, draft => {
            if (draft.caseLabels.elements[0].element.kind != J.Kind.Identifier || (draft.caseLabels.elements[0].element as J.Identifier).simpleName != "default") {
                draft.caseLabels.before.whitespace = " ";
            }
        });
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitClassDeclaration(classDecl, p) as J.ClassDeclaration;
        // TODO typeParameters - IntelliJ doesn't seem to provide a setting for angleBrackets spacing for Typescript (while it does for Java),
        // thus we either introduce our own setting or just enforce the natural spacing with no setting

        return produce(ret, draft => {
            draft.body.prefix.whitespace = this.style.beforeLeftBrace.catchLeftBrace ? " " : "";
        }) as J.ClassDeclaration;
    }

    public async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        const ret = await super.visitContainer(container, p) as J.Container<T>;
        return produce(ret, draft => {
            if (draft.elements.length > 0) {
                // Apply beforeComma rule to all elements except the last
                // (last element's after is before closing bracket, not a comma)
                for (let i = 0; i < draft.elements.length - 1; i++) {
                    const afterWs = draft.elements[i].after.whitespace;
                    // Preserve newlines - only adjust when on same line
                    if (!afterWs.includes("\n")) {
                        draft.elements[i].after.whitespace = this.style.other.beforeComma ? " " : "";
                    }
                }
            }
            if (draft.elements.length > 1) {
                // Apply afterComma rule to elements after the first
                for (let i = 1; i < draft.elements.length; i++) {
                    const currentWs = draft.elements[i].element.prefix.whitespace;
                    // Also check for Spread marker with newline (for spread elements the newline is in the marker)
                    const element = draft.elements[i].element as J;
                    const spreadMarker = element.markers?.markers?.find(m => m.kind === JS.Markers.Spread) as { prefix: J.Space } | undefined;
                    const hasNewlineInSpread = spreadMarker?.prefix?.whitespace?.includes("\n");
                    // Preserve original newlines - only adjust spacing when elements are on same line
                    if (!currentWs.includes("\n") && !hasNewlineInSpread) {
                        draft.elements[i].element.prefix.whitespace = this.style.other.afterComma ? " " : "";
                    }
                }
            }
        });
    }

    protected async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitExportDeclaration(exportDeclaration, p) as JS.ExportDeclaration;
        return produce(ret, draft => {
            if (draft.exportClause) {
                draft.exportClause.prefix.whitespace = " ";
                if (draft.exportClause.kind == JS.Kind.NamedExports) {
                    const ne = (draft.exportClause as Draft<JS.NamedExports>);
                    if (ne.elements.elements.length > 0) {
                        // Check if this is a multi-line export (any element's prefix has a newline)
                        const isMultiLine = ne.elements.elements.some(e => e.element.prefix.whitespace.includes("\n"));
                        if (!isMultiLine) {
                            // Single-line: adjust brace spacing
                            ne.elements.elements[0].element.prefix.whitespace = this.style.within.es6ImportExportBraces ? " " : "";
                            ne.elements.elements[ne.elements.elements.length - 1].after.whitespace = this.style.within.es6ImportExportBraces ? " " : "";
                        } else {
                            // Multi-line: apply beforeComma rule to last element's after (for trailing commas)
                            // If it has only spaces (no newline), it's the space before a trailing comma
                            const lastAfter = ne.elements.elements[ne.elements.elements.length - 1].after.whitespace;
                            if (!lastAfter.includes("\n") && lastAfter.trim() === "") {
                                ne.elements.elements[ne.elements.elements.length - 1].after.whitespace = this.style.other.beforeComma ? " " : "";
                            }
                        }
                    }
                }
            }
            draft.typeOnly.before.whitespace = draft.typeOnly.element ? " " : "";
            if (draft.moduleSpecifier) {
                draft.moduleSpecifier.before.whitespace = " ";
                draft.moduleSpecifier.element.prefix.whitespace = " ";
            }
        })
    }

    protected async visitForLoop(forLoop: J.ForLoop, p: P): Promise<J | undefined> {
        const ret = await super.visitForLoop(forLoop, p) as J.ForLoop;
        return produceAsync(ret, async draft => {
            draft.control.prefix.whitespace = this.style.beforeParentheses.forParentheses ? " " : "";
            draft.control.init = await Promise.all(draft.control.init.map(async (oneInit, index) => {
                if (oneInit.element.kind === J.Kind.VariableDeclarations) {
                    const vd = oneInit.element as Draft<J.VariableDeclarations>;
                    if (vd.modifiers && vd.modifiers.length > 0) {
                        vd.modifiers[0].prefix.whitespace = "";
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

    protected async visitIf(iff: J.If, p: P): Promise<J | undefined> {
        const ret = await super.visitIf(iff, p) as J.If;
        return produceAsync(ret, async draft => {
            draft.ifCondition = await this.spaceBefore(draft.ifCondition, this.style.beforeParentheses.ifParentheses);
            draft.ifCondition.tree = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(draft.ifCondition.tree, this.style.within.ifParentheses), this.style.within.ifParentheses);
            draft.thenPart = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(ret.thenPart, this.style.beforeLeftBrace.ifLeftBrace), false);
        });
    }

    protected async visitImportDeclaration(jsImport: JS.Import, p: P): Promise<J | undefined> {
        const ret = await super.visitImportDeclaration(jsImport, p) as JS.Import;
        return produce(ret, draft => {
            if (draft.importClause) {
                // Space after 'import' keyword:
                // - If there's a default import (name), space goes in importClause.prefix
                // - If typeOnly (import type ...), space goes in importClause.prefix (before 'type')
                // - If only namedBindings (no default, no type), space goes in namedBindings.prefix (importClause.prefix is empty)
                const hasDefaultImport = !!draft.importClause.name;
                draft.importClause.prefix.whitespace = (hasDefaultImport || draft.importClause.typeOnly) ? " " : "";
                if (draft.importClause.name) {
                    draft.importClause.name.after.whitespace = "";
                }
                if (draft.importClause.namedBindings) {
                    // Space before namedBindings - always needed
                    draft.importClause.namedBindings.prefix.whitespace = " ";
                    if (draft.importClause.namedBindings.kind == JS.Kind.NamedImports) {
                        const ni = draft.importClause.namedBindings as Draft<JS.NamedImports>;
                        // Check if this is a multi-line import (any element's prefix has a newline)
                        const isMultiLine = ni.elements.elements.some(e => e.element.prefix.whitespace.includes("\n"));
                        if (!isMultiLine) {
                            // Single-line: adjust brace spacing
                            ni.elements.elements[0].element.prefix.whitespace = this.style.within.es6ImportExportBraces ? " " : "";
                            ni.elements.elements[ni.elements.elements.length - 1].after.whitespace = this.style.within.es6ImportExportBraces ? " " : "";
                        } else {
                            // Multi-line: apply beforeComma rule to last element's after (for trailing commas)
                            // If it has only spaces (no newline), it's the space before a trailing comma
                            const lastAfter = ni.elements.elements[ni.elements.elements.length - 1].after.whitespace;
                            if (!lastAfter.includes("\n") && lastAfter.trim() === "") {
                                ni.elements.elements[ni.elements.elements.length - 1].after.whitespace = this.style.other.beforeComma ? " " : "";
                            }
                        }
                    }
                }
            }
            if (draft.moduleSpecifier) {
                draft.moduleSpecifier.before.whitespace = " ";
                draft.moduleSpecifier.element.prefix.whitespace = draft.importClause ? " " : "";
            }
        })
    }

    protected async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitIndexSignatureDeclaration(indexSignatureDeclaration, p) as JS.IndexSignatureDeclaration;
        return produce(ret, draft => {
            draft.typeExpression.before.whitespace = this.style.other.beforeTypeReferenceColon ? " " : "";
            draft.typeExpression.element.prefix.whitespace = this.style.other.afterTypeReferenceColon ? " " : "";
        });
    }

    protected async visitMethodDeclaration(methodDecl: J.MethodDeclaration, p: P): Promise<J | undefined> {
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

            // Handle generator asterisk spacing
            // - space before * is in the Generator marker's prefix
            // - space after * is in the method name's prefix
            const generatorIndex = ret.markers.markers.findIndex(m => m.kind === JS.Markers.Generator);
            if (generatorIndex >= 0) {
                const generator = draft.markers.markers[generatorIndex] as Draft<Generator>;
                generator.prefix.whitespace = this.style.other.beforeAsteriskInGenerator ? " " : "";
                draft.name.prefix.whitespace = this.style.other.afterAsteriskInGenerator ? " " : "";
            }

            // TODO typeParameters handling - see visitClassDeclaration
        });
    }

    protected async visitMethodInvocation(methodInv: J.MethodInvocation, p: P): Promise<J | undefined> {
        const ret = await super.visitMethodInvocation(methodInv, p) as J.MethodInvocation;
        return produceAsync(ret, async draft => {
            if (draft.select) {
                draft.arguments = await this.spaceBeforeContainer(draft.arguments, this.style.beforeParentheses.functionCallParentheses);
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
        });
    }

    protected async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: P): Promise<J | undefined> {
        const pa = await super.visitPropertyAssignment(propertyAssignment, p) as JS.PropertyAssignment;
        // Only adjust the space before the colon if there's an initializer (not a shorthand property)
        // For shorthand properties like { headers }, name.after.whitespace is the space before }
        if (pa.initializer) {
            return produceAsync(pa, draft => {
                draft.name.after.whitespace = this.style.other.beforePropertyNameValueSeparator ? " " : "";
            });
        }
        return pa;
    }

    protected async visitSwitch(switchNode: J.Switch, p: P): Promise<J | undefined> {
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

    protected async visitTernary(ternary: J.Ternary, p: P): Promise<J | undefined> {
        const ret = await super.visitTernary(ternary, p) as J.Ternary;
        return produceAsync(ret, async draft => {
            draft.truePart = await this.spaceBeforeLeftPaddedElement(draft.truePart, this.style.ternaryOperator.beforeQuestionMark, this.style.ternaryOperator.afterQuestionMark);
            draft.falsePart = await this.spaceBeforeLeftPaddedElement(draft.falsePart, this.style.ternaryOperator.beforeColon, this.style.ternaryOperator.afterColon);
        });
    }

    protected async visitTry(try_: J.Try, p: P): Promise<J | undefined> {
        const ret = await super.visitTry(try_, p) as J.Try;
        return produceAsync(ret, async draft => {
            draft.body.prefix.whitespace = this.style.beforeLeftBrace.tryLeftBrace ? " " : "";
            draft.catches = await Promise.all(draft.catches.map(async (catch_, index) => {
                catch_ = await this.spaceBefore(catch_, this.style.beforeKeywords.catchKeyword);
                catch_.parameter.prefix.whitespace = this.style.beforeParentheses.catchParentheses ? " " : "";
                catch_.parameter.tree = await this.spaceAfterRightPadded(await this.spaceBeforeRightPaddedElement(catch_.parameter.tree, this.style.within.catchParentheses), this.style.within.catchParentheses);
                if (catch_.parameter.tree.element.variables.length > 0) {
                    catch_.parameter.tree.element.variables[catch_.parameter.tree.element.variables.length - 1].after.whitespace = "";
                }
                catch_.body.prefix.whitespace = this.style.beforeLeftBrace.catchLeftBrace ? " " : "";
                return catch_;
            }));
            draft.finally = draft.finally && await this.spaceBeforeLeftPaddedElement(draft.finally, this.style.beforeKeywords.finallyKeyword, this.style.beforeLeftBrace.finallyLeftBrace) as Draft<J.LeftPadded<J.Block>>;
        });
    }

    protected async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeDeclaration(typeDeclaration, p) as JS.TypeDeclaration;
        return produce(ret, draft => {
            if (draft.modifiers.length > 0) {
                draft.name.before.whitespace = " ";
            }
            draft.name.element.prefix.whitespace = " ";
            // Preserve newlines - only modify if no newlines present
            if (!draft.initializer.before.whitespace.includes("\n")) {
                draft.initializer.before.whitespace = this.style.aroundOperators.assignment ? " " : "";
            }
            if (!draft.initializer.element.prefix.whitespace.includes("\n")) {
                draft.initializer.element.prefix.whitespace = this.style.aroundOperators.assignment ? " " : "";
            }
        });
    }

    protected async visitTypeInfo(typeInfo: JS.TypeInfo, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeInfo(typeInfo, p) as JS.TypeInfo;
        return produceAsync(ret, async draft => {
            draft.prefix.whitespace = this.style.other.beforeTypeReferenceColon ? " " : "";
            draft.typeIdentifier = await this.spaceBefore(draft.typeIdentifier, this.style.other.afterTypeReferenceColon);
        });
    }

    protected async visitTypeLiteral(typeLiteral: JS.TypeLiteral, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeLiteral(typeLiteral, p) as JS.TypeLiteral;
        // Apply objectLiteralTypeBraces spacing for single-line type literals
        const isSingleLine = !ret.members.end.whitespace.includes("\n") &&
            ret.members.statements.every(s => !s.element.prefix.whitespace.includes("\n"));
        if (isSingleLine && ret.members.statements.length > 0) {
            return produce(ret, draft => {
                const space = this.style.within.objectLiteralTypeBraces ? " " : "";
                draft.members.statements[0].element.prefix.whitespace = space;
                draft.members.end.whitespace = space;
            });
        }
        return ret;
    }

    protected async visitUnary(unary: J.Unary, p: P): Promise<J | undefined> {
        const ret = await super.visitUnary(unary, p) as J.Unary;
        return produce(ret, draft => {
            const spacing = this.style.aroundOperators.unary;

            switch (draft.operator.element) {
                case J.Unary.Type.Not:
                    draft.expression.prefix.whitespace = this.style.aroundOperators.afterUnaryNotAndNotNull ? " " : "";
                    break;
                case J.Unary.Type.PreIncrement:
                case J.Unary.Type.PreDecrement:
                case J.Unary.Type.Negative:
                case J.Unary.Type.Positive:
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
    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: P): Promise<J | undefined> {
        const ret = await super.visitVariable(variable, p) as J.VariableDeclarations.NamedVariable;
        if (variable.initializer?.element?.kind == JS.Kind.StatementExpression
            && (variable.initializer.element as JS.StatementExpression).statement.kind == J.Kind.MethodDeclaration) {
            return ret;
        }
        return produceAsync(ret, async draft => {
            if (draft.initializer) {
                // Preserve newlines - only modify if no newlines present
                if (!draft.initializer.before.whitespace.includes("\n")) {
                    draft.initializer.before.whitespace = this.style.aroundOperators.assignment ? " " : "";
                }
                if (!draft.initializer.element.prefix.whitespace.includes("\n")) {
                    draft.initializer.element.prefix.whitespace = this.style.aroundOperators.assignment ? " " : "";
                }
            }
        });
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, p: P): Promise<J | undefined> {
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
        return (await produceAsync(left, async draft => {
            if (draft.before.comments.length == 0) {
                // Preserve newlines - only modify if no newlines present
                if (!draft.before.whitespace.includes("\n")) {
                    draft.before.whitespace = spaceBeforePadding ? " " : "";
                }
            }
            draft.element = await this.spaceBefore(left.element, spaceBeforeElement) as Draft<T>;
        }))!;
    }

    private async spaceBeforeRightPaddedElement<T extends J>(right: J.RightPadded<T>, spaceBefore: boolean): Promise<J.RightPadded<T>> {
        return (await produceAsync(right, async draft => {
            draft.element = await this.spaceBefore(right.element, spaceBefore) as Draft<T>;
        }))!;
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

export class WrappingAndBracesVisitor<P> extends JavaScriptVisitor<P> {
    constructor(private readonly style: WrappingAndBracesStyle, private stopAfter?: Tree) {
        super();
    }

    override async visit<R extends J>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        if (this.cursor?.getNearestMessage("stop") != null) {
            return tree as R;
        }
        return super.visit(tree, p, parent);
    }

    override async postVisit(tree: J, p: P): Promise<J | undefined> {
        if (this.stopAfter != null && isScope(this.stopAfter, tree)) {
            this.cursor?.root.messages.set("stop", true);
        }
        return super.postVisit(tree, p);
    }

    protected async visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: P): Promise<J.VariableDeclarations> {
        const v = await super.visitVariableDeclarations(multiVariable, p) as J.VariableDeclarations;
        const parent = this.cursor.parentTree()?.value;
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

    protected async visitMethodDeclaration(method: J.MethodDeclaration, p: P): Promise<J.MethodDeclaration> {
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

    protected async visitElse(elsePart: J.If.Else, p: P): Promise<J.If.Else> {
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

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: P): Promise<J.ClassDeclaration> {
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

    protected async visitBlock(block: J.Block, p: P): Promise<J.Block> {
        const b = await super.visitBlock(block, p) as J.Block;
        return produce(b, draft => {
            if (!draft.end.whitespace.includes("\n") && (draft.statements.length == 0 || !draft.statements[draft.statements.length - 1].after.whitespace.includes("\n"))) {
                // Skip newline for object literals, type literals, and empty lambda/function bodies
                const parentKind = this.cursor.parent?.value.kind;
                if (parentKind !== J.Kind.NewClass &&
                    parentKind !== JS.Kind.TypeLiteral &&
                    !(draft.statements.length === 0 && (parentKind === J.Kind.Lambda || parentKind === J.Kind.MethodDeclaration))) {
                    draft.end = this.withNewlineSpace(draft.end);
                }
            }
        });
    }

    protected async visitSwitch(aSwitch: J.Switch, p: P): Promise<J | undefined> {
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


export class MinimumViableSpacingVisitor<P> extends JavaScriptVisitor<P> {
    constructor(private stopAfter?: Tree) {
        super();
    }

    override async visit<R extends J>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        if (this.cursor?.getNearestMessage("stop") != null) {
            return tree as R;
        }
        return super.visit(tree, p, parent);
    }

    override async postVisit(tree: J, p: P): Promise<J | undefined> {
        if (this.stopAfter != null && isScope(this.stopAfter, tree)) {
            this.cursor?.root.messages.set("stop", true);
        }
        return super.postVisit(tree, p);
    }

    protected async visitAwait(await_: JS.Await, p: P): Promise<J | undefined> {
        const ret = await super.visitAwait(await_, p) as JS.Await;
        return produce(ret, draft => {
            this.ensureSpace(draft.expression.prefix)
        });
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: P): Promise<J | undefined> {
        let c = await super.visitClassDeclaration(classDecl, p) as J.ClassDeclaration;
        let first = c.leadingAnnotations.length === 0;

        if (c.modifiers.length > 0) {
            if (!first && c.modifiers[0].prefix.whitespace === "") {
                c = produce(c, draft => {
                    this.ensureSpace(draft.modifiers[0].prefix);
                });
            }
            c = produce(c, draft => {
                for (let i = 1; i < draft.modifiers.length; i++) {
                    this.ensureSpace(draft.modifiers[i].prefix);
                }
            });
            first = false;
        }

        if (c.classKind.prefix.whitespace === "" && !first) {
            c = produce(c, draft => {
                this.ensureSpace(draft.classKind.prefix);
            });
            first = false;
        }

        // anonymous classes have an empty name
        if (c.name.simpleName !== "") {
            c = produce(c, draft => {
                this.ensureSpace(draft.name.prefix);
            });
        }

        if (c.typeParameters && c.typeParameters.elements.length > 0 && c.typeParameters.before.whitespace === "" && !first) {
            c = produce(c, draft => {
                this.ensureSpace(draft.typeParameters!.before);
            });
        }

        if (c.extends && c.extends.before.whitespace === "") {
            c = produce(c, draft => {
                this.ensureSpace(draft.extends!.before);
            });
        }

        if (c.implements && c.implements.before.whitespace === "") {
            c = produce(c, draft => {
                this.ensureSpace(draft.implements!.before);
                if (draft.implements != undefined && draft.implements.elements.length > 0) {
                    this.ensureSpace(draft.implements.elements[0].element.prefix);
                }
            });
        }

        c = produce(c, draft => {
            draft.body.prefix.whitespace = "";
        });

        return c;
    }

    protected async visitMethodDeclaration(method: J.MethodDeclaration, p: P): Promise<J | undefined> {
        let m = await super.visitMethodDeclaration(method, p) as J.MethodDeclaration;
        let first = m.leadingAnnotations.length === 0;

        if (m.modifiers.length > 0) {
            if (!first && m.modifiers[0].prefix.whitespace === "") {
                m = produce(m, draft => {
                    this.ensureSpace(draft.modifiers[0].prefix);
                });
            }
            m = produce(m, draft => {
                for (let i = 1; i < draft.modifiers.length; i++) {
                    this.ensureSpace(draft.modifiers[i].prefix);
                }
            });
            first = false;
        }

        // FunctionDeclaration marker check must come AFTER modifiers processing
        // to avoid adding unwanted space before the first modifier (e.g., 'async')
        if (findMarker(method, JS.Markers.FunctionDeclaration)) {
            first = false;
        }

        if (!first && m.name.prefix.whitespace === "") {
            m = produce(m, draft => {
                this.ensureSpace(draft.name.prefix);
            });
        }

        if (m.throws && m.throws.before.whitespace === "") {
            m = produce(m, draft => {
                this.ensureSpace(draft.throws!.before);
            });
        }

        return m;
    }

    protected async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitNamespaceDeclaration(namespaceDeclaration, p) as JS.NamespaceDeclaration;
        return produce(ret, draft => {
            if (draft.modifiers.length > 0) {
                draft.keywordType.before.whitespace=" ";
            }
            this.ensureSpace(draft.name.element.prefix);
        });
    }

    protected async visitNewClass(newClass: J.NewClass, p: P): Promise<J | undefined> {
        const ret = await super.visitNewClass(newClass, p) as J.NewClass;
        return produce(ret, draft => {
            if (draft.class) {
                if (draft.class.kind == J.Kind.Identifier) {
                    this.ensureSpace((draft.class as Draft<J.Identifier>).prefix);
                }
            }
        });
    }

    protected async visitReturn(returnNode: J.Return, p: P): Promise<J | undefined> {
        const r = await super.visitReturn(returnNode, p) as J.Return;
        if (r.expression && r.expression.prefix.whitespace === "" &&
            !r.markers.markers.find(m => m.id === "org.openrewrite.java.marker.ImplicitReturn")) {
            return produce(r, draft => {
                this.ensureSpace(draft.expression!.prefix);
            });
        }
        return r;
    }

    protected async visitThrow(thrown: J.Throw, p: P): Promise<J | undefined> {
        const ret = await super.visitThrow(thrown, p) as J.Throw;
        return ret && produce(ret, draft => {
           this.ensureSpace(draft.exception.prefix);
        });
    }

    protected async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeDeclaration(typeDeclaration, p) as JS.TypeDeclaration;
        return produce(ret, draft => {
            if (draft.modifiers.length > 0) {
                this.ensureSpace(draft.name.before);
            }
            this.ensureSpace(draft.name.element.prefix);
        });
    }

    protected async visitTypeOf(typeOf: JS.TypeOf, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeOf(typeOf, p) as JS.TypeOf;
        return produce(ret, draft => {
            this.ensureSpace(draft.expression.prefix);
        });
    }

    protected async visitTypeParameter(typeParam: J.TypeParameter, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeParameter(typeParam, p) as J.TypeParameter;
        return produce(ret, draft => {
            if (draft.bounds && draft.bounds.elements.length > 0) {
                this.ensureSpace(draft.bounds.before);
                this.ensureSpace(draft.bounds.elements[0].element.prefix);
            }
        });
    }

    protected async visitVariableDeclarations(v: J.VariableDeclarations, p: P): Promise<J | undefined> {
        let ret = await super.visitVariableDeclarations(v, p) as J.VariableDeclarations;
        let first = ret.leadingAnnotations.length === 0;

        if (first && ret.modifiers.length > 0) {
            ret = produce(ret, draft => {
                for (let i = 1; i < draft.modifiers.length; i++) {
                    this.ensureSpace(draft.modifiers[i].prefix);
                }
            });
            first = false;
        }

        if (!first) {
            ret = produce(ret, draft => {
                this.ensureSpace(draft.variables[0].element.prefix);
            });
        }

        return ret;
    }


    protected async visitCase(caseNode: J.Case, p: P): Promise<J | undefined> {
        const c = await super.visitCase(caseNode, p) as J.Case;

        if (c.guard && c.caseLabels.elements.length > 0 && c.caseLabels.elements[c.caseLabels.elements.length - 1].after.whitespace === "") {
            return produce(c, draft => {
                const last = draft.caseLabels.elements.length - 1;
                draft.caseLabels.elements[last].after.whitespace = " ";
            });
        }

        return c;
    }

    private ensureSpace(spaceDraft: Draft<J.Space>) {
        if (spaceDraft.whitespace.length === 0 && spaceDraft.comments.length === 0) {
            spaceDraft.whitespace = " ";
        }
    }
}

export class BlankLinesVisitor<P> extends JavaScriptVisitor<P> {
    constructor(private readonly style: BlankLinesStyle, private stopAfter?: Tree) {
        super();
    }

    protected async preVisit(tree: J, p: P): Promise<J | undefined> {
        let ret = await super.preVisit(tree, p) as J;

        if (ret.kind === JS.Kind.CompilationUnit) {
            ret = produce(ret as JS.CompilationUnit, draft => {
                if (draft.prefix.comments.length == 0) {
                    draft.prefix.whitespace = "";
                }
            });
        }
        if (ret.kind === J.Kind.MethodDeclaration
            && this.cursor.parent?.parent?.parent?.value.kind === J.Kind.ClassDeclaration) {
            ret = produce(ret as JS.StatementExpression, draft => {
                this.ensurePrefixHasNewLine(draft);
            });
        }

        return ret;
    }

    override async visit<R extends J>(tree: Tree, p: P, cursor?: Cursor): Promise<R | undefined> {
        if (this.cursor?.getNearestMessage("stop") != null) {
            return tree as R;
        }
        return super.visit(tree, p, cursor);
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: P): Promise<J.ClassDeclaration> {
        let ret = await super.visitClassDeclaration(classDecl, p) as J.ClassDeclaration;
        if (!ret.body) return ret;
        return produce(ret, draft => {
            const statements = draft.body.statements;
            if (statements.length > 0) {
                this.keepMaximumBlankLines(draft.body.statements[0].element, 0);

                const isInterface = draft.classKind.type === J.ClassDeclaration.Kind.Type.Interface;
                for (let i = 1; i < statements.length; i++) {
                    const previousElement = statements[i - 1].element;
                    let currentElement = statements[i].element;
                    if (previousElement.kind == J.Kind.VariableDeclarations || currentElement.kind == J.Kind.VariableDeclarations) {
                        const fieldBlankLines = isInterface
                            ? this.style.minimum.aroundFieldInInterface ?? 0
                            : this.style.minimum.aroundField;
                        this.minimumBlankLines(currentElement, fieldBlankLines);
                    }
                    if (previousElement.kind == J.Kind.MethodDeclaration || currentElement.kind == J.Kind.MethodDeclaration) {
                        const methodBlankLines = isInterface
                            ? this.style.minimum.aroundMethodInInterface ?? 0
                            : this.style.minimum.aroundMethod;
                        this.minimumBlankLines(currentElement, methodBlankLines);
                    }
                    this.keepMaximumBlankLines(currentElement, this.style.keepMaximum.inCode);
                    draft.body.statements[i].element = currentElement;
                }
            }

            const cu = this.cursor.firstEnclosing((x: any): x is JS.CompilationUnit => x.kind === JS.Kind.CompilationUnit) as JS.CompilationUnit | undefined;
            const topLevelIndex = cu?.statements.findIndex(s => s.element.id == draft.id);

            if (topLevelIndex !== undefined) {
                const isImportJustBeforeThis = topLevelIndex > 0 && cu?.statements[topLevelIndex - 1].element.kind === JS.Kind.Import;

                if (isImportJustBeforeThis) {
                    this.minimumBlankLines(draft, this.style.minimum.afterImports ?? 0);
                }
                if (topLevelIndex > 0) {
                    this.minimumBlankLines(draft, this.style.minimum.aroundClass);
                }
                this.keepMaximumBlankLines(draft, this.style.keepMaximum.inCode);
            }
        });
    }

    override async visitStatement(statement: Statement, p: P): Promise<Statement> {
        const ret = await super.visitStatement(statement, p) as Statement;
        const parent = this.cursor.parentTree()?.value;
        const grandparent = this.cursor.parentTree()?.parent?.value;

        return produce(ret, draft => {
            if (grandparent?.kind === J.Kind.ClassDeclaration && parent?.kind === J.Kind.Block) {
                const classDecl = grandparent as J.ClassDeclaration;
                const block = parent as J.Block;

                const isFirst = block.statements.length > 0 && block.statements[0].element.id === draft.id;
                if (!isFirst) {
                    if (draft.kind === J.Kind.VariableDeclarations) {
                        const declMax = classDecl.classKind.type === J.ClassDeclaration.Kind.Type.Interface
                            ? this.style.minimum.aroundFieldInInterface ?? 0
                            : this.style.minimum.aroundField;
                        this.minimumBlankLines(draft, declMax);
                    } else if (draft.kind === J.Kind.MethodDeclaration) {
                        const declMax = classDecl.classKind.type === J.ClassDeclaration.Kind.Type.Interface
                            ? this.style.minimum.aroundMethodInInterface ?? 0
                            : this.style.minimum.aroundMethod;
                        this.minimumBlankLines(draft, declMax);
                    } else if (draft.kind === J.Kind.Block) {
                        this.minimumBlankLines(draft, this.style.minimum.aroundFunction);
                    } else if (draft.kind === J.Kind.ClassDeclaration) {
                        this.minimumBlankLines(draft, this.style.minimum.aroundClass);
                    }
                    this.keepMaximumBlankLines(draft, this.style.keepMaximum.inCode);
                }
            } else if (parent?.kind === J.Kind.Block && grandparent?.kind !== J.Kind.NewClass && grandparent?.kind !== JS.Kind.TypeLiteral ||
                      (parent?.kind === JS.Kind.CompilationUnit && (parent! as JS.CompilationUnit).statements[0].element.id != draft.id) ||
                      (parent?.kind === J.Kind.Case)) {
                if (draft.kind != J.Kind.Case) {
                    this.ensurePrefixHasNewLine(draft);
                }
            }
        });
    }

    protected async visitBlock(block: J.Block, p: P): Promise<J.Block> {
        const b = await super.visitBlock(block, p) as J.Block;
        return produce(b, draft => {
            const parentKind = this.cursor.parent?.value.kind;
            // Skip newline for object literals (NewClass) and type literals (TypeLiteral)
            if (parentKind != J.Kind.NewClass && parentKind != JS.Kind.TypeLiteral) {
                draft.end = replaceLastWhitespace(draft.end, ws =>
                    ws.includes("\n") ? ws : ws.replace(/[ \t]+$/, '') + "\n"
                );
            }
        });
    }

    protected async visitEnumValue(enumValue: J.EnumValue, p: P): Promise<J.EnumValue> {
        const e = await super.visitEnumValue(enumValue, p) as J.EnumValue;
        this.keepMaximumBlankLines(e, this.style.keepMaximum.inCode);
        return e;
    }

    private keepMaximumBlankLines<T extends J>(node: Draft<T>, max: number) {
        const whitespace = node.prefix.whitespace;
        const blankLines = BlankLinesVisitor.countNewlines(whitespace) - 1;
        if (blankLines > max) {
            let idx = 0;
            for (let i = 0; i < blankLines - max + 1; i++, idx++) {
                idx = whitespace.indexOf('\n', idx);
            }
            idx--;
            node.prefix.whitespace = whitespace.substring(idx);
        }
    }

    private minimumBlankLines<T extends J>(node: Draft<T>, min: number) {
        if (min <= 0) {
            return;
        }
        const currentNewlines = BlankLinesVisitor.countNewlines(node.prefix.whitespace);
        const needed = min - currentNewlines + 1;
        if (needed > 0) {
            node.prefix.whitespace = "\n".repeat(needed) + node.prefix.whitespace;
        }
    }

    private ensurePrefixHasNewLine<T extends J>(node: Draft<J>) {
        if (!node.prefix) return;

        // Check if newline already exists in the effective last whitespace
        if (lastWhitespace(node.prefix).includes("\n")) {
            return; // Already has a newline
        }

        if (node.kind === JS.Kind.ExpressionStatement) {
            this.ensurePrefixHasNewLine((node as JS.ExpressionStatement).expression);
        } else {
            node.prefix = replaceLastWhitespace(node.prefix, () => "\n");
        }
    }

    private static countNewlines(s: string): number {
        return [...s].filter(c => c === "\n").length;
    }

    override async postVisit(tree: J, p: P): Promise<J | undefined> {
        if (this.stopAfter != null && isScope(this.stopAfter, tree)) {
            this.cursor?.root.messages.set("stop", true);
        }
        return super.postVisit(tree, p);
    }
}
