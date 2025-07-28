/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.xml.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

import static org.openrewrite.Preconditions.not;

@Value
@EqualsAndHashCode(callSuper = false)
public class DoesNotUseNamespaceUri extends Recipe {

    @Option(displayName = "Namespace URI",
            description = "The Namespace URI to check.",
            example = "http://www.w3.org/2001/XMLSchema-instance")
    String namespaceUri;

    @Override
    public String getDisplayName() {
        return "Find files without Namespace URI";
    }

    @Override
    public String getDescription() {
        return "Find XML root elements that do not have a specific Namespace URI, optionally restricting the search by an XPath expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
            return not(new HasNamespaceUri(namespaceUri, null).getVisitor());
    }
}
