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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveCatchOfExceptionNoLongerThrown extends Recipe {

    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_INVOCATIONS_DESCRIPTION,
            example = "org.springframework.test.util.JsonPathExpectationsHelper *(..)")
    String methodPattern;

    @Option(displayName = "Exception type",
            description = "The fully qualified name of the checked exception the methods no longer declare.",
            example = "java.text.ParseException")
    String exceptionType;

    String displayName = "Remove `catch` of an exception that methods no longer throw";

    String description = "Removes a `catch` of a checked exception from `try` statements whose body throws it only from calls " +
            "of the given methods, for a new version of their library that no longer declares it. Runs on code still " +
            "compiled against the old version, where those calls declare the exception, so the methods are found by the " +
            "method pattern. A `catch` stays where another call, a `throw` or the `close()` of a try-with-resources " +
            "resource can still throw the exception. A `try` left with no `catch`, `finally` or resources is replaced " +
            "by its body, as a block when the body declares local variables.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher matcher = new MethodMatcher(methodPattern, true);
        return Preconditions.check(new UsesMethod<>(matcher), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitTry(J.Try tryable, ExecutionContext ctx) {
                J.Try t = (J.Try) super.visitTry(tryable, ctx);
                JavaType.FullyQualified caught = caughtType(t);
                if (caught == null) {
                    return t;
                }
                ExceptionSources sources = ExceptionSources.of(t, matcher, caught);
                if (!sources.matchingCalls || sources.others) {
                    return t;
                }
                if (completesOnlyThroughCatch(t)) {
                    return t;
                }
                maybeRemoveImport(exceptionType);
                J.Try withoutCatch = t.withCatches(ListUtils.map(t.getCatches(), this::withoutException));
                if (isBareTry(withoutCatch) && !(getCursor().getParentTreeCursor().getValue() instanceof J.Block)) {
                    // the statement position of a braceless `if`, a loop or a `case` takes a block, not its statements
                    return withoutCatch.getBody().withPrefix(t.getPrefix());
                }
                return withoutCatch;
            }

            @Override
            public J visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = (J.Block) super.visitBlock(block, ctx);
                return b.withStatements(ListUtils.flatMap(b.getStatements(), statement -> {
                    if (!(statement instanceof J.Try) || !isBareTry((J.Try) statement)) {
                        return statement;
                    }
                    J.Block body = ((J.Try) statement).getBody();
                    if (declaresLocal(body)) {
                        // a block keeps the locals of the `try` out of the enclosing scope, where a later declaration may reuse their names
                        return body.withPrefix(statement.getPrefix());
                    }
                    return ListUtils.map(
                            ListUtils.mapFirst(body.getStatements(), first -> first.withComments(ListUtils.concatAll(statement.getComments(), first.getComments()))),
                            lifted -> autoFormat(lifted, ctx, getCursor()));
                }));
            }

            /**
             * Whether the statements after the `try` can be reached only through a `catch`, because its body ends with a
             * `return`, `throw`, `break` or `continue`: without the `catch` they would be unreachable.
             */
            private boolean completesOnlyThroughCatch(J.Try t) {
                List<Statement> statements = t.getBody().getStatements();
                if (statements.isEmpty() || t.getFinally() != null) {
                    return false;
                }
                Statement last = statements.get(statements.size() - 1);
                if (!(last instanceof J.Return || last instanceof J.Throw || last instanceof J.Break || last instanceof J.Continue)) {
                    return false;
                }
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (!(parent instanceof J.Block)) {
                    return true;
                }
                List<Statement> siblings = ((J.Block) parent).getStatements();
                for (int i = 0; i < siblings.size(); i++) {
                    if (siblings.get(i).getId().equals(t.getId())) {
                        return i < siblings.size() - 1;
                    }
                }
                return true;
            }

            /**
             * The exception as one of the try's catches names it, which carries its type hierarchy.
             */
            private JavaType.@Nullable FullyQualified caughtType(J.Try t) {
                for (J.Try.Catch c : t.getCatches()) {
                    for (JavaType type : exceptionTypes(c)) {
                        if (TypeUtils.isOfClassType(type, exceptionType)) {
                            return TypeUtils.asFullyQualified(type);
                        }
                    }
                }
                return null;
            }

            /**
             * Removes the exception from the `catch`: the whole `catch` when it names only the exception, otherwise
             * the alternative of the multi-catch.
             */
            private J.Try.@Nullable Catch withoutException(J.Try.Catch c) {
                J.VariableDeclarations parameter = c.getParameter().getTree();
                if (!(parameter.getTypeExpression() instanceof J.MultiCatch)) {
                    return TypeUtils.isOfClassType(parameter.getType(), exceptionType) ? null : c;
                }
                J.MultiCatch multiCatch = (J.MultiCatch) parameter.getTypeExpression();
                List<JRightPadded<NameTree>> alternatives = multiCatch.getPadding().getAlternatives();
                List<JRightPadded<NameTree>> kept = ListUtils.filter(alternatives,
                        alternative -> !TypeUtils.isOfClassType(alternative.getElement().getType(), exceptionType));
                if (kept.size() == alternatives.size()) {
                    return c;
                }
                if (kept.size() == 1 && kept.get(0).getElement() instanceof TypeTree) {
                    TypeTree single = (TypeTree) kept.get(0).getElement();
                    return withType(c, single.withPrefix(multiCatch.getPrefix()), single.getType());
                }
                kept = ListUtils.mapLast(ListUtils.mapFirst(kept,
                                first -> first.withElement(first.getElement().withPrefix(alternatives.get(0).getElement().getPrefix()))),
                        last -> last.withAfter(alternatives.get(alternatives.size() - 1).getAfter()));
                return withType(c, multiCatch.getPadding().withAlternatives(kept), commonSuperclass(kept));
            }
        });
    }

    /**
     * The `catch` with fewer alternatives in its multi-catch, whose type is then the type of its parameter and of
     * the parameter's uses in the body.
     */
    private static J.Try.Catch withType(J.Try.Catch c, TypeTree typeExpression, @Nullable JavaType type) {
        J.VariableDeclarations parameter = c.getParameter().getTree();
        J.VariableDeclarations.NamedVariable variable = parameter.getVariables().get(0);
        JavaType.Variable oldVariableType = variable.getVariableType();
        J.VariableDeclarations.NamedVariable typed = variable.withType(type);
        JavaType.Variable newVariableType = typed.getVariableType();
        J.Block body = oldVariableType == null ? c.getBody() : (J.Block) new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                return identifier.getFieldType() == oldVariableType ?
                        identifier.withType(type).withFieldType(newVariableType) : identifier;
            }
        }.visitNonNull(c.getBody(), 0);
        return c.withBody(body).withParameter(c.getParameter().withTree(parameter
                .withTypeExpression(typeExpression)
                .withVariables(ListUtils.map(parameter.getVariables(), v -> v == variable ?
                        typed.withName(typed.getName().withType(type).withFieldType(newVariableType)) : v))));
    }

    /**
     * The type a multi-catch parameter has: the nearest class all its alternatives extend.
     */
    private static @Nullable JavaType commonSuperclass(List<JRightPadded<NameTree>> alternatives) {
        for (JavaType.FullyQualified candidate = TypeUtils.asFullyQualified(alternatives.get(0).getElement().getType());
             candidate != null; candidate = candidate.getSupertype()) {
            JavaType.FullyQualified superclass = candidate;
            if (alternatives.stream().allMatch(alternative -> TypeUtils.isAssignableTo(superclass, alternative.getElement().getType()))) {
                return superclass;
            }
        }
        return null;
    }

    private static List<JavaType> exceptionTypes(J.Try.Catch c) {
        List<JavaType> types = new ArrayList<>();
        TypeTree typeExpression = c.getParameter().getTree().getTypeExpression();
        if (typeExpression instanceof J.MultiCatch) {
            for (NameTree alternative : ((J.MultiCatch) typeExpression).getAlternatives()) {
                types.add(alternative.getType());
            }
        } else if (typeExpression != null) {
            types.add(typeExpression.getType());
        }
        return types;
    }

    private static boolean declaresLocal(J.Block body) {
        return body.getStatements().stream().anyMatch(s -> s instanceof J.VariableDeclarations || s instanceof J.ClassDeclaration);
    }

    private static boolean isBareTry(J.Try t) {
        return t.getCatches().isEmpty() && t.getFinally() == null && t.getResources() == null;
    }
}
