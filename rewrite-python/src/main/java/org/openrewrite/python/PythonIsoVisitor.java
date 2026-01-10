/*
 * Copyright 2024 the original author or authors.
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

/*
 * -------------------THIS FILE IS AUTO GENERATED--------------------------
 * Changes to this file may cause incorrect behavior and will be lost if
 * the code is regenerated.
*/

package org.openrewrite.python;

import org.openrewrite.java.tree.J;
import org.openrewrite.python.tree.Py;

public class PythonIsoVisitor<P> extends PythonVisitor<P>
{
    @Override
    public Py.Async visitAsync(Py.Async async, P p) {
        return (Py.Async) super.visitAsync(async, p);
    }

    @Override
    public Py.Await visitAwait(Py.Await await, P p) {
        return (Py.Await) super.visitAwait(await, p);
    }

    @Override
    public Py.Binary visitBinary(Py.Binary binary, P p) {
        return (Py.Binary) super.visitBinary(binary, p);
    }

    @Override
    public Py.ChainedAssignment visitChainedAssignment(Py.ChainedAssignment chainedAssignment, P p) {
        return (Py.ChainedAssignment) super.visitChainedAssignment(chainedAssignment, p);
    }

    @Override
    public Py.ExceptionType visitExceptionType(Py.ExceptionType exceptionType, P p) {
        return (Py.ExceptionType) super.visitExceptionType(exceptionType, p);
    }

    @Override
    public Py.LiteralType visitLiteralType(Py.LiteralType literalType, P p) {
        return (Py.LiteralType) super.visitLiteralType(literalType, p);
    }

    @Override
    public Py.TypeHint visitTypeHint(Py.TypeHint typeHint, P p) {
        return (Py.TypeHint) super.visitTypeHint(typeHint, p);
    }

    @Override
    public Py.CompilationUnit visitCompilationUnit(Py.CompilationUnit compilationUnit, P p) {
        return (Py.CompilationUnit) super.visitCompilationUnit(compilationUnit, p);
    }

    @Override
    public Py.ExpressionStatement visitExpressionStatement(Py.ExpressionStatement expressionStatement, P p) {
        return (Py.ExpressionStatement) super.visitExpressionStatement(expressionStatement, p);
    }

    @Override
    public Py.ExpressionTypeTree visitExpressionTypeTree(Py.ExpressionTypeTree expressionTypeTree, P p) {
        return (Py.ExpressionTypeTree) super.visitExpressionTypeTree(expressionTypeTree, p);
    }

    @Override
    public Py.StatementExpression visitStatementExpression(Py.StatementExpression statementExpression, P p) {
        return (Py.StatementExpression) super.visitStatementExpression(statementExpression, p);
    }

    @Override
    public Py.MultiImport visitMultiImport(Py.MultiImport multiImport, P p) {
        return (Py.MultiImport) super.visitMultiImport(multiImport, p);
    }

    @Override
    public Py.KeyValue visitKeyValue(Py.KeyValue keyValue, P p) {
        return (Py.KeyValue) super.visitKeyValue(keyValue, p);
    }

    @Override
    public Py.DictLiteral visitDictLiteral(Py.DictLiteral dictLiteral, P p) {
        return (Py.DictLiteral) super.visitDictLiteral(dictLiteral, p);
    }

    @Override
    public Py.CollectionLiteral visitCollectionLiteral(Py.CollectionLiteral collectionLiteral, P p) {
        return (Py.CollectionLiteral) super.visitCollectionLiteral(collectionLiteral, p);
    }

    @Override
    public Py.FormattedString visitFormattedString(Py.FormattedString formattedString, P p) {
        return (Py.FormattedString) super.visitFormattedString(formattedString, p);
    }

    @Override
    public Py.FormattedString.Value visitFormattedStringValue(Py.FormattedString.Value value, P p) {
        return (Py.FormattedString.Value) super.visitFormattedStringValue(value, p);
    }

    @Override
    public Py.Pass visitPass(Py.Pass pass, P p) {
        return (Py.Pass) super.visitPass(pass, p);
    }

    @Override
    public Py.TrailingElseWrapper visitTrailingElseWrapper(Py.TrailingElseWrapper trailingElseWrapper, P p) {
        return (Py.TrailingElseWrapper) super.visitTrailingElseWrapper(trailingElseWrapper, p);
    }

