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
package org.openrewrite.quark;

import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;

public class QuarkVisitor<P> extends TreeVisitor<Quark, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Quark;
    }

    @Override
    public String getLanguage() {
        return "other";
    }

    public Quark visitQuark(Quark quark, P p) {
        Quark q = quark;
        return q.withMarkers(visitMarkers(q.getMarkers(), p));
    }
}
