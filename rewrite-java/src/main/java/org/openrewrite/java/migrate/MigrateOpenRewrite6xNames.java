/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.migrate;


import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;

/**
 * This recipe will migrate the OpenRewrite's 6.x naming conventions to version 7.0.
 */
public class MigrateOpenRewrite6xNames extends Recipe {

    public MigrateOpenRewrite6xNames() {
        //Java Tree Type Changes
        doNext(new ChangeType("org.openrewrite.java.tree.J.Assign", "org.openrewrite.java.tree.J.Assignment"));
        doNext(new ChangeType("org.openrewrite.java.tree.J.AssignOp", "org.openrewrite.java.tree.J.AssignmentOperation"));
        doNext(new ChangeType("org.openrewrite.java.tree.J.ClassDecl", "org.openrewrite.java.tree.J.ClassDeclaration"));
        doNext(new ChangeType("org.openrewrite.java.tree.J.Ident", "org.openrewrite.java.tree.J.Identifier"));
        doNext(new ChangeType("org.openrewrite.java.tree.J.MethodDecl", "org.openrewrite.java.tree.J.MethodDeclaration"));
        doNext(new ChangeType("org.openrewrite.java.tree.J.VariableDecls", "org.openrewrite.java.tree.J.VariableDeclarations"));
        doNext(new ChangeType("org.openrewrite.java.tree.J.VariableDeclarations.NamedVar", "org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable"));

        //Java Tree Type Attribute Changes
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.AnnotatedType getTypeExpr(..)", "getTypeExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.AnnotatedType withTypeExpr(..)", "withTypeExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Annotation getArgs(..)", "getArguments"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Annotation withArgs(..)", "withArguments"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.CompilationUnit getPackageDecl(..)", "getPackageDeclaration"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.CompilationUnit withPackageDecl(..)", "withPackageDeclaration"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Identifier getIdent(..)", "getTypeInformation"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Identifier withIdent(..)", "withTypeInformation"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.InstanceOf getExpr(..)", "getExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.InstanceOf withExpr(..)", "withExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Lambda.Parameters getParams(..)", "getParameters"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Lambda.Parameters withParams(..)", "withParameters"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.MethodDeclaration getReturnTypeExpr(..)", "getReturnTypeExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.MethodDeclaration withReturnTypeExpr(..)", "withReturnTypeExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.MethodDeclaration getParams(..)", "getParameters"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.MethodDeclaration withParams(..)", "withParameters"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.MethodInvocation getArgs(..)", "getArguments"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.MethodInvocation withArgs(..)", "withArguments"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.NewArray getTypeExpr(..)", "getTypeExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.NewArray withTypeExpr(..)", "withTypeExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.NewClass getEncl(..)", "getTypeEnclosing"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.NewClass withEncl(..)", "withTypeEnclosing"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Package getExpr(..)", "getExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Package withExpr(..)", "withExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Return getExpr(..)", "getExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Return withExpr(..)", "withExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Try.Resource getVariableDecls(..)", "getVariableDeclarations"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Try.Resource withVariableDecls(..)", "withVariableDeclarations"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Try.Catch getParam(..)", "getParameter"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.Try.Catch withParam(..)", "withParameter"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.TypeCast getExpr(..)", "getExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.TypeCast withExpr(..)", "withExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.VariableDeclarations getTypeExpr(..)", "getTypeExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.VariableDeclarations withTypeExpr(..)", "withTypeExpression"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.VariableDeclarations getVars(..)", "getVariables"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.J.VariableDeclarations withVars(..)", "withVariables"));

        //Coordinates Type Changes
        doNext(new ChangeType("org.openrewrite.java.tree.Coordinates.Assign", "org.openrewrite.java.tree.Coordinates.Assignment"));
        doNext(new ChangeType("org.openrewrite.java.tree.Coordinates.AssignOp", "org.openrewrite.java.tree.Coordinates.AssignmentOperation"));
        doNext(new ChangeType("org.openrewrite.java.tree.Coordinates.ClassDecl", "org.openrewrite.java.tree.Coordinates.ClassDeclaration"));
        doNext(new ChangeType("org.openrewrite.java.tree.Coordinates.Ident", "org.openrewrite.java.tree.Coordinates.Identifier"));
        doNext(new ChangeType("org.openrewrite.java.tree.Coordinates.MethodDecl", "org.openrewrite.java.tree.Coordinates.MethodDeclaration"));
        doNext(new ChangeType("org.openrewrite.java.tree.Coordinates.VariableDecls", "org.openrewrite.java.tree.Coordinates.VariableDeclarations"));
        doNext(new ChangeType("org.openrewrite.java.tree.Coordinates.VariableDeclarations.NamedVar", "org.openrewrite.java.tree.Coordinates.VariableDeclarations.NamedVariable"));

        //Java Visitor Names
        doNext(new ChangeMethodName("org.openrewrite.java.JavaVisitor visitAssign(..)", "visitAssignment"));
        doNext(new ChangeMethodName("org.openrewrite.java.JavaVisitor visitAssignOp(..)", "visitAssignmentOperation"));
        doNext(new ChangeMethodName("org.openrewrite.java.JavaVisitor visitClassDecl(..)", "visitClassDeclaration"));
        doNext(new ChangeMethodName("org.openrewrite.java.JavaVisitor visitMethod(..)", "visitMethodDeclaration"));
        doNext(new ChangeMethodName("org.openrewrite.java.JavaVisitor visitMultiVariable(..)", "visitVariableDeclarations"));

        //JRightPadded/JLeftPadded/JContainer Element attributes
        doNext(new ChangeMethodName("org.openrewrite.java.tree.JRightPadded getElem(..)", "getElement"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.JRightPadded withElem(..)", "withElement"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.JLeftPadded getElem(..)", "getElement"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.JLeftPadded withElem(..)", "withElement"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.JContainer getElems(..)", "getElements"));
        doNext(new ChangeMethodName("org.openrewrite.java.tree.JContainer withElems(..)", "withElements"));
        //TODO Enum Renames.
        //ASSIGN_OP_PREFIX              > COMPOUND_ASSIGNMENT_PREFIX
        //ASSIGN_OP_OPERATOR            > COMPOUND_ASSIGNMENT_OPERATOR
        //ASSIGN_PREFIX                 > ASSIGNMENT_PREFIX
        //CLASS_DECL_PREFIX             > CLASS_DECLARATION_PREFIX
        //
        //METHOD_DECL_PARAMETERS        > METHOD_DECLARATION_PARAMETERS
        //METHOD_DECL_PARAMETER_SUFFIX  > METHOD_DECLARATION_PARAMETERS_SUFFIX
        //METHOD_DECL_DEFAULT_VALUE     > METHOD_DECLARATION_DEFAULT_VALUE
        //METHOD_DECL_PREFIX            > METHOD_DECLARATION_PREFIX
        //
        //NEW_CLASS_ARGS                > NEW_CLASS_ARGUMENTS
        //NEW_CLASS_ARGS_SUFFIX         > NEW_CLASS_ARGUMENTS_SUFFIX
        //NEW_CLASS_ENCL_SUFFIX         > NEW_CLASS_ENCLOSING_SUFFIX

    }

    @Override
    public String getDisplayName() {
        return "Migrate OpenRewrite 6.x names";
    }

    @Override
    public String getDescription() {
        return "Change types and method names from OpenRewrite 6.x to 7.";
    }
}
