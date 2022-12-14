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
package org.openrewrite.xml;

import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.xml.tree.Xml;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {
    }

    public static SourceSpecs xml(@Language("xml") @Nullable String before) {
        return xml(before, s -> {
        });
    }

    public static SourceSpecs xml(@Language("xml") @Nullable String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> xml = new SourceSpec<>(Xml.Document.class, null, XmlParser.builder(), before, null);
        spec.accept(xml);
        return xml;
    }

    public static SourceSpecs xml(@Language("xml") @Nullable String before, @Language("xml") @Nullable String after) {
        return xml(before, after, s -> {
        });
    }

    public static SourceSpecs xml(@Language("xml") @Nullable String before, @Language("xml") @Nullable String after,
                             Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> xml = new SourceSpec<>(Xml.Document.class, null, XmlParser.builder(), before, s -> after);
        spec.accept(xml);
        return xml;
    }
}
