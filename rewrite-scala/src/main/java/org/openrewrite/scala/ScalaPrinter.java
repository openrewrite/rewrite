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
package org.openrewrite.scala;

import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Marker;
import org.openrewrite.scala.marker.BlockArgument;
import org.openrewrite.scala.marker.IndentedSyntax;
import org.openrewrite.scala.marker.PackageBraces;
import org.openrewrite.scala.marker.SObject;
import org.openrewrite.scala.marker.Semicolon;
import org.openrewrite.scala.marker.TypeProjection;
import org.openrewrite.scala.marker.ScalaForLoop;
import org.openrewrite.scala.marker.TypeAscription;
import org.openrewrite.scala.marker.PartialFunctionLiteral;
import org.openrewrite.scala.marker.UnderscorePlaceholderLambda;
import org.openrewrite.scala.marker.ValVarKeyword;
import org.openrewrite.scala.marker.PackageSemicolon;
import org.openrewrite.scala.tree.S;

import java.util.List;
import java.util.Optional;

/**
 * ScalaPrinter is responsible for converting the Scala LST back to source code.
 * It extends JavaPrinter to reuse most of the Java printing logic.
 */
public class ScalaPrinter<P> extends JavaPrinter<P> {

    @Override
    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, 
                                  JContainer.Location location, String suffixBetween, 
                                  @Nullable String after, PrintOutputCapture<P> p) {
        if (location == JContainer.Location.TYPE_PARAMETERS) {
            // For type parameters, check if we're being called with explicit brackets
            // If so, use them; otherwise default to Scala-style square brackets
            String openBracket = before.isEmpty() ? "[" : before;
            String closeBracket = (after == null || after.isEmpty()) ? "]" : after;
            
            if (container != null) {
                visitSpace(container.getBefore(), location.getBeforeLocation(), p);
                p.append(openBracket);
                visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
                p.append(closeBracket);
            }
        } else {
            // Delegate to superclass for other container types
            super.visitContainer(before, container, location, suffixBetween, after, p);
        }
    }
    
    @Override
    public J visitTypeParameters(J.TypeParameters typeParams, PrintOutputCapture<P> p) {
        // Use Scala-style square brackets instead of angle brackets
        visitSpace(typeParams.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
        visit(typeParams.getAnnotations(), p);
        p.append('[');
        visitRightPadded(typeParams.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
        p.append(']');
        return typeParams;
    }
    
    @Override
    public J visitTypeParameter(J.TypeParameter typeParam, PrintOutputCapture<P> p) {
        // Print type parameter, but bounds use Scala syntax
        beforeSyntax(typeParam, Space.Location.TYPE_PARAMETERS_PREFIX, p);
        visit(typeParam.getAnnotations(), p);
        visit(typeParam.getName(), p);

        // Print bounds if present using Scala syntax.
        // Each bound element may be a J.TypeBound (with explicit Kind) or a plain
        // TypeTree (context bound, printed with `:`).
        if (typeParam.getPadding().getBounds() != null) {
            List<JRightPadded<TypeTree>> boundElems = typeParam.getPadding().getBounds().getPadding().getElements();
            for (int i = 0; i < boundElems.size(); i++) {
                JRightPadded<TypeTree> elem = boundElems.get(i);
                TypeTree boundTree = elem.getElement();
                if (boundTree instanceof J.TypeBound) {
                    J.TypeBound tb = (J.TypeBound) boundTree;
                    visitSpace(tb.getPrefix(), Space.Location.TYPE_BOUNDS, p);
                    p.append(tb.getKind() == J.TypeBound.Kind.Lower ? ">:" : "<:");
                    visit((J) tb.getBoundedType(), p);
                } else {
                    // Context bound or plain type — use `:`
                    visitSpace(typeParam.getPadding().getBounds().getBefore(), Space.Location.TYPE_BOUNDS, p);
                    p.append(":");
                    visit(boundTree, p);
                }
            }
        }
        
        afterSyntax(typeParam, p);
        return typeParam;
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
        // Skip compiler-synthetic _root_.scala.Predef.??? chain (from procedure syntax desugaring)
        if (isSyntheticPredefChain(fieldAccess)) {
            return fieldAccess;
        }
        // Type projection: Foo#Bar uses # instead of .
        if (fieldAccess.getMarkers().findFirst(TypeProjection.class).isPresent()) {
            beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
            visit(fieldAccess.getTarget(), p);
            p.append('#');
            visit(fieldAccess.getName(), p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }
        // Empty target (import qualids): skip the dot
        if (fieldAccess.getTarget() instanceof J.Empty) {
            beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
            visit(fieldAccess.getName(), p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }
        return super.visitFieldAccess(fieldAccess, p);
    }

    @Override
    public J visitAssignment(J.Assignment assignment, PrintOutputCapture<P> p) {
        beforeSyntax(assignment, Space.Location.ASSIGNMENT_PREFIX, p);
        visit(assignment.getVariable(), p);
        visitLeftPadded("=", assignment.getPadding().getAssignment(), JLeftPadded.Location.ASSIGNMENT, p);
        afterSyntax(assignment, p);
        return assignment;
    }
    
    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (assignOp.getOperator()) {
            case Addition:
                keyword = "+=";
                break;
            case Subtraction:
                keyword = "-=";
                break;
            case Multiplication:
                keyword = "*=";
                break;
            case Division:
                keyword = "/=";
                break;
            case Modulo:
                keyword = "%=";
                break;
            case BitAnd:
                keyword = "&=";
                break;
            case BitOr:
                keyword = "|=";
                break;
            case BitXor:
                keyword = "^=";
                break;
            case LeftShift:
                keyword = "<<=";
                break;
            case RightShift:
                keyword = ">>=";
                break;
            case UnsignedRightShift:
                keyword = ">>>=";
                break;
        }
        beforeSyntax(assignOp, Space.Location.ASSIGNMENT_OPERATION_PREFIX, p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), Space.Location.ASSIGNMENT_OPERATION_OPERATOR, p);
        p.append(keyword);
        visit(assignOp.getAssignment(), p);
        afterSyntax(assignOp, p);
        return assignOp;
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, PrintOutputCapture<P> p) {
        if (!whileLoop.getMarkers().findFirst(IndentedSyntax.class).isPresent()) {
            return super.visitWhileLoop(whileLoop, p);
        }
        // Scala 3 paren-less form: `while cond do body`
        beforeSyntax(whileLoop, Space.Location.WHILE_PREFIX, p);
        p.append("while");
        J.ControlParentheses<Expression> cp = whileLoop.getCondition();
        visitSpace(cp.getPrefix(), Space.Location.WHILE_CONDITION, p);
        JRightPadded<Expression> condPadded = cp.getPadding().getTree();
        visit(condPadded.getElement(), p);
        visitSpace(condPadded.getAfter(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
        p.append("do");
        visit(whileLoop.getBody(), p);
        afterSyntax(whileLoop, p);
        return whileLoop;
    }

    @Override
    public J visitIf(J.If iff, PrintOutputCapture<P> p) {
        if (!iff.getMarkers().findFirst(IndentedSyntax.class).isPresent()) {
            return super.visitIf(iff, p);
        }
        // Scala 3 paren-less form: `if cond then thenp [else elsep]`
        beforeSyntax(iff, Space.Location.IF_PREFIX, p);
        p.append("if");
        J.ControlParentheses<Expression> cp = iff.getIfCondition();
        visitSpace(cp.getPrefix(), Space.Location.IF_PREFIX, p);
        JRightPadded<Expression> condPadded = cp.getPadding().getTree();
        visit(condPadded.getElement(), p);
        visitSpace(condPadded.getAfter(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
        p.append("then");
        visit(iff.getThenPart(), p);
        if (iff.getElsePart() != null) {
            visit(iff.getElsePart(), p);
        }
        afterSyntax(iff, p);
        return iff;
    }

    @Override
    public J visitTry(J.Try tryable, PrintOutputCapture<P> p) {
        beforeSyntax(tryable, Space.Location.TRY_PREFIX, p);
        p.append("try");
        visit(tryable.getBody(), p);
        boolean indentedCatch = tryable.getMarkers().findFirst(IndentedSyntax.class).isPresent();
        if (!tryable.getCatches().isEmpty()) {
            // Print catch block with cases from AST whitespace
            for (int i = 0; i < tryable.getCatches().size(); i++) {
                J.Try.Catch aCatch = tryable.getCatches().get(i);
                if (i == 0) {
                    // First catch — prefix is the space before "catch"
                    visitSpace(aCatch.getPrefix(), Space.Location.CATCH_PREFIX, p);
                    p.append("catch");
                    if (!indentedCatch) {
                        visitSpace(aCatch.getParameter().getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
                        p.append("{");
                    }
                }
                // Print case with AST whitespace
                JRightPadded<J.VariableDeclarations> paramPadded = aCatch.getParameter().getPadding().getTree();
                J.VariableDeclarations varDecl = paramPadded.getElement();
                visitSpace(varDecl.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                p.append("case");
                if (!varDecl.getVariables().isEmpty()) {
                    visit(varDecl.getVariables().get(0).getName(), p);
                }
                if (varDecl.getTypeExpression() != null) {
                    if (varDecl.getVarargs() != null) {
                        visitSpace(varDecl.getVarargs(), Space.Location.VARARGS, p);
                    }
                    p.append(":");
                    visit(varDecl.getTypeExpression(), p);
                }
                visitSpace(paramPadded.getAfter(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
                p.append("=>");
                // Catch case body is a J.Block with OmitBraces — delegate to visitBlock
                // so the block prefix, inter-statement padding (incl. Semicolon markers),
                // and end space are all printed correctly.
                visit(aCatch.getBody(), p);
            }
            if (!indentedCatch) {
                p.append("}");
            }
        }
        if (tryable.getPadding().getFinally() != null) {
            visitSpace(tryable.getPadding().getFinally().getBefore(), Space.Location.TRY_FINALLY, p);
            p.append("finally");
            visit(tryable.getPadding().getFinally().getElement(), p);
        }
        afterSyntax(tryable, p);
        return tryable;
    }

    @Override
    public J visitSwitch(J.Switch switch_, PrintOutputCapture<P> p) {
        beforeSyntax(switch_, Space.Location.SWITCH_PREFIX, p);
        JRightPadded<Expression> selector = switch_.getSelector().getPadding().getTree();
        visit(selector.getElement(), p);
        visitSpace(selector.getAfter(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
        p.append("match");
        boolean indented = switch_.getMarkers().findFirst(IndentedSyntax.class).isPresent();
        if (!indented) {
            visitSpace(switch_.getCases().getPrefix(), Space.Location.BLOCK_PREFIX, p);
            p.append("{");
        }
        // Print cases directly (not via visitBlock which adds { })
        J.Block casesBlock = switch_.getCases();
        List<JRightPadded<Statement>> casePadding = casesBlock.getPadding().getStatements();
        for (int i = 0; i < casePadding.size(); i++) {
            JRightPadded<Statement> rp = casePadding.get(i);
            visit(rp.getElement(), p);
            visitSpace(rp.getAfter(), Space.Location.LANGUAGE_EXTENSION, p);
            // Preserve explicit `;` between cases on the same line.
            if (rp.getMarkers().findFirst(Semicolon.class).isPresent()) {
                p.append(';');
            }
        }
        visitSpace(casesBlock.getEnd(), Space.Location.BLOCK_END, p);
        if (!indented) {
            p.append("}");
        }
        afterSyntax(switch_, p);
        return switch_;
    }

    @Override
    public J visitCase(J.Case case_, PrintOutputCapture<P> p) {
        beforeSyntax(case_, Space.Location.CASE_PREFIX, p);
        p.append("case");
        List<JRightPadded<J>> labelPadding = case_.getPadding().getCaseLabels().getPadding().getElements();
        for (int li = 0; li < labelPadding.size(); li++) {
            JRightPadded<J> lp = labelPadding.get(li);
            visit(lp.getElement(), p);
            // The last label's after space is the space before "if" guard, or
            // the space before "=>" when there is no guard.
            if (li == labelPadding.size() - 1) {
                visitSpace(lp.getAfter(), JRightPadded.Location.CASE.getAfterLocation(), p);
            }
        }
        if (case_.getGuard() != null) {
            p.append("if");
            visit(case_.getGuard(), p);
            visitSpace(case_.getPadding().getStatements().getBefore(), Space.Location.CASE, p);
        }
        p.append("=>");
        if (case_.getPadding().getBody() != null) {
            visit(case_.getPadding().getBody().getElement(), p);
        }
        afterSyntax(case_, p);
        return case_;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
        beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
        visit(method.getLeadingAnnotations(), p);
        boolean defAlreadyPrinted = false;
        String defaultKeyword = "def";
        for (J.Modifier m : method.getModifiers()) {
            if (m.getType() == J.Modifier.Type.LanguageExtension &&
                    ("def".equals(m.getKeyword()) || "given".equals(m.getKeyword()))) {
                visitSpace(m.getPrefix(), Space.Location.MODIFIER_PREFIX, p);
                p.append(m.getKeyword());
                defAlreadyPrinted = true;
            } else {
                visit(m, p);
            }
        }
        if (!defAlreadyPrinted) {
            p.append(defaultKeyword);
        }
        visit(method.getName(), p);

        if (method.getPadding().getTypeParameters() != null) {
            visit(method.getPadding().getTypeParameters(), p);
        }

        // Print parameters — skip parens for parameterless methods (marked with OmitBraces)
        JContainer<Statement> params = method.getPadding().getParameters();
        boolean hasParens = !params.getMarkers().findFirst(
                org.openrewrite.scala.marker.OmitBraces.class).isPresent();
        if (hasParens) {
            visitSpace(params.getBefore(), Space.Location.METHOD_DECLARATION_PARAMETERS, p);
            p.append('(');
        }
        List<JRightPadded<Statement>> paramList = params.getPadding().getElements();
        for (int i = 0; i < paramList.size(); i++) {
            JRightPadded<Statement> param = paramList.get(i);
            Statement element = param.getElement();
            if (element instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecl = (J.VariableDeclarations) element;
                visitSpace(varDecl.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                // Print parameter annotations (@unchecked, etc.)
                visit(varDecl.getLeadingAnnotations(), p);
                // Print parameter modifiers (e.g., implicit, using)
                visit(varDecl.getModifiers(), p);
                boolean omitParamName = !varDecl.getVariables().isEmpty() &&
                    varDecl.getVariables().get(0).getMarkers().findFirst(
                        org.openrewrite.scala.marker.OmitName.class).isPresent();
                if (!omitParamName && !varDecl.getVariables().isEmpty()) {
                    visit(varDecl.getVariables().get(0).getName(), p);
                }
                if (varDecl.getTypeExpression() != null) {
                    // The colon and space between name and type in Scala parameter syntax
                    // Type prefix from parser may or may not include the space
                    TypeTree typeExpr = varDecl.getTypeExpression();
                    if (varDecl.getVarargs() != null) {
                        visitSpace(varDecl.getVarargs(), Space.Location.VARARGS, p);
                    }
                    if (!omitParamName) {
                        p.append(":");
                    }
                    visit(typeExpr, p);
                }
                if (!varDecl.getVariables().isEmpty() &&
                    varDecl.getVariables().get(0).getPadding().getInitializer() != null) {
                    JLeftPadded<Expression> init = varDecl.getVariables().get(0).getPadding().getInitializer();
                    visitLeftPadded("=", init, JLeftPadded.Location.VARIABLE_INITIALIZER, p);
                }
            } else {
                visit(element, p);
            }
            visitSpace(param.getAfter(), JRightPadded.Location.METHOD_DECLARATION_PARAMETER.getAfterLocation(), p);
            if (i < paramList.size() - 1) {
                p.append(',');
            }
        }
        if (hasParens) {
            p.append(')');
        }

        // For curried methods, walk the lambda chain in the body to print additional param lists
        boolean isCurried = method.getMarkers().findFirst(
                org.openrewrite.scala.marker.Curried.class).isPresent();
        J actualBody = null;
        if (isCurried && method.getBody() != null) {
            // Body is an OmitBraces block containing the lambda chain
            J.Block wrapperBlock = method.getBody();
            if (!wrapperBlock.getStatements().isEmpty()) {
                J current = wrapperBlock.getStatements().get(0);
                // Walk the lambda chain, printing each lambda's params as curried param lists
                while (current instanceof J.Lambda) {
                    J.Lambda lambda = (J.Lambda) current;
                    printLambdaParamsAsCurried(lambda.getParameters(), p);
                    boolean lambdaCurried = lambda.getMarkers().findFirst(
                            org.openrewrite.scala.marker.Curried.class).isPresent();
                    if (lambdaCurried && lambda.getBody() instanceof J.Lambda) {
                        current = lambda.getBody();
                    } else {
                        actualBody = lambda.getBody();
                        break;
                    }
                }
            }
        }

        if (method.getReturnTypeExpression() != null) {
            method.getReturnTypeExpression().getMarkers()
                    .findFirst(org.openrewrite.scala.marker.ReturnTypeColonPrefix.class)
                    .ifPresent(m -> visitSpace(m.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
            p.append(':');
            visit(method.getReturnTypeExpression(), p);
        }

        if (isCurried && actualBody != null) {
            // Curried method: body comes from innermost lambda.
            // If body is an empty OmitBraces block, it's an abstract method — skip body printing.
            boolean isAbstract = (actualBody instanceof J.Block) &&
                    ((J.Block) actualBody).getStatements().isEmpty() &&
                    ((J.Block) actualBody).getMarkers().findFirst(
                            org.openrewrite.scala.marker.OmitBraces.class).isPresent();
            if (!isAbstract) {
                boolean procedureSyntax = method.getMarkers().findFirst(
                        org.openrewrite.scala.marker.OmitBraces.class).isPresent();
                if (!procedureSyntax) {
                    final J finalActualBody = actualBody;
                    Space beforeEquals = method.getMarkers()
                            .findFirst(org.openrewrite.scala.marker.MethodBodyEqualsPrefix.class)
                            .map(org.openrewrite.scala.marker.MethodBodyEqualsPrefix::getPrefix)
                            .orElseGet(() -> finalActualBody instanceof J.Block ?
                                    ((J.Block) finalActualBody).getPrefix() : Space.SINGLE_SPACE);
                    visitSpace(beforeEquals, Space.Location.BLOCK_PREFIX, p);
                    p.append("=");
                }
                // If body is OmitBraces block with single statement, print just the statement
                if (actualBody instanceof J.Block) {
                    J.Block bodyBlock = (J.Block) actualBody;
                    boolean omit = bodyBlock.getMarkers().findFirst(
                            org.openrewrite.scala.marker.OmitBraces.class).isPresent();
                    if (omit && bodyBlock.getStatements().size() == 1) {
                        visit(bodyBlock.getStatements().get(0), p);
                    } else {
                        visit(actualBody, p);
                    }
                } else {
                    visit(actualBody, p);
                }
            }
        } else if (method.getBody() != null) {
            // Normal method body
            boolean procedureSyntax = method.getMarkers().findFirst(
                    org.openrewrite.scala.marker.OmitBraces.class).isPresent();
            J.Block body = method.getBody();
            boolean omitBodyBraces = body.getMarkers().findFirst(
                    org.openrewrite.scala.marker.OmitBraces.class).isPresent();
            if (!procedureSyntax) {
                Space beforeEquals = method.getMarkers()
                        .findFirst(org.openrewrite.scala.marker.MethodBodyEqualsPrefix.class)
                        .map(org.openrewrite.scala.marker.MethodBodyEqualsPrefix::getPrefix)
                        .orElse(body.getPrefix());
                visitSpace(beforeEquals, Space.Location.BLOCK_PREFIX, p);
                p.append("=");
            }
            if (omitBodyBraces && body.getStatements().size() == 1) {
                visit(body.getStatements().get(0), p);
            } else {
                visit(body, p);
            }
        }

        afterSyntax(method, p);
        return method;
    }

    @Override
    protected void printStatementTerminator(Statement s, PrintOutputCapture<P> p) {
        // In Scala, semicolons are optional. An explicit ';' found in the source
        // is preserved via a Semicolon marker on the JRightPadded and emitted by
        // visitMarker below. No default terminator is printed here.
        return;
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof Semicolon) {
            p.append(';');
        }
        return super.visitMarker(marker, p);
    }
    
    /**
     * Print a J.Lambda.Parameters as a curried parameter list: (param1, param2)
     */
    private void printLambdaParamsAsCurried(J.Lambda.Parameters lambdaParams, PrintOutputCapture<P> p) {
        if (lambdaParams.isParenthesized()) {
            p.append('(');
        }
        List<JRightPadded<J>> lps = lambdaParams.getPadding().getParameters();
        for (int j = 0; j < lps.size(); j++) {
            JRightPadded<J> lp = lps.get(j);
            J elem = lp.getElement();
            if (elem instanceof J.VariableDeclarations) {
                J.VariableDeclarations vd = (J.VariableDeclarations) elem;
                visitSpace(vd.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                visit(vd.getLeadingAnnotations(), p);
                visit(vd.getModifiers(), p);
                boolean omitName = !vd.getVariables().isEmpty() && vd.getVariables().get(0).getMarkers().findFirst(
                        org.openrewrite.scala.marker.OmitName.class).isPresent();
                if (!omitName && !vd.getVariables().isEmpty()) {
                    visit(vd.getVariables().get(0).getName(), p);
                }
                if (vd.getTypeExpression() != null) {
                    TypeTree te = vd.getTypeExpression();
                    if (vd.getVarargs() != null) {
                        visitSpace(vd.getVarargs(), Space.Location.VARARGS, p);
                    }
                    if (!omitName) {
                        p.append(":");
                    }
                    visit(te, p);
                }
            } else {
                visit(elem, p);
            }
            visitSpace(lp.getAfter(), JRightPadded.Location.METHOD_DECLARATION_PARAMETER.getAfterLocation(), p);
            if (j < lps.size() - 1) {
                p.append(',');
            }
        }
        if (lambdaParams.isParenthesized()) {
            p.append(')');
        }
    }

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (tree instanceof S.CompilationUnit) {
            return visitScalaCompilationUnit((S.CompilationUnit) tree, p);
        } else if (tree instanceof S.Wildcard) {
            return visitWildcard((S.Wildcard) tree, p);
        } else if (tree instanceof S.TuplePattern) {
            return visitTuplePattern((S.TuplePattern) tree, p);
        } else if (tree instanceof S.StatementExpression) {
            // Transparent — visit the inner statement
            return visit(((S.StatementExpression) tree).getStatement(), p);
        } else if (tree instanceof S.ExpressionStatement) {
            // Transparent — visit the inner expression
            return visit(((S.ExpressionStatement) tree).getExpression(), p);
        } else if (tree instanceof S.TypeAscription) {
            return visitTypeAscription((S.TypeAscription) tree, p);
        } else if (tree instanceof S.TypeAlias) {
            return visitTypeAlias((S.TypeAlias) tree, p);
        } else if (tree instanceof S.Export) {
            return visitExport((S.Export) tree, p);
        } else if (tree instanceof S.PatternDefinition) {
            return visitPatternDefinition((S.PatternDefinition) tree, p);
        } else if (tree instanceof S.AnonymousGiven) {
            return visitAnonymousGiven((S.AnonymousGiven) tree, p);
        } else if (tree instanceof S.FunctionCall) {
            return visitFunctionCall((S.FunctionCall) tree, p);
        } else if (tree instanceof S.SingletonType) {
            return visitSingletonType((S.SingletonType) tree, p);
        } else if (tree instanceof S.RepeatedType) {
            return visitRepeatedType((S.RepeatedType) tree, p);
        } else if (tree instanceof S.SplatExpression) {
            return visitSplatExpression((S.SplatExpression) tree, p);
        } else if (tree instanceof S.XmlLiteral) {
            return visitXmlLiteral((S.XmlLiteral) tree, p);
        } else if (tree instanceof S.Alternative) {
            return visitAlternative((S.Alternative) tree, p);
        } else if (tree instanceof S.QualifiedSuper) {
            return visitQualifiedSuper((S.QualifiedSuper) tree, p);
        } else if (tree instanceof S.AnnotatedExpression) {
            return visitAnnotatedExpression((S.AnnotatedExpression) tree, p);
        } else if (tree instanceof S.RefinedType) {
            return visitRefinedType((S.RefinedType) tree, p);
        } else if (tree instanceof S.FunctionType) {
            return visitFunctionType((S.FunctionType) tree, p);
        } else if (tree instanceof S.TupleType) {
            return visitTupleType((S.TupleType) tree, p);
        } else if (tree instanceof S.Macro) {
            return visitMacro((S.Macro) tree, p);
        } else if (tree instanceof S.ExtensionMethods) {
            return visitExtensionMethods((S.ExtensionMethods) tree, p);
        } else if (tree instanceof S.For) {
            return visitFor((S.For) tree, p);
        } else if (tree instanceof S.For.Enumerator) {
            return visitForEnumerator((S.For.Enumerator) tree, p);
        }
        return super.visit(tree, p);
    }
    
    public J visitScalaCompilationUnit(S.CompilationUnit scu, PrintOutputCapture<P> p) {
        beforeSyntax(scu, Space.Location.COMPILATION_UNIT_PREFIX, p);

        if (scu.getPackageDeclaration() != null) {
            visit(scu.getPackageDeclaration(), p);
            boolean packageEndsWithSemicolon = scu.getPackageDeclaration().getMarkers().findFirst(PackageSemicolon.class).isPresent();
            // In Scala, package declarations are followed by a newline
            // Check if the next element has a newline in its prefix, if not add one
            if (!packageEndsWithSemicolon && !scu.getImports().isEmpty()) {
                J.Import firstImport = scu.getImports().get(0);
                String firstImportPrefix = firstImport.getPrefix().getWhitespace();
                if (!firstImportPrefix.startsWith("\n") && !firstImportPrefix.startsWith(";")) {
                    p.append("\n");
                }
            } else if (!packageEndsWithSemicolon && !scu.getStatements().isEmpty()) {
                Statement firstStatement = scu.getStatements().get(0);
                String firstStatementPrefix = firstStatement.getPrefix().getWhitespace();
                if (!firstStatementPrefix.startsWith("\n") && !firstStatementPrefix.startsWith(";")) {
                    p.append("\n");
                }
            }
        }

        for (J.Import anImport : scu.getImports()) {
            visit(anImport, p);
            // Scala imports don't end with semicolons but need newlines between them
            if (!anImport.getPrefix().getWhitespace().isEmpty() || scu.getImports().indexOf(anImport) < scu.getImports().size() - 1) {
                // Already has whitespace or not the last import
            }
        }

        for (int i = 0; i < scu.getStatements().size(); i++) {
            Statement statement = scu.getStatements().get(i);
            visit(statement, p);
        }

        if (scu.getPackageDeclaration() != null) {
            Optional<PackageBraces> braces = scu.getPackageDeclaration().getMarkers().findFirst(PackageBraces.class);
            if (braces.isPresent()) {
                p.append(braces.get().afterBody());
                p.append('}');
            }
        }

        visitSpace(scu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        afterSyntax(scu, p);
        return scu;
    }

    @Override
    public J visitPackage(J.Package pkg, PrintOutputCapture<P> p) {
        beforeSyntax(pkg, Space.Location.PACKAGE_PREFIX, p);
        p.append("package");
        visit(pkg.getExpression(), p);
        if (pkg.getMarkers().findFirst(IndentedSyntax.class).isPresent()) {
            p.append(':');
        }
        Optional<PackageBraces> braces = pkg.getMarkers().findFirst(PackageBraces.class);
        if (braces.isPresent()) {
            p.append(braces.get().beforeBrace());
            p.append('{');
        }
        if (pkg.getMarkers().findFirst(PackageSemicolon.class).isPresent()) {
            p.append(';');
        }
        // Note: No semicolon in Scala package declarations
        afterSyntax(pkg, p);
        return pkg;
    }
    
    @Override
    public J visitImport(J.Import import_, PrintOutputCapture<P> p) {
        beforeSyntax(import_, Space.Location.IMPORT_PREFIX, p);
        p.append("import ");
        
        // Visit the import expression
        // Wildcard imports: Scala 2 uses `._`, Scala 3 uses `.*`
        // The name field preserves which was used in source.
        J.FieldAccess qualid = import_.getQualid();
        if (isWildcardImport(qualid)) {
            visitFieldAccessUpToWildcard(qualid, p);
            // Preserve the original wildcard syntax from the name
            String wildcardName = qualid.getName().getSimpleName();
            p.append("." + wildcardName);
        } else {
            visit(qualid, p);
        }

        afterSyntax(import_, p);
        return import_;
    }
    
    private boolean isSyntheticPredefChain(J.FieldAccess fa) {
        // Detect _root_.scala.Predef.??? chains added by compiler for procedure syntax
        if ("???".equals(fa.getSimpleName()) || "$qmark$qmark$qmark".equals(fa.getSimpleName())) {
            return true;
        }
        if (fa.getTarget() instanceof J.FieldAccess) {
            return isSyntheticPredefChain((J.FieldAccess) fa.getTarget());
        }
        if (fa.getTarget() instanceof J.Identifier) {
            return "_root_".equals(((J.Identifier) fa.getTarget()).getSimpleName());
        }
        return false;
    }

    private boolean isWildcardImport(J.FieldAccess qualid) {
        J.Identifier name = qualid.getName();
        String n = name.getSimpleName();
        // Scala 2 (`._`), Scala 3 (`.*`), and the Scala 3 given form (`.given`) are all
        // wildcard-style selectors. Printing routes through the wildcard path so the
        // selector name (preserved verbatim by the parser) is appended as-is.
        return "*".equals(n) || "_".equals(n) || "given".equals(n);
    }
    
    private void visitFieldAccessUpToWildcard(J.FieldAccess qualid, PrintOutputCapture<P> p) {
        // Visit the target part (everything before the wildcard)
        visit(qualid.getTarget(), p);
    }

    @Override  
    public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
        // Check if this is a Scala object declaration
        boolean isObject = classDecl.getMarkers().findFirst(SObject.class).isPresent();
        
        // For Scala classes, we need special handling for extends/with clauses
        // Use custom handling only if this is actually a Scala class
        boolean needsScalaHandling = isObject;
        
        // Check if this is a trait (Interface kind in Scala)
        if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
            needsScalaHandling = true;
        }
        
        // Check if we have Scala-style "with" clauses
        if (classDecl.getImplements() != null && !classDecl.getImplements().isEmpty()) {
            needsScalaHandling = true;
        }
        
        // Or if we have a primary constructor at all — Scala distinguishes
        // `class Foo`, `class Foo()`, and `class Foo(x)` via container presence,
        // emptiness, and an OmitParentheses marker on the container.
        if (classDecl.getPadding().getPrimaryConstructor() != null) {
            needsScalaHandling = true;
        }
        
        // Or if we have type parameters (to ensure square brackets in Scala)
        if (classDecl.getPadding().getTypeParameters() != null &&
            !classDecl.getPadding().getTypeParameters().getElements().isEmpty()) {
            needsScalaHandling = true;
        }
        
        if (needsScalaHandling) {
            // Custom handling for Scala classes
            beforeSyntax(classDecl, Space.Location.CLASS_DECLARATION_PREFIX, p);
            visit(classDecl.getLeadingAnnotations(), p);
            
            // For objects, skip the final modifier (it's implicit)
            for (J.Modifier m : classDecl.getModifiers()) {
                if (!(isObject && m.getType() == J.Modifier.Type.Final)) {
                    visit(m, p);
                }
            }
            
            visit(classDecl.getPadding().getKind().getAnnotations(), p);
            visitSpace(classDecl.getPadding().getKind().getPrefix(), Space.Location.CLASS_KIND, p);
            
            // Print the appropriate keyword
            String kind = "";
            if (isObject && classDecl.getKind() == J.ClassDeclaration.Kind.Type.Enum) {
                // Enum case: `case Red extends Color`
                kind = "case";
            } else if (isObject) {
                kind = "object";
            } else {
                switch (classDecl.getKind()) {
                    case Class:
                        kind = "class";
                        break;
                    case Enum:
                        kind = "enum";
                        break;
                    case Interface:
                        kind = "trait";  // Scala uses trait, not interface
                        break;
                    case Annotation:
                        kind = "@interface";
                        break;
                    case Record:
                        kind = "record";
                        break;
                }
            }
            p.append(kind);

            visit(classDecl.getName(), p);
            visitTypeParameters(classDecl.getPadding().getTypeParameters(), p);
            
            // Print primaryConstructor with parens and comma separators. Each element is a
            // J.VariableDeclarations modeled like a Scala parameter (no implicit val/var,
            // type comes after the name with `:`). We can't fall through to visitVariableDeclarations
            // because that one is for field/local declarations and always emits a val/var keyword.
            if (classDecl.getPadding().getPrimaryConstructor() != null &&
                !classDecl.getPadding().getPrimaryConstructor().getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                JContainer<Statement> primaryConstructor = classDecl.getPadding().getPrimaryConstructor();
                visitSpace(primaryConstructor.getBefore(), Space.Location.RECORD_STATE_VECTOR, p);
                p.append('(');
                List<JRightPadded<Statement>> ctorElements = primaryConstructor.getPadding().getElements();
                for (int i = 0; i < ctorElements.size(); i++) {
                    JRightPadded<Statement> rp = ctorElements.get(i);
                    Statement element = rp.getElement();
                    if (element instanceof J.VariableDeclarations) {
                        J.VariableDeclarations varDecl = (J.VariableDeclarations) element;
                        visitSpace(varDecl.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                        visit(varDecl.getLeadingAnnotations(), p);
                        // Print modifiers as-is: includes `val`/`var`/`private`/etc. when present
                        // on a class constructor param; absent for plain `(name: T)` form.
                        for (J.Modifier m : varDecl.getModifiers()) {
                            visit(m, p);
                        }
                        if (!varDecl.getVariables().isEmpty()) {
                            visit(varDecl.getVariables().get(0).getName(), p);
                        }
                        if (varDecl.getTypeExpression() != null) {
                            TypeTree typeExpr = varDecl.getTypeExpression();
                            if (varDecl.getVarargs() != null) {
                                visitSpace(varDecl.getVarargs(), Space.Location.VARARGS, p);
                            }
                            p.append(":");
                            visit(typeExpr, p);
                        }
                        if (!varDecl.getVariables().isEmpty() &&
                            varDecl.getVariables().get(0).getPadding().getInitializer() != null) {
                            JLeftPadded<Expression> init = varDecl.getVariables().get(0).getPadding().getInitializer();
                            visitLeftPadded("=", init, JLeftPadded.Location.VARIABLE_INITIALIZER, p);
                        }
                    } else {
                        visit(element, p);
                    }
                    visitSpace(rp.getAfter(), Space.Location.RECORD_STATE_VECTOR_SUFFIX, p);
                    if (i < ctorElements.size() - 1) {
                        p.append(',');
                    }
                }
                p.append(')');
                // Re-emit any additional curried constructor param lists captured verbatim
                // from source (e.g. `(using Executor)`).
                primaryConstructor.getMarkers()
                    .findFirst(org.openrewrite.scala.marker.ExtraConstructorParamLists.class)
                    .ifPresent(m -> p.append(m.text()));
            }

            if (classDecl.getPadding().getExtends() != null) {
                visitSpace(classDecl.getPadding().getExtends().getBefore(), Space.Location.EXTENDS, p);
                p.append("extends");
                visit(classDecl.getPadding().getExtends().getElement(), p);
            }

            if (classDecl.getPadding().getImplements() != null) {
                // In Scala, implements are printed with "with" keyword
                // The container already has the proper space before the first keyword
                
                String firstKeyword = "";
                String separator = "";
                
                if (classDecl.getPadding().getExtends() != null) {
                    // If we have extends, traits use "with"
                    firstKeyword = "with";
                    separator = "with";
                } else {
                    // If no extends, first trait uses "extends"
                    firstKeyword = "extends";
                    separator = "with";
                }
                
                // Custom handling for Scala traits
                JContainer<TypeTree> implContainer = classDecl.getPadding().getImplements();
                visitSpace(implContainer.getBefore(), Space.Location.IMPLEMENTS, p);
                p.append(firstKeyword);
                
                List<JRightPadded<TypeTree>> elements = implContainer.getPadding().getElements();
                for (int i = 0; i < elements.size(); i++) {
                    JRightPadded<TypeTree> elem = elements.get(i);
                    visit(elem.getElement(), p);
                    
                    if (i < elements.size() - 1) {
                        // Print space after element and the separator
                        visitSpace(elem.getAfter(), Space.Location.IMPLEMENTS_SUFFIX, p);
                        p.append(separator);
                    }
                }
            }

            if (classDecl.getPadding().getPermits() != null) {
                visitContainer(" permits", classDecl.getPadding().getPermits(), JContainer.Location.PERMITS, ",", "", p);
            }

            visit(classDecl.getBody(), p);
            afterSyntax(classDecl, p);
            return classDecl;
        } else {
            // No Scala-specific structure (no primary constructor, no object/trait/with/type-params).
            // Fall through to default Java printing.
            return super.visitClassDeclaration(classDecl, p);
        }
    }
    
    private void visitTypeParameters(@Nullable JContainer<J.TypeParameter> typeParams, PrintOutputCapture<P> p) {
        if (typeParams != null && !typeParams.getElements().isEmpty()) {
            // In Scala, type parameters use square brackets, not angle brackets
            visitSpace(typeParams.getBefore(), Space.Location.TYPE_PARAMETERS, p);
            p.append('[');
            List<JRightPadded<J.TypeParameter>> elements = typeParams.getPadding().getElements();
            for (int i = 0; i < elements.size(); i++) {
                visit(elements.get(i).getElement(), p);
                visitSpace(elements.get(i).getAfter(), Space.Location.TYPE_PARAMETER_SUFFIX, p);
                if (i < elements.size() - 1) {
                    p.append(',');
                }
            }
            p.append(']');
        }
    }

    @Override
    public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
        // OmitBraces blocks print statements without { } — used for braceless bodies,
        // synthetic lambda body blocks, and expression-position blocks
        if (block.getMarkers().findFirst(org.openrewrite.scala.marker.OmitBraces.class).isPresent()) {
            beforeSyntax(block, Space.Location.BLOCK_PREFIX, p);
            visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
            visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            afterSyntax(block, p);
            return block;
        }
        // Scala 3 braceless (indentation-based) blocks use `:` instead of `{}`
        if (block.getMarkers().findFirst(IndentedSyntax.class).isPresent()) {
            beforeSyntax(block, Space.Location.BLOCK_PREFIX, p);
            p.append(':');
            visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
            visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            afterSyntax(block, p);
            return block;
        }
        return super.visitBlock(block, p);
    }
    
    @Override
    public J visitReturn(J.Return return_, PrintOutputCapture<P> p) {
        // Check if this is an implicit return (last expression in a block)
        if (return_.getMarkers().findFirst(ImplicitReturn.class).isPresent()) {
            // Print only the expression, not the return keyword
            beforeSyntax(return_, Space.Location.RETURN_PREFIX, p);
            visit(return_.getExpression(), p);
            afterSyntax(return_, p);
            return return_;
        }
        // Otherwise use the default Java printing
        return super.visitReturn(return_, p);
    }
    
    @Override
    public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
        if (forEachLoop.getMarkers().findFirst(ScalaForLoop.class).isPresent()) {
            // Scala for-comprehension: for (x <- iterable) body
            beforeSyntax(forEachLoop, Space.Location.FOR_EACH_LOOP_PREFIX, p);
            p.append("for");
            J.ForEachLoop.Control ctrl = forEachLoop.getControl();
            visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
            p.append('(');

            // Print the variable (just the name, no type)
            JRightPadded<Statement> variable = ctrl.getPadding().getVariable();
            Statement varStmt = variable.getElement();
            if (varStmt instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecl = (J.VariableDeclarations) varStmt;
                visitSpace(varDecl.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                if (!varDecl.getVariables().isEmpty()) {
                    visit(varDecl.getVariables().get(0).getName(), p);
                }
            } else {
                visit(varStmt, p);
            }

            // Print "<-" with spaces from the padding
            visitSpace(variable.getAfter(), JRightPadded.Location.FOREACH_VARIABLE.getAfterLocation(), p);
            p.append("<-");

            // Print the iterable
            JRightPadded<Expression> iterable = ctrl.getPadding().getIterable();
            visit(iterable.getElement(), p);
            visitSpace(iterable.getAfter(), JRightPadded.Location.FOREACH_ITERABLE.getAfterLocation(), p);
            p.append(')');

            // Print the body
            visitStatement(forEachLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
            afterSyntax(forEachLoop, p);
            return forEachLoop;
        }
        return super.visitForEachLoop(forEachLoop, p);
    }

    public J visitTypeAscription(S.TypeAscription typeAscription, PrintOutputCapture<P> p) {
        beforeSyntax(typeAscription, Space.Location.LANGUAGE_EXTENSION, p);
        visit(typeAscription.getExpression(), p);
        typeAscription.getMarkers()
                .findFirst(org.openrewrite.scala.marker.TypeAscriptionColonPrefix.class)
                .ifPresent(m -> visitSpace(m.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        p.append(':');
        visit(typeAscription.getTypeTree(), p);
        afterSyntax(typeAscription, p);
        return typeAscription;
    }

    public J visitFunctionCall(S.FunctionCall fc, PrintOutputCapture<P> p) {
        beforeSyntax(fc, Space.Location.LANGUAGE_EXTENSION, p);
        visitRightPadded(fc.getPadding().getFunction(), JRightPadded.Location.LANGUAGE_EXTENSION, "", p);
        if (fc.getMarkers().findFirst(BlockArgument.class).isPresent()) {
            // Trailing block argument: `foo(1) { ... }`. The block prints its own braces,
            // so we skip the parentheses and delimiters entirely.
            for (Expression arg : fc.getArguments()) {
                visit(arg, p);
            }
        } else if (fc.getMarkers().findFirst(IndentedSyntax.class).isPresent()) {
            // Colon-indented arg list: `foo(x):\n  ...`. The arg's prefix carries the indent.
            p.append(':');
            for (Expression arg : fc.getArguments()) {
                visit(arg, p);
            }
        } else {
            visitContainer("(", fc.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
        }
        afterSyntax(fc, p);
        return fc;
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
        // asInstanceOf handling
        beforeSyntax(typeCast, Space.Location.TYPE_CAST_PREFIX, p);
        visit(typeCast.getExpression(), p);
        p.append(".asInstanceOf");
        if (typeCast.getClazz() instanceof J.ControlParentheses) {
            J.ControlParentheses<?> controlParens = (J.ControlParentheses<?>) typeCast.getClazz();
            visitSpace(controlParens.getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
            p.append('[');
            visitRightPadded(controlParens.getPadding().getTree(), JRightPadded.Location.PARENTHESES, "", p);
            p.append(']');
        }
        afterSyntax(typeCast, p);
        return typeCast;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
        visit(multiVariable.getLeadingAnnotations(), p);

        // Check if this is a lambda parameter - if so, don't print val/var
        boolean isLambdaParam = multiVariable.getMarkers().findFirst(
            org.openrewrite.scala.marker.LambdaParameter.class).isPresent();

        // Print modifiers, but handle Final specially since Scala uses val/var.
        // The implicit Final modifier (keyword=null) marks val (or given, when a Given marker
        // is present on the declaration). Its prefix carries the source whitespace between the
        // last visible modifier (or annotations) and the val/given keyword.
        // An explicit "final" modifier prints normally.
        boolean isGiven = multiVariable.getMarkers().findFirst(
            org.openrewrite.scala.marker.Given.class).isPresent();
        boolean hasVisibleModifier = false;
        String valVarKeyword = isGiven ? "given" : "var";
        Space valVarPrefix = null;
        Optional<ValVarKeyword> valVarKeywordMarker = multiVariable.getMarkers().findFirst(ValVarKeyword.class);
        boolean annotationGapBridged = false;
        for (J.Modifier m : multiVariable.getModifiers()) {
            if (m.getType() == J.Modifier.Type.Final && !"final".equals(m.getKeyword())) {
                // Implicit Final marking val/given — capture prefix, don't visit.
                if (!isGiven) {
                    valVarKeyword = "val";
                }
                valVarPrefix = m.getPrefix();
            } else {
                if (m.getType() == J.Modifier.Type.Final && "final".equals(m.getKeyword())) {
                    // Explicit "final" keyword still implies val (Scala val is implicitly final).
                    valVarKeyword = "val";
                }
                if (!hasVisibleModifier && !multiVariable.getLeadingAnnotations().isEmpty() && m.getPrefix().isEmpty()) {
                    p.append(" ");
                    annotationGapBridged = true;
                }
                visit(m, p);
                hasVisibleModifier = true;
            }
        }

        // Print val/var/given (unless it's a lambda parameter)
        if (!isLambdaParam) {
            if (valVarPrefix != null) {
                visitSpace(valVarPrefix, Space.Location.MODIFIER_PREFIX, p);
            } else if (valVarKeywordMarker.isPresent()) {
                p.append(valVarKeywordMarker.get().beforeKeyword());
            } else if (hasVisibleModifier) {
                p.append(" ");
            } else if (!annotationGapBridged && !multiVariable.getLeadingAnnotations().isEmpty()) {
                p.append(" ");
            }
            p.append(valVarKeyword);
        }
        
        // In Scala, variable declarations don't have a type at the declaration level
        // Each variable has its own type annotation
        
        // Visit each variable (the variable's prefix already contains the space)
        visitRightPadded(multiVariable.getPadding().getVariables(), JRightPadded.Location.NAMED_VARIABLE, ",", p);
        
        afterSyntax(multiVariable, p);
        return multiVariable;
    }
    
    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
        beforeSyntax(variable, Space.Location.VARIABLE_PREFIX, p);

        // Print the variable name unless it's a synthesized name suppressed by OmitName
        // (e.g. anonymous `using` parameters: `def f(using Ord[T])`).
        boolean omitName = variable.getMarkers().findFirst(
            org.openrewrite.scala.marker.OmitName.class).isPresent();
        if (!omitName) {
            visit(variable.getName(), p);
        }

        // In Scala, type annotation comes after the name
        J.VariableDeclarations parent = getCursor().getParentOrThrow().getValue();
        if (parent.getTypeExpression() != null) {
            // Print space before colon if present (e.g., `given IntSchema : SchemaFor[Int]`)
            // Stored in varargs field (repurposed, unused in Scala)
            if (parent.getVarargs() != null) {
                visitSpace(parent.getVarargs(), Space.Location.VARARGS, p);
            }
            // Skip the colon when the name is omitted (anonymous `using` param).
            if (!omitName) {
                p.append(":");
            }
            visit(parent.getTypeExpression(), p);

            // If there's an initializer, use visitLeftPadded to handle it properly
            visitLeftPadded("=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
        } else {
            // No type annotation, handle initializer normally
            visitLeftPadded("=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
        }

        afterSyntax(variable, p);
        return variable;
    }
    
    @Override
    public J visitNewClass(J.NewClass newClass, PrintOutputCapture<P> p) {
        beforeSyntax(newClass, Space.Location.NEW_CLASS_PREFIX, p);
        if (newClass.getPadding().getEnclosing() != null) {
            visitRightPadded(newClass.getPadding().getEnclosing(), JRightPadded.Location.NEW_CLASS_ENCLOSING, ".", p);
        }
        p.append("new");
        visit(newClass.getClazz(), p);
        // In Scala, constructors can be called without parentheses
        if (newClass.getPadding().getArguments() != null) {
            visitContainer("(", newClass.getPadding().getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
        }
        visit(newClass.getBody(), p);
        afterSyntax(newClass, p);
        return newClass;
    }

    @Override
    public J visitIntersectionType(J.IntersectionType intersectionType, PrintOutputCapture<P> p) {
        // In Scala, parents of an anonymous class are joined with `with` (not Java's `&`).
        beforeSyntax(intersectionType, Space.Location.INTERSECTION_TYPE_PREFIX, p);
        visitContainer("", intersectionType.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, "with", "", p);
        afterSyntax(intersectionType, p);
        return intersectionType;
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, PrintOutputCapture<P> p) {
        beforeSyntax(type, Space.Location.PARAMETERIZED_TYPE_PREFIX, p);
        visit(type.getClazz(), p);
        
        // Use Scala-style square brackets for type parameters
        visitContainer("[", type.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", "]", p);
        
        afterSyntax(type, p);
        return type;
    }
    
    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, PrintOutputCapture<P> p) {
        beforeSyntax(arrayAccess, Space.Location.ARRAY_ACCESS_PREFIX, p);
        visit(arrayAccess.getIndexed(), p);
        
        // In Scala, array access uses parentheses, not square brackets
        J.ArrayDimension dimension = arrayAccess.getDimension();
        visitSpace(dimension.getPrefix(), Space.Location.DIMENSION_PREFIX, p);
        p.append('(');
        visitRightPadded(dimension.getPadding().getIndex(), JRightPadded.Location.ARRAY_INDEX, "", p);
        p.append(')');
        
        afterSyntax(arrayAccess, p);
        return arrayAccess;
    }
    
    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, PrintOutputCapture<P> p) {
        beforeSyntax(instanceOf, Space.Location.INSTANCEOF_PREFIX, p);
        
        // In Scala, instanceof is written as expression.isInstanceOf[Type]
        visitRightPadded(instanceOf.getPadding().getExpression(), JRightPadded.Location.INSTANCEOF, "", p);
        p.append(".isInstanceOf");
        
        // Extract the type and wrap in square brackets
        p.append('[');
        visit(instanceOf.getClazz(), p);
        p.append(']');
        
        afterSyntax(instanceOf, p);
        return instanceOf;
    }
    
    // visitFieldAccess is defined above with TypeProjection and Empty target handling

    @Override
    public J visitModifier(J.Modifier mod, PrintOutputCapture<P> p) {
        // For Private and Protected, use the keyword field which may contain
        // scope qualifiers like private[testing] or protected[this]
        if ((mod.getType() == J.Modifier.Type.Private || mod.getType() == J.Modifier.Type.Protected)
                && mod.getKeyword() != null && mod.getKeyword().contains("[")) {
            visit(mod.getAnnotations(), p);
            beforeSyntax(mod, Space.Location.MODIFIER_PREFIX, p);
            p.append(mod.getKeyword());
            afterSyntax(mod, p);
            return mod;
        }
        return super.visitModifier(mod, p);
    }

    @Override
    public J visitNewArray(J.NewArray newArray, PrintOutputCapture<P> p) {
        beforeSyntax(newArray, Space.Location.NEW_ARRAY_PREFIX, p);
        
        // In Scala, array creation uses Array(elements) or Array[Type](elements) syntax
        p.append("Array");
        
        // Print type parameter if present
        if (newArray.getTypeExpression() != null) {
            p.append('[');
            visit(newArray.getTypeExpression(), p);
            p.append(']');
        }
        
        // If we have an initializer, print the elements
        if (newArray.getInitializer() != null) {
            // The initializer container already has the proper parentheses spacing
            visitContainer("", newArray.getPadding().getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, ",", "", p);
        } else {
            // Empty array
            p.append("()");
        }
        
        afterSyntax(newArray, p);
        return newArray;
    }
    
    @Override
    public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
        // Colon-indented argument: `f: arg`, `obj.method: arg`, or `obj.method[T]: arg`.
        // The arg's prefix carries the indent; we emit `:` in place of `(...)`.
        if (method.getMarkers().findFirst(IndentedSyntax.class).isPresent()) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);

            if (method.getMarkers().findFirst(org.openrewrite.scala.marker.FunctionApplication.class).isPresent()) {
                // Function application: `f: arg` — skip the `.apply` name
                visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, "", p);
            } else {
                if (method.getPadding().getSelect() != null) {
                    visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, ".", p);
                }
                visit(method.getName(), p);
            }

            if (method.getTypeParameters() != null && !method.getTypeParameters().isEmpty()) {
                visitContainer("[", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", "]", p);
            }

            JContainer<Expression> colonArgs = method.getPadding().getArguments();
            if (colonArgs != null) {
                visitSpace(colonArgs.getBefore(), Space.Location.METHOD_INVOCATION_ARGUMENTS, p);
                p.append(':');
                for (Expression arg : method.getArguments()) {
                    visit(arg, p);
                }
            }

            afterSyntax(method, p);
            return method;
        }

        // Check block argument BEFORE function application — when both are present
        // (e.g. `Seq { 1 }`), the block-arg path should win.
        if (method.getMarkers().findFirst(BlockArgument.class).isPresent()) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);

            if (method.getMarkers().findFirst(org.openrewrite.scala.marker.FunctionApplication.class).isPresent()) {
                // Function application with block arg: `Seq { 1 }`
                // Print select (function name), skip ".apply", print args directly
                visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, "", p);
            } else {
                // Dot-notation with block arg: `list.foreach { x => ... }`
                if (method.getPadding().getSelect() != null) {
                    visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, ".", p);
                }
                visit(method.getName(), p);
            }

            // Type arguments: `intercept[T] { ... }` or `list.foreach[T] { ... }`
            if (method.getTypeParameters() != null && !method.getTypeParameters().isEmpty()) {
                visitContainer("[", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", "]", p);
            }

            // Print the block argument — it's typically an S.StatementExpression(J.Block)
            // The J.Block contains the lambda. visitBlock prints the { } braces.
            if (method.getArguments() != null) {
                for (Expression arg : method.getArguments()) {
                    visit(arg, p);
                }
            }

            afterSyntax(method, p);
            return method;
        }

        // Check if this is function application syntax (arr(0) instead of arr.apply(0))
        if (method.getMarkers().findFirst(org.openrewrite.scala.marker.FunctionApplication.class).isPresent()) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);

            // Print the select (e.g., "arr" or "println")
            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, "", p);

            // Print arguments directly with parentheses (no ".apply")
            visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);

            afterSyntax(method, p);
            return method;
        }

        // Check if this is infix notation (list map func instead of list.map(func))
        if (method.getMarkers().findFirst(org.openrewrite.scala.marker.InfixNotation.class).isPresent()) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);

            boolean rightAssoc = method.getMarkers().findFirst(org.openrewrite.scala.marker.RightAssociative.class).isPresent();

            if (rightAssoc) {
                // AST stores right-associative ops semantically (select = right, arg = left).
                // Restore source order on output: <argument> <name> <select>.
                if (method.getArguments() != null) {
                    for (Expression arg : method.getArguments()) {
                        visit(arg, p);
                    }
                }
                visit(method.getName(), p);
                visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, "", p);
            } else {
                visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, "", p);
                visit(method.getName(), p);
                if (method.getArguments() != null) {
                    for (Expression arg : method.getArguments()) {
                        visit(arg, p);
                    }
                }
            }

            afterSyntax(method, p);
            return method;
        }
        
        // In Scala, method-level type arguments go AFTER the name (e.g., `foo.bar[T](x)`)
        // and use square brackets. Also honor OmitParentheses on the arguments container
        // for parenless calls like `List.newBuilder[Instant]`.
        if (method.getTypeParameters() != null && !method.getTypeParameters().isEmpty()) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);
            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, ".", p);
            visit(method.getName(), p);
            visitContainer("[", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", "]", p);
            JContainer<Expression> args = method.getPadding().getArguments();
            if (args == null || !args.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                visitContainer("(", args, JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
            }
            afterSyntax(method, p);
            return method;
        }

        // If arguments have OmitParentheses (no type args path), suppress the `()` too.
        JContainer<Expression> args = method.getPadding().getArguments();
        if (args != null && args.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);
            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, ".", p);
            visit(method.getName(), p);
            afterSyntax(method, p);
            return method;
        }

        // For regular method calls, use the default Java printing
        return super.visitMethodInvocation(method, p);
    }
    
    @Override
    public J visitMemberReference(J.MemberReference memberRef, PrintOutputCapture<P> p) {
        beforeSyntax(memberRef, Space.Location.MEMBER_REFERENCE_PREFIX, p);
        
        // Print the containing object
        visitRightPadded(memberRef.getPadding().getContaining(), JRightPadded.Location.MEMBER_REFERENCE_CONTAINING, p);
        
        // In Scala, member references use space + underscore instead of ::
        // e.g., "greet _" instead of "greet::apply"
        visit(memberRef.getPadding().getReference().getElement(), p);
        
        afterSyntax(memberRef, p);
        return memberRef;
    }
    
    public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
        beforeSyntax(lambda, Space.Location.LAMBDA_PREFIX, p);

        // Check if this is an underscore placeholder lambda
        if (lambda.getMarkers().findFirst(UnderscorePlaceholderLambda.class).isPresent()) {
            // For underscore placeholder lambdas, just print the body
            // The underscores in the body will be printed as S.Wildcard
            visit(lambda.getBody(), p);
            afterSyntax(lambda, p);
            return lambda;
        }

        // Partial-function literal: brace form `{ case pat => ... }` or indented form
        // (after a colon-arg call) where braces are absent.
        if (lambda.getMarkers().findFirst(PartialFunctionLiteral.class).isPresent() &&
                lambda.getBody() instanceof J.Block) {
            boolean indented = lambda.getMarkers().findFirst(IndentedSyntax.class).isPresent();
            J.Block cases = (J.Block) lambda.getBody();
            if (!indented) {
                p.append('{');
            }
            for (Statement caseStmt : cases.getStatements()) {
                visit(caseStmt, p);
            }
            visitSpace(cases.getEnd(), Space.Location.BLOCK_END, p);
            if (!indented) {
                p.append('}');
            }
            afterSyntax(lambda, p);
            return lambda;
        }
        
        // Print lambda parameters
        J.Lambda.Parameters params = lambda.getParameters();
        visitSpace(params.getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
        
        if (params.isParenthesized()) {
            p.append('(');
        }
        
        visitRightPadded(params.getPadding().getParameters(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
        
        if (params.isParenthesized()) {
            p.append(')');
        }
        
        // Print arrow with spacing
        visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
        p.append("=>");
        
        // Print lambda body
        visit(lambda.getBody(), p);
        
        afterSyntax(lambda, p);
        return lambda;
    }

    public J visitTuplePattern(S.TuplePattern tuplePattern, PrintOutputCapture<P> p) {
        beforeSyntax(tuplePattern, Space.Location.LANGUAGE_EXTENSION, p);
        p.append('(');
        visitContainer("", tuplePattern.getPadding().getElements(), JContainer.Location.LANGUAGE_EXTENSION, ",", "", p);
        p.append(')');
        afterSyntax(tuplePattern, p);
        return tuplePattern;
    }

    public J visitWildcard(S.Wildcard wildcard, PrintOutputCapture<P> p) {
        beforeSyntax(wildcard, Space.Location.LANGUAGE_EXTENSION, p);
        p.append('_');
        afterSyntax(wildcard, p);
        return wildcard;
    }

    public J visitTypeAlias(S.TypeAlias typeAlias, PrintOutputCapture<P> p) {
        beforeSyntax(typeAlias, Space.Location.LANGUAGE_EXTENSION, p);
        p.append(typeAlias.getText());
        afterSyntax(typeAlias, p);
        return typeAlias;
    }

    public J visitExport(S.Export export, PrintOutputCapture<P> p) {
        beforeSyntax(export, Space.Location.LANGUAGE_EXTENSION, p);
        p.append("export ");
        Expression clause = export.getExportClause();
        if (clause instanceof J.FieldAccess && isWildcardImport((J.FieldAccess) clause)) {
            J.FieldAccess fa = (J.FieldAccess) clause;
            visitFieldAccessUpToWildcard(fa, p);
            p.append("." + fa.getName().getSimpleName());
        } else {
            visit(clause, p);
        }
        afterSyntax(export, p);
        return export;
    }

    public J visitPatternDefinition(S.PatternDefinition patDef, PrintOutputCapture<P> p) {
        beforeSyntax(patDef, Space.Location.LANGUAGE_EXTENSION, p);
        p.append(patDef.getText());
        afterSyntax(patDef, p);
        return patDef;
    }

    public J visitAnonymousGiven(S.AnonymousGiven g, PrintOutputCapture<P> p) {
        beforeSyntax(g, Space.Location.LANGUAGE_EXTENSION, p);
        visit(g.getLeadingAnnotations(), p);
        for (J.Modifier m : g.getModifiers()) {
            visit(m, p);
        }
        // Whitespace between the last modifier (or annotations) and `given`. Captured by
        // the visitor when a modifier precedes the keyword.
        g.getMarkers().findFirst(ValVarKeyword.class)
                .ifPresent(m -> p.append(m.beforeKeyword()));
        p.append("given");
        visit(g.getType(), p);
        JLeftPadded<Expression> init = g.getInitializer();
        if (init != null) {
            visitSpace(init.getBefore(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append("=");
            visit(init.getElement(), p);
        }
        afterSyntax(g, p);
        return g;
    }

    public J visitSingletonType(S.SingletonType singletonType, PrintOutputCapture<P> p) {
        beforeSyntax(singletonType, Space.Location.LANGUAGE_EXTENSION, p);
        visit(singletonType.getQualifier(), p);
        visitSpace(singletonType.getBeforeType(), Space.Location.LANGUAGE_EXTENSION, p);
        p.append(".type");
        afterSyntax(singletonType, p);
        return singletonType;
    }

    public J visitRepeatedType(S.RepeatedType repeatedType, PrintOutputCapture<P> p) {
        beforeSyntax(repeatedType, Space.Location.LANGUAGE_EXTENSION, p);
        visit(repeatedType.getElementType(), p);
        visitSpace(repeatedType.getBeforeStar(), Space.Location.LANGUAGE_EXTENSION, p);
        p.append('*');
        afterSyntax(repeatedType, p);
        return repeatedType;
    }

    public J visitSplatExpression(S.SplatExpression splatExpression, PrintOutputCapture<P> p) {
        beforeSyntax(splatExpression, Space.Location.LANGUAGE_EXTENSION, p);
        visit(splatExpression.getExpression(), p);
        if (splatExpression.getBeforeColon() != null) {
            visitSpace(splatExpression.getBeforeColon(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append(':');
            visitSpace(splatExpression.getAfterColon(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append('_');
        }
        visitSpace(splatExpression.getBeforeStar(), Space.Location.LANGUAGE_EXTENSION, p);
        p.append('*');
        afterSyntax(splatExpression, p);
        return splatExpression;
    }

    public J visitXmlLiteral(S.XmlLiteral xmlLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(xmlLiteral, Space.Location.LANGUAGE_EXTENSION, p);
        p.append(xmlLiteral.getSource());
        afterSyntax(xmlLiteral, p);
        return xmlLiteral;
    }

    public J visitAlternative(S.Alternative alternative, PrintOutputCapture<P> p) {
        beforeSyntax(alternative, Space.Location.LANGUAGE_EXTENSION, p);
        visitContainer("", alternative.getPadding().getPatterns(), JContainer.Location.LANGUAGE_EXTENSION, "|", "", p);
        afterSyntax(alternative, p);
        return alternative;
    }

    public J visitQualifiedSuper(S.QualifiedSuper qualifiedSuper, PrintOutputCapture<P> p) {
        beforeSyntax(qualifiedSuper, Space.Location.LANGUAGE_EXTENSION, p);
        if (qualifiedSuper.getQualifier() != null) {
            visit(qualifiedSuper.getQualifier(), p);
            p.append('.');
        }
        p.append("super");
        if (qualifiedSuper.getMixName() != null) {
            p.append('[');
            visit(qualifiedSuper.getMixName(), p);
            p.append(']');
        }
        afterSyntax(qualifiedSuper, p);
        return qualifiedSuper;
    }

    public J visitAnnotatedExpression(S.AnnotatedExpression annotatedExpression, PrintOutputCapture<P> p) {
        beforeSyntax(annotatedExpression, Space.Location.LANGUAGE_EXTENSION, p);
        visit(annotatedExpression.getExpression(), p);
        visitSpace(annotatedExpression.getBeforeColon(), Space.Location.LANGUAGE_EXTENSION, p);
        p.append(':');
        visit(annotatedExpression.getAnnotation(), p);
        afterSyntax(annotatedExpression, p);
        return annotatedExpression;
    }

    public J visitFunctionType(S.FunctionType functionType, PrintOutputCapture<P> p) {
        beforeSyntax(functionType, Space.Location.LANGUAGE_EXTENSION, p);
        JContainer<TypeTree> params = functionType.getPadding().getParameters();
        visitSpace(params.getBefore(), Space.Location.LANGUAGE_EXTENSION, p);
        if (functionType.isParenthesized()) {
            p.append('(');
        }
        List<JRightPadded<TypeTree>> elements = params.getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            JRightPadded<TypeTree> element = elements.get(i);
            visit(element.getElement(), p);
            visitSpace(element.getAfter(), Space.Location.LANGUAGE_EXTENSION, p);
            if (i < elements.size() - 1) {
                p.append(',');
            }
        }
        if (functionType.isParenthesized()) {
            p.append(')');
        }
        JLeftPadded<TypeTree> rt = functionType.getPadding().getReturnType();
        visitSpace(rt.getBefore(), Space.Location.LANGUAGE_EXTENSION, p);
        p.append("=>");
        visit(rt.getElement(), p);
        afterSyntax(functionType, p);
        return functionType;
    }

    public J visitTupleType(S.TupleType tupleType, PrintOutputCapture<P> p) {
        beforeSyntax(tupleType, Space.Location.LANGUAGE_EXTENSION, p);
        JContainer<TypeTree> elements = tupleType.getPadding().getElements();
        visitSpace(elements.getBefore(), Space.Location.LANGUAGE_EXTENSION, p);
        p.append('(');
        List<JRightPadded<TypeTree>> padded = elements.getPadding().getElements();
        for (int i = 0; i < padded.size(); i++) {
            JRightPadded<TypeTree> element = padded.get(i);
            visit(element.getElement(), p);
            visitSpace(element.getAfter(), Space.Location.LANGUAGE_EXTENSION, p);
            if (i < padded.size() - 1) {
                p.append(',');
            }
        }
        p.append(')');
        afterSyntax(tupleType, p);
        return tupleType;
    }

    public J visitRefinedType(S.RefinedType refinedType, PrintOutputCapture<P> p) {
        beforeSyntax(refinedType, Space.Location.LANGUAGE_EXTENSION, p);
        if (refinedType.getParent() != null) {
            visit(refinedType.getParent(), p);
        }
        visit(refinedType.getRefinements(), p);
        afterSyntax(refinedType, p);
        return refinedType;
    }

    public J visitMacro(S.Macro macro, PrintOutputCapture<P> p) {
        beforeSyntax(macro, Space.Location.LANGUAGE_EXTENSION, p);
        switch (macro.getKind()) {
            case Splice:
                p.append("${");
                visit(macro.getExpression(), p);
                p.append('}');
                break;
            case QuoteBlock:
                p.append("'{");
                visit(macro.getExpression(), p);
                p.append('}');
                break;
            case QuoteIdent:
                p.append('\'');
                visit(macro.getExpression(), p);
                break;
        }
        afterSyntax(macro, p);
        return macro;
    }

    public J visitExtensionMethods(S.ExtensionMethods ext, PrintOutputCapture<P> p) {
        beforeSyntax(ext, Space.Location.LANGUAGE_EXTENSION, p);
        p.append("extension");
        JContainer<Statement> params = ext.getPadding().getParameters();
        p.append(params.getBefore().getWhitespace());
        p.append('(');
        List<JRightPadded<Statement>> paramList = params.getPadding().getElements();
        for (int i = 0; i < paramList.size(); i++) {
            JRightPadded<Statement> param = paramList.get(i);
            Statement element = param.getElement();
            if (element instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecl = (J.VariableDeclarations) element;
                visitSpace(varDecl.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                visit(varDecl.getLeadingAnnotations(), p);
                visit(varDecl.getModifiers(), p);
                if (!varDecl.getVariables().isEmpty()) {
                    visit(varDecl.getVariables().get(0).getName(), p);
                }
                if (varDecl.getTypeExpression() != null) {
                    TypeTree typeExpr = varDecl.getTypeExpression();
                    if (varDecl.getVarargs() != null) {
                        visitSpace(varDecl.getVarargs(), Space.Location.VARARGS, p);
                    }
                    p.append(":");
                    visit(typeExpr, p);
                }
                if (!varDecl.getVariables().isEmpty() &&
                    varDecl.getVariables().get(0).getPadding().getInitializer() != null) {
                    JLeftPadded<Expression> init = varDecl.getVariables().get(0).getPadding().getInitializer();
                    visitLeftPadded("=", init, JLeftPadded.Location.VARIABLE_INITIALIZER, p);
                }
            } else {
                visit(element, p);
            }
            visitSpace(param.getAfter(), JRightPadded.Location.METHOD_DECLARATION_PARAMETER.getAfterLocation(), p);
            if (i < paramList.size() - 1) {
                p.append(',');
            }
        }
        p.append(')');
        visit(ext.getBody(), p);
        afterSyntax(ext, p);
        return ext;
    }

    public J visitFor(S.For forLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forLoop, Space.Location.LANGUAGE_EXTENSION, p);
        p.append("for");
        char open = forLoop.getOpenBracket();
        boolean parenless = open == ' ';  // Scala 3 indented form: no '(' or '{'
        char close = open == '(' ? ')' : '}';
        JContainer<S.For.Enumerator> enums = forLoop.getPadding().getEnumerators();
        visitSpace(enums.getBefore(), Space.Location.LANGUAGE_EXTENSION, p);
        if (!parenless) {
            p.append(open);
        }
        List<JRightPadded<S.For.Enumerator>> elems = enums.getPadding().getElements();
        for (int i = 0; i < elems.size(); i++) {
            JRightPadded<S.For.Enumerator> rp = elems.get(i);
            visit(rp.getElement(), p);
            visitSpace(rp.getAfter(), Space.Location.LANGUAGE_EXTENSION, p);
            // An explicit ';' separator is preserved via a Semicolon marker on the
            // JRightPadded. Newline-separated generators have no marker and no ';'.
            if (rp.getMarkers().findFirst(Semicolon.class).isPresent()) {
                p.append(';');
            }
        }
        if (!parenless) {
            p.append(close);
        }
        visitSpace(forLoop.getBeforeBody(), Space.Location.LANGUAGE_EXTENSION, p);
        if (forLoop.isYielding()) {
            p.append("yield");
        } else if (parenless) {
            p.append("do");
        }
        visit(forLoop.getBody(), p);
        afterSyntax(forLoop, p);
        return forLoop;
    }

    public J visitForEnumerator(S.For.Enumerator enumerator, PrintOutputCapture<P> p) {
        beforeSyntax(enumerator.getPrefix(), enumerator.getMarkers(), Space.Location.LANGUAGE_EXTENSION, p);
        switch (enumerator.getKind()) {
            case Generator:
                if (enumerator.getLhs() != null) visit(enumerator.getLhs(), p);
                visitSpace(enumerator.getBeforeOp(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append("<-");
                visit(enumerator.getRhs(), p);
                break;
            case Guard:
                p.append("if");
                visitSpace(enumerator.getBeforeOp(), Space.Location.LANGUAGE_EXTENSION, p);
                visit(enumerator.getRhs(), p);
                break;
            case Assignment:
                if (enumerator.getLhs() != null) visit(enumerator.getLhs(), p);
                visitSpace(enumerator.getBeforeOp(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append("=");
                visit(enumerator.getRhs(), p);
                break;
        }
        afterSyntax(enumerator.getMarkers(), p);
        return enumerator;
    }
}
