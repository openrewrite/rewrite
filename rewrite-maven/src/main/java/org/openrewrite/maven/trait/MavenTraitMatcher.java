/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.maven.trait;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.tree.Xml;

import java.util.concurrent.atomic.AtomicReference;

public abstract class MavenTraitMatcher<U extends Trait<?>> extends SimpleTraitMatcher<U> {

    protected @Nullable MavenResolutionResult getResolutionResult(Cursor cursor) {
        AtomicReference<MavenResolutionResult> mrr = new AtomicReference<>();
        new MavenVisitor<Integer>() {
            @Override
            public Xml visitDocument(Xml.Document document, Integer integer) {
                mrr.set(getResolutionResult());
                return document;
            }
        }.visit(cursor.firstEnclosing(SourceFile.class), 0);
        return mrr.get();
    }
}
