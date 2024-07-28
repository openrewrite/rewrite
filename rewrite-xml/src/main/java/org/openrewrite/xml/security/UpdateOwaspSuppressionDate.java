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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.time.LocalDate;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateOwaspSuppressionDate extends Recipe {

    @Override
    public String getDisplayName() {
        return "Update OWASP suppression date bounds";
    }

    @Override
    public String getDescription() {
        return "Updates the expiration date for OWASP suppressions having a matching cve tag. " +
               "For use with the OWASP `dependency-check` tool. " +
               "More details: https://jeremylong.github.io/DependencyCheck/general/suppression.html.";
    }

    @Option(displayName = "CVE List",
            description = "Update suppressions having any of the specified CVE tags.",
            example = "CVE-2022-1234")
    List<String> cveList;

    @Option(displayName = "Until date",
            required = false,
            description = "Optional. The date to add to the suppression. Default will be 30 days from today.",
            example = "2023-01-01")
    @Nullable
    String untilDate;

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("untilDate", "Must be empty or a valid date of format yyyy-MM-dd", untilDate, date -> {
            if (date != null && !date.isEmpty()) {
                try {
                    LocalDate.parse(date);
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsOwaspSuppressionsFile(), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (new XPathMatcher("/suppressions/suppress").matches(getCursor())) {
                    boolean hasCve = false;
                    List<Xml.Tag> cveTags = t.getChildren("cve");
                    for (Xml.Tag xml : cveTags) {
                        String cveNum = xml.getValue().orElse("");
                        for (String cve : cveList) {
                            if (!StringUtils.isNullOrEmpty(cve) && cve.equals(cveNum)) {
                                hasCve = true;
                                break;
                            }
                        }
                    }
                    if (hasCve) {
                        String date = (untilDate != null && !untilDate.isEmpty()) ? untilDate : LocalDate.now().plusDays(30).toString();
                        final String zuluDate = date + "Z";
                        t = t.withAttributes(ListUtils.map(t.getAttributes(), attr -> {
                            if ("until".equals(attr.getKeyAsString())) {
                                if (!zuluDate.equals(attr.getValueAsString())) {
                                    attr = attr.withValue(attr.getValue().withValue(zuluDate));
                                }
                            }
                            return attr;
                        }));
                    }
                }
                return t;
            }
        });
    }
}
