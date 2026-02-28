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

        if (c.extends && c.extends.padding.before.whitespace === "") {
            c = produce(c, draft => {
                this.ensureSpace(draft.extends!.padding.before);
            });
        }

        if (c.implements && c.implements.before.whitespace === "") {
            c = produce(c, draft => {
                this.ensureSpace(draft.implements!.before);
                if (draft.implements != undefined && draft.implements.elements.length > 0) {
                    // For tree types, the padded value IS the element (intersection type)
                    this.ensureSpace(draft.implements.elements[0].prefix);
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
                draft.keywordType.padding.before.whitespace=" ";
            }
            // For tree types, the padded value IS the element (intersection type)
            this.ensureSpace(draft.name.prefix);
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
                this.ensureSpace(draft.name.padding.before);
            }
            // For tree types, the padded value IS the element (intersection type)
            this.ensureSpace(draft.name.prefix);
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
                // For tree types, the padded value IS the element (intersection type)
                this.ensureSpace(draft.bounds.elements[0].prefix);
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
                // For tree types, the padded value IS the element (intersection type)
                this.ensureSpace(draft.variables[0].prefix);
            });
        }

        return ret;
    }


    protected async visitCase(caseNode: J.Case, p: P): Promise<J | undefined> {
        const c = await super.visitCase(caseNode, p) as J.Case;

        if (c.guard && c.caseLabels.elements.length > 0 && c.caseLabels.elements[c.caseLabels.elements.length - 1].padding.after.whitespace === "") {
            return produce(c, draft => {
                const last = draft.caseLabels.elements.length - 1;
                draft.caseLabels.elements[last].padding.after.whitespace = " ";
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
