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

/**
 * A recipe that renames something, such as a type or a package, which a later step of a composite may still look for
 * by its old name.
 */
@Incubating(since = "8.94.0")
public interface RenamingRecipe {

    /**
     * @return The kind of name the recipe renames, such as "Java type or package". Only renames of the same kind of
     * name are compared.
     */
    String kindOfName();

    /**
     * @return The name the recipe looks for.
     */
    String oldName();

    /**
     * @return What {@code name} becomes once the recipe has run, which is {@code name} itself when the recipe leaves
     * it as it is.
     */
    String rename(String name);
}
