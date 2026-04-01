/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp;

import org.openrewrite.ExecutionContext;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

/**
 * Precondition that matches .csproj files which have an MSBuildProject marker attached.
 * Use with {@link org.openrewrite.Preconditions#check} to scope recipes to .NET projects.
 */
class FindMSBuildProject extends XmlIsoVisitor<ExecutionContext> {
    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
        if (document.getMarkers().findFirst(MSBuildProject.class).isPresent()) {
            return SearchResult.found(document);
        }
        return document;
    }
}
