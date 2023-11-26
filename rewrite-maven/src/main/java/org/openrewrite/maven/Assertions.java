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
package org.openrewrite.maven;

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.xml.tree.Xml;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {
    }
    static void customizeExecutionContext(ExecutionContext ctx) {
        if(MavenSettings.readFromDiskEnabled()) {
            MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
            mctx.setMavenSettings(MavenSettings.readMavenSettingsFromDisk(mctx));
        }
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before) {
        return pomXml(before, s -> {
        });
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", MavenParser.builder(), before,
                SourceSpec.ValidateSource.noop, Assertions::customizeExecutionContext);
        maven.path("pom.xml");
        spec.accept(maven);
        return maven;
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before, @Language("xml") @Nullable String after) {
        return pomXml(before, after, s -> {
        });
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before, @Language("xml") @Nullable String after,
                               Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", MavenParser.builder(), before,
                SourceSpec.ValidateSource.noop, Assertions::customizeExecutionContext).after(s -> after);
        maven.path("pom.xml");
        spec.accept(maven);
        return maven;
    }

}
