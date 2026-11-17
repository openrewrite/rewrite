/*
 * Copyright 2026 the original author or authors.
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

import org.apache.groovy.ast.tools.ClassNodeUtils;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.MethodCall;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;
import org.codehaus.groovy.transform.stc.TypeCheckingExtension;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tells the type checker what a name or call that only a static import declares refers to. On Groovy 3 and later
 * the parser leaves out {@code StaticImportVisitor}, so such a name stays a dynamic variable and such a call has
 * no target. A name resolves in that visitor's order. A call resolves only to a static method of the imported class.
 */
class StaticImportTypeCheckingExtension extends TypeCheckingExtension {
    StaticImportTypeCheckingExtension(StaticTypeCheckingVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor);
    }

    @Override
    public boolean handleUnresolvedVariableExpression(VariableExpression vexp) {
        AnnotatedNode member = staticallyImportedProperty(module(), vexp.getName());
        if (member instanceof FieldNode) {
            storeType(vexp, ((FieldNode) member).getType());
            return true;
        } else if (member instanceof MethodNode) {
            storeType(vexp, ((MethodNode) member).getReturnType());
            return true;
        }
        return false;
    }

    @Override
    public List<MethodNode> handleMissingMethod(ClassNode receiver, String name, @Nullable ArgumentListExpression argumentList,
                                                ClassNode @Nullable [] argumentTypes, @Nullable MethodCall call) {
        if (!(call instanceof MethodCallExpression) || !((MethodCallExpression) call).isImplicitThis() || argumentTypes == null) {
            return Collections.emptyList();
        }
        ModuleNode module = module();
        ImportNode importNode = module.getStaticImports().get(name);
        if (importNode != null) {
            List<MethodNode> chosen = bestStaticMethod(importNode.getType(), importNode.getFieldName(), argumentTypes);
            if (!chosen.isEmpty()) {
                return chosen;
            }
        }
        for (ImportNode starImport : module.getStaticStarImports().values()) {
            List<MethodNode> chosen = bestStaticMethod(starImport.getType(), name, argumentTypes);
            if (!chosen.isEmpty()) {
                return chosen;
            }
        }
        return Collections.emptyList();
    }

    private ModuleNode module() {
        return typeCheckingVisitor.getTypeCheckingContext().getSource().getAST();
    }

    private static List<MethodNode> bestStaticMethod(ClassNode owner, String name, ClassNode[] argumentTypes) {
        List<MethodNode> candidates = new ArrayList<>();
        for (MethodNode method : owner.getMethods(name)) {
            if (method.isStatic()) {
                candidates.add(method);
            }
        }
        return candidates.isEmpty() ? candidates : StaticTypeCheckingSupport.chooseBestMethod(owner, candidates, argumentTypes);
    }

    /**
     * The static field or static getter read by a name that only a static import declares.
     */
    static @Nullable AnnotatedNode staticallyImportedProperty(ModuleNode module, String name) {
        Map<String, ImportNode> staticImports = module.getStaticImports();
        String capitalized = MetaClassHelper.capitalize(name);
        for (String accessor : new String[]{"is" + capitalized, "get" + capitalized}) {
            ImportNode importNode = staticImports.get(accessor);
            if (importNode != null) {
                MethodNode getter = staticGetter(importNode.getType(), importNode.getFieldName());
                if (getter != null) {
                    return getter;
                }
                // A property of a class compiled from source has no getter until class generation
                PropertyNode property = ClassNodeUtils.getStaticProperty(importNode.getType(),
                        ClassNodeUtils.getPropNameForAccessor(importNode.getFieldName()));
                if (property != null) {
                    return property.getField();
                }
            }
        }
        ImportNode importNode = staticImports.get(name);
        if (importNode != null) {
            AnnotatedNode member = staticPropertyOrField(importNode.getType(), importNode.getFieldName());
            if (member != null) {
                return member;
            }
        }
        for (ImportNode starImport : module.getStaticStarImports().values()) {
            AnnotatedNode member = staticPropertyOrField(starImport.getType(), name);
            if (member != null) {
                return member;
            }
        }
        return null;
    }

    private static @Nullable AnnotatedNode staticPropertyOrField(ClassNode owner, String name) {
        String capitalized = MetaClassHelper.capitalize(name);
        MethodNode getter = staticGetter(owner, "is" + capitalized);
        if (getter == null) {
            getter = staticGetter(owner, "get" + capitalized);
        }
        if (getter != null) {
            return getter;
        }
        FieldNode field = ClassNodeUtils.getField(owner, name);
        return field != null && field.isStatic() ? field : null;
    }

    private static @Nullable MethodNode staticGetter(ClassNode owner, String name) {
        MethodNode method = owner.getMethod(name, Parameter.EMPTY_ARRAY);
        return method != null && method.isStatic() ? method : null;
    }
}
