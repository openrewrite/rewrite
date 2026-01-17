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
import {JS} from "../tree";
import {JavaScriptVisitor} from "../visitor";
import {Comment, J, lastWhitespace, replaceLastWhitespace, Statement} from "../../java";
import {create as produce, Draft} from "mutative";
import {Cursor, isScope, Tree} from "../../tree";
import {BlankLinesStyle, getStyle, SpacesStyle, StyleKind, TabsAndIndentsStyle, WrappingAndBracesStyle} from "../style";
import {NamedStyles} from "../../style";
import {produceAsync} from "../../visitor";
import {Generator} from "../markers";
import {TabsAndIndentsVisitor} from "./tabs-and-indents-visitor";
import {NormalizeWhitespaceVisitor} from "./normalize-whitespace-visitor";
import {MinimumViableSpacingVisitor} from "./minimum-viable-spacing-visitor";
import {applyPrettierFormatting, getPrettierStyle} from "./prettier-format";

export {TabsAndIndentsVisitor} from "./tabs-and-indents-visitor";
export {NormalizeWhitespaceVisitor} from "./normalize-whitespace-visitor";
export {MinimumViableSpacingVisitor} from "./minimum-viable-spacing-visitor";

export const maybeAutoFormat = async <J2 extends J, P>(before: J2, after: J2, p: P, stopAfter?: J, parent?: Cursor): Promise<J2> => {
    if (before !== after) {
        return autoFormat(after, p, stopAfter, parent);
    }
    return after;
}

export const autoFormat = async <J2 extends J, P>(
    j: J2,
    p: P,
    stopAfter?: J,
    parent?: Cursor,
    styles?: NamedStyles<string>[]
): Promise<J2> =>
    (await new AutoformatVisitor(stopAfter, styles).visit(j, p, parent) as J2);

/**
 * Formats JavaScript/TypeScript code using a comprehensive set of formatting rules.
 *
 * Style resolution order (first match wins):
 * 1. Styles passed to the constructor
 * 2. Styles from source file markers (NamedStyles)
 * 3. IntelliJ defaults
 *
 * When a PrettierStyle is present (either in the styles array or as a marker on the source file),
 * Prettier is used for formatting. Otherwise, built-in formatting visitors are used.
 */
export class AutoformatVisitor<P> extends JavaScriptVisitor<P> {
    private readonly styles?: NamedStyles<string>[];

    constructor(private stopAfter?: Tree, styles?: NamedStyles<string>[]) {
        super();
        this.styles = styles;
    }

    async visit<R extends J>(tree: Tree, p: P, cursor?: Cursor): Promise<R | undefined> {
        // Check for PrettierStyle in styles array or as marker on source file
        // If found, delegate entirely to Prettier (skip other formatting visitors)
        const prettierStyle = getPrettierStyle(tree, cursor, this.styles);
        if (prettierStyle) {
            return applyPrettierFormatting(tree as R, prettierStyle, p, cursor, this.stopAfter);
        }

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
            draft.propertyName.padding.after.whitespace = " ";
        });
    }

    protected async visitArrayAccess(arrayAccess: J.ArrayAccess, p: P): Promise<J | undefined> {
        const ret = await super.visitArrayAccess(arrayAccess, p) as J.ArrayAccess;
        return produce(ret, draft => {
            // Preserve newlines - only modify if no newlines present
            if (!draft.dimension.index.prefix.whitespace.includes("\n")) {
                draft.dimension.index.prefix.whitespace = this.style.within.arrayBrackets ? " " : "";
            }
            if (!draft.dimension.index.padding.after.whitespace.includes("\n")) {
                draft.dimension.index.padding.after.whitespace = this.style.within.arrayBrackets ? " " : "";
            }
        });
    }

    protected async visitAssignment(assignment: J.Assignment, p: P): Promise<J | undefined> {
        const ret = await super.visitAssignment(assignment, p) as J.Assignment;
        return produce(ret, draft => {
            // Preserve newlines - only modify if no newlines present
            if (!draft.assignment.padding.before.whitespace.includes("\n")) {
                draft.assignment.padding.before.whitespace = this.style.aroundOperators.assignment ? " " : "";
            }
            if (!draft.assignment.prefix.whitespace.includes("\n")) {
                draft.assignment.prefix.whitespace = this.style.aroundOperators.assignment ? " " : "";
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
            if (!draft.operator.padding.before.whitespace.includes("\n")) {
                draft.operator.padding.before.whitespace = property ? " " : "";
            }
            if (!draft.right.prefix.whitespace.includes("\n")) {
                draft.right.prefix.whitespace = property ? " " : "";
            }
        }) as J.Binary;
    }

    protected async visitCase(aCase: J.Case, p: P): Promise<J | undefined> {
        const ret = await super.visitCase(aCase, p) as J.Case;
        return ret && produce(ret, draft => {
            if (draft.caseLabels.elements[0].kind != J.Kind.Identifier || (draft.caseLabels.elements[0] as J.RightPadded<J.Identifier>).simpleName != "default") {
                // Preserve newlines - only set to space if no newline exists
                if (!draft.caseLabels.before.whitespace.includes("\n")) {
                    draft.caseLabels.before.whitespace = " ";
                }
            }
        });
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitClassDeclaration(classDecl, p) as J.ClassDeclaration;
        // TODO typeParameters - IntelliJ doesn't seem to provide a setting for angleBrackets spacing for Typescript (while it does for Java),
        // thus we either introduce our own setting or just enforce the natural spacing with no setting

        return produce(ret, draft => {
            draft.body.prefix.whitespace = this.style.beforeLeftBrace.classInterfaceModuleLeftBrace ? " " : "";
        }) as J.ClassDeclaration;
    }

    public async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        const ret = await super.visitContainer(container, p) as J.Container<T>;
        return produce(ret, draft => {
            if (draft.elements.length > 0) {
                // Apply beforeComma rule to all elements except the last
                // (last element's after is before closing bracket, not a comma)
                for (let i = 0; i < draft.elements.length - 1; i++) {
                    const afterWs = draft.elements[i].padding.after.whitespace;
                    // Preserve newlines - only adjust when on same line
                    if (!afterWs.includes("\n")) {
                        draft.elements[i].padding.after.whitespace = this.style.other.beforeComma ? " " : "";
                    }
                }
            }
            if (draft.elements.length > 1) {
                // Apply afterComma rule to elements after the first
                for (let i = 1; i < draft.elements.length; i++) {
                    const currentWs = draft.elements[i].prefix.whitespace;
                    // Preserve original newlines - only adjust spacing when elements are on same line
                    if (!currentWs.includes("\n")) {
                        draft.elements[i].prefix.whitespace = this.style.other.afterComma ? " " : "";
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
                        const isMultiLine = ne.elements.elements.some(e => e.prefix.whitespace.includes("\n"));
                        if (!isMultiLine) {
                            // Single-line: adjust brace spacing
                            ne.elements.elements[0].prefix.whitespace = this.style.within.es6ImportExportBraces ? " " : "";
                            ne.elements.elements[ne.elements.elements.length - 1].padding.after.whitespace = this.style.within.es6ImportExportBraces ? " " : "";
                        } else {
                            // Multi-line: apply beforeComma rule to last element's after (for trailing commas)
                            // If it has only spaces (no newline), it's the space before a trailing comma
                            const lastAfter = ne.elements.elements[ne.elements.elements.length - 1].padding.after.whitespace;
                            if (!lastAfter.includes("\n") && lastAfter.trim() === "") {
                                ne.elements.elements[ne.elements.elements.length - 1].padding.after.whitespace = this.style.other.beforeComma ? " " : "";
                            }
                        }
                    }
                }
            }
            draft.typeOnly.padding.before.whitespace = draft.typeOnly.element ? " " : "";
            if (draft.moduleSpecifier) {
                draft.moduleSpecifier.padding.before.whitespace = " ";
                draft.moduleSpecifier.prefix.whitespace = " ";
            }
        })
    }

    protected async visitForLoop(forLoop: J.ForLoop, p: P): Promise<J | undefined> {
        const ret = await super.visitForLoop(forLoop, p) as J.ForLoop;
        return produceAsync(ret, async draft => {
            draft.control.prefix.whitespace = this.style.beforeParentheses.forParentheses ? " " : "";
            draft.control.init = await Promise.all(draft.control.init.map(async (oneInit, index) => {
                if (oneInit.kind === J.Kind.VariableDeclarations) {
                    const vd = oneInit as Draft<J.VariableDeclarations & J.RightPaddingMixin>;
                    if (vd.modifiers && vd.modifiers.length > 0) {
                        vd.modifiers[0].prefix.whitespace = "";
                    }
                }
                oneInit.padding.after.whitespace = "";
                this.spaceBeforeRightPaddedElementDraft(oneInit, index === 0 ? this.style.within.forParentheses : true);
                return oneInit;
            }));
            if (draft.control.condition) {
                draft.control.condition.prefix.whitespace = " ";
                draft.control.condition.padding.after.whitespace = this.style.other.beforeForSemicolon ? " " : "";
            }
            draft.control.update.forEach((oneUpdate, index) => {
                oneUpdate.prefix.whitespace = " ";
                oneUpdate.padding.after.whitespace = (index === draft.control.update.length - 1 ? this.style.within.forParentheses : this.style.other.beforeForSemicolon) ? " " : "";
            });

            this.spaceBeforeRightPaddedElementDraft(draft.body, this.style.beforeLeftBrace.forLeftBrace);
            this.spaceAfterRightPaddedDraft(draft.body, false);
        });
    }

    protected async visitIf(iff: J.If, p: P): Promise<J | undefined> {
        const ret = await super.visitIf(iff, p) as J.If;
        return produceAsync(ret, async draft => {
            this.spaceBeforeDraft(draft.ifCondition, this.style.beforeParentheses.ifParentheses);
            this.spaceBeforeRightPaddedElementDraft(draft.ifCondition.tree, this.style.within.ifParentheses);
            this.spaceAfterRightPaddedDraft(draft.ifCondition.tree, this.style.within.ifParentheses);
            this.spaceBeforeRightPaddedElementDraft(draft.thenPart, this.style.beforeLeftBrace.ifLeftBrace);
            this.spaceAfterRightPaddedDraft(draft.thenPart, false);
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
                    // For import equals declarations (import X = Y), use assignment spacing
                    // For regular imports (import X from 'Y'), no space after name
                    draft.importClause.name.padding.after.whitespace = draft.initializer
                        ? (this.style.aroundOperators.assignment ? " " : "")
                        : "";
                }
                if (draft.importClause.namedBindings) {
                    // Space before namedBindings - always needed
                    draft.importClause.namedBindings.prefix.whitespace = " ";
                    if (draft.importClause.namedBindings.kind == JS.Kind.NamedImports) {
                        const ni = draft.importClause.namedBindings as Draft<JS.NamedImports>;
                        // Check if this is a multi-line import (any element's prefix has a newline)
                        const isMultiLine = ni.elements.elements.some(e => e.prefix.whitespace.includes("\n"));
                        if (!isMultiLine) {
                            // Single-line: adjust brace spacing
                            ni.elements.elements[0].prefix.whitespace = this.style.within.es6ImportExportBraces ? " " : "";
                            ni.elements.elements[ni.elements.elements.length - 1].padding.after.whitespace = this.style.within.es6ImportExportBraces ? " " : "";
                        } else {
                            // Multi-line: apply beforeComma rule to last element's after (for trailing commas)
                            // If it has only spaces (no newline), it's the space before a trailing comma
                            const lastAfter = ni.elements.elements[ni.elements.elements.length - 1].padding.after.whitespace;
                            if (!lastAfter.includes("\n") && lastAfter.trim() === "") {
                                ni.elements.elements[ni.elements.elements.length - 1].padding.after.whitespace = this.style.other.beforeComma ? " " : "";
                            }
                        }
                    }
                }
            }
            if (draft.moduleSpecifier) {
                draft.moduleSpecifier.padding.before.whitespace = " ";
                draft.moduleSpecifier.prefix.whitespace = draft.importClause ? " " : "";
            }
        })
    }

    protected async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitIndexSignatureDeclaration(indexSignatureDeclaration, p) as JS.IndexSignatureDeclaration;
        return produce(ret, draft => {
            draft.typeExpression.padding.before.whitespace = this.style.other.beforeTypeReferenceColon ? " " : "";
            draft.typeExpression.prefix.whitespace = this.style.other.afterTypeReferenceColon ? " " : "";
        });
    }

    protected async visitMethodDeclaration(methodDecl: J.MethodDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitMethodDeclaration(methodDecl, p) as J.MethodDeclaration;
        return produceAsync(ret, async draft => {
            if (draft.body) {
                this.spaceBeforeDraft(draft.body, this.style.beforeLeftBrace.functionLeftBrace);
            }

            if (draft.parameters.elements.length > 0 && draft.parameters.elements[0].kind != J.Kind.Empty) {
                draft.parameters.elements.forEach((param, index) => {
                    this.spaceAfterRightPaddedDraft(param, index === draft.parameters.elements.length - 1 ? this.style.within.functionDeclarationParentheses : this.style.other.beforeComma);
                    this.spaceBeforeDraft(param, index === 0 ? this.style.within.functionDeclarationParentheses : this.style.other.afterComma);
                    (param as Draft<J.VariableDeclarations & J.RightPaddingMixin>).variables[0].name.prefix.whitespace = "";
                    (param as Draft<J.VariableDeclarations & J.RightPaddingMixin>).variables[0].padding.after.whitespace = "";
                });
            } else if (draft.parameters.elements.length == 1) {
                this.spaceBeforeRightPaddedElementDraft(draft.parameters.elements[0], this.style.within.functionDeclarationParentheses);
                this.spaceAfterRightPaddedDraft(draft.parameters.elements[0], false);
            }
            this.spaceBeforeContainerDraft(draft.parameters, this.style.beforeParentheses.functionDeclarationParentheses);

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
                this.spaceBeforeContainerDraft(draft.arguments, this.style.beforeParentheses.functionCallParentheses);
            }
            if (ret.arguments.elements.length > 0 && ret.arguments.elements[0].kind != J.Kind.Empty) {
                draft.arguments.elements.forEach((arg, index) => {
                    this.spaceAfterRightPaddedDraft(arg, index === draft.arguments.elements.length - 1 ? this.style.within.functionCallParentheses : this.style.other.beforeComma);
                    this.spaceBeforeDraft(arg, index === 0 ? this.style.within.functionCallParentheses : this.style.other.afterComma);
                });
            } else if (ret.arguments.elements.length == 1) {
                this.spaceBeforeRightPaddedElementDraft(draft.arguments.elements[0], this.style.within.functionCallParentheses);
                this.spaceAfterRightPaddedDraft(draft.arguments.elements[0], false);
            }
            // TODO typeParameters handling - see visitClassDeclaration
        });
    }

    protected async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: P): Promise<J | undefined> {
        const pa = await super.visitPropertyAssignment(propertyAssignment, p) as JS.PropertyAssignment;
        // Only adjust the space before the colon/equals if there's an initializer (not a shorthand property)
        if (pa.initializer) {
            return produceAsync(pa, draft => {
                draft.name.padding.after.whitespace = this.style.other.beforePropertyNameValueSeparator ? " " : "";
            });
        }
        return pa;
    }

    protected async visitSwitch(switchNode: J.Switch, p: P): Promise<J | undefined> {
        const ret = await super.visitSwitch(switchNode, p) as J.Switch;
        return produceAsync(ret, async draft => {
            this.spaceBeforeDraft(draft.selector, this.style.beforeParentheses.switchParentheses);
            this.spaceBeforeRightPaddedElementDraft(draft.selector.tree, this.style.within.switchParentheses);
            this.spaceAfterRightPaddedDraft(draft.selector.tree, this.style.within.switchParentheses);
            this.spaceBeforeDraft(draft.cases, this.style.beforeLeftBrace.switchLeftBrace);

            for (const case_ of draft.cases.statements) {
                if (case_.kind === J.Kind.Case) {
                    (case_ as Draft<J.Case & J.RightPaddingMixin>).caseLabels.elements[0].padding.after.whitespace = "";
                }
            }
        });
    }

    protected async visitTernary(ternary: J.Ternary, p: P): Promise<J | undefined> {
        const ret = await super.visitTernary(ternary, p) as J.Ternary;
        return produceAsync(ret, async draft => {
            this.spaceBeforeLeftPaddedElementDraft(draft.truePart, this.style.ternaryOperator.beforeQuestionMark, this.style.ternaryOperator.afterQuestionMark);
            this.spaceBeforeLeftPaddedElementDraft(draft.falsePart, this.style.ternaryOperator.beforeColon, this.style.ternaryOperator.afterColon);
        });
    }

    protected async visitTry(try_: J.Try, p: P): Promise<J | undefined> {
        const ret = await super.visitTry(try_, p) as J.Try;
        return produceAsync(ret, async draft => {
            draft.body.prefix.whitespace = this.style.beforeLeftBrace.tryLeftBrace ? " " : "";
            draft.catches.forEach(catch_ => {
                this.spaceBeforeDraft(catch_, this.style.beforeKeywords.catchKeyword);
                catch_.parameter.prefix.whitespace = this.style.beforeParentheses.catchParentheses ? " " : "";
                this.spaceBeforeRightPaddedElementDraft(catch_.parameter.tree, this.style.within.catchParentheses);
                this.spaceAfterRightPaddedDraft(catch_.parameter.tree, this.style.within.catchParentheses);
                if (catch_.parameter.tree.variables.length > 0) {
                    catch_.parameter.tree.variables[catch_.parameter.tree.variables.length - 1].padding.after.whitespace = "";
                }
                catch_.body.prefix.whitespace = this.style.beforeLeftBrace.catchLeftBrace ? " " : "";
            });
            if (draft.finally) {
                this.spaceBeforeLeftPaddedElementDraft(draft.finally, this.style.beforeKeywords.finallyKeyword, this.style.beforeLeftBrace.finallyLeftBrace);
            }
        });
    }

    protected async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeDeclaration(typeDeclaration, p) as JS.TypeDeclaration;
        return produce(ret, draft => {
            if (draft.modifiers.length > 0) {
                draft.name.padding.before.whitespace = " ";
            }
            draft.name.prefix.whitespace = " ";
            // Preserve newlines - only modify if no newlines present
            if (!draft.initializer.padding.before.whitespace.includes("\n")) {
                draft.initializer.padding.before.whitespace = this.style.aroundOperators.assignment ? " " : "";
            }
            if (!draft.initializer.prefix.whitespace.includes("\n")) {
                draft.initializer.prefix.whitespace = this.style.aroundOperators.assignment ? " " : "";
            }
        });
    }

    protected async visitTypeInfo(typeInfo: JS.TypeInfo, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeInfo(typeInfo, p) as JS.TypeInfo;
        return produceAsync(ret, async draft => {
            draft.prefix.whitespace = this.style.other.beforeTypeReferenceColon ? " " : "";
            this.spaceBeforeDraft(draft.typeIdentifier, this.style.other.afterTypeReferenceColon);
        });
    }

    protected async visitTypeLiteral(typeLiteral: JS.TypeLiteral, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeLiteral(typeLiteral, p) as JS.TypeLiteral;
        // Apply objectLiteralTypeBraces spacing for single-line type literals
        if (ret.members && ret.members.statements.length > 0) {
            const stmts = ret.members.statements;
            const isSingleLine = !ret.members.end.whitespace.includes("\n") &&
                stmts.every(s => !s.prefix.whitespace.includes("\n"));
            if (isSingleLine) {
                return produce(ret, draft => {
                    const space = this.style.within.objectLiteralTypeBraces ? " " : "";
                    draft.members.statements[0].prefix.whitespace = space;
                    // For type literals, the space before } is in members.end, not in last statement's after
                    draft.members.end.whitespace = space;
                });
            }
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
        if (variable.initializer?.kind == JS.Kind.StatementExpression
            && (variable.initializer as JS.StatementExpression & J.LeftPaddingMixin).statement.kind == J.Kind.MethodDeclaration) {
            return ret;
        }
        return produceAsync(ret, async draft => {
            if (draft.initializer) {
                // Preserve newlines - only modify if no newlines present
                if (!draft.initializer.padding.before.whitespace.includes("\n")) {
                    draft.initializer.padding.before.whitespace = this.style.aroundOperators.assignment ? " " : "";
                }
                if (!draft.initializer.prefix.whitespace.includes("\n")) {
                    draft.initializer.prefix.whitespace = this.style.aroundOperators.assignment ? " " : "";
                }
            }
        });
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, p: P): Promise<J | undefined> {
        const ret = await super.visitWhileLoop(whileLoop, p) as J.WhileLoop;
        return produceAsync(ret, async draft => {
            this.spaceBeforeRightPaddedElementDraft(draft.body, this.style.beforeLeftBrace.whileLeftBrace);
            this.spaceAfterRightPaddedDraft(draft.body, false);
            this.spaceBeforeDraft(draft.condition, this.style.beforeParentheses.whileParentheses);
            this.spaceBeforeRightPaddedElementDraft(draft.condition.tree, this.style.within.whileParentheses);
            this.spaceAfterRightPaddedDraft(draft.condition.tree, this.style.within.whileParentheses);
        });
    }

    protected async visitTypeParameter(typeParam: J.TypeParameter, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeParameter(typeParam, p) as J.TypeParameter;
        return produce(ret, draft => {
            if (draft.bounds && draft.bounds.elements.length >= 2) {
                const constraintType = draft.bounds.elements[0];
                const defaultType = draft.bounds.elements[1];
                const hasConstraint = constraintType.kind !== J.Kind.Empty;
                const hasDefault = defaultType.kind !== J.Kind.Empty;

                if (hasConstraint) {
                    // Space before '=' for default type (in the `after` of constraint type)
                    if (hasDefault && !constraintType.padding.after.whitespace.includes("\n")) {
                        constraintType.padding.after.whitespace = this.style.aroundOperators.assignment ? " " : "";
                    }
                } else if (hasDefault) {
                    // No constraint, just default: space before '=' is in bounds.before
                    if (!draft.bounds.before.whitespace.includes("\n")) {
                        draft.bounds.before.whitespace = this.style.aroundOperators.assignment ? " " : "";
                    }
                }
            }
        });
    }

    /**
     * Modifies the after space of a RightPadded draft in place.
     */
    private spaceAfterRightPaddedDraft<T extends J>(draft: Draft<J.RightPadded<T>>, spaceAfter: boolean): void {
        // Guard against missing padding (can happen with template-generated trees)
        if (!draft.padding?.after) {
            return;
        }
        if (draft.padding.after.comments.length > 0) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            SpacesVisitor.spaceLastCommentSuffixDraft(draft.padding.after.comments, spaceAfter);
            return;
        }

        if (spaceAfter && SpacesVisitor.isNotSingleSpace(draft.padding.after.whitespace)) {
            draft.padding.after.whitespace = " ";
        } else if (!spaceAfter && SpacesVisitor.isOnlySpacesAndNotEmpty(draft.padding.after.whitespace)) {
            draft.padding.after.whitespace = "";
        }
    }

    /**
     * Modifies the before space of a LeftPadded draft and the element's prefix in place.
     */
    private spaceBeforeLeftPaddedElementDraft<T extends J>(draft: Draft<J.LeftPadded<T>>, spaceBeforePadding: boolean, spaceBeforeElement: boolean): void {
        // Guard against missing padding (can happen with template-generated trees)
        if (draft.padding?.before) {
            if (draft.padding.before.comments.length == 0) {
                // Preserve newlines - only modify if no newlines present
                if (!draft.padding.before.whitespace.includes("\n")) {
                    draft.padding.before.whitespace = spaceBeforePadding ? " " : "";
                }
            }
        }
        // With intersection types, the draft IS the element with padding mixed in
        this.spaceBeforeDraft(draft as unknown as Draft<T>, spaceBeforeElement);
    }

    /**
     * Modifies the element's prefix of a RightPadded draft in place.
     */
    private spaceBeforeRightPaddedElementDraft<T extends J>(draft: Draft<J.RightPadded<T>>, spaceBefore: boolean): void {
        // With intersection types, the draft IS the element with padding mixed in
        this.spaceBeforeDraft(draft as unknown as Draft<T>, spaceBefore);
    }

    /**
     * Modifies the prefix whitespace of a J draft in place.
     */
    private spaceBeforeDraft<T extends J>(draft: Draft<T>, spaceBefore: boolean): void {
        // Guard against missing prefix (can happen with template-generated trees)
        if (!draft.prefix) {
            return;
        }
        if (draft.prefix.comments.length > 0) {
            return;
        }
        if (spaceBefore && SpacesVisitor.isNotSingleSpace(draft.prefix.whitespace)) {
            draft.prefix.whitespace = " ";
        } else if (!spaceBefore && SpacesVisitor.isOnlySpacesAndNotEmpty(draft.prefix.whitespace)) {
            draft.prefix.whitespace = "";
        }
    }

    /**
     * Modifies the before space of a Container draft in place.
     */
    private spaceBeforeContainerDraft<T extends J>(draft: Draft<J.Container<T>>, spaceBefore: boolean): void {
        if (draft.before.comments.length > 0) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            SpacesVisitor.spaceLastCommentSuffixDraft(draft.before.comments, spaceBefore);
            return;
        }

        if (spaceBefore && SpacesVisitor.isNotSingleSpace(draft.before.whitespace)) {
            draft.before.whitespace = " ";
        } else if (!spaceBefore && SpacesVisitor.isOnlySpacesAndNotEmpty(draft.before.whitespace)) {
            draft.before.whitespace = "";
        }
    }

    /**
     * Modifies the suffix of the last comment in an array of draft comments in place.
     */
    private static spaceLastCommentSuffixDraft(comments: Draft<Comment>[], spaceSuffix: boolean): void {
        this.spaceSuffixDraft(comments[comments.length - 1], spaceSuffix);
    }

    /**
     * Modifies the suffix of a Comment draft in place.
     */
    private static spaceSuffixDraft(draft: Draft<Comment>, spaceSuffix: boolean): void {
        if (spaceSuffix && this.isNotSingleSpace(draft.suffix)) {
            draft.suffix = " ";
        } else if (!spaceSuffix && this.isOnlySpacesAndNotEmpty(draft.suffix)) {
            draft.suffix = "";
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

    protected async visitNewClass(newClass: J.NewClass, p: P): Promise<J | undefined> {
        const ret = await super.visitNewClass(newClass, p) as J.NewClass;

        // Only handle object literals (NewClass with no class/constructor)
        if (ret.class) {
            return ret;
        }

        // Handle object literal brace spacing: { foo: 1 } vs {foo: 1}
        if (ret.body && ret.body.statements.length > 0) {
            return produce(ret, draft => {
                const stmts = draft.body!.statements;
                // Check if this is a multi-line object literal
                const isMultiLine = stmts.some(s => s.prefix.whitespace.includes("\n")) ||
                    draft.body!.end.whitespace.includes("\n");

                if (!isMultiLine) {
                    // Single-line: apply objectLiteralBraces spacing
                    const space = this.style.within.objectLiteralBraces ? " " : "";
                    stmts[0].prefix.whitespace = space;
                    draft.body!.end.whitespace = space;
                }
            });
        }
        return ret;
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
        const hasBody = e.body.kind === J.Kind.Block || e.body.kind === J.Kind.If;

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
            const parentKind = this.cursor.parent?.value.kind;

            // Check if this is a "simple" block (empty or contains only a single J.Empty)
            const isSimpleBlock = draft.statements.length === 0 ||
                (draft.statements.length === 1 && draft.statements[0].kind === J.Kind.Empty);

            // Helper to format block on one line
            const formatOnOneLine = () => {
                // Format as {} - remove any newlines from end whitespace
                if (draft.end.whitespace.includes("\n")) {
                    draft.end.whitespace = draft.end.whitespace.replace(/\n\s*/g, "");
                }
                // Also remove newlines from statement padding if there's a J.Empty
                if (draft.statements.length === 1) {
                    if (draft.statements[0].prefix.whitespace.includes("\n")) {
                        draft.statements[0].prefix.whitespace = "";
                    }
                    if (draft.statements[0].padding.after.whitespace.includes("\n")) {
                        draft.statements[0].padding.after.whitespace = "";
                    }
                }
            };

            // Object literals and type literals: always format empty ones as {} on single line
            if (parentKind === J.Kind.NewClass || parentKind === JS.Kind.TypeLiteral) {
                if (isSimpleBlock) {
                    formatOnOneLine();
                }
                return;
            }

            if (isSimpleBlock) {
                // Determine which style option applies based on parent
                const isMethodOrFunctionBody = parentKind === J.Kind.Lambda ||
                    parentKind === J.Kind.MethodDeclaration;
                const keepInOneLine = isMethodOrFunctionBody
                    ? this.style.keepWhenReformatting.simpleMethodsInOneLine
                    : this.style.keepWhenReformatting.simpleBlocksInOneLine;

                if (keepInOneLine) {
                    formatOnOneLine();
                } else {
                    // Format with newline between { and }
                    if (!draft.end.whitespace.includes("\n")) {
                        draft.end = this.withNewlineSpace(draft.end);
                    }
                }
            } else {
                // Non-simple blocks: ensure closing brace is on its own line
                if (!draft.end.whitespace.includes("\n") && !draft.statements[draft.statements.length - 1].padding.after.whitespace.includes("\n")) {
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
                this.keepMaximumBlankLines(draft.body.statements[0], 0);

                const isInterface = draft.classKind.type === J.ClassDeclaration.Kind.Type.Interface;
                for (let i = 1; i < statements.length; i++) {
                    const previousElement = statements[i - 1];
                    let currentElement = statements[i];
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
                }
            }

            const cu = this.cursor.firstEnclosing((x: any): x is JS.CompilationUnit => x.kind === JS.Kind.CompilationUnit) as JS.CompilationUnit | undefined;
            const topLevelIndex = cu?.statements.findIndex(s => s.id == draft.id);

            if (topLevelIndex !== undefined) {
                const isImportJustBeforeThis = topLevelIndex > 0 && cu?.statements[topLevelIndex - 1].kind === JS.Kind.Import;

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

                const isFirst = block.statements.length > 0 && block.statements[0].id === draft.id;
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
                      (parent?.kind === JS.Kind.CompilationUnit && (parent! as JS.CompilationUnit).statements[0].id != draft.id) ||
                      (parent?.kind === J.Kind.Case && this.isInsideCaseStatements(parent as J.Case, draft))) {
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

    /**
     * Check if a statement is inside a Case's statements container or body,
     * not inside the caseLabels container.
     */
    private isInsideCaseStatements(caseNode: J.Case, statement: J): boolean {
        // Check if statement is in the statements container
        if (caseNode.statements.elements.some(s => s.id === statement.id)) {
            return true;
        }
        // Check if statement is the body
        if (caseNode.body?.id === statement.id) {
            return true;
        }
        return false;
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

// Re-export prettier formatting utilities
export {prettierFormat} from "./prettier-format";
