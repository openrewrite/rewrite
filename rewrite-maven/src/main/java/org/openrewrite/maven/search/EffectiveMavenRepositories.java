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
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryMirror;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.PathUtils.separatorsToUnix;

@Value
@EqualsAndHashCode(callSuper = false)
public class EffectiveMavenRepositories extends Recipe {

    @Override
    public String getDisplayName() {
        return "List effective Maven repositories";
    }

    @Override
    public String getDescription() {
        return "Lists the Maven repositories that would be used for dependency resolution, in order of precedence. " +
               "This includes Maven repositories defined in the Maven settings file (and those contributed by active profiles) as " +
               "determined when the LST was produced.";
    }

    @Option(displayName = "Use markers",
            description = "Whether to add markers for each effective Maven repository to the POM. Default `false`.",
            required = false)
    @Nullable
    Boolean useMarkers;

    transient EffectiveMavenRepositoriesTable table = new EffectiveMavenRepositoriesTable(this);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
                MavenSettings settings = mctx.effectiveSettings(mrr);

                StringJoiner repositories = new StringJoiner("\n");
                if (settings == null) {
                    for (MavenRepository repository : mrr.getPom().getRepositories()) {
                        repositories.add(repository.getUri());
                        table.insertRow(ctx, new EffectiveMavenRepositoriesTable.Row(
                                separatorsToUnix(document.getSourcePath().toString()),
                                repository.getUri()));
                    }
                    repositories.add(MavenRepository.MAVEN_CENTRAL.getUri());
                    table.insertRow(ctx, new EffectiveMavenRepositoriesTable.Row(
                            separatorsToUnix(document.getSourcePath().toString()),
                            MavenRepository.MAVEN_CENTRAL.getUri()));
                } else {
                    Collection<MavenRepositoryMirror> mirrors = mctx.getMirrors(settings);
                    List<MavenRepository> effectiveRepositories = Stream.concat(
                                    settings.getActiveRepositories(settings.getActiveProfiles() == null ? emptyList() : settings.getActiveProfiles().getActiveProfiles())
                                            .stream()
                                            .map(rawRepo -> MavenRepository.builder().uri(rawRepo.getUrl()).build()),
                                    Stream.concat(mrr.getPom().getRepositories().stream(), Stream.of(MavenRepository.MAVEN_CENTRAL)))
                            .map(repository -> MavenRepositoryMirror.apply(mirrors, repository))
                            .collect(toList());

                    for (MavenRepository repository : effectiveRepositories) {
                        repositories.add(repository.getUri());
                        table.insertRow(ctx, new EffectiveMavenRepositoriesTable.Row(
                                separatorsToUnix(document.getSourcePath().toString()),
                                repository.getUri()));
                    }
                }

                if (Boolean.TRUE.equals(useMarkers)) {
                    return SearchResult.found(document, repositories.toString());
                }

                return document;
            }
        };
    }
}
