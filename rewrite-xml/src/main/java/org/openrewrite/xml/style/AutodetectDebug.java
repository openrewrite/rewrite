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
package org.openrewrite.xml.style;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.table.XmlStyleReport;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public class AutodetectDebug extends ScanningRecipe<AutodetectDebug.Accumulator> {

    private final transient XmlStyleReport report = new XmlStyleReport(this);

    @Override
    public String getDisplayName() {
        return "XML style Auto-detection debug";
    }

    @Override
    public String getDescription() {
        return "Runs XML Autodetect and records the results in data tables and search markers. " +
               "A debugging tool for figuring out why XML documents get styled the way they do.";
    }

    public static class Accumulator {
        Autodetect.Detector overallDetector = new Autodetect.Detector();

        @Nullable
        private TabsAndIndentsStyle overallProjectStyle;
        TabsAndIndentsStyle overallProjectStyle() {
            if(overallProjectStyle == null) {
                overallProjectStyle = requireNonNull(overallDetector.build().getStyle(TabsAndIndentsStyle.class));
            }
            return overallProjectStyle;
        }
    }

    @Override
    public AutodetectDebug.Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AutodetectDebug.Accumulator acc) {
        return new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                acc.overallDetector.sample(document);
                return document;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(AutodetectDebug.Accumulator acc, ExecutionContext ctx) {
        TabsAndIndentsStyle tis = acc.overallProjectStyle();

        report.insertRow(ctx, new XmlStyleReport.Row(
                "Overall Project Style",
                tis.getUseTabCharacter(),
                tis.getIndentSize(),
                tis.getTabSize(),
                tis.getContinuationIndentSize(),
                -1,
                -1,
                -1,
                -1,
                -1,
                -1
        ));
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new XmlVisitor<ExecutionContext>() {

            @SuppressWarnings("NotNullFieldNotInitialized")
            TabsAndIndentsStyle currentDocumentStyle;
            int tagPrefixesCounted;
            int tagIndentsMatchingOwnStyle;
            int tagIndentsMatchingProjectStyle;
            int attributePrefixesCounted;
            int attributeIndentsMatchingOwnStyle;
            int attributeIndentsMatchingProjectStyle;

            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Autodetect.Detector detector = new Autodetect.Detector();
                detector.sample(document);
                currentDocumentStyle = requireNonNull(detector.build().getStyle(TabsAndIndentsStyle.class));
                super.visitDocument(document, ctx);

                report.insertRow(ctx, new XmlStyleReport.Row(
                        document.getSourcePath().toString(),
                        currentDocumentStyle.getUseTabCharacter(),
                        currentDocumentStyle.getIndentSize(),
                        currentDocumentStyle.getTabSize(),
                        currentDocumentStyle.getContinuationIndentSize(),
                        tagPrefixesCounted,
                        tagIndentsMatchingOwnStyle,
                        tagIndentsMatchingProjectStyle,
                        attributePrefixesCounted,
                        attributeIndentsMatchingOwnStyle,
                        attributeIndentsMatchingProjectStyle
                ));

                return document;
            }

            int depth = 0;
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                // Depth 0 the expected prefix is always "" for all styles
                if (depth > 0) {
                    tagPrefixesCounted++;
                    if (indentMatchesStyle(tag.getPrefix(), depth, currentDocumentStyle)) {
                        tagIndentsMatchingOwnStyle++;
                    }
                    if (indentMatchesStyle(tag.getPrefix(), depth, acc.overallProjectStyle())) {
                        tagIndentsMatchingProjectStyle++;
                    }
                }

                // Attributes have the same depth as their parent tag
                ListUtils.map(tag.getAttributes(), a -> visitAndCast(a, ctx));

                depth++;
                ListUtils.map(tag.getContent(), a -> visitAndCast(a, ctx));
                depth--;
                return tag;
            }

            @Override
            public Xml visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                // Only attributes with newlines in their prefix are continuation indented
                if (attribute.getPrefix().contains("\n")) {
                    attributePrefixesCounted++;
                    if (continuationIndentMatchesStyle(attribute.getPrefix(), depth, currentDocumentStyle)) {
                        attributeIndentsMatchingOwnStyle++;
                    }
                    if (continuationIndentMatchesStyle(attribute.getPrefix(), depth, acc.overallProjectStyle())) {
                        attributeIndentsMatchingProjectStyle++;
                    }
                }
                return attribute;
            }
        };
    }

    private static boolean indentMatchesStyle(String prefix, int depth, TabsAndIndentsStyle s) {
        String lastLineOfPrefix = prefix.substring(prefix.lastIndexOf("\n") + 1);
        String expectedPrefix = (s.getUseTabCharacter()) ?
                StringUtils.repeat("\t", depth) :
                StringUtils.repeat(" ", s.getIndentSize() * depth);
        return lastLineOfPrefix.equals(expectedPrefix);
    }

    private static boolean continuationIndentMatchesStyle(String prefix, int depth, TabsAndIndentsStyle s) {
        String lastLineOfPrefix = prefix.substring(prefix.lastIndexOf("\n") + 1);
        String expectedPrefix = (s.getUseTabCharacter()) ?
                StringUtils.repeat("\t", depth) + StringUtils.repeat("\t", s.getContinuationIndentSize()) :
                StringUtils.repeat(" ", s.getIndentSize() * depth) + StringUtils.repeat(" ", s.getContinuationIndentSize());
        return lastLineOfPrefix.equals(expectedPrefix);
    }
}
