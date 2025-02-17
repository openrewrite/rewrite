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
package org.openrewrite.rpc;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * An instance that is passed to the remote by reference (i.e. for instances
 * that are referentially deduplicated in the LST).
 */
@Getter
public class Reference {
    @SuppressWarnings("AccessStaticViaInstance")
    private static final ThreadLocal<Reference> flyweight = new ThreadLocal<>()
            .withInitial(Reference::new);

    @Nullable
    private Object value;

    /**
     * @param t Any instance.
     * @return A reference wrapper, which assists the sender to know when to pass by reference
     * rather than by value.
     */
    public static Reference asRef(@Nullable Object t) {
        Reference ref = flyweight.get();
        ref.value = t;
        return ref;
    }

    /**
     * @param maybeRef A reference (or not).
     * @param <T>      The type of the value.
     * @return The value of the reference, or the value itself if it is not a reference.
     */
    public static <T> @Nullable T getValue(@Nullable Object maybeRef) {
        // noinspection unchecked
        return (T) (maybeRef instanceof Reference ? ((Reference) maybeRef).getValue() : maybeRef);
    }
}
