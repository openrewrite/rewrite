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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.scala.marker.BlockArgument;
import org.openrewrite.scala.marker.IndentedBlock;
import org.openrewrite.scala.marker.SObject;
import org.openrewrite.scala.marker.TypeProjection;
import org.openrewrite.scala.marker.ScalaForLoop;
import org.openrewrite.scala.marker.UnderscorePlaceholderLambda;
import org.openrewrite.scala.tree.S;

import java.util.List;

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
    public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
        beforeSyntax(typeCast, Space.Location.TYPE_CAST_PREFIX, p);
        // In Scala, type casts are written as expression.asInstanceOf[Type]
        visit(typeCast.getExpression(), p);
        p.append(".asInstanceOf");
        
        // Extract the type from the control parentheses
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
    public J visitTry(J.Try tryable, PrintOutputCapture<P> p) {
        beforeSyntax(tryable, Space.Location.TRY_PREFIX, p);
        p.append("try");
        visit(tryable.getBody(), p);
        if (!tryable.getCatches().isEmpty()) {
            // Scala catch block: catch { case ... }
            J.Try.Catch firstCatch = tryable.getCatches().get(0);
            visitSpace(firstCatch.getPrefix(), Space.Location.CATCH_PREFIX, p);
            p.append("catch");
            p.append(" {");
            for (J.Try.Catch aCatch : tryable.getCatches()) {
                // Print each case clause
                J.ControlParentheses<J.VariableDeclarations> param = aCatch.getParameter();
                J.VariableDeclarations varDecl = param.getTree();
                p.append("\n");
                // Extract indentation from the catch prefix or body prefix
                String catchPrefix = aCatch.getBody().getStatements().isEmpty() ? "  " : "";
                p.append("  case");
                if (!varDecl.getVariables().isEmpty()) {
                    visit(varDecl.getVariables().get(0).getName(), p);
                }
                if (varDecl.getTypeExpression() != null) {
                    p.append(":");
                    visit(varDecl.getTypeExpression(), p);
                }
                p.append(" =>");
                // Print body statements
                for (Statement stmt : aCatch.getBody().getStatements()) {
                    visit(stmt, p);
                }
            }
            p.append("\n}");
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
        // Scala match expression: expr match { case ... }
        beforeSyntax(switch_, Space.Location.SWITCH_PREFIX, p);
        // The selector is the expression before "match"
        J.ControlParentheses<Expression> selector = switch_.getSelector();
        visit(selector.getTree(), p);
        p.append(" match");
        visit(switch_.getCases(), p);
        afterSyntax(switch_, p);
        return switch_;
    }

    @Override
    public J visitCase(J.Case case_, PrintOutputCapture<P> p) {
        beforeSyntax(case_, Space.Location.CASE_PREFIX, p);
        p.append("case");
        // Print pattern labels
        List<J> labels = case_.getCaseLabels();
        for (J label : labels) {
            visit(label, p);
        }
        p.append(" =>");
        // Print body
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
        for (J.Modifier m : method.getModifiers()) {
            visit(m, p);
        }

        // Add space before "def" if there were modifiers
        if (!method.getModifiers().isEmpty()) {
            p.append(" ");
        }
        p.append("def");
        visit(method.getName(), p);

        if (method.getPadding().getTypeParameters() != null) {
            visit(method.getPadding().getTypeParameters(), p);
        }

        // Print parameters (name: Type)
        JContainer<Statement> params = method.getPadding().getParameters();
        visitSpace(params.getBefore(), Space.Location.METHOD_DECLARATION_PARAMETERS, p);
        p.append('(');
        List<JRightPadded<Statement>> paramList = params.getPadding().getElements();
        for (int i = 0; i < paramList.size(); i++) {
            JRightPadded<Statement> param = paramList.get(i);
            Statement element = param.getElement();
            if (element instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecl = (J.VariableDeclarations) element;
                visitSpace(varDecl.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
                if (!varDecl.getVariables().isEmpty()) {
                    visit(varDecl.getVariables().get(0).getName(), p);
                }
                if (varDecl.getTypeExpression() != null) {
                    // The colon and space between name and type in Scala parameter syntax
                    // Type prefix from parser may or may not include the space
                    TypeTree typeExpr = varDecl.getTypeExpression();
                    if (typeExpr.getPrefix().isEmpty()) {
                        p.append(": ");
                    } else {
                        p.append(":");
                    }
                    visit(typeExpr, p);
                }
                if (!varDecl.getVariables().isEmpty() &&
                    varDecl.getVariables().get(0).getPadding().getInitializer() != null) {
                    JLeftPadded<Expression> init = varDecl.getVariables().get(0).getPadding().getInitializer();
                    if (init.getBefore().isEmpty()) {
                        p.append(" ");
                    }
                    visitLeftPadded("=", init, JLeftPadded.Location.VARIABLE_INITIALIZER, p);
                }
            } else {
                visit(element, p);
            }
            if (i < paramList.size() - 1) {
                visitSpace(param.getAfter(), JRightPadded.Location.METHOD_DECLARATION_PARAMETER.getAfterLocation(), p);
                p.append(',');
            }
        }
        p.append(')');

        if (method.getReturnTypeExpression() != null) {
            p.append(':');
            visit(method.getReturnTypeExpression(), p);
        }

        if (method.getBody() != null) {
            J.Block body = method.getBody();
            boolean omitBraces = body.getMarkers().findFirst(
                    org.openrewrite.scala.marker.OmitBraces.class).isPresent();
            if (omitBraces && body.getStatements().size() == 1) {
                p.append(" =");
                visit(body.getStatements().get(0), p);
            } else {
                p.append(" =");
                visit(body, p);
            }
        }

        afterSyntax(method, p);
        return method;
    }

    @Override
    protected void printStatementTerminator(Statement s, PrintOutputCapture<P> p) {
        // In Scala, semicolons are optional and generally not used
        // Only print them if they were explicitly in the source
        // For now, we'll skip semicolons entirely as proper semicolon preservation
        // would require tracking whether they were present in the original source
        return;
    }
    
    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (tree instanceof S.CompilationUnit) {
            return visitScalaCompilationUnit((S.CompilationUnit) tree, p);
        } else if (tree instanceof S.Wildcard) {
            return visitWildcard((S.Wildcard) tree, p);
        } else if (tree instanceof S.TuplePattern) {
            return visitTuplePattern((S.TuplePattern) tree, p);
        } else if (tree instanceof S.BlockExpression) {
            return visitBlockExpression((S.BlockExpression) tree, p);
        }
        return super.visit(tree, p);
    }
    
    public J visitScalaCompilationUnit(S.CompilationUnit scu, PrintOutputCapture<P> p) {
        beforeSyntax(scu, Space.Location.COMPILATION_UNIT_PREFIX, p);

        if (scu.getPackageDeclaration() != null) {
            visit(scu.getPackageDeclaration(), p);
            // In Scala, package declarations are followed by a newline
            // Check if the next element has a newline in its prefix, if not add one
            if (!scu.getImports().isEmpty()) {
                J.Import firstImport = scu.getImports().get(0);
                if (!firstImport.getPrefix().getWhitespace().startsWith("\n")) {
                    p.append("\n");
                }
            } else if (!scu.getStatements().isEmpty()) {
                Statement firstStatement = scu.getStatements().get(0);
                if (!firstStatement.getPrefix().getWhitespace().startsWith("\n")) {
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

        visitSpace(scu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        afterSyntax(scu, p);
        return scu;
    }

    @Override
    public J visitPackage(J.Package pkg, PrintOutputCapture<P> p) {
        beforeSyntax(pkg, Space.Location.PACKAGE_PREFIX, p);
        p.append("package");
        visit(pkg.getExpression(), p);
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
        
        // Handle aliases if present (for future use)
        if (import_.getAlias() != null) {
            p.append(" => ");
            visit(import_.getAlias(), p);
        }
        
        // Note: No semicolon in Scala import declarations
        afterSyntax(import_, p);
        return import_;
    }
    
    private boolean isWildcardImport(J.FieldAccess qualid) {
        J.Identifier name = qualid.getName();
        return "*".equals(name.getSimpleName()) || "_".equals(name.getSimpleName());
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
        
        // Or if we have a primary constructor with actual parameters
        if (classDecl.getPadding().getPrimaryConstructor() != null && 
            !classDecl.getPadding().getPrimaryConstructor().getElements().isEmpty()) {
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
            
            // For Scala: print primaryConstructor only if it has elements
            // The primaryConstructor container includes the parentheses and parameters
            if (classDecl.getPadding().getPrimaryConstructor() != null) {
                JContainer<Statement> primaryConstructor = classDecl.getPadding().getPrimaryConstructor();
                if (!primaryConstructor.getElements().isEmpty()) {
                    // Visit each element in the primary constructor
                    for (JRightPadded<Statement> statement : primaryConstructor.getPadding().getElements()) {
                        visit(statement.getElement(), p);
                        visitSpace(statement.getAfter(), Space.Location.RECORD_STATE_VECTOR_SUFFIX, p);
                    }
                }
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
            // For classes without Scala features, use Java printing but skip empty primary constructors
            // The Java printer would print empty parentheses for primary constructors
            if (classDecl.getPadding().getPrimaryConstructor() != null && 
                classDecl.getPadding().getPrimaryConstructor().getElements().isEmpty()) {
                // We have an empty primary constructor that shouldn't be printed
                // Use the default Java printer logic but without the primary constructor
                beforeSyntax(classDecl, Space.Location.CLASS_DECLARATION_PREFIX, p);
                visit(classDecl.getLeadingAnnotations(), p);
                for (J.Modifier m : classDecl.getModifiers()) {
                    visit(m, p);
                }
                visit(classDecl.getPadding().getKind().getAnnotations(), p);
                visitSpace(classDecl.getPadding().getKind().getPrefix(), Space.Location.CLASS_KIND, p);
                // For Scala, print "trait" for Interface kind
                String classKind = classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface ? 
                    "trait" : classDecl.getKind().name().toLowerCase();
                p.append(classKind);
                visit(classDecl.getName(), p);
                // Use our custom type parameter printing for Scala
                visitTypeParameters(classDecl.getPadding().getTypeParameters(), p);
                // Skip the empty primary constructor
                
                if (classDecl.getPadding().getExtends() != null) {
                    visitSpace(classDecl.getPadding().getExtends().getBefore(), Space.Location.EXTENDS, p);
                    p.append("extends");
                    visit(classDecl.getPadding().getExtends().getElement(), p);
                }

                if (classDecl.getPadding().getImplements() != null) {
                    visitContainer(" implements", classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", "", p);
                }

                if (classDecl.getPadding().getPermits() != null) {
                    visitContainer(" permits", classDecl.getPadding().getPermits(), JContainer.Location.PERMITS, ",", "", p);
                }

                visit(classDecl.getBody(), p);
                afterSyntax(classDecl, p);
                return classDecl;
            } else {
                // Use the default Java printing
                return super.visitClassDeclaration(classDecl, p);
            }
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
                if (i < elements.size() - 1) {
                    visitSpace(elements.get(i).getAfter(), Space.Location.TYPE_PARAMETER_SUFFIX, p);
                    p.append(',');
                }
            }
            p.append(']');
        }
    }

    @Override
    public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
        // Check if this block has the OmitBraces marker (for objects without body)
        if (block.getMarkers().findFirst(org.openrewrite.scala.marker.OmitBraces.class).isPresent()) {
            return block;
        }
        // Scala 3 braceless (indentation-based) blocks use `:` instead of `{}`
        if (block.getMarkers().findFirst(IndentedBlock.class).isPresent()) {
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
    public J visitForLoop(J.ForLoop forLoop, PrintOutputCapture<P> p) {
        // Check if this is a Scala range-based for loop
        ScalaForLoop marker = forLoop.getMarkers().findFirst(ScalaForLoop.class).orElse(null);
        if (marker != null && marker.getOriginalSource() != null && !marker.getOriginalSource().isEmpty()) {
            // Print the original Scala syntax
            beforeSyntax(forLoop, Space.Location.FOR_PREFIX, p);
            p.append(marker.getOriginalSource());
            afterSyntax(forLoop, p);
            return forLoop;
        }
        // Otherwise use Java syntax
        return super.visitForLoop(forLoop, p);
    }
    
    // Override additional methods here for Scala-specific syntax as needed

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
        visit(multiVariable.getLeadingAnnotations(), p);
        
        // If we have annotations, ensure space after them before modifiers/val/var
        if (!multiVariable.getLeadingAnnotations().isEmpty()) {
            p.append(" ");
        }
        
        // Check if this is a lambda parameter - if so, don't print val/var
        boolean isLambdaParam = multiVariable.getMarkers().findFirst(
            org.openrewrite.scala.marker.LambdaParameter.class).isPresent();
        
        // Print modifiers, but handle Final specially since Scala uses val/var.
        // The implicit Final modifier (keyword=null) prints as "val".
        // An explicit "final" modifier prints normally.
        // The "lazy" modifier is a LanguageExtension and prints via visitModifier.
        boolean isVal = false;
        boolean hasVisibleModifier = false;

        String valVarKeyword = "var";
        for (J.Modifier m : multiVariable.getModifiers()) {
            if (m.getType() == J.Modifier.Type.Final) {
                // Any Final modifier means this is a val (or given)
                isVal = true;
                if ("given".equals(m.getKeyword())) {
                    valVarKeyword = "given";
                } else {
                    valVarKeyword = "val";
                }
                // Only print the modifier if it has an explicit "final" keyword
                if ("final".equals(m.getKeyword())) {
                    visit(m, p);
                    hasVisibleModifier = true;
                }
            } else {
                visit(m, p);
                hasVisibleModifier = true;
            }
        }

        // Print val/var/given (unless it's a lambda parameter)
        if (!isLambdaParam) {
            if (hasVisibleModifier) {
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
        
        // Print the variable name
        visit(variable.getName(), p);

        // In Scala, type annotation comes after the name
        J.VariableDeclarations parent = getCursor().getParentOrThrow().getValue();
        if (parent.getTypeExpression() != null) {
            p.append(":");
            // The type expression should have the space after colon in its prefix
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
        // Ensure space between "new" and the class name
        if (newClass.getClazz() != null && newClass.getClazz().getPrefix().isEmpty()) {
            p.append(" ");
        }
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

            // Print the block argument directly (no parentheses)
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
            
            // Print the select (e.g., "list")
            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, "", p);
            
            // Print the method name with its prefix space (e.g., " map")
            visit(method.getName(), p);
            
            // Print the arguments without parentheses, just with their prefix space
            if (method.getArguments() != null && !method.getArguments().isEmpty()) {
                for (Expression arg : method.getArguments()) {
                    visit(arg, p);
                }
            }
            
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

    public J visitBlockExpression(S.BlockExpression blockExpression, PrintOutputCapture<P> p) {
        beforeSyntax(blockExpression, Space.Location.LANGUAGE_EXTENSION, p);
        // Simply visit the contained block - it will print itself with braces
        visit(blockExpression.getBlock(), p);
        afterSyntax(blockExpression, p);
        return blockExpression;
    }
}