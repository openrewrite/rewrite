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
package org.openrewrite.groovy;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.ResolveVisitor;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Visitor to resolve Types and convert VariableExpression to
 * ClassExpressions if needed. The ResolveVisitor will try to
 * find the Class for a ClassExpression and prints an error if
 * it fails to do so. Constructions like C[], foo as C, (C) foo
 * will force creation of a ClassExpression for C
 * <p>
 * Note: the method to start the resolving is  startResolving(ClassNode, SourceUnit).
 *
 * @implNote Extension of {@link ResolveVisitor}, except for `transformInlineConstants()` which doesn't call `ExpressionUtils.transformInlineConstants()`
 */
class NoInlineAnnotationTransformationResolveVisitor extends ResolveVisitor {
    public NoInlineAnnotationTransformationResolveVisitor(CompilationUnit compilationUnit) {
        super(compilationUnit);
    }

    @Override
    public void visitAnnotations(final AnnotatedNode node) {
        List<AnnotationNode> annotations = node.getAnnotations();
        if (annotations.isEmpty()) return;
        Map<String, AnnotationNode> tmpAnnotations = new HashMap<>();
        for (AnnotationNode an : annotations) {
            if (an.isBuiltIn()) continue;
            ClassNode annType = an.getClassNode();
            resolveOrFail(annType, " for annotation", an);
            for (Map.Entry<String, Expression> member : an.getMembers().entrySet()) {
                Expression newValue = transform(member.getValue());
                Expression adjusted = transformInlineConstants(newValue);
                member.setValue(adjusted);
                checkAnnotationMemberValue(adjusted);
            }
            if (annType.isResolved()) {
                Class<?> annTypeClass = annType.getTypeClass();
                Retention retAnn = annTypeClass.getAnnotation(Retention.class);
                if (retAnn != null && retAnn.value() != RetentionPolicy.SOURCE && !isRepeatable(annTypeClass)) {
                    AnnotationNode anyPrevAnnNode = tmpAnnotations.put(annTypeClass.getName(), an);
                    if (anyPrevAnnNode != null) {
                        addError("Cannot specify duplicate annotation on the same member : " + annType.getName(), an);
                    }
                }
            }
        }
    }

    private static Expression transformInlineConstants(final Expression exp) {
        if (exp instanceof AnnotationConstantExpression) {
            ConstantExpression ce = (ConstantExpression) exp;
            if (ce.getValue() instanceof AnnotationNode) {
                // replicate a little bit of AnnotationVisitor here
                // because we can't wait until later to do this
                AnnotationNode an = (AnnotationNode) ce.getValue();
                for (Map.Entry<String, Expression> member : an.getMembers().entrySet()) {
                    member.setValue(transformInlineConstants(member.getValue()));
                }
            }
//       } else {
//          return ExpressionUtils.transformInlineConstants(exp);
        }
        return exp;
    }

    // Call `resolveOrFail` by reflection (because of private access)
    private void resolveOrFail(final ClassNode type, final String msg, final ASTNode node) {
        try {
            Method method = ResolveVisitor.class.getDeclaredMethod("resolveOrFail", ClassNode.class, String.class, ASTNode.class);
            method.setAccessible(true);
            method.invoke(this, type, msg, node);
        } catch (Exception e) {
            throw new RuntimeException("Error invoking `resolveOrFail` method", e);
        }
    }

    // Copy of parent `checkAnnotationMemberValue` (because of private access)
    private void checkAnnotationMemberValue(final Expression newValue) {
        if (newValue instanceof PropertyExpression) {
            PropertyExpression pe = (PropertyExpression) newValue;
            if (!(pe.getObjectExpression() instanceof ClassExpression)) {
                addError("unable to find class '" + pe.getText() + "' for annotation attribute constant", pe.getObjectExpression());
            }
        } else if (newValue instanceof ListExpression) {
            ListExpression le = (ListExpression) newValue;
            for (Expression e : le.getExpressions()) {
                checkAnnotationMemberValue(e);
            }
        }
    }

    // Copy of parent `checkAnnotationMemberValue` (because of private access)
    private boolean isRepeatable(final Class<?> annTypeClass) {
        Annotation[] annTypeAnnotations = annTypeClass.getAnnotations();
        for (Annotation annTypeAnnotation : annTypeAnnotations) {
            if ("java.lang.annotation.Repeatable".equals(annTypeAnnotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }
}
