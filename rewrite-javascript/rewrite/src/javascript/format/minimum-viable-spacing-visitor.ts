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
import {JavaScriptVisitor} from "../visitor";
import {J} from "../../java";
import {Cursor, isScope, Tree} from "../../tree";
import {JS} from "../tree";
import {create as produce, Draft} from "mutative";
import {findMarker} from "../../markers";

/**
 * Ensures minimum viable spacing between AST elements.
 * Adds required spaces where they are missing (e.g., after keywords).
 */
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

        // Note: typeParameters should NOT have space before them - they immediately follow the class name
        // e.g., "class DataTable<Row>" not "class DataTable <Row>"
        // Note: body.prefix spacing (space before '{') is handled by SpacesVisitor, not here.

        if (c.extends) {
            c = produce(c, draft => {
                this.ensureSpace(draft.extends!.before);
                this.ensureSpace(draft.extends!.element.prefix);
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
            // a parenthesized class expression separates itself from `new`
            if (draft.class && draft.class.kind !== J.Kind.Parentheses) {
                this.ensureSpace((draft.class as Draft<J>).prefix);
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

        // `catch { }` has a parameter with no variables
        if (!first && ret.variables.length > 0) {
            ret = produce(ret, draft => {
                this.ensureSpace(draft.variables[0].element.prefix);
            });
        }

        return ret;
    }

    protected async visitAlias(alias: JS.Alias, p: P): Promise<J | undefined> {
        const ret = await super.visitAlias(alias, p) as JS.Alias;
        return produce(ret, draft => {
            this.ensureSpace(draft.propertyName.after);
            this.ensureSpace(draft.alias.prefix);
        });
    }

    protected async visitAs(as_: JS.As, p: P): Promise<J | undefined> {
        const ret = await super.visitAs(as_, p) as JS.As;
        return produce(ret, draft => {
            this.ensureSpace(draft.left.after);
            this.ensureSpace(draft.right.prefix);
        });
    }

    protected async visitBinaryExtensions(jsBinary: JS.Binary, p: P): Promise<J | undefined> {
        const ret = await super.visitBinaryExtensions(jsBinary, p) as JS.Binary;
        // `in` is the only word operator here; the rest are punctuators that need no separation
        if (ret.operator.element !== JS.Binary.Type.In) {
            return ret;
        }
        return produce(ret, draft => {
            this.ensureSpace(draft.operator.before);
            this.ensureSpace(draft.right.prefix);
        });
    }

    protected async visitBreak(breakNode: J.Break, p: P): Promise<J | undefined> {
        const ret = await super.visitBreak(breakNode, p) as J.Break;
        return ret.label ? produce(ret, draft => {
            this.ensureSpace(draft.label!.prefix);
        }) : ret;
    }

    protected async visitCase(caseNode: J.Case, p: P): Promise<J | undefined> {
        let c = await super.visitCase(caseNode, p) as J.Case;

        // `default` prints without the `case` keyword, so its label needs no separator
        const first = c.caseLabels.elements[0]?.element;
        if (first && (first.kind !== J.Kind.Identifier || (first as J.Identifier).simpleName !== "default")) {
            c = produce(c, draft => {
                this.ensureSpace(draft.caseLabels.before);
            });
        }

        if (c.guard && c.caseLabels.elements.length > 0 && c.caseLabels.elements[c.caseLabels.elements.length - 1].after.whitespace === "") {
            c = produce(c, draft => {
                const last = draft.caseLabels.elements.length - 1;
                draft.caseLabels.elements[last].after.whitespace = " ";
            });
        }

        return c;
    }

    protected async visitContinue(continueNode: J.Continue, p: P): Promise<J | undefined> {
        const ret = await super.visitContinue(continueNode, p) as J.Continue;
        return ret.label ? produce(ret, draft => {
            this.ensureSpace(draft.label!.prefix);
        }) : ret;
    }

    protected async visitDelete(delete_: JS.Delete, p: P): Promise<J | undefined> {
        const ret = await super.visitDelete(delete_, p) as JS.Delete;
        return produce(ret, draft => {
            this.ensureSpace(draft.expression.prefix);
        });
    }

    protected async visitExportAssignment(exportAssignment: JS.ExportAssignment, p: P): Promise<J | undefined> {
        const ret = await super.visitExportAssignment(exportAssignment, p) as JS.ExportAssignment;
        if (ret.exportEquals) {
            return ret;
        }
        return produce(ret, draft => {
            this.ensureSpace(draft.expression.before);
            this.ensureSpace(draft.expression.element.prefix);
        });
    }

    protected async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, p: P): Promise<J | undefined> {
        const ret = await super.visitExportDeclaration(exportDeclaration, p) as JS.ExportDeclaration;
        return produce(ret, draft => {
            if (draft.typeOnly.element) {
                this.ensureSpace(draft.typeOnly.before);
            }
            // a namespace re-export ends its clause with the alias, unlike `}` or a bare `*`
            if (draft.moduleSpecifier && draft.exportClause?.kind === JS.Kind.Alias) {
                this.ensureSpace(draft.moduleSpecifier.before);
            }
        });
    }

    protected async visitForInLoop(forInLoop: JS.ForInLoop, p: P): Promise<J | undefined> {
        const ret = await super.visitForInLoop(forInLoop, p) as JS.ForInLoop;
        return produce(ret, draft => {
            this.ensureSpace(draft.control.variable.after);
            this.ensureSpace(draft.control.iterable.element.prefix);
        });
    }

    protected async visitForOfLoop(forOfLoop: JS.ForOfLoop, p: P): Promise<J | undefined> {
        const ret = await super.visitForOfLoop(forOfLoop, p) as JS.ForOfLoop;
        return produce(ret, draft => {
            this.ensureSpace(draft.loop.control.variable.after);
            this.ensureSpace(draft.loop.control.iterable.element.prefix);
        });
    }

    protected async visitImportClause(importClause: JS.ImportClause, p: P): Promise<J | undefined> {
        const ret = await super.visitImportClause(importClause, p) as JS.ImportClause;
        return produce(ret, draft => {
            // named bindings open with a punctuator, which separates itself from `import` and `type`
            if (draft.typeOnly || draft.name) {
                this.ensureSpace(draft.prefix);
            }
            if (draft.typeOnly && draft.name) {
                this.ensureSpace(draft.name.element.prefix);
            }
        });
    }

    protected async visitImportDeclaration(jsImport: JS.Import, p: P): Promise<J | undefined> {
        const ret = await super.visitImportDeclaration(jsImport, p) as JS.Import;
        // `from` prints only with a clause, and not at all in the `= require(...)` form; `}` separates
        // named imports from it, and the specifier after it is always a string literal
        const bindings = ret.importClause?.namedBindings;
        if (!ret.importClause || !ret.moduleSpecifier || bindings?.kind === JS.Kind.NamedImports) {
            return ret;
        }
        return produce(ret, draft => {
            this.ensureSpace(draft.moduleSpecifier!.before);
        });
    }

    protected async visitInstanceOf(instanceOf: J.InstanceOf, p: P): Promise<J | undefined> {
        const ret = await super.visitInstanceOf(instanceOf, p) as J.InstanceOf;
        return produce(ret, draft => {
            this.ensureSpace(draft.expression.after);
            this.ensureSpace(draft.class.prefix);
        });
    }

    protected async visitSatisfiesExpression(satisfies: JS.SatisfiesExpression, p: P): Promise<J | undefined> {
        const ret = await super.visitSatisfiesExpression(satisfies, p) as JS.SatisfiesExpression;
        return produce(ret, draft => {
            this.ensureSpace(draft.satisfiesType.before);
            this.ensureSpace(draft.satisfiesType.element.prefix);
        });
    }

    protected async visitScopedVariableDeclarations(scoped: JS.ScopedVariableDeclarations, p: P): Promise<J | undefined> {
        const ret = await super.visitScopedVariableDeclarations(scoped, p) as JS.ScopedVariableDeclarations;
        if (ret.modifiers.length === 0 || ret.variables.length === 0) {
            return ret;
        }
        return produce(ret, draft => {
            for (let i = 1; i < draft.modifiers.length; i++) {
                this.ensureSpace(draft.modifiers[i].prefix);
            }
            this.ensureSpace(draft.variables[0].element.prefix);
        });
    }

    protected async visitTypeOperator(typeOperator: JS.TypeOperator, p: P): Promise<J | undefined> {
        const ret = await super.visitTypeOperator(typeOperator, p) as JS.TypeOperator;
        return produce(ret, draft => {
            this.ensureSpace(draft.expression.before);
        });
    }

    protected async visitTypePredicate(typePredicate: JS.TypePredicate, p: P): Promise<J | undefined> {
        const ret = await super.visitTypePredicate(typePredicate, p) as JS.TypePredicate;
        return produce(ret, draft => {
            if (draft.asserts.element) {
                this.ensureSpace(draft.parameterName.prefix);
            }
            if (draft.expression) {
                this.ensureSpace(draft.expression.before);
                this.ensureSpace(draft.expression.element.prefix);
            }
        });
    }

    protected async visitVoid(void_: JS.Void, p: P): Promise<J | undefined> {
        const ret = await super.visitVoid(void_, p) as JS.Void;
        return produce(ret, draft => {
            this.ensureSpace(draft.expression.prefix);
        });
    }

    protected async visitYield(aYield: J.Yield, p: P): Promise<J | undefined> {
        const ret = await super.visitYield(aYield, p) as J.Yield;
        // `yield*` separates itself from the value
        if (!ret.value || findMarker(ret, JS.Markers.DelegatedYield)) {
            return ret;
        }
        return produce(ret, draft => {
            this.ensureSpace(draft.value!.prefix);
        });
    }

    private ensureSpace(spaceDraft: Draft<J.Space>) {
        if (spaceDraft.whitespace.length === 0 && spaceDraft.comments.length === 0) {
            spaceDraft.whitespace = " ";
        }
    }
}