    @Override
    public Py.ComprehensionExpression visitComprehensionExpression(Py.ComprehensionExpression comprehensionExpression, P p) {
        return (Py.ComprehensionExpression) super.visitComprehensionExpression(comprehensionExpression, p);
    }

    @Override
    public Py.ComprehensionExpression.Condition visitComprehensionCondition(Py.ComprehensionExpression.Condition condition, P p) {
        return (Py.ComprehensionExpression.Condition) super.visitComprehensionCondition(condition, p);
    }

    @Override
    public Py.ComprehensionExpression.Clause visitComprehensionClause(Py.ComprehensionExpression.Clause clause, P p) {
        return (Py.ComprehensionExpression.Clause) super.visitComprehensionClause(clause, p);
    }

    @Override
    public Py.TypeAlias visitTypeAlias(Py.TypeAlias typeAlias, P p) {
        return (Py.TypeAlias) super.visitTypeAlias(typeAlias, p);
    }

    @Override
    public Py.YieldFrom visitYieldFrom(Py.YieldFrom yieldFrom, P p) {
        return (Py.YieldFrom) super.visitYieldFrom(yieldFrom, p);
    }

    @Override
    public Py.UnionType visitUnionType(Py.UnionType unionType, P p) {
        return (Py.UnionType) super.visitUnionType(unionType, p);
    }

    @Override
    public Py.VariableScope visitVariableScope(Py.VariableScope variableScope, P p) {
        return (Py.VariableScope) super.visitVariableScope(variableScope, p);
    }

    @Override
    public Py.Del visitDel(Py.Del del, P p) {
        return (Py.Del) super.visitDel(del, p);
    }

    @Override
    public Py.SpecialParameter visitSpecialParameter(Py.SpecialParameter specialParameter, P p) {
        return (Py.SpecialParameter) super.visitSpecialParameter(specialParameter, p);
    }

    @Override
    public Py.Star visitStar(Py.Star star, P p) {
        return (Py.Star) super.visitStar(star, p);
    }

    @Override
    public Py.NamedArgument visitNamedArgument(Py.NamedArgument namedArgument, P p) {
        return (Py.NamedArgument) super.visitNamedArgument(namedArgument, p);
    }

    @Override
    public Py.TypeHintedExpression visitTypeHintedExpression(Py.TypeHintedExpression typeHintedExpression, P p) {
        return (Py.TypeHintedExpression) super.visitTypeHintedExpression(typeHintedExpression, p);
    }

    @Override
    public Py.ErrorFrom visitErrorFrom(Py.ErrorFrom errorFrom, P p) {
        return (Py.ErrorFrom) super.visitErrorFrom(errorFrom, p);
    }

    @Override
    public Py.MatchCase visitMatchCase(Py.MatchCase matchCase, P p) {
        return (Py.MatchCase) super.visitMatchCase(matchCase, p);
    }

    @Override
    public Py.MatchCase.Pattern visitMatchCasePattern(Py.MatchCase.Pattern pattern, P p) {
        return (Py.MatchCase.Pattern) super.visitMatchCasePattern(pattern, p);
    }

    @Override
    public Py.Slice visitSlice(Py.Slice slice, P p) {
        return (Py.Slice) super.visitSlice(slice, p);
    }

