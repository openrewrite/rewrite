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
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser.Input;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openrewrite.xml.Assertions.html;
import static org.openrewrite.xml.Assertions.xml;

class HtmlParserTest implements RewriteTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "misnested.html | <div><b><i>x</b></i></div>",
      "conditional.jsp | <div><% if (x) { %><a href=\"/x\"><% } else { %><span><% } %>text<% if (x) { %></a><% } else { %></span><% } %></div>",
    })
    void toleratesMismatchedClosingTagsThatStillBalance(String path, String source) {
        rewriteRun(
          xml(source, spec -> spec.path(path))
        );
    }

    @Test
    void reportsTheMismatchBehindAnUnclosedElement() {
        SourceFile parsed = XmlParser.builder().build()
          .parseInputs(List.of(Input.fromString(Path.of("rest.html"),
            "<table><tr><td><label><details><summary>Request</summary></label></td></tr></table>")),
            null, new InMemoryExecutionContext(t -> {
          }))
          .findFirst().orElseThrow();
        assertThat(parsed.getMarkers().findFirst(ParseExceptionResult.class).orElseThrow().getMessage())
          .contains("Mismatched closing tag </label>");
    }

    @Test
    void booleanAttributes() {
        rewriteRun(
          html(
            """
              <form>
                <input type="checkbox" checked disabled>
                <input  required   name="q" readonly/>
              </form>
              """
          )
        );
    }

    @Test
    void booleanAttributeIsAnAttributeWithAnEmptyValue() {
        Xml.Document doc = (Xml.Document) XmlParser.builder().build()
          .parseInputs(List.of(Input.fromString(Path.of("a.html"), "<input checked name=q>")), null,
            new InMemoryExecutionContext(Throwable::printStackTrace))
          .findFirst().orElseThrow();
        List<Xml.Attribute> attributes = doc.getRoot().getAttributes();
        org.assertj.core.api.Assertions.assertThat(attributes.stream().map(Xml.Attribute::getKeyAsString)).containsExactly("checked", "name");
        assertThat(attributes.get(0).getValueAsString()).isEmpty();
        assertThat(attributes.get(1).getValueAsString()).isEqualTo("q");
    }

    @Test
    void booleanAttributeIsStillAnErrorInXml() {
        SourceFile parsed = XmlParser.builder().build()
          .parseInputs(List.of(Input.fromString(Path.of("a.xml"), "<input checked/>")), null,
            new InMemoryExecutionContext(t -> {
            }))
          .findFirst().orElseThrow();
        assertThat(parsed).isInstanceOf(ParseError.class);
    }

    @Test
    void unquotedAttributes() {
        rewriteRun(
          html(
            """
              <div class=main id=x-1 data-url=/a/b?c=d&e=f>
                <a href=https://example.com/>link</a>
                <p class = spaced >text</p>
              </div>
              """
          )
        );
    }

    @Test
    void lessThanInInlineScript() {
        rewriteRun(
          html(
            """
              <html>
              <head>
                <script>if (a < b && c) { x = "</p>"; }</script>
                <script type="text/javascript">
                  for (var i = 0; i < 10; i++) { if (i < 5 && i > 2) go(); }
                </script>
                <style>a > b { color: red }</style>
              </head>
              </html>
              """
          )
        );
    }

    @Test
    void lessThanInAttributeValue() {
        rewriteRun(
          html(
            """
              <button onclick="if(a<b) go()" title='x < y'>go</button>
              """
          )
        );
    }

    @Test
    void lessThanInTextarea() {
        rewriteRun(
          html(
            """
              <form><textarea name="t">a < b && c</textarea></form>
              """
          )
        );
    }

    @Test
    void lessThanInTitle() {
        rewriteRun(
          html(
            """
              <html><head><title>Find functions returning `CompletableFuture<T>`</title></head></html>
              """
          )
        );
    }

    @Test
    void loneAmpersand() {
        rewriteRun(
          html(
            """
              <p>Tom & Jerry &amp; friends &#169; &copy 2024, a < b, 1 <2 & more</p>
              <p>AT&T</p>
              """
          )
        );
    }

    @Test
    void closingTagsLeftOff() {
        rewriteRun(
          html(
            """
              <html>
              <body>
              <ul>
                <li>one
                <li>two
                <li>three <b>bold</b>
              </ul>
              <p>a
              <p>b
              <div>c</div>
              <table>
                <tr><td>1<td>2
                <tr><td>3<td>4
              </table>
              <dl><dt>t<dd>d<dt>u<dd>e</dl>
              <select><option>a<option>b</select>
            """
          )
        );
    }

    @Test
    void impliedCloseStructure() {
        Xml.Document doc = (Xml.Document) XmlParser.builder().build()
          .parseInputs(List.of(Input.fromString(Path.of("a.html"), "<ul><li>one<li>two</ul><p>a<p>b")), null,
            new InMemoryExecutionContext(Throwable::printStackTrace))
          .findFirst().orElseThrow();
        assertThat(doc.getRoot().getName()).isEqualTo("ul");
        List<? extends org.openrewrite.xml.tree.Content> items = doc.getRoot().getContent();
        org.assertj.core.api.Assertions.assertThat(items).hasSize(2).allSatisfy(i -> {
            assertThat(((Xml.Tag) i).getName()).isEqualTo("li");
            assertThat(((Xml.Tag) i).getMarkers().findFirst(org.openrewrite.xml.marker.HtmlImpliedClose.class)).isPresent();
            assertThat(((Xml.Tag) i).getClosing()).isNull();
        });
        assertThat(((Xml.Tag) items.get(0)).getValue()).contains("one");
    }

    @Test
    void missingBodyAndHtmlEndTags() {
        rewriteRun(
          html(
            """
              <html>
              <head><title>t</title>
              <body>
              <p>hello
              """
          )
        );
    }

    @Test
    void frameworkAttributeSyntax() {
        rewriteRun(
          html(
            """
              <div *ngIf="show" [title]="t" (click)="go($event)" @click="go" :prop="x" #ref [(ngModel)]="m"
                   v-on:click.prevent="go" v-bind:foo="bar" x-data>
                <my-comp [input]="a" (output)="b()"></my-comp>
              </div>
              """
          )
        );
    }

    @Test
    void pageUsingEverything() {
        rewriteRun(
          html(
            """
              <!DOCTYPE html>
              <html lang=en>
              <head>
                <meta charset=utf-8>
                <title>Tom & Jerry</title>
                <script>
                  if (a < b && c) { document.write("<p>x</p>"); }
                </script>
              </head>
              <body class=home>
                <ul>
                  <li>One
                  <li *ngIf="x" (click)="go()">Two
                </ul>
                <input type=checkbox checked disabled>
                <textarea>a < b</textarea>
                <button onclick="if(a<b) go()">go</button>
                <p>last
              """
          )
        );
    }

    @Test
    void stillRejectsGenuinelyBrokenMarkup() {
        SourceFile parsed = XmlParser.builder().build()
          .parseInputs(List.of(Input.fromString(Path.of("a.html"), "<div><span></div>")), null,
            new InMemoryExecutionContext(t -> {
            }))
          .findFirst().orElseThrow();
        assertThat(parsed).isInstanceOf(ParseError.class);
    }

    @Test
    void strictXmlIsUnaffectedByHtmlRules() {
        SourceFile parsed = XmlParser.builder().build()
          .parseInputs(List.of(Input.fromString(Path.of("a.xml"), "<a><li>x<li>y</a>")), null,
            new InMemoryExecutionContext(t -> {
            }))
          .findFirst().orElseThrow();
        assertThat(parsed).isInstanceOf(ParseError.class);
    }

    @Test
    void closingTagsMatchIgnoringCase() {
        rewriteRun(
          xml(
            "<TABLE><TR><TD>cell</td></tr></table>",
            spec -> spec.path("legacy.jsp")
          )
        );
    }

    @Test
    void whitespaceAfterImpliedEndTagPrefixesWhatFollows() {
        Xml.Document doc = (Xml.Document) XmlParser.builder().build()
          .parseInputs(List.of(Input.fromString(Path.of("a.html"), "<ul>\n  <li>a\n  <li>b\n</ul>")), null,
            new InMemoryExecutionContext(Throwable::printStackTrace))
          .findFirst().orElseThrow();
        Xml.Tag ul = doc.getRoot();
        for (Xml.Tag li : ul.getChildren()) {
            assertThat(li.getPrefix()).isEqualTo("\n  ");
            assertThat(((Xml.CharData) requireNonNull(li.getContent()).get(0)).getAfterText()).isEmpty();
        }
        assertNotNull(ul.getClosing());
        assertThat(ul.getClosing().getPrefix()).isEqualTo("\n");
    }

    @Test
    void jspFragmentIsParsedAsHtml() {
        rewriteRun(
          xml(
            """
              <%@ page pageEncoding="UTF-8" %>
              <nav class=main><a href="/">Home</a><br><input disabled></nav>
              """,
            spec -> spec.path("WEB-INF/jspf/header.jspf")
          )
        );
    }
}
