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
package org.openrewrite.properties;

import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.properties.tree.Properties;

public class PropertiesVisitor<P> extends TreeVisitor<Properties, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Properties.File;
    }

    @Override
    public String getLanguage() {
        return "properties";
    }

    public Properties visitFile(Properties.File file, P p) {
        return file.withContent(ListUtils.map(file.getContent(), c -> visitAndCast(c, p)));
    }

    public Properties visitEntry(Properties.Entry entry, P p) {
        return entry;
    }

    public Properties visitComment(Properties.Comment comment, P p) {
        return comment;
    }
}
