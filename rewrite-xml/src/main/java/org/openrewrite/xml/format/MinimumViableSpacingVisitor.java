/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.xml.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

public class MinimumViableSpacingVisitor<P> extends XmlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public MinimumViableSpacingVisitor() {
        this(null);
    }

    public MinimumViableSpacingVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Xml.Attribute visitAttribute(Xml.Attribute attribute, P p) {
        Xml.Attribute a = super.visitAttribute(attribute, p);
        if (a.getPrefix().isEmpty()) {
            return a.withPrefix(" ");
        }
        return a;
    }

    @Override
    public @Nullable Xml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Xml) tree;
        }
        return super.visit(tree, p);
    }

    @Override
    public @Nullable Xml postVisit(Xml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }
}
