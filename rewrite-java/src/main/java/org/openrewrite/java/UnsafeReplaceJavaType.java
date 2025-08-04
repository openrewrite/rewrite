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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.JavaType;

import java.util.IdentityHashMap;
import java.util.Set;

import static java.util.Collections.newSetFromMap;

@Incubating(since = "7.17.0")
@Value
@EqualsAndHashCode(callSuper = false)
class UnsafeReplaceJavaType extends UnsafeJavaTypeVisitor<Integer> {
    JavaType replace;
    JavaType replaceWith;

    Set<JavaType> stack = newSetFromMap(new IdentityHashMap<>());

    @Override
    public @Nullable JavaType visit(@Nullable JavaType javaType, Integer p) {
        if (javaType == null) {
            //noinspection ConstantConditions
            return null;
        }

        if (javaType == replace) {
            return replaceWith;
        }

        if (stack.add(javaType)) {
            return super.visit(javaType, p);
        }
        return javaType;
    }
}
