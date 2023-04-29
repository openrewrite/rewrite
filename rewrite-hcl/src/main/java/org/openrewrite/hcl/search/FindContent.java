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
package org.openrewrite.hcl.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.JsonPathMatcher;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindContent extends Recipe {
    @Option(
            displayName = "Content path",
            description = "A JSONPath expression specifying the block to find.",
            example = "$.provider"
    )
    String contentPath;

    @Override
    public String getDisplayName() {
        return "Find content";
    }

    @Override
    public String getDescription() {
        return "Find HCL content by path.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher pathMatcher = new JsonPathMatcher(contentPath);
        return new HclIsoVisitor<ExecutionContext>() {
            @Override
            public BodyContent visitBodyContent(BodyContent bodyContent, ExecutionContext ctx) {
                BodyContent b = super.visitBodyContent(bodyContent, ctx);
                if (pathMatcher.matches(getCursor())) {
                    return SearchResult.found(b);
                }
                return b;
            }
        };
    }
}
