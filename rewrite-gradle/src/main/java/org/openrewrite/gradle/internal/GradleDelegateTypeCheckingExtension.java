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
package org.openrewrite.gradle.internal;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.transform.stc.ExtensionMethodNode;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;
import org.codehaus.groovy.transform.stc.TypeCheckingContext;
import org.codehaus.groovy.transform.stc.TypeCheckingExtension;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Makes a closure passed where Gradle expects an {@code Action<T>} delegate to the {@code T}, as Gradle does when it
 * runs the closure.
 */
public class GradleDelegateTypeCheckingExtension extends TypeCheckingExtension {
    /**
     * The type checker keeps a closure's delegate in a {@code DelegationMetadata}, which is package-private.
     */
    private static final @Nullable Constructor<?> DELEGATION_METADATA = delegationMetadata("constructor");
    private static final @Nullable Method DELEGATION_METADATA_TYPE = delegationMetadata("getType");

    private static final String ACTION = "org.gradle.api.Action";
    private static final ClassNode DELEGATES_TO = ClassHelper.make(DelegatesTo.class);

    public GradleDelegateTypeCheckingExtension(StaticTypeCheckingVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor);
    }

    /**
     * In {@code task copyDocs(type: Copy) { }} the closure configures a task of the given type, or a plain
     * {@code Task} without one. Gradle rewrites this form when it compiles a script, so to Groovy it is a call to an
     * unknown method {@code copyDocs}.
     */
    @Override
    public boolean beforeMethodCall(MethodCall call) {
        if (!(call instanceof MethodCallExpression) || !"task".equals(call.getMethodAsString()) ||
            !((MethodCallExpression) call).isImplicitThis() || !(call.getArguments() instanceof ArgumentListExpression)) {
            return false;
        }
        List<Expression> taskArguments = ((ArgumentListExpression) call.getArguments()).getExpressions();
        if (taskArguments.size() != 1 || !(taskArguments.get(0) instanceof MethodCallExpression)) {
            return false;
        }
        MethodCallExpression definition = (MethodCallExpression) taskArguments.get(0);
        if (!definition.isImplicitThis() || !(definition.getArguments() instanceof ArgumentListExpression)) {
            return false;
        }
        ClassNode type = null;
        for (Expression argument : ((ArgumentListExpression) definition.getArguments()).getExpressions()) {
            if (argument instanceof MapExpression) {
                for (MapEntryExpression option : ((MapExpression) argument).getMapEntryExpressions()) {
                    if ("type".equals(option.getKeyExpression().getText()) &&
                        option.getValueExpression() instanceof ClassExpression) {
                        type = option.getValueExpression().getType();
                    }
                }
            } else if (argument instanceof ClosureExpression) {
                delegateTo((ClosureExpression) argument, type == null ? scriptTaskType() : type);
            }
        }
        return false;
    }

    /**
     * In a block configuring a container, Gradle treats {@code main { }} as configuring the element named {@code main},
     * and {@code maven(MavenPublication) { }} as creating one of the given type. No method declares either name, so
     * the call stays untyped, but the closure delegates to the element.
     */
    @Override
    public List<MethodNode> handleMissingMethod(ClassNode receiver, String name, @Nullable ArgumentListExpression argumentList,
                                                ClassNode @Nullable [] argumentTypes, @Nullable MethodCall call) {
        if (!(call instanceof MethodCallExpression) || !((MethodCallExpression) call).isImplicitThis() || argumentList == null) {
            return Collections.emptyList();
        }
        List<Expression> arguments = argumentList.getExpressions();
        ClassNode container = arguments.isEmpty() ? null : enclosingDelegate();
        if (container == null) {
            return Collections.emptyList();
        }
        if (supertype(container, "org.gradle.api.artifacts.dsl.DependencyHandler") != null) {
            return everyEnclosingClosureHasADelegate() ? configurationMethod(container, name, arguments) : Collections.emptyList();
        }
        if (arguments.size() > 2 || !(arguments.get(arguments.size() - 1) instanceof ClosureExpression)) {
            return Collections.emptyList();
        }
        ClassNode element = null;
        if (arguments.size() == 2) {
            if (arguments.get(0) instanceof ClassExpression &&
                supertype(container, "org.gradle.api.PolymorphicDomainObjectContainer") != null) {
                element = arguments.get(0).getType();
            }
        } else {
            ClassNode declaration = supertype(container, "org.gradle.api.NamedDomainObjectContainer");
            ClassNode parameterized = declaration == null ? null : GenericsUtils.findParameterizedType(declaration, container);
            GenericsType[] generics = parameterized == null ? null : parameterized.getGenericsTypes();
            if (generics != null && generics.length == 1 && !generics[0].getType().isGenericsPlaceHolder()) {
                element = generics[0].getType();
            }
        }
        delegateTo((ClosureExpression) arguments.get(arguments.size() - 1), element);
        return Collections.emptyList();
    }

    /**
     * Gradle adds {@code optional 'g:a:v'} in a {@code dependencies} block to the configuration named
     * {@code optional}. The method has the shape of the base script's {@code implementation}, so that a configuration
     * the base script does not name is a dependency declaration all the same.
     */
    private static List<MethodNode> configurationMethod(ClassNode dependencyHandler, String name, List<Expression> arguments) {
        boolean configured = arguments.get(arguments.size() - 1) instanceof ClosureExpression;
        for (MethodNode implementation : dependencyHandler.getMethods("implementation")) {
            Parameter[] parameters = implementation.getParameters();
            boolean takesClosure = parameters.length == 2 && "groovy.lang.Closure".equals(parameters[1].getType().getName());
            if (takesClosure == configured) {
                MethodNode method = new MethodNode(name, implementation.getModifiers(), implementation.getReturnType(),
                        parameters, implementation.getExceptions(), null);
                method.setDeclaringClass(implementation.getDeclaringClass());
                return Collections.singletonList(method);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Inside a block of a plugin the classpath lacks, such as {@code dependencyManagement { dependencies { } }}, the
     * type checker resolves {@code dependencies} on the script, though Gradle resolves it on the plugin's extension.
     */
    private boolean everyEnclosingClosureHasADelegate() {
        for (TypeCheckingContext.EnclosingClosure closure : typeCheckingVisitor.getTypeCheckingContext().getEnclosingClosureStack()) {
            if (closure.getClosureExpression().getNodeMetaData(StaticTypesMarker.DELEGATION_METADATA) == null) {
                return false;
            }
        }
        return true;
    }

    private @Nullable ClassNode enclosingDelegate() {
        TypeCheckingContext.EnclosingClosure closure = typeCheckingVisitor.getTypeCheckingContext().getEnclosingClosure();
        Object delegation = closure == null ? null :
                closure.getClosureExpression().getNodeMetaData(StaticTypesMarker.DELEGATION_METADATA);
        if (delegation == null || DELEGATION_METADATA_TYPE == null) {
            return null;
        }
        try {
            return (ClassNode) DELEGATION_METADATA_TYPE.invoke(delegation);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static @Nullable ClassNode supertype(ClassNode type, String name) {
        if (name.equals(type.getName())) {
            return type.redirect();
        }
        for (ClassNode supertype : type.getInterfaces()) {
            ClassNode found = supertype(supertype, name);
            if (found != null) {
                return found;
            }
        }
        return type.getSuperClass() == null ? null : supertype(type.getSuperClass(), name);
    }

    @Override
    public void onMethodSelection(Expression expression, MethodNode target) {
        if (!(expression instanceof MethodCallExpression) || target instanceof ExtensionMethodNode ||
            !(((MethodCallExpression) expression).getArguments() instanceof ArgumentListExpression)) {
            return;
        }
        MethodCallExpression call = (MethodCallExpression) expression;
        List<Expression> arguments = ((ArgumentListExpression) call.getArguments()).getExpressions();
        Parameter[] parameters = target.getParameters();
        for (int i = 0; i < arguments.size() && i < parameters.length; i++) {
            ClassNode action = arguments.get(i) instanceof ClosureExpression ? actionType(target, i) : null;
            if (action != null) {
                ClassNode delegate = actionTarget(call, target, arguments, action);
                if (delegate != null) {
                    delegateTo((ClosureExpression) arguments.get(i), delegate);
                }
            }
        }
    }

    /**
     * The {@code Action} type of a parameter. For a {@code Closure} parameter it comes from the overload that takes an
     * {@code Action} in its place, since Gradle runs the closure with the same delegate.
     */
    private static @Nullable ClassNode actionType(MethodNode target, int index) {
        ClassNode type = target.getParameters()[index].getType();
        if (ACTION.equals(type.getName())) {
            return type;
        }
        if (!"groovy.lang.Closure".equals(type.getName()) ||
            !target.getParameters()[index].getAnnotations(DELEGATES_TO).isEmpty()) {
            return null;
        }
        for (MethodNode overload : target.getDeclaringClass().getAllDeclaredMethods()) {
            Parameter[] parameters = overload.getParameters();
            if (overload.getName().equals(target.getName()) && parameters.length == target.getParameters().length &&
                ACTION.equals(parameters[index].getType().getName())) {
                return parameters[index].getType();
            }
        }
        return null;
    }

    private @Nullable ClassNode actionTarget(MethodCallExpression call, MethodNode target,
                                             List<Expression> arguments, ClassNode action) {
        GenericsType[] generics = action.getGenericsTypes();
        if (generics == null || generics.length != 1) {
            return null;
        }
        // Gradle declares most of these parameters as Action<? super T>. An Action<?>, as FileContentMerger.whenMerged
        // takes, names no type for the closure to delegate to.
        ClassNode type = generics[0].getLowerBound() != null ? generics[0].getLowerBound() :
                generics[0].isWildcard() ? null : generics[0].getType();
        if (type == null) {
            return null;
        }
        if (!type.isGenericsPlaceHolder()) {
            return type;
        }
        String placeholder = type.getUnresolvedName();
        ClassNode bound = classArgumentFor(placeholder, target, arguments);
        if (bound == null) {
            bound = receiverTypeArgumentFor(placeholder, call, target);
        }
        return bound == null || bound.isGenericsPlaceHolder() ? null : bound;
    }

    /**
     * The class passed for a method type variable, as {@code Copy} in {@code register('copyDocs', Copy) { }}.
     */
    private @Nullable ClassNode classArgumentFor(String placeholder, MethodNode target, List<Expression> arguments) {
        Parameter[] parameters = target.getParameters();
        for (int i = 0; i < arguments.size() && i < parameters.length; i++) {
            GenericsType[] generics = parameters[i].getType().getGenericsTypes();
            if ("java.lang.Class".equals(parameters[i].getType().getName()) && generics != null &&
                generics.length == 1 && placeholder.equals(generics[0].getName())) {
                Expression argument = arguments.get(i);
                if (argument instanceof ClassExpression) {
                    return argument.getType();
                }
                GenericsType[] argumentGenerics = getType(argument).getGenericsTypes();
                return argumentGenerics == null || argumentGenerics.length != 1 ? null : argumentGenerics[0].getType();
            }
        }
        return null;
    }

    /**
     * The receiver's type argument for a type variable of the method's class, as {@code Copy} in
     * {@code tasks.withType(Copy).configureEach { }}.
     */
    private @Nullable ClassNode receiverTypeArgumentFor(String placeholder, MethodCallExpression call, MethodNode target) {
        if (call.isImplicitThis()) {
            return null;
        }
        ClassNode declaringClass = target.getDeclaringClass();
        GenericsType[] declared = declaringClass.redirect().getGenericsTypes();
        if (declared == null) {
            return null;
        }
        ClassNode parameterized = GenericsUtils.findParameterizedType(declaringClass, getType(call.getObjectExpression()));
        GenericsType[] actual = parameterized == null ? null : parameterized.getGenericsTypes();
        if (actual == null || actual.length != declared.length) {
            return null;
        }
        for (int i = 0; i < declared.length; i++) {
            if (placeholder.equals(declared[i].getName())) {
                return actual[i].getType();
            }
        }
        return null;
    }

    /**
     * {@code Task} as the base script's {@code task(String)} returns it, already resolved against the classpath.
     */
    private @Nullable ClassNode scriptTaskType() {
        for (MethodNode task : typeCheckingVisitor.getTypeCheckingContext().getEnclosingClassNode().getMethods("task")) {
            if (task.getParameters().length == 1 && "java.lang.String".equals(task.getParameters()[0].getType().getName())) {
                return task.getReturnType();
            }
        }
        return null;
    }

    private static void delegateTo(ClosureExpression closure, @Nullable ClassNode delegate) {
        if (delegate != null && DELEGATION_METADATA != null) {
            try {
                closure.putNodeMetaData(StaticTypesMarker.DELEGATION_METADATA,
                        DELEGATION_METADATA.newInstance(delegate, Closure.DELEGATE_FIRST));
            } catch (ReflectiveOperationException ignored) {
                // the closure keeps the type checker's default delegate
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends AccessibleObject> @Nullable T delegationMetadata(String member) {
        try {
            Class<?> type = Class.forName("org.codehaus.groovy.transform.stc.DelegationMetadata");
            AccessibleObject accessible = "constructor".equals(member) ?
                    type.getDeclaredConstructor(ClassNode.class, int.class) : type.getDeclaredMethod(member);
            accessible.setAccessible(true);
            return (T) accessible;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
