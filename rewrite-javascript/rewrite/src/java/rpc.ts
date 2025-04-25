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
import { JavaVisitor } from "./visitor";
import { asRef, RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue } from "../rpc";
import {
    J,
    JavaKind,
    Expression,
    JRightPadded,
    JLeftPadded,
    JContainer,
    Space,
    CompilationUnit,
    Package,
    ClassDeclaration,
    MethodDeclaration,
    Block,
    VariableDeclarations,
    Identifier, isJava, isSpace
} from "./tree";
import { produceAsync } from "../visitor";
import { createDraft, Draft, finishDraft } from "immer";

class JavaSender extends JavaVisitor<RpcSendQueue> {

    protected async preVisit(j: J, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(j, j2 => j2.id);
        await q.getAndSend(j, j2 => asRef(j2.prefix), space => this.visitSpace(space, q));
        await q.sendMarkers(j, j2 => j2.markers);

        return j;
    }

    protected async visitCompilationUnit(cu: CompilationUnit, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(cu, c => c.sourcePath);
        await q.getAndSend(cu, c => c.charsetName);
        await q.getAndSend(cu, c => c.charsetBomMarked);
        await q.getAndSend(cu, c => c.checksum);
        await q.getAndSend(cu, c => c.fileAttributes);
        await q.getAndSend(cu, c => c.packageDeclaration, pkg => this.visitRightPadded(pkg, q));
        await q.getAndSendList(cu, c => c.imports, imp => imp.element.id, imp => this.visitRightPadded(imp, q));
        await q.getAndSendList(cu, c => c.classes, cls => cls.id, cls => this.visit(cls, q));
        await q.getAndSend(cu, c => asRef(c.eof), space => this.visitSpace(space, q));

        return cu;
    }

    protected async visitPackage(pkg: Package, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(pkg, p => p.expression, expr => this.visit(expr, q));
        await q.getAndSendList(pkg, p => p.annotations, annot => annot.id, annot => this.visit(annot, q));

        return pkg;
    }

