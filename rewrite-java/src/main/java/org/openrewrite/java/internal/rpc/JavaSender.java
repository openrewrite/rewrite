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
package org.openrewrite.java.internal.rpc;

import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Space;
import org.openrewrite.rpc.Reference;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;

public class JavaSender extends JavaVisitor<RpcSendQueue> {

    @Override
    public J preVisit(J j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, j2 -> asRef(j2.getPrefix()), space ->
                visitSpace(Reference.getValueNonNull(space), Space.Location.ANY, q));
        q.sendMarkers(j, Tree::getMarkers);
        return j;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, J.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, J.CompilationUnit::getChecksum);
        q.getAndSend(cu, J.CompilationUnit::getFileAttributes);
        q.getAndSend(cu, J.CompilationUnit::getPackageDeclaration, pkg -> visit(pkg, q));
        q.getAndSendList(cu, J.CompilationUnit::getImports, Tree::getId, j -> visit(j, q));
        q.getAndSendList(cu, J.CompilationUnit::getClasses, Tree::getId, j -> visit(j, q));
        q.getAndSend(cu, c -> asRef(c.getEof()), space ->
                visitSpace(Reference.getValueNonNull(space), Space.Location.ANY, q));
        return cu;
    }

    @Override
    public J visitPackage(J.Package pkg, RpcSendQueue q) {
        q.getAndSendList(pkg, J.Package::getAnnotations, Tree::getId, j -> visit(j, q));
        q.getAndSend(pkg, J.Package::getExpression, j -> visit(j, q));
        return pkg;
    }

    @Override
    public J visitImport(J.Import importz, RpcSendQueue q) {
        q.getAndSend(importz, J.Import::isStatic);
        q.getAndSend(importz, J.Import::getQualid, j -> visit(j, q));
        return importz;
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, RpcSendQueue q) {
        q.getAndSendList(classDecl, J.ClassDeclaration::getLeadingAnnotations, Tree::getId, j -> visit(j, q));
        q.getAndSendList(classDecl, J.ClassDeclaration::getModifiers, Tree::getId, j -> visit(j, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getKind);
        q.getAndSend(classDecl, J.ClassDeclaration::getName, j -> visit(j, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getTypeParameters, tp -> visit(tp, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getExtends, ext -> visit(ext, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getImplements, impl -> visit(impl, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getBody, j -> visit(j, q));
        return classDecl;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, RpcSendQueue q) {
        q.getAndSendList(method, J.MethodDeclaration::getLeadingAnnotations, Tree::getId, j -> visit(j, q));
        q.getAndSendList(method, J.MethodDeclaration::getModifiers, Tree::getId, j -> visit(j, q));
        q.getAndSendList(method, J.MethodDeclaration::getTypeParameters, Tree::getId, tp -> visit(tp, q));
        q.getAndSend(method, J.MethodDeclaration::getReturnTypeExpression, j -> visit(j, q));
        q.getAndSend(method, J.MethodDeclaration::getName, j -> visit(j, q));
        q.getAndSendList(method, J.MethodDeclaration::getParameters, Tree::getId, j -> visit(j, q));
        q.getAndSendList(method, J.MethodDeclaration::getThrows, Tree::getId, j -> visit(j, q));
        q.getAndSend(method, J.MethodDeclaration::getBody, body -> visit(body, q));
        q.getAndSend(method, J.MethodDeclaration::getDefaultValue, defaultValue -> visit(defaultValue, q));
        return method;
    }

    @Override
    public J visitBlock(J.Block block, RpcSendQueue q) {
        q.getAndSend(block, J.Block::isStatic);
        q.getAndSendList(block, J.Block::getStatements, Tree::getId, j -> visit(j, q));
        return block;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, RpcSendQueue q) {
        q.getAndSendList(multiVariable, J.VariableDeclarations::getLeadingAnnotations, Tree::getId, j -> visit(j, q));
        q.getAndSendList(multiVariable, J.VariableDeclarations::getModifiers, Tree::getId, j -> visit(j, q));
        q.getAndSend(multiVariable, J.VariableDeclarations::getTypeExpression, j -> visit(j, q));
        q.getAndSendList(multiVariable, J.VariableDeclarations::getVariables, Tree::getId, j -> visit(j, q));
        return multiVariable;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, RpcSendQueue q) {
        q.getAndSend(method, m -> m.getPadding().getTypeParameters(),
                tp -> visitContainer(tp, JContainer.Location.ANY, q));
        q.getAndSend(method, J.MethodInvocation::getSelect, select -> visit(select, q));
        q.getAndSend(method, J.MethodInvocation::getName, j -> visit(j, q));
        q.getAndSendList(method, J.MethodInvocation::getArguments, Tree::getId, j -> visit(j, q));
        return method;
    }

}
