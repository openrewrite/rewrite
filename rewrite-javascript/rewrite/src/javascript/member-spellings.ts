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
import {create as produce, Draft} from "mutative";
import {isIdentifier, J} from "../java";
import {JS, moduleExportName} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {resolve} from "./scope";
import {aliasing, moduleNameOf, respecify} from "./add-import";
import {sameModule, spelledLike} from "./package-name";

/**
 * The spellings of a member that name it through its module rather than through a binding of it,
 * renamed when `maybeRebind` moves the member: `NS.Old` where `NS` is a module object the file binds
 * at module scope (`import * as NS`, `import NS = require`, `const NS = require`), `import("m").Old`,
 * and `export {Old} from "m"`, which keeps the name it publishes as `export {New as Old} from "m"`.
 *
 * A module object still names `from`'s module after the move, so `NS.Old` moves only within one module.
 * An `import()` type spells its module, which moves with it. So does a re-export, where it re-exports
 * nothing else; one that does is left as it stands.
 */
export class RenameMemberSpellings<P> extends JavaScriptVisitor<P> {
    constructor(
        readonly from: {module: string; member: string},
        readonly to: {module: string; member: string},
        /** The names the file binds `from`'s module object under at module scope. */
        readonly moduleObjects: ReadonlySet<string>
    ) {
        super();
    }

    private get withinModule(): boolean {
        return sameModule(this.from.module, this.to.module);
    }

    /** Whether `qualifier`, held by the node at the cursor, reads one of the module objects. */
    private readsModuleObject(qualifier: J | undefined): boolean {
        return this.withinModule && qualifier !== undefined && isIdentifier(qualifier) && this.moduleObjects.has(qualifier.simpleName) &&
            resolve(this.cursor, qualifier)?.kind === JS.Kind.CompilationUnit;
    }

    override async visitFieldAccess(fieldAccess: J.FieldAccess, p: P): Promise<J | undefined> {
        const visited = await super.visitFieldAccess(fieldAccess, p);
        if (visited?.kind !== J.Kind.FieldAccess || fieldAccess.name.element.simpleName !== this.from.member ||
            !this.readsModuleObject(fieldAccess.target)) {
            return visited;
        }
        return produce(visited as J.FieldAccess, draft => {
            draft.name.element.simpleName = this.to.member;
        });
    }

    override async visitMethodInvocation(method: J.MethodInvocation, p: P): Promise<J | undefined> {
        const visited = await super.visitMethodInvocation(method, p);
        if (visited?.kind !== J.Kind.MethodInvocation || method.name.simpleName !== this.from.member ||
            !this.readsModuleObject(method.select?.element)) {
            return visited;
        }
        return produce(visited as J.MethodInvocation, draft => {
            draft.name.simpleName = this.to.member;
        });
    }

    override async visitImportType(importType: JS.ImportType, p: P): Promise<J | undefined> {
        const visited = await super.visitImportType(importType, p);
        if (visited?.kind !== JS.Kind.ImportType) {
            return visited;
        }
        const argument = importType.argumentAndAttributes.elements[0]?.element;
        const literal = argument?.kind === JS.Kind.LiteralType ? (argument as JS.LiteralType).literal : undefined;
        const qualifier = importType.qualifier?.element;
        if (literal?.kind !== J.Kind.Literal || typeof (literal as J.Literal).value !== "string" ||
            !sameModule(moduleNameOf(literal as J.Literal), this.from.module) ||
            qualifier === undefined || !isIdentifier(qualifier) || qualifier.simpleName !== this.from.member) {
            return visited;
        }
        return produce(visited as JS.ImportType, draft => {
            (draft.qualifier!.element as Draft<J.Identifier>).simpleName = this.to.member;
            if (!this.withinModule) {
                const spelled = (draft.argumentAndAttributes.elements[0].element as Draft<JS.LiteralType>).literal as Draft<J.Literal>;
                respecify(spelled, spelledLike(this.to.module, String(spelled.value)));
            }
        });
    }

    override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, p: P): Promise<J | undefined> {
        const visited = await super.visitExportDeclaration(exportDeclaration, p);
        if (visited?.kind !== JS.Kind.ExportDeclaration) {
            return visited;
        }
        const declaration = visited as JS.ExportDeclaration;
        const source = declaration.moduleSpecifier?.element;
        const clause = declaration.exportClause;
        if (source?.kind !== J.Kind.Literal || typeof (source as J.Literal).value !== "string" ||
            !sameModule(moduleNameOf(source as J.Literal), this.from.module) || clause?.kind !== JS.Kind.NamedExports) {
            return visited;
        }
        const specifiers = (clause as JS.NamedExports).elements.elements;
        const moving = specifiers.map(s => this.reExportsMember(s.element));
        // A re-export that moves to another module takes its statement along, so it has to be alone in it
        if (!moving.some(Boolean) || !this.withinModule && specifiers.length !== 1) {
            return visited;
        }
        return produce(declaration, draft => {
            const elements = (draft.exportClause as Draft<JS.NamedExports>).elements.elements;
            elements.forEach((element, i) => {
                if (!moving[i]) {
                    return;
                }
                const specifier = element.element as Draft<JS.ExportSpecifier>;
                if (specifier.specifier.kind === JS.Kind.Alias) {
                    // `export {Old as X}`: only the member it reads changes, X is the module's own name for it
                    const name = (specifier.specifier as Draft<JS.Alias>).propertyName;
                    name.element = {...name.element, kind: J.Kind.Identifier, simpleName: this.to.member} as Draft<J.Identifier>;
                } else {
                    // `export {Old}` keeps publishing `Old`, now as `export {New as Old}`
                    specifier.specifier = aliasing(specifier.specifier as J.Identifier, this.to.member) as Draft<JS.Alias>;
                }
            });
            if (!this.withinModule) {
                const literal = draft.moduleSpecifier!.element as Draft<J.Literal>;
                respecify(literal, spelledLike(this.to.module, String(literal.value)));
            }
        });
    }

    /** Whether an `export {…} from` entry re-exports the member being moved. */
    private reExportsMember(element: J): boolean {
        if (element.kind !== JS.Kind.ExportSpecifier) {
            return false;
        }
        const specifier = (element as JS.ExportSpecifier).specifier;
        const read = specifier.kind === JS.Kind.Alias ? (specifier as JS.Alias).propertyName.element : specifier;
        return (read.kind === J.Kind.Identifier || read.kind === J.Kind.Literal) &&
            moduleExportName(read as J.Identifier | J.Literal) === this.from.member;
    }
}

/** The names `import X = require("m")` declarations bind `module`'s module object under. */
export function importEqualsNames(cu: JS.CompilationUnit, module: string): string[] {
    return cu.statements.flatMap(s => {
        const jsImport = s.element as JS.Import;
        if (jsImport?.kind !== JS.Kind.Import) {
            return [];
        }
        const required = jsImport.initializer?.element as J.MethodInvocation | undefined;
        const argument = required?.kind === J.Kind.MethodInvocation && required.name.simpleName === "require" && !required.select ?
            required.arguments.elements[0]?.element : undefined;
        const name = jsImport.importClause?.name?.element;
        return argument?.kind === J.Kind.Literal && typeof (argument as J.Literal).value === "string" &&
            name !== undefined && isIdentifier(name) &&
            sameModule(moduleNameOf(argument as J.Literal), module) ? [name.simpleName] : [];
    });
}