    protected async visitClassDeclaration(cls: ClassDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(cls, c => c.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(cls, c => c.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(cls, c => c.classKind, kind => this.visit(kind, q));
        await q.getAndSend(cls, c => c.name, name => this.visit(name, q));
        await q.getAndSend(cls, c => c.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(cls, c => c.primaryConstructor, cons => this.visitContainer(cons, q));
        await q.getAndSend(cls, c => c.extends, ext => this.visitLeftPadded(ext, q));
        await q.getAndSend(cls, c => c.implements, impl => this.visitContainer(impl, q));
        await q.getAndSend(cls, c => c.permitting, perm => this.visitContainer(perm, q));
        await q.getAndSend(cls, c => c.body, body => this.visit(body, q));

        return cls;
    }

    protected async visitBlock(block: Block, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(block, b => b.static, s => this.visitRightPadded(s, q));
        await q.getAndSendList(block, b => b.statements, stmt => stmt.element.id, stmt => this.visitRightPadded(stmt, q));
        await q.getAndSend(block, b => asRef(b.end), space => this.visitSpace(space, q));

        return block;
    }

    protected async visitMethodDeclaration(method: MethodDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(method, m => m.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(method, m => m.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(method, m => m.typeParameters, params => this.visit(params, q));
        await q.getAndSend(method, m => m.returnTypeExpression, type => this.visit(type, q));
        await q.getAndSend(method, m => m.name, name => this.visit(name, q));
        await q.getAndSend(method, m => m.parameters, params => this.visitContainer(params, q));
        await q.getAndSend(method, m => m.throws, throws => this.visitContainer(throws, q));
        await q.getAndSend(method, m => m.body, body => this.visit(body, q));
        await q.getAndSend(method, m => m.defaultValue, def => this.visitLeftPadded(def, q));

        return method;
    }

    protected async visitVariableDeclarations(varDecls: VariableDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(varDecls, v => v.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(varDecls, v => v.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(varDecls, v => v.typeExpression, type => this.visit(type, q));
        await q.getAndSend(varDecls, v => v.varargs && asRef(v.varargs), space => this.visitSpace(space, q));
        await q.getAndSendList(varDecls, v => v.variables, variable => variable.element.id, variable => this.visitRightPadded(variable, q));

        return varDecls;
    }

    protected async visitIdentifier(ident: Identifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(ident, id => id.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(ident, id => id.simpleName);

        return ident;
    }

    protected async visitSpace(space: Space, q: RpcSendQueue): Promise<Space> {
        await q.getAndSendList(space, s => s.comments, c => c.text + c.suffix, async c => {
            await q.getAndSend(c, c2 => c2.multiline);
            await q.getAndSend(c, c2 => c2.text);
            await q.getAndSend(c, c2 => c2.suffix);
            await q.sendMarkers(c, c2 => c2.markers);
        });
        await q.getAndSend(space, s => s.whitespace);
        return space;
    }

    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T>, p: RpcSendQueue): Promise<JLeftPadded<T>>;
    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T> | undefined, p: RpcSendQueue): Promise<JLeftPadded<T> | undefined>;
    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T> | undefined, q: RpcSendQueue): Promise<JLeftPadded<T> | undefined> {
        if (!left) {
            return undefined;
        }

        if (isJava(left.element)) {
            await q.getAndSend(left, l => l.element, elem => this.visit(elem as J, q));
        } else if (isSpace(left.element)) {
            await q.getAndSend(left, l => asRef(l.before), space => this.visitSpace(space, q));
        } else {
            await q.getAndSend(left, l => l.element);
        }

        await q.getAndSend(left, l => asRef(l.before), space => this.visitSpace(space, q));
        await q.sendMarkers(left, l => l.markers);

        return left;
    }

    protected async visitRightPadded<T extends J>(right: JRightPadded<T>, q: RpcSendQueue): Promise<JRightPadded<T>>;
    protected async visitRightPadded<T extends boolean>(right: JRightPadded<T>, q: RpcSendQueue): Promise<JRightPadded<T>>;
    protected async visitRightPadded<T extends J | boolean>(right: JRightPadded<T> | undefined, q: RpcSendQueue): Promise<JRightPadded<T> | undefined>;
    protected async visitRightPadded<T extends J | boolean>(right: JRightPadded<T> | undefined, q: RpcSendQueue): Promise<JRightPadded<T> | undefined> {
        if (!right) {
            return undefined;
        }
        await q.getAndSend(right, r => r.element,
            async elem => typeof elem === 'object' && 'kind' in elem
                ? await this.visit(elem as J, q)
                : elem);

        await q.getAndSend(right, r => asRef(r.after), space => this.visitSpace(space, q));
        await q.sendMarkers(right, r => r.markers);

        return right;
    }

    protected async visitContainer<T extends J>(container: JContainer<T>, q: RpcSendQueue): Promise<JContainer<T>> {
        await q.getAndSend(container, c => asRef(c.before), space => this.visitSpace(space, q));
        await q.getAndSendList(container, c => c.elements, elem => elem.element.id, elem => this.visitRightPadded(elem, q));
        await q.sendMarkers(container, c => c.markers);

        return container;
    }
}

class JavaReceiver extends JavaVisitor<RpcReceiveQueue> {

    protected async preVisit(j: J, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(j);

        draft.id = await q.receive(j.id);
        draft.prefix = await q.receive(j.prefix, space => this.visitSpace(space, q));
        draft.markers = await q.receiveMarkers(j.markers);

        return finishDraft(draft);
    }

    protected async visitCompilationUnit(cu: CompilationUnit, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(cu);

        draft.sourcePath = await q.receive(cu.sourcePath);
        draft.charsetName = await q.receive(cu.charsetName);
        draft.charsetBomMarked = await q.receive(cu.charsetBomMarked);
        draft.checksum = await q.receive(cu.checksum);
        draft.fileAttributes = await q.receive(cu.fileAttributes);
        draft.packageDeclaration = await q.receive(cu.packageDeclaration, pkg => this.visitRightPadded(pkg, q));
        draft.imports = await q.receiveListDefined(cu.imports, imp => this.visitRightPadded(imp, q));
        draft.classes = await q.receiveListDefined(cu.classes, cls => this.visit(cls, q));
        draft.eof = await q.receive(cu.eof, space => this.visitSpace(space, q));

        return finishDraft(draft);
    }

    protected async visitPackage(pkg: Package, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(pkg);

        draft.expression = await q.receive<Expression>(pkg.expression, expr => this.visit(expr, q));
        draft.annotations = await q.receiveListDefined(pkg.annotations, annot => this.visit(annot, q));

        return finishDraft(draft);
    }

    protected async visitClassDeclaration(cls: ClassDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(cls);

        draft.leadingAnnotations = await q.receiveListDefined(cls.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(cls.modifiers, mod => this.visit(mod, q));
        draft.classKind = await q.receive(cls.classKind, kind => this.visit(kind, q));
        draft.name = await q.receive(cls.name, name => this.visit(name, q));
        draft.typeParameters = await q.receive(cls.typeParameters, params => this.visitContainer(params, q));
        draft.primaryConstructor = await q.receive(cls.primaryConstructor, cons => this.visitContainer(cons, q));
        draft.extends = await q.receive(cls.extends, ext => this.visitLeftPadded(ext, q));
        draft.implements = await q.receive(cls.implements, impl => this.visitContainer(impl, q));
        draft.permitting = await q.receive(cls.permitting, perm => this.visitContainer(perm, q));
        draft.body = await q.receive(cls.body, body => this.visit(body, q));

        return finishDraft(draft);
    }

    protected async visitBlock(block: Block, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(block);

        draft.static = await q.receive(block.static, s => this.visitRightPadded(s, q));
        draft.statements = await q.receiveListDefined(block.statements, stmt => this.visitRightPadded(stmt, q));
        draft.end = await q.receive(block.end, space => this.visitSpace(space, q));

        return finishDraft(draft);
    }

    protected async visitMethodDeclaration(method: MethodDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(method);

        draft.leadingAnnotations = await q.receiveListDefined(method.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(method.modifiers, mod => this.visit(mod, q));
        draft.typeParameters = await q.receive(method.typeParameters, params => this.visit(params, q));
        draft.returnTypeExpression = await q.receive(method.returnTypeExpression, type => this.visit(type, q));
        draft.name = await q.receive(method.name, name => this.visit(name, q));
        draft.parameters = await q.receive(method.parameters, params => this.visitContainer(params, q));
        draft.throws = await q.receive(method.throws, throws => this.visitContainer(throws, q));
        draft.body = await q.receive(method.body, body => this.visit(body, q));
        draft.defaultValue = await q.receive(method.defaultValue, def => this.visitLeftPadded(def, q));

        return finishDraft(draft);
    }

    protected async visitVariableDeclarations(varDecls: VariableDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(varDecls);

        draft.leadingAnnotations = await q.receiveListDefined(varDecls.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(varDecls.modifiers, mod => this.visit(mod, q));
        draft.typeExpression = await q.receive(varDecls.typeExpression, type => this.visit(type, q));
        draft.varargs = await q.receive(varDecls.varargs, space => this.visitSpace(space, q));
        draft.variables = await q.receiveListDefined(varDecls.variables, variable => this.visitRightPadded(variable, q));

        return finishDraft(draft);
    }

    protected async visitIdentifier(ident: Identifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(ident);

        draft.annotations = await q.receiveListDefined(ident.annotations, annot => this.visit(annot, q));
        draft.simpleName = await q.receive(ident.simpleName);

        return finishDraft(draft);
    }

    protected async visitSpace(space: Space, q: RpcReceiveQueue): Promise<Space> {
        return produceAsync<Space>(space, async draft => {
            draft.comments = await q.receiveListDefined(space.comments, async c => {
                return await produceAsync(c, async draft => {
                    draft.multiline = await q.receive(c.multiline);
                    draft.text = await q.receive(c.text);
                    draft.suffix = await q.receive(c.suffix);
                    draft.markers = await q.receiveMarkers(c.markers);
                });
            });
            draft.whitespace = await q.receive(space.whitespace);
        });
    }

    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T>, q: RpcReceiveQueue): Promise<JLeftPadded<T>> {
        if (!left) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty left padding");
        }

        return produceAsync<JLeftPadded<T>>(left, async draft => {
            // Handle different element types
            if (isJava(left.element)) {
                draft.element = await q.receive(left.element, elem => this.visit(elem, q)) as Draft<T>;
            } else if (isSpace(left.element)) {
                draft.element = await q.receive<Space>(left.element, space => this.visitSpace(space, q)) as Draft<T>;
            } else {
                draft.element = await q.receive(left.element) as Draft<T>;
            }

            draft.before = await q.receive(left.before, space => this.visitSpace(space, q));
            draft.markers = await q.receiveMarkers(left.markers);
        });
    }

    protected async visitRightPadded<T extends J | boolean>(right: JRightPadded<T>, q: RpcReceiveQueue): Promise<JRightPadded<T>> {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty right padding");
        }

        return produceAsync<JRightPadded<T>>(right, async draft => {
            // Handle different element types
            if (isJava(right.element)) {
                draft.element = await q.receive(right.element, elem => this.visit(elem as J, q)) as Draft<T>;
            } else {
                draft.element = await q.receive(right.element) as Draft<T>;
            }

            draft.after = await q.receive(right.after, space => this.visitSpace(space, q));
            draft.markers = await q.receiveMarkers(right.markers);
        });
    }

    protected async visitContainer<T extends J>(container: JContainer<T>, q: RpcReceiveQueue): Promise<JContainer<T>> {
        return produceAsync<JContainer<T>>(container, async draft => {
            draft.before = await q.receive(container.before, space => this.visitSpace(space, q));
            draft.elements = await q.receiveListDefined(container.elements, elem => this.visitRightPadded(elem, q)) as Draft<JRightPadded<T>[]>;
            draft.markers = await q.receiveMarkers(container.markers);
        });
    }
}

const javaCodec: RpcCodec<J> = {
    async rpcReceive(before: J, q: RpcReceiveQueue): Promise<J> {
        return (await new JavaReceiver().visit(before, q))!;
    },

    async rpcSend(after: J, q: RpcSendQueue): Promise<void> {
        await new JavaSender().visit(after, q);
    }
}

// Register codec for all Java AST node types
Object.values(JavaKind).forEach(kind => {
    RpcCodecs.registerCodec(kind, javaCodec);
});
