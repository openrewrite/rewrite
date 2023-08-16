/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.EffectiveMavenSettings;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

import java.io.UncheckedIOException;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindMavenSettings extends Recipe {
    @Option(displayName = "Existence check only",
            description = "Only record that a maven settings file exists; do not include its contents.",
            required = false)
    @Nullable
    Boolean existenceCheckOnly;

    transient EffectiveMavenSettings settings = new EffectiveMavenSettings(this);

    @Override
    public String getDisplayName() {
        return "Find effective maven settings";
    }

    @Override
    public String getDescription() {
        return "List the effective maven settings file for the current project.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XmlMapper mapper = new XmlMapper();

        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                if (mrr.getMavenSettings() != null) {
                    try {
                        settings.insertRow(ctx, new EffectiveMavenSettings.Row(
                                document.getSourcePath().toString(),
                                Boolean.TRUE.equals(existenceCheckOnly) ?
                                        "exists" :
                                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mrr.getMavenSettings())
                        ));
                    } catch (JsonProcessingException e) {
                        throw new UncheckedIOException(e);
                    }
                } else {
                    settings.insertRow(ctx, new EffectiveMavenSettings.Row(
                            document.getSourcePath().toString(),
                            ""
                    ));
                }
                return document;
            }
        };
    }
}
