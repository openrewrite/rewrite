/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class StreamUtils {
    // Returns a predicate suitable for use with stream().filter() that will result in a set filtered to
    // contain only items that are distinct as evaluated by keyFunc
    public static <T> Predicate<T> distinctBy(Function<? super T, ?> keyFunc) {
        Set<Object> seen = new HashSet<>();
        return t -> {
            Object it = keyFunc.apply(t);
            boolean alreadySeen = seen.contains(it);
            seen.add(it);
            return !alreadySeen;
        };
    }
}
