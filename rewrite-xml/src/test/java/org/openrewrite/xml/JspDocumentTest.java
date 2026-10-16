/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.tree.ParseError;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.format.AutoFormatVisitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JspDocumentTest {
    @Test
    void retainsOrderedTopLevelNodesWithoutSyntheticWrapper() {
        String source = "<%@ taglib prefix=\"h\" uri=\"http://java.sun.com/jsf/html\" %>\r\n" +
                        "<!-- license -->\r\n<!DOCTYPE html>\r\n" +
                        "<h:form id=\"friendlyName\"/><h:panelGroup/>\r\n<!-- tail -->\r\n";
        Xml.JspDocument document = jsp(source);
        assertThat(document.getContent()).extracting(node -> node.getClass().getSimpleName())
          .containsExactly("JspDirective", "Comment", "DocTypeDecl", "Tag", "Tag", "Comment");
        assertThat(document.getEof()).isEqualTo("\r\n");
        assertThat(document.printAll()).isEqualTo(source);
    }

    @Test
    void recipesReachEveryTopLevelElementAndPreserveFriendlyIds() {
        Xml.JspDocument document = jsp("<h:form id=\"authorIndexForm\"/><h:form id=\"questionForm\"/>");
        List<String> ids = new ArrayList<>();
        Xml.JspDocument changed = (Xml.JspDocument) new XmlIsoVisitor<List<String>>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, List<String> names) {
                names.add(tag.getAttributes().get(0).getValueAsString());
                return super.visitTag(tag, names).withName("h:panelGroup");
            }
        }.visitNonNull(document, ids);
        assertThat(ids).containsExactly("authorIndexForm", "questionForm");
        assertThat(changed.printAll())
          .isEqualTo("<h:panelGroup id=\"authorIndexForm\"/><h:panelGroup id=\"questionForm\"/>");
    }

    @Test
    void equalityChecksAllRootsAndJspCode() {
        Xml.JspDocument document = jsp("<h:form/><h:panelGroup/><% before(); %>");
        assertThat(SemanticallyEqual.areEqual(document, jsp(document.printAll()))).isTrue();
        assertThat(SemanticallyEqual.areEqual(document, jsp("<h:form/><h:panelGroup/><% after(); %>"))).isFalse();
        assertThat(SemanticallyEqual.areEqual(document, jsp("<h:form/><h:outputText/><% before(); %>"))).isFalse();
    }

    @Test
    void absoluteXPathMatchesEachTopLevelElement() {
        Xml.JspDocument document = jsp("<h:form id=\"authorIndexForm\"/><h:form id=\"questionForm\"/>");
        List<String> ids = new ArrayList<>();
        XPathMatcher matcher = new XPathMatcher("/h:form");
        new XmlVisitor<List<String>>() {
            @Override
            public Xml visitTag(Xml.Tag tag, List<String> names) {
                if (matcher.matches(getCursor())) {
                    names.add(tag.getAttributes().get(0).getValueAsString());
                }
                return super.visitTag(tag, names);
            }
        }.visit(document, ids);
        assertThat(ids).containsExactly("authorIndexForm", "questionForm");
    }

    @Test
    void formattingSupportsTheJspRootAndRemainsIdempotent() {
        String source = "<h:form id=\"authorIndexForm\"><h:outputText value=\"#{author.title}\"/></h:form>" +
                        "<h:form id=\"questionForm\"/>";
        Xml.JspDocument document = jsp(source);
        Xml.JspDocument formatted = (Xml.JspDocument) new AutoFormatVisitor<>().visitNonNull(document, 0);
        assertThat(formatted.printAll()).isNotEqualTo(source)
          .contains("id=\"authorIndexForm\"", "id=\"questionForm\"", "#{author.title}");
        Xml.JspDocument twice = (Xml.JspDocument) new AutoFormatVisitor<>().visitNonNull(formatted, 0);
        assertThat(twice.printAll()).isEqualTo(formatted.printAll());
    }

    @Test
    void parsesDirectiveInsideElementAsARealNode() {
        Xml.JspDocument document = jsp("<div><%@ include file=\"permissionsNav.jsp\" %></div>");
        Xml.Tag div = (Xml.Tag) document.getContent().get(0);
        assertThat(div.getContent()).hasSize(1);
        assertThat(div.getContent().get(0)).isInstanceOf(Xml.JspDirective.class);
        assertThat(((Xml.JspDirective) div.getContent().get(0)).getType()).isEqualTo("include");
    }

    @Test
    void scriptletsAndExpressionsRemainOrderedBeforeAndAfterMarkup() {
        Xml.JspDocument document = jsp("<% String name = \"test\"; %><div/><%= name %><% done(); %>");
        assertThat(document.getContent()).extracting(node -> node.getClass().getSimpleName())
          .containsExactly("JspScriptlet", "Tag", "JspExpression", "JspScriptlet");
    }

    @Test
    void doctypeCanBeOutputByVerbatimContent() {
        Xml.JspDocument document = jsp("<f:view><f:verbatim><!DOCTYPE html></f:verbatim><html/></f:view>");
        Xml.Tag view = (Xml.Tag) document.getContent().get(0);
        Xml.Tag verbatim = (Xml.Tag) view.getContent().get(0);
        assertThat(verbatim.getContent().get(0)).isInstanceOf(Xml.DocTypeDecl.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fragment.jsp", "fragment.jspf", "header.inc"})
    void explicitJspParserAcceptsIncludeFragments(String name) {
        assertThat(parse(JspParser.builder().build(), name, "<h:form/><h:panelGroup/>"))
          .isInstanceOf(Xml.JspDocument.class);
    }

    @Test
    void genericXmlParserDoesNotClaimArbitraryIncFiles() {
        assertThat(XmlParser.builder().build().accept(Path.of("fortran.inc"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"<div/><broken", "<div/></stray>", "<div><p></div>"})
    void neverHidesMalformedTrailingMarkup(String source) {
        SourceFile parsed = parse(JspParser.builder().build(), "page.jsp", source);
        assertThat(parsed).isInstanceOf(ParseError.class);
        assertThat(parsed.printAll()).isEqualTo(source);
    }

    @Test
    void bomAndNonAsciiTextRoundTripAsBytes() {
        String source = "\uFEFF<%@ page language=\"java\" %>\r\n<p>Été 😀</p><br>\r\n";
        SourceFile parsed = parse(JspParser.builder().build(), "page.jsp", source);
        assertThat(parsed).isInstanceOf(Xml.JspDocument.class);
        assertThat(parsed.printAllAsBytes()).isEqualTo(source.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void strictXmlStillHasOneRootAndAllowsLegalTrailingMisc() {
        String source = "<?xml version=\"1.0\"?><root/><!-- tail --><?tail x?>\n";
        SourceFile valid = parse(XmlParser.builder().build(), "page.xml", source);
        assertThat(valid).isInstanceOf(Xml.Document.class);
        assertThat(valid.printAll()).isEqualTo(source);
        assertThat(parse(XmlParser.builder().build(), "page.xml", "<root/><other/>"))
          .isInstanceOf(ParseError.class);
        assertThat(parse(XmlParser.builder().build(), "page.xml", "<root><%@ include file=\"x\" %></root>"))
          .isInstanceOf(ParseError.class);
    }

    private Xml.JspDocument jsp(String source) {
        List<Throwable> errors = new ArrayList<>();
        SourceFile result;
        try (Stream<SourceFile> parsed = JspParser.builder().build().parse(new InMemoryExecutionContext(errors::add), source)) {
            result = parsed.findFirst().orElseThrow();
        }
        assertThat(errors).isEmpty();
        assertThat(result).isInstanceOf(Xml.JspDocument.class);
        assertThat(result.printAll()).isEqualTo(source);
        return (Xml.JspDocument) result;
    }

    private SourceFile parse(Parser parser, String name, String source) {
        Parser.Input input = Parser.Input.fromString(Path.of(name), source);
        try (Stream<SourceFile> parsed = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext(t -> { }))) {
            return parsed.findFirst().orElseThrow();
        }
    }
}
