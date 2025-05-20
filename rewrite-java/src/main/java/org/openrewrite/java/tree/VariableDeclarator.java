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
package org.openrewrite.java.tree;

import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;

import java.util.List;

/**
 * A variable declarator is a variable name(s). As used in {@link NamedVariable} it forms a name and its initializer.
 * In the most common case, a variable declarator is a single name, but in the case of destructuring
 * assignments, it can be multiple names that are each assigned by the single {@link NamedVariable#getInitializer()}.
 * <p>
 * A fun note on the name: the word VariableDeclarator comes from the Typescript compiler's definition
 * of this concept, and it fits the concept well for all languages in the {@link J} grammar island.
 */
public interface VariableDeclarator extends TypedTree {
    List<J.Identifier> getNames();
}
