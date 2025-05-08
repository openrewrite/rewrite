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
package org.openrewrite.java.tree;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.StringAssert;
import org.openrewrite.java.JavaIsoVisitor;

import java.util.*;

class TypeUtilsAssertions extends AutoCloseableSoftAssertions {
    Map<String, List<JavaType>> types = new HashMap<>();

    public TypeUtilsAssertions(J.CompilationUnit cu) {
        EnumSet.complementOf(EnumSet.of(JavaType.Primitive.String, JavaType.Primitive.None))
          .forEach(e -> types.put(e.getKeyword(), new ArrayList<>(Collections.singletonList(e))));
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer o) {
                if (multiVariable.getTypeExpression() != null) {
                    String type = multiVariable.getTypeExpression().printTrimmed(getCursor());
                    types.computeIfAbsent(type, k -> new ArrayList<>(2))
                      .add(Objects.requireNonNull(multiVariable.getTypeExpression().getType()));
                }
                for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                    types.computeIfAbsent(variable.getSimpleName(), k -> new ArrayList<>(2))
                      .add(Objects.requireNonNull(variable.getVariableType()));
                }
                return super.visitVariableDeclarations(multiVariable, o);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer o) {
                String type = method.printTrimmed(getCursor());
                types.computeIfAbsent(type, k -> new ArrayList<>(2))
                  .add(Objects.requireNonNull(method.getMethodType()));
                return super.visitMethodInvocation(method, o);
            }
        }.visit(cu, 0);
    }

    public ObjectAssert<JavaType> type(String type) {
        JavaType javaType = getFirst(type);
        return assertThat(javaType);
    }

    public BooleanAssert isAssignableTo(String to, String from) {
        return isAssignableTo(to, from, false);
    }

    public BooleanAssert isAssignableTo(String to, String from, boolean capture) {
        JavaType toType = getFirst(to);
        JavaType fromType = getLast(from);
        return assertThat(TypeUtils.isAssignableTo(toType, fromType))
          .describedAs("isAssignableTo(%s, %s, %s)", to, from, capture);
    }

    public BooleanAssert isOfType(String to, String from) {
        return isOfType(to, from, false);
    }

    public BooleanAssert isOfType(String to, String from, boolean capture) {
        JavaType toType = getFirst(to);
        JavaType fromType = getLast(from);
        return assertThat(TypeUtils.isOfType(toType, fromType))
          .describedAs("isOfType(%s, %s, %s)", to, from, capture);
    }

    public StringAssert toString(String type) {
        JavaType javaType = getFirst(type);
        return assertThat(TypeUtils.toString(javaType))
          .describedAs("toString(%s)", type);
    }

    public StringAssert toGenericTypeString(String type) {
        JavaType javaType = getFirst(type);
        return assertThat(TypeUtils.toGenericTypeString((JavaType.GenericTypeVariable) javaType))
          .describedAs("toGenericTypeString(%s)", type);
    }

    private JavaType getFirst(String type) {
        return Optional.ofNullable(types.get(type))
          .flatMap(list -> list.stream().findFirst())
          .orElseThrow(() -> new IllegalArgumentException("Type not found: " + type));
    }

    private JavaType getLast(String type) {
        return Optional.ofNullable(types.get(type))
          .map(list -> list.get(list.size() - 1))
          .orElseThrow(() -> new IllegalArgumentException("Type not found: " + type));
    }
}
