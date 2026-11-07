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
package org.openrewrite.properties.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.properties.tree.Properties;

public class PropertiesTreeShapeValidationService implements TreeShapeValidationService {

    @Override
    public @Nullable Object value(Tree tree) {
        if (tree instanceof Properties.Entry) {
            Properties.Entry entry = (Properties.Entry) tree;
            return "the key `" + entry.getKey() + "` and value `" + entry.getValue().getText() + "`";
        } else if (tree instanceof Properties.Comment) {
            return "the comment `" + ((Properties.Comment) tree).getMessage().trim() + "`";
        }
        return null;
    }
}
