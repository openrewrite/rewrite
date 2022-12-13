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
import org.openrewrite.internal.lang.Nullable;
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
        Properties.File f = file;
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withContent(ListUtils.map(f.getContent(), c -> (Properties.Content) visit(c, p)));
        return f;
    }

    public Properties visitEntry(Properties.Entry entry, P p) {
        Properties.Entry e = entry;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        if (e.getValue() != null) {
            e = e.withValue(visitValue(e.getValue(), p));
        }
        return e;
    }

    //Note: Properties.Value does not currently implement Properties, so this is a bit of an outlier.
    @Nullable
    public Properties.Value visitValue(Properties.Value value, P p) {
        return value.withMarkers(visitMarkers(value.getMarkers(), p));
    }

    public Properties visitComment(Properties.Comment comment, P p) {
        Properties.Comment c = comment;
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c;
    }
}
