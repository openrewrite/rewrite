/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.style.HiddenFieldStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = true)
@Incubating(since = "7.6.0")
public class HiddenFieldVisitor<P> extends JavaIsoVisitor<P> {
    private static final Pattern NEXT_NAME_PATTERN = Pattern.compile("(.+)(\\d+)");
    HiddenFieldStyle style;

    /**
     * Returns either the current block or a J.Type that may create a reference to a variable.
     * I.E. for(int target = 0; target < N; target++) creates a new name scope for `target`.
     * The name scope in the next J.Block `{}` cannot create new variables with the name `target`.
     * <p>
     * J.* types that may only reference an existing name and do not create a new name scope are excluded.
     * <p>
     * Kindly borrowed from {@link RenameVariable}
     */
    private static Cursor getCursorToParentScope(Cursor cursor) {
        return cursor.dropParentUntil(is ->
                is instanceof J.Block ||
                        is instanceof J.MethodDeclaration ||
                        is instanceof J.ForLoop ||
                        is instanceof J.ForEachLoop ||
                        is instanceof J.Case ||
                        is instanceof J.Try ||
                        is instanceof J.Try.Catch ||
                        is instanceof J.MultiCatch ||
                        is instanceof J.Lambda
        );
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        List<J.VariableDeclarations.NamedVariable> classFields = classDecl.getBody().getStatements().stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast)
                .flatMap(vd -> vd.getVariables().stream())
                .collect(Collectors.toList());

        classFields.forEach(cf -> FindNameShadows.find(classDecl, cf, classDecl, style)
                .forEach(shadow -> doAfterVisit(new RenameShadowedName<>(shadow, style))));

