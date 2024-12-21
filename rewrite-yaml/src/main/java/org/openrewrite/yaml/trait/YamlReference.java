/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.yaml.trait;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.trait.Reference;
import org.openrewrite.yaml.tree.Yaml;

public abstract class YamlReference implements Reference {
    @Override
    public String getValue() {
        if (getTree() instanceof Yaml.Scalar) {
            return ((Yaml.Scalar) getTree()).getValue();
        }
        throw new IllegalArgumentException("getTree() must be an Yaml.Scalar: " + getTree().getClass());
    }

    @Override
    public boolean supportsRename() {
        return true;
    }

    @Override
    public Tree rename(Renamer renamer, Cursor cursor, ExecutionContext ctx) {
        Tree tree = cursor.getValue();
        if (tree instanceof Yaml.Scalar) {
            return ((Yaml.Scalar) tree).withValue(renamer.rename(this));
        }
        throw new IllegalArgumentException("cursor.getValue() must be an Yaml.Scalar but is: " + tree.getClass());
    }

    static abstract class YamlProvider extends AbstractProvider<YamlReference> {
        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            return sourceFile instanceof Yaml.Documents;
        }
    }
}
