/*
 * Copyright 2024 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

public class ModernizeObsoletePoms extends Recipe {

    @Override
    public String getDisplayName() {
        return "Modernize obsolete Maven poms";
    }

    @Override
    public String getDescription() {
        return "Very old Maven poms are no longer supported by current versions of Maven. " +
               "This recipe updates poms with `<pomVersion>3</pomVersion>` to `<modelVersion>4.0.0</modelVersion>` of the Maven pom schema. " +
               "This does not attempt to upgrade old dependencies or plugins and is best regarded as the starting point of a migration rather than an end-point.";
    }

    private static final XPathMatcher POM_VERSION = new XPathMatcher("/project/pomVersion");
    private static final XPathMatcher CURRENT_VERSION = new XPathMatcher("/project/currentVersion");
    private static final XPathMatcher LOGO = new XPathMatcher("/project/organization/logo");
    private static final XPathMatcher UNIT_TEST = new XPathMatcher("/project/build/unitTest");
    private static final XPathMatcher REPOSITORY = new XPathMatcher("/project/repository");
    private static final XPathMatcher ISSUE_TRACKING_URL = new XPathMatcher("/project/issueTrackingUrl");
    private static final XPathMatcher PACKAGE = new XPathMatcher("/project/package");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return Preconditions.check(new FindSourceFiles("**/pom.xml"), new XmlIsoVisitor<ExecutionContext>() {

            @SuppressWarnings({"DataFlowIssue", "ConcatenationWithEmptyString"})
            @Override
            public  Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                Cursor c = getCursor();
                if (POM_VERSION.matches(c)) {
                    t = t.withName("modelVersion")
                            .withValue("4.0.0");
                } else if (CURRENT_VERSION.matches(c)) {
                    t = t.withName("version");
                } else if (REPOSITORY.matches(c)) {
                    if (t.getChildValue("url").isPresent()) {
                        t = autoFormat(
                                Xml.Tag.build("" +
                                              "<repositories>\n" +
                                              "    <repository>\n" +
                                              "        <id>repo</id>\n" +
                                              "        <url>" + t.getChildValue("url").get() + "</url>\n" +
                                              "    </repository>\n" +
                                              "</repositories>\n"),
                                ctx, c.getParentTreeCursor());
                    } else {
                        return null;
                    }
                } else if (ISSUE_TRACKING_URL.matches(c)) {
                    if (t.getValue().isPresent()) {
                        t = autoFormat(
                                Xml.Tag.build("" +
                                              "<issueManagement>\n" +
                                              "    <system>IssueTracker</system>\n" +
                                              "    <url>" + t.getValue().get() + "</url>\n" +
                                              "</issueManagement>\n"),
                                ctx, c.getParentTreeCursor());
                    } else {
                        return null;
                    }
                } else if (LOGO.matches(c) ||
                           UNIT_TEST.matches(c) ||
                           PACKAGE.matches(c)) {
                    return null;
                }
                return t;
            }
        });
    }
}
