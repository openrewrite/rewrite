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

import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.ScopedVisitorSupport;
import org.openrewrite.Tree;

import java.util.UUID;

public class JavaRetrieveCursorVisitor extends JavaSourceVisitor<Cursor> implements ScopedVisitorSupport {
    @Getter
    private final UUID scope;

    public JavaRetrieveCursorVisitor(UUID scope) {
        this.scope = scope;
    }

    @Override
    public boolean isCursored() {
        return true;
    }

    @Override
    public Cursor defaultTo(Tree t) {
        return null;
    }

    @Override
    public Cursor visitTree(Tree tree) {
        if (tree.getId().equals(getScope()))
            return getCursor();
        return super.visitTree(tree);
    }
}