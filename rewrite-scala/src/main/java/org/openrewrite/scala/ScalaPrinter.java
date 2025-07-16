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
package org.openrewrite.scala;

import org.jspecify.annotations.Nullable;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.scala.marker.SObject;
import org.openrewrite.scala.marker.ScalaForLoop;
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
            // Use Scala-style square brackets for type parameters
            if (container != null) {
                visitSpace(container.getBefore(), location.getBeforeLocation(), p);
                p.append('['); // Use [ instead of <
                visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
                p.append(']'); // Use ] instead of >
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
        
        // Print bounds if present using Scala syntax
        if (typeParam.getPadding().getBounds() != null) {
            visitSpace(typeParam.getPadding().getBounds().getBefore(), Space.Location.TYPE_BOUNDS, p);
            p.append(":");  // Scala uses : instead of extends for bounds
            visitRightPadded(typeParam.getPadding().getBounds().getPadding().getElements(), 
                JRightPadded.Location.TYPE_BOUND, " with", p);  // Scala uses "with" instead of "&"
        }
        
        afterSyntax(typeParam, p);
        return typeParam;
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
        // Need to handle wildcard imports specially for Scala (_ instead of *)
        J.FieldAccess qualid = import_.getQualid();
        if (isWildcardImport(qualid)) {
            // Print the package part
            visitFieldAccessUpToWildcard(qualid, p);
            p.append("._");
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
        return "*".equals(name.getSimpleName());
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
            if (isObject) {
                // For objects, we print "object" - the "case" modifier is printed separately
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
                visit(classDecl.getTypeParameters(), p);
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
                JRightPadded<J.TypeParameter> elem = elements.get(i);
                visit(elem.getElement(), p);
                if (i < elements.size() - 1) {
                    visitSpace(elem.getAfter(), Space.Location.TYPE_PARAMETER_SUFFIX, p);
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
            // Don't print the block at all
            return block;
        }
        return super.visitBlock(block, p);
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
    public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
        beforeSyntax(lambda, Space.Location.LAMBDA_PREFIX, p);
        
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
}