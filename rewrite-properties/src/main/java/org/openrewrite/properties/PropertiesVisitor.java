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
package org.openrewrite.properties;

import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.properties.tree.Properties;

public class PropertiesVisitor<P> extends TreeVisitor<Properties, P> {

    public Properties visitFile(Properties.File file, P p) {
        Properties.File f = visitAndCast(file, p, this::preVisit);
        return f.withContent(ListUtils.map(file.getContent(), c -> visitAndCast(c, p)));
    }

    public Properties visitEntry(Properties.Entry entry, P p) {
        return visitAndCast(entry, p, this::preVisit);
    }

    public Properties visitComment(Properties.Comment comment, P p) {
        return visitAndCast(comment, p, this::preVisit);
    }
}
