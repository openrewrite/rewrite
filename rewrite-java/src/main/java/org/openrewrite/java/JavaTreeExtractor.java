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
package org.openrewrite.java;

import org.openrewrite.Tree;
import org.openrewrite.TreeProcessor;
import org.openrewrite.java.tree.J;

/**
 * An abstract class that can be used to extract arbitrary information from an AST. This class provides often-used
 * boilerplate code to simplify walking over an abstract syntax tree and collecting data/information from the tree.
 *
 * The extracted data is managed via a specialized value context.
 *
 * @param <V> The type of data that will be extracted from the tree
 */
public abstract class JavaTreeExtractor<V> extends JavaIsoProcessor<JavaTreeExtractor<V>.ValueContext> {

    public V extractFromTree(J tree) {
        ValueContext context = new ValueContext(defaultExtractedValue());
        this.visit(tree, context);
        return context.getValue();
    }

    protected V defaultExtractedValue() {
        return null;
    }

    public final class ValueContext {

        private V value;

        private ValueContext(V value) {
            this.value = value;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }
}
