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
package org.openrewrite.text;

import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;

public class PlainTextVisitor<P> extends TreeVisitor<Tree, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof PlainText;
    }

    public boolean isAdaptableTo(@SuppressWarnings("rawtypes") Class<? extends TreeVisitor> adaptTo) {
        if (adaptTo.isAssignableFrom(getClass())) {
            return true;
        }
        Class<? extends Tree> theirs = visitorTreeType(adaptTo);
        return theirs.equals(PlainText.class) || theirs.equals(PlainText.Snippet.class);
    }

    public PlainText visitText(PlainText text, P p) {
        PlainText t = text;
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withSnippets(ListUtils.map(t.getSnippets(), s -> visitAndCast(s, p)));
        return t;
    }

    public PlainText.Snippet visitSnippet(PlainText.Snippet snippet, P p) {
        return snippet.withMarkers(visitMarkers(snippet.getMarkers(), p));
    }
}
