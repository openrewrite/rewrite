/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;

public class SingleLineComments extends Recipe {
    @Override
    public String getDisplayName() {
        return "Single line comments begin with a whitespace";
    }

    @Override
    public String getDescription() {
        return "Write `// hi` instead of `//hi`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                return space.withComments(ListUtils.map(space.getComments(), c -> {
                    if (!c.isMultiline()) {
                        TextComment tc = (TextComment) c;
                        String commentText = tc.getText();
                        if (!commentText.isEmpty() && !commentText.startsWith(" ")) {
                            return tc.withText(" " + commentText);
                        }
                    }
                    return c;
                }));
            }
        };
    }
}
