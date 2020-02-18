/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite;

import java.lang.annotation.*;

/**
 * Marks a method as defining a refactoring rule. Library authors should use this to annotate methods
 * that define refactoring operations and release with new versions of their libraries. Build tool
 * plugins then scan for the annotation in the classpath and apply the operations to the codebase.
 *
 * Methods marked with @Rewrite should be public static and return a <pre>List{@literal <}RefactorVisitor></pre>.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Rewrite {
    String value();
    String description();
}
