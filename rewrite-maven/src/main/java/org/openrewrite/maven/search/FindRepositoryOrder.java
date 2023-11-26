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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.MavenRepositoryOrder;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FindRepositoryOrder extends Recipe {
    transient MavenRepositoryOrder repositoryOrder = new MavenRepositoryOrder(this);

    @Override
    public String getDisplayName() {
        return "Maven repository order";
    }

    @Override
    public String getDescription() {
        return "Determine the order in which dependencies will be resolved for each `pom.xml` based on its defined repositories and effective `settings.xml`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();

                Map<String, MavenRepository> repositories = new LinkedHashMap<>();
                for (MavenRepository repository : mrr.getPom().getRepositories()) {
                    repositories.put(repository.getUri(), repository);
                }
                for (MavenRepository repository : MavenExecutionContextView.view(ctx)
                        .getRepositories(
                                mrr.getMavenSettings(),
                                StreamSupport.stream(mrr.getPom().getActiveProfiles().spliterator(), false)
                                        .collect(Collectors.toList())
                        )) {
                    repositories.put(repository.getUri(), repository);
                }

                int i = 0;
                for (MavenRepository repository : repositories.values()) {
                    repositoryOrder.insertRow(ctx, new MavenRepositoryOrder.Row(
                            repository.getId(),
                            repository.getUri(),
                            repository.isKnownToExist(),
                            i++
                    ));
                }

                return SearchResult.found(document, repositories.values().stream()
                        .map(MavenRepository::getUri)
                        .collect(Collectors.joining("\n")));
            }
        };
    }
}
