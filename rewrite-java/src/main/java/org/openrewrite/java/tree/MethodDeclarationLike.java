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
package org.openrewrite.java.tree;

import org.jspecify.annotations.Nullable;

/**
 * Either a {@link org.openrewrite.java.tree.J.MethodDeclaration} or a method-declaration-shaped
 * node from another language (e.g. {@code JS.ComputedPropertyMethodDeclaration}).
 * <p>
 * Implementations' {@link #withType(JavaType)} is expected to throw
 * {@link UnsupportedOperationException}; callers wishing to change the declared return type
 * must use {@link #withMethodType(JavaType.Method)} instead.
 */
public interface MethodDeclarationLike extends TypedTree {

    JavaType.@Nullable Method getMethodType();

    MethodDeclarationLike withMethodType(JavaType.@Nullable Method methodType);
}
