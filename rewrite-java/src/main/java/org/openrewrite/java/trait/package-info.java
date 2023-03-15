/*
 * Copyright 2023 the original author or authors.
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
/**
 * Traits represent higher-level abstractions of Java syntax that are useful for writing recipes.
 * </p>
 * They offer APIs that allow users to write recipes that are more concise and easier to understand by offering
 * APIs that hide the details of the underlying {@link org.openrewrite.Cursor} navigation.
 * </p>
 * The traits are modeled after CodeQL's representation of the Java syntax and the queries and predicates it provides.
 */
@Incubating(since = "7.41.0")
@NonNullApi
package org.openrewrite.java.trait;

import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.NonNullApi;
