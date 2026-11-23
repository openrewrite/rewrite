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
package org.openrewrite;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link Recipe} class as conceptually abstract: instantiable so that
 * concrete instances can be deserialized (e.g., via Jackson from a
 * {@code recipes.csv}/JSON reference plus property values), but not a
 * self-standing recipe that should be enumerated by classpath scanning.
 * <p>
 * Classes carrying this annotation are skipped by
 * {@link org.openrewrite.config.ClasspathScanningLoader}, so they do not appear
 * in {@code Environment.listRecipeDescriptors()} or in generated
 * {@code recipes.csv} marketplace listings. They remain reachable by name, so
 * Jackson and other explicit loaders can still construct concrete instances.
 * <p>
 * Use this when a Recipe subclass exists only as a serialization vehicle for
 * captured state (e.g., a fixed list of child recipes) and would otherwise
 * be picked up as a no-op "empty" recipe by the scanner.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AbstractRecipe {
}
