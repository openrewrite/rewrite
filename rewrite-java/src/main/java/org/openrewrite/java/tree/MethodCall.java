/*
 * Copyright 2022 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Either a {@link org.openrewrite.java.tree.J.MethodInvocation} or
 * a {@link org.openrewrite.java.tree.J.MemberReference} or
 * a {@link org.openrewrite.java.tree.J.NewClass}.
 */
public interface MethodCall extends Expression {

    @Override
    @Nullable
    JavaType getType();

    @Override
    MethodCall withType(@Nullable JavaType type);

    JavaType.@Nullable Method getMethodType();

    MethodCall withMethodType(JavaType.@Nullable Method methodType);

    List<Expression> getArguments();

    MethodCall withArguments(List<Expression> arguments);
}