        @Override
        public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
            return (J.AnnotatedType) super.visitAnnotatedType(annotatedType, p);
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, P p) {
            return (J.Annotation) super.visitAnnotation(annotation, p);
        }

        @Override
        public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
            return (J.ArrayAccess) super.visitArrayAccess(arrayAccess, p);
        }

        @Override
        public J.ArrayType visitArrayType(J.ArrayType arrayType, P p) {
            return (J.ArrayType) super.visitArrayType(arrayType, p);
        }

        @Override
        public J.Assert visitAssert(J.Assert assert_, P p) {
            return (J.Assert) super.visitAssert(assert_, p);
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, P p) {
            return (J.Assignment) super.visitAssignment(assignment, p);
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignmentOperation, P p) {
            return (J.AssignmentOperation) super.visitAssignmentOperation(assignmentOperation, p);
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, P p) {
            return (J.Binary) super.visitBinary(binary, p);
        }

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            return (J.Block) super.visitBlock(block, p);
        }

        @Override
        public J.Break visitBreak(J.Break break_, P p) {
            return (J.Break) super.visitBreak(break_, p);
        }

        @Override
        public J.Case visitCase(J.Case case_, P p) {
            return (J.Case) super.visitCase(case_, p);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, P p) {
            return (J.ClassDeclaration) super.visitClassDeclaration(classDeclaration, p);
        }

        @Override
        public J.Continue visitContinue(J.Continue continue_, P p) {
            return (J.Continue) super.visitContinue(continue_, p);
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
            return (J.DoWhileLoop) super.visitDoWhileLoop(doWhileLoop, p);
        }

        @Override
        public J.Empty visitEmpty(J.Empty empty, P p) {
            return (J.Empty) super.visitEmpty(empty, p);
        }

        @Override
        public J.EnumValue visitEnumValue(J.EnumValue enumValue, P p) {
            return (J.EnumValue) super.visitEnumValue(enumValue, p);
        }

        @Override
        public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enumValueSet, P p) {
            return (J.EnumValueSet) super.visitEnumValueSet(enumValueSet, p);
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
            return (J.FieldAccess) super.visitFieldAccess(fieldAccess, p);
        }

        @Override
        public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop, P p) {
            return (J.ForEachLoop) super.visitForEachLoop(forEachLoop, p);
        }

        @Override
        public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
            return (J.ForEachLoop.Control) super.visitForEachControl(control, p);
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
            return (J.ForLoop) super.visitForLoop(forLoop, p);
        }

        @Override
        public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
            return (J.ForLoop.Control) super.visitForControl(control, p);
        }

        @Override
        public J.ParenthesizedTypeTree visitParenthesizedTypeTree(J.ParenthesizedTypeTree parenthesizedTypeTree, P p) {
            return (J.ParenthesizedTypeTree) super.visitParenthesizedTypeTree(parenthesizedTypeTree, p);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, P p) {
            return (J.Identifier) super.visitIdentifier(identifier, p);
        }

        @Override
        public J.If visitIf(J.If if_, P p) {
            return (J.If) super.visitIf(if_, p);
        }

        @Override
        public J.If.Else visitElse(J.If.Else else_, P p) {
            return (J.If.Else) super.visitElse(else_, p);
        }

        @Override
        public J.Import visitImport(J.Import import_, P p) {
            return (J.Import) super.visitImport(import_, p);
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, P p) {
            return (J.InstanceOf) super.visitInstanceOf(instanceOf, p);
        }

        @Override
        public J.DeconstructionPattern visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, P p) {
            return (J.DeconstructionPattern) super.visitDeconstructionPattern(deconstructionPattern, p);
        }

        @Override
        public J.IntersectionType visitIntersectionType(J.IntersectionType intersectionType, P p) {
            return (J.IntersectionType) super.visitIntersectionType(intersectionType, p);
        }

        @Override
        public J.Label visitLabel(J.Label label, P p) {
            return (J.Label) super.visitLabel(label, p);
        }

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, P p) {
            return (J.Lambda) super.visitLambda(lambda, p);
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, P p) {
            return (J.Literal) super.visitLiteral(literal, p);
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberReference, P p) {
            return (J.MemberReference) super.visitMemberReference(memberReference, p);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDeclaration, P p) {
            return (J.MethodDeclaration) super.visitMethodDeclaration(methodDeclaration, p);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation methodInvocation, P p) {
            return (J.MethodInvocation) super.visitMethodInvocation(methodInvocation, p);
        }

        @Override
        public J.Modifier visitModifier(J.Modifier modifier, P p) {
            return (J.Modifier) super.visitModifier(modifier, p);
        }

        @Override
        public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, P p) {
            return (J.MultiCatch) super.visitMultiCatch(multiCatch, p);
        }

        @Override
        public J.NewArray visitNewArray(J.NewArray newArray, P p) {
            return (J.NewArray) super.visitNewArray(newArray, p);
        }

        @Override
        public J.ArrayDimension visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
            return (J.ArrayDimension) super.visitArrayDimension(arrayDimension, p);
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, P p) {
            return (J.NewClass) super.visitNewClass(newClass, p);
        }

        @Override
        public J.NullableType visitNullableType(J.NullableType nullableType, P p) {
            return (J.NullableType) super.visitNullableType(nullableType, p);
        }

        @Override
        public J.Package visitPackage(J.Package package_, P p) {
            return (J.Package) super.visitPackage(package_, p);
        }

        @Override
        public J.ParameterizedType visitParameterizedType(J.ParameterizedType parameterizedType, P p) {
            return (J.ParameterizedType) super.visitParameterizedType(parameterizedType, p);
        }

        @Override
        public <J2 extends J> J.Parentheses<J2> visitParentheses(J.Parentheses<J2> parentheses, P p) {
            return (J.Parentheses<J2>) super.visitParentheses(parentheses, p);
        }

        @Override
        public <J2 extends J> J.ControlParentheses<J2> visitControlParentheses(J.ControlParentheses<J2> controlParentheses, P p) {
            return (J.ControlParentheses<J2>) super.visitControlParentheses(controlParentheses, p);
        }

        @Override
        public J.Primitive visitPrimitive(J.Primitive primitive, P p) {
            return (J.Primitive) super.visitPrimitive(primitive, p);
        }

        @Override
        public J.Return visitReturn(J.Return return_, P p) {
            return (J.Return) super.visitReturn(return_, p);
        }

        @Override
        public J.Switch visitSwitch(J.Switch switch_, P p) {
            return (J.Switch) super.visitSwitch(switch_, p);
        }

        @Override
        public J.SwitchExpression visitSwitchExpression(J.SwitchExpression switchExpression, P p) {
            return (J.SwitchExpression) super.visitSwitchExpression(switchExpression, p);
        }

        @Override
        public J.Synchronized visitSynchronized(J.Synchronized synchronized_, P p) {
            return (J.Synchronized) super.visitSynchronized(synchronized_, p);
        }

        @Override
        public J.Ternary visitTernary(J.Ternary ternary, P p) {
            return (J.Ternary) super.visitTernary(ternary, p);
        }

        @Override
        public J.Throw visitThrow(J.Throw throw_, P p) {
            return (J.Throw) super.visitThrow(throw_, p);
        }

        @Override
        public J.Try visitTry(J.Try try_, P p) {
            return (J.Try) super.visitTry(try_, p);
        }

        @Override
        public J.Try.Resource visitTryResource(J.Try.Resource resource, P p) {
            return (J.Try.Resource) super.visitTryResource(resource, p);
        }

        @Override
        public J.Try.Catch visitCatch(J.Try.Catch catch_, P p) {
            return (J.Try.Catch) super.visitCatch(catch_, p);
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, P p) {
            return (J.TypeCast) super.visitTypeCast(typeCast, p);
        }

        @Override
        public J.TypeParameter visitTypeParameter(J.TypeParameter typeParameter, P p) {
            return (J.TypeParameter) super.visitTypeParameter(typeParameter, p);
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, P p) {
            return (J.Unary) super.visitUnary(unary, p);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDeclarations, P p) {
            return (J.VariableDeclarations) super.visitVariableDeclarations(variableDeclarations, p);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable namedVariable, P p) {
            return (J.VariableDeclarations.NamedVariable) super.visitVariable(namedVariable, p);
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
            return (J.WhileLoop) super.visitWhileLoop(whileLoop, p);
        }

        @Override
        public J.Wildcard visitWildcard(J.Wildcard wildcard, P p) {
            return (J.Wildcard) super.visitWildcard(wildcard, p);
        }

        @Override
        public J.Yield visitYield(J.Yield yield, P p) {
            return (J.Yield) super.visitYield(yield, p);
        }

        @Override
        public J.Unknown visitUnknown(J.Unknown unknown, P p) {
            return (J.Unknown) super.visitUnknown(unknown, p);
        }

        @Override
        public J.Unknown.Source visitUnknownSource(J.Unknown.Source source, P p) {
            return (J.Unknown.Source) super.visitUnknownSource(source, p);
        }

        @Override
        public J.Erroneous visitErroneous(J.Erroneous erroneous, P p) {
            return (J.Erroneous) super.visitErroneous(erroneous, p);
        }

}