        return super.visitClassDeclaration(classDecl, p);
    }

    private static class FindExistingVariableDeclarations extends JavaIsoVisitor<Set<J.VariableDeclarations.NamedVariable>> {
        private final Cursor childTargetReference;
        private final String childTargetName;

        private FindExistingVariableDeclarations(Cursor childTargetReference, String childTargetName) {
            this.childTargetReference = childTargetReference;
            this.childTargetName = childTargetName;
        }

        /**
         * In the context of {@link HiddenFieldVisitor}, this is used to determine whether there is an existing variable definition
         * within the same name scope as the provided {@param childTargetReference}. This ensures that when we want to increment
         * the name of a variable we're renaming, we aren't renaming it to something that will cause a name collision with existing variable declarations.
         *
         * @param j                    The subtree to search.
         * @param childTargetReference The location of the variable declaration of our original search target.
         * @param childTargetName      The name of the {@param childTargetReference} we'd like to see if anything exists.
         * @return A set of existing variable definition of the {@param childTargetName} within the same name scope as the {@param childTargetName}.
         */
        public static Set<J.VariableDeclarations.NamedVariable> find(J j, Cursor childTargetReference, String childTargetName) {
            Set<J.VariableDeclarations.NamedVariable> references = new LinkedHashSet<>();
            new FindExistingVariableDeclarations(childTargetReference, childTargetName).visit(j, references);
            return references;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<J.VariableDeclarations.NamedVariable> ctx) {
            if (variable.getSimpleName().equals(childTargetName) && isInSameNameScope(getCursor(), childTargetReference)) {
                ctx.add(variable);
            }
            return super.visitVariable(variable, ctx);
        }
    }

    private static class RenameShadowedName<P> extends JavaIsoVisitor<P> {
        private final J.VariableDeclarations.NamedVariable targetVariable;
        private final HiddenFieldStyle hiddenFieldStyle;

        public RenameShadowedName(J.VariableDeclarations.NamedVariable targetVariable, HiddenFieldStyle hiddenFieldStyle) {
            this.targetVariable = targetVariable;
            this.hiddenFieldStyle = hiddenFieldStyle;
        }

        private static String nextName(String name) {
            Matcher nameMatcher = NEXT_NAME_PATTERN.matcher(name);
            return nameMatcher.matches() ?
                    nameMatcher.group(1) + (Integer.parseInt(nameMatcher.group(2)) + 1) :
                    name + "1";
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
            if (v.isScope(targetVariable)) {
                String nextName = nextName(v.getSimpleName());
                JavaSourceFile enclosingCU = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
                Cursor parentScope = getCursorToParentScope(getCursor());
                J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass == null) {
                    return v;
                }
                while (// don't use a variable name of any existing variable "downstream" of the renamed variable's scope
                        !FindNameShadows.find(parentScope.getValue(), v.withName(v.getName().withSimpleName(nextName)), enclosingClass, hiddenFieldStyle).isEmpty() ||
                                // don't use a variable name of any existing variables already defined in the "upstream" cursor path of the renamed variable's scope
                                !FindExistingVariableDeclarations.find(enclosingCU, getCursor(), nextName).isEmpty()
                ) {
                    nextName = nextName(nextName);
                }
                doAfterVisit(new RenameVariable<>(v, nextName));
                if (parentScope.getValue() instanceof J.MethodDeclaration) {
                    Optional<J.VariableDeclarations> variableParameter = ((J.MethodDeclaration) parentScope.getValue()).getParameters().stream()
                            .filter(it -> it instanceof J.VariableDeclarations)
                            .map(it -> (J.VariableDeclarations) it)
                            .filter(it -> it.getVariables().contains(v))
                            .findFirst();
                    if (variableParameter.isPresent()) {
                        doAfterVisit(new RenameJavaDocParamNameVisitor<>(parentScope.getValue(), v.getSimpleName(), nextName));
                    }
                }
            }
            return v;
        }

    }

    private static class FindNameShadows extends JavaIsoVisitor<Set<J.VariableDeclarations.NamedVariable>> {
        private final J.VariableDeclarations.NamedVariable targetVariable;
        private final J.ClassDeclaration targetVariableEnclosingClass;
        private final HiddenFieldStyle hiddenFieldStyle;

        public FindNameShadows(J.VariableDeclarations.NamedVariable targetVariable, J.ClassDeclaration targetVariableEnclosingClass, HiddenFieldStyle hiddenFieldStyle) {
            this.targetVariable = targetVariable;
            this.targetVariableEnclosingClass = targetVariableEnclosingClass;
            this.hiddenFieldStyle = hiddenFieldStyle;
        }

        /**
         * Find {@link J.VariableDeclarations.NamedVariable} definitions within the searched tree which "hide" the target variable definition
         * from an outer tree. Specifically, used to find local variables or method parameters which shadow a class field.
         *
         * @param j                            The subtree to search.
         * @param targetVariable               The {@link J.VariableDeclarations.NamedVariable} to identify whether any other variables shadow it.
         * @param targetVariableEnclosingClass The enclosing class of where the {@param targetVariable} is defined.
         * @param hiddenFieldStyle             The {@link HiddenFieldStyle} to use as part of search criteria.
         * @return A set representing any found {@link J.VariableDeclarations.NamedVariable} which shadow the provided {@param targetVariable}.
         */
        public static Set<J.VariableDeclarations.NamedVariable> find(J j, J.VariableDeclarations.NamedVariable targetVariable, J.ClassDeclaration targetVariableEnclosingClass, HiddenFieldStyle hiddenFieldStyle) {
            Set<J.VariableDeclarations.NamedVariable> references = new LinkedHashSet<>();
            new FindNameShadows(targetVariable, targetVariableEnclosingClass, hiddenFieldStyle).visit(j, references);
            return references;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<J.VariableDeclarations.NamedVariable> ctx) {
            // do not go into static inner classes, interfaces, or enums which have a different name scope
            if (!(classDecl.getKind().equals(J.ClassDeclaration.Kind.Type.Class)) || classDecl.hasModifier(J.Modifier.Type.Static)) {
                return classDecl;
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Set<J.VariableDeclarations.NamedVariable> ctx) {
            // do not go into static methods-- local variables of static methods don't hide instance fields
            if (method.hasModifier(J.Modifier.Type.Static)) {
                return method;
            }
            return super.visitMethodDeclaration(method, ctx);
        }

        @Override
        public J.Block visitBlock(J.Block block, Set<J.VariableDeclarations.NamedVariable> ctx) {
            // do not go into static initialization blocks-- local variables of static initializers don't hide instance fields
            if (block.isStatic()) {
                return block;
            }
            return super.visitBlock(block, ctx);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<J.VariableDeclarations.NamedVariable> ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);

            // skip if we are visiting the original target variable, or else this will consider a variable to be a shadow of itself.
            if (v.getSimpleName().equals(targetVariable.getSimpleName()) && !v.isScope(targetVariable)) {
                Tree maybeMethodDecl = getCursor()
                        .getParentTreeCursor() // J.VariableDeclarations
                        .getParentTreeCursor() // maybe J.MethodDeclaration
                        .getValue();

                boolean isIgnorableConstructorParam = hiddenFieldStyle.getIgnoreConstructorParameter();
                if (isIgnorableConstructorParam) {
                    isIgnorableConstructorParam = maybeMethodDecl instanceof J.MethodDeclaration && ((J.MethodDeclaration) maybeMethodDecl).isConstructor();
                }

                boolean isIgnorableSetter = hiddenFieldStyle.getIgnoreSetter();
                if (isIgnorableSetter &= maybeMethodDecl instanceof J.MethodDeclaration) {
                    J.MethodDeclaration md = (J.MethodDeclaration) maybeMethodDecl;

                    boolean doesSetterReturnItsClass = md.getReturnTypeExpression() != null && TypeUtils.isOfType(targetVariableEnclosingClass.getType(), md.getReturnTypeExpression().getType());
                    boolean isSetterVoid = md.getReturnTypeExpression() != null && JavaType.Primitive.Void.equals(md.getReturnTypeExpression().getType());
                    boolean doesMethodNameCorrespondToVariable = md.getSimpleName().startsWith("set") && md.getSimpleName().toLowerCase().endsWith(variable.getSimpleName().toLowerCase());
                    isIgnorableSetter = doesMethodNameCorrespondToVariable &&
                            (hiddenFieldStyle.getSetterCanReturnItsClass() ? (doesSetterReturnItsClass || isSetterVoid) : isSetterVoid);
                }

                boolean isIgnorableAbstractMethod = hiddenFieldStyle.getIgnoreAbstractMethods();
                if (isIgnorableAbstractMethod) {
                    isIgnorableAbstractMethod = maybeMethodDecl instanceof J.MethodDeclaration && ((J.MethodDeclaration) maybeMethodDecl).isAbstract();
                }

                if (!isIgnorableSetter && !isIgnorableConstructorParam && !isIgnorableAbstractMethod) {
                    ctx.add(v);
                }
            }
            return v;
        }

    }

}

