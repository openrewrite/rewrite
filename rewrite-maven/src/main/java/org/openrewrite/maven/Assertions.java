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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.xml.tree.Xml;
import org.opentest4j.AssertionFailedError;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_LOCAL_DEFAULT;

public class Assertions {
    private Assertions() {
    }

    static void customizeExecutionContext(ExecutionContext ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        boolean nothingConfigured = mctx.getSettings() == null &&
                                    mctx.getLocalRepository().equals(MAVEN_LOCAL_DEFAULT) &&
                                    mctx.getRepositories().isEmpty() &&
                                    mctx.getActiveProfiles().isEmpty() &&
                                    mctx.getMirrors().isEmpty();
        if (nothingConfigured) {
            // Load Maven settings to pick up any mirrors, proxies etc. that might be required in corporate environments
            mctx.setMavenSettings(MavenSettings.readMavenSettingsFromDisk(mctx));
        }
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before) {
        return pomXml(before, s -> {
        });
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", MavenParser.builder(), before,
                Assertions::pomResolvedSuccessfully, Assertions::customizeExecutionContext);
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
                Assertions::pomResolvedSuccessfully, Assertions::customizeExecutionContext).after(s -> after);
        maven.path("pom.xml");
        spec.accept(maven);
        return maven;
    }

    private static SourceFile pomResolvedSuccessfully(SourceFile sourceFile, TypeValidation typeValidation) {
        if (typeValidation.dependencyModel()) {
            sourceFile.getMarkers()
                    .findFirst(ParseExceptionResult.class)
                    .ifPresent(parseExceptionResult -> fail("Problem parsing " + sourceFile.getSourcePath() + ":\n" + parseExceptionResult.getMessage()));
            sourceFile.getMarkers()
                    .findFirst(MavenResolutionResult.class)
                    .orElseThrow(() -> new AssertionFailedError("No MavenResolutionResult found on " + sourceFile.getSourcePath()));
        }
        return sourceFile;
    }
}
