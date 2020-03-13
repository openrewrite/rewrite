/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml.refactor;

import lombok.Getter;
import org.openrewrite.ScopedVisitorSupport;
import org.openrewrite.xml.tree.Xml;

import java.util.UUID;

public abstract class ScopedXmlRefactorVisitor extends XmlRefactorVisitor implements ScopedVisitorSupport {
    @Getter
    private final UUID scope;

    public ScopedXmlRefactorVisitor(UUID scope) {
        this.scope = scope;
    }

    @Override
    public boolean isCursored() {
        return true;
    }

    public Xml.Tag enclosingTag() {
        return getCursor().firstEnclosing(Xml.Tag.class);
    }

    public Xml.Tag enclosingRootTag() {
        return getCursor().getPathAsStream()
                .filter(t -> t instanceof Xml.Tag)
                .map(Xml.Tag.class::cast)
                .reduce((t1, t2) -> t2)
                .orElseThrow(() -> new IllegalStateException("No root tag. This operation should be called from a cursor scope that is inside of the root tag."));
    }
}
