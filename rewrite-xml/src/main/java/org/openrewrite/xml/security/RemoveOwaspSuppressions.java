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
package org.openrewrite.xml.security;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveOwaspSuppressions extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove out-of-date OWASP suppressions";
    }

    @Override
    public String getDescription() {
        return "Remove all OWASP suppressions with a suppression end date in the past, as these are no longer valid. " +
                "For use with the OWASP `dependency-check` tool. " +
                "More details on OWASP suppression files: https://jeremylong.github.io/DependencyCheck/general/suppression.html.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveSuppressionsVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsOwaspSuppressionsFile().getVisitor();
    }

    private static class RemoveSuppressionsVisitor extends XmlIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (!new XPathMatcher("/suppressions").matches(getCursor()) || t.getContent() == null) {
                return t;
            }
            return t.withContent(ListUtils.flatMap(t.getContent(), (i, c) -> isPastDueSuppression(c) ? null : c));
        }

        private boolean isPastDueSuppression(Content content) {
            if (content instanceof Xml.Tag) {
                Xml.Tag child = (Xml.Tag) content;
                if (child.getName().equals("suppress")) {
                    for (Xml.Attribute attribute : child.getAttributes()) {
                        if (attribute.getKeyAsString().equals("until")) {
                            String maybeDate = attribute.getValueAsString();
                            if (maybeDate.endsWith("Z")) {
                                maybeDate = maybeDate.substring(0, maybeDate.length() - 1);
                            }
                            try {
                                LocalDate date = LocalDate.parse(maybeDate);
                                if (date.isBefore(LocalDate.now().minus(1, ChronoUnit.DAYS))) {
                                    return true;
                                }
                            } catch (DateTimeParseException e) {
                                return false;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }


}
