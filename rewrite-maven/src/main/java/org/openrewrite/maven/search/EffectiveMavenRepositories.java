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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

import java.util.StringJoiner;

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

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                StringJoiner repositories = new StringJoiner("\n");
                for (MavenRepository repository : mrr.getPom().getRepositories()) {
                    repositories.add(repository.getUri());
                }
                for (MavenRepository repository : MavenExecutionContextView.view(ctx)
                        .getRepositories(mrr.getMavenSettings(), mrr.getActiveProfiles())) {
                    repositories.add(repository.getUri());
                }
                repositories.add(MavenRepository.MAVEN_CENTRAL.getUri());
                return SearchResult.found(document, repositories.toString());
            }
        };
    }
}
