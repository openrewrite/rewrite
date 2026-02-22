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
package org.openrewrite.config;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Indicates that a recipe exposes a list of recipes used as preconditions.
 * This is purely informational and not taken into consideration by recipe execution.
 * Imperative recipes interact with preconditions as implementation details of their visitor(s).
 * There is no reason for an imperative recipe to implement this, it will not affect the behavior of the recipe in any way.
 */
public interface RecipePreconditions {
    List<Recipe> getPreconditions();
}
