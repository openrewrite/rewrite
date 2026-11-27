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
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser.Input;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;
import org.openrewrite.xml.marker.HtmlImpliedClose;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.html;
import static org.openrewrite.xml.Assertions.xml;

class HtmlParserTest implements RewriteTest {

    @DocumentExample
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
        SourceFile parsed = parse("rest.html",
          "<table><tr><td><label><details><summary>Request</summary></label></td></tr></table>");
        assertThat(parsed.getMarkers().findFirst(ParseExceptionResult.class))
          .hasValueSatisfying(e -> assertThat(e.getMessage()).contains("Mismatched closing tag </label>"));
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
        rewriteRun(
          html(
            "<input checked name=q>",
            spec -> spec.afterRecipe(doc -> {
                List<Xml.Attribute> attributes = doc.getRoot().getAttributes();
                assertThat(attributes).extracting(Xml.Attribute::getKeyAsString).containsExactly("checked", "name");
                assertThat(attributes).extracting(Xml.Attribute::getValueAsString).containsExactly("", "q");
            })
          )
        );
    }

    @Test
    void booleanAttributeIsStillAnErrorInXml() {
        assertThat(parse("a.xml", "<input checked/>")).isInstanceOf(ParseError.class);
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
    void lessThanBeforeCharacterThatCannotBeginMarkup() {
        rewriteRun(
          html(
            """
              <p>x <- y, a<-b, (1<)</p>
              """
          )
        );
    }

    @Test
    void loneAmpersand() {
        rewriteRun(
          html(
            """
              <div>
                <p>Tom & Jerry &amp; friends &#169; &copy 2024, a < b, 1 <2 & more</p>
                <p>AT&T</p>
              </div>
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
        rewriteRun(
          html(
            "<div><ul><li>one<li>two</ul><p>a<p>b</div>",
            spec -> spec.afterRecipe(doc -> {
                Xml.Tag div = doc.getRoot();
                assertThat(div.getChildren()).extracting(Xml.Tag::getName).containsExactly("ul", "p", "p");
                assertThat(div.getChildren().subList(1, 3)).extracting(Xml.Tag::getValue)
                  .containsExactly(Optional.of("a"), Optional.of("b"));

                Xml.Tag ul = div.getChildren().get(0);
                assertThat(ul.getContent()).hasSize(2);
                assertThat(ul.getChildren()).extracting(Xml.Tag::getValue)
                  .containsExactly(Optional.of("one"), Optional.of("two"));
                assertThat(div.getChildren().subList(1, 3)).allSatisfy(this::assertImpliedClose);
                assertThat(ul.getChildren()).allSatisfy(this::assertImpliedClose);
            })
          )
        );
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
    void stillRejectsGenuinelyBrokenMarkup() {
        assertThat(parse("a.html", "<div><span></div>")).isInstanceOf(ParseError.class);
    }

    @Test
    void strictXmlIsUnaffectedByHtmlRules() {
        assertThat(parse("a.xml", "<a><li>x<li>y</a>")).isInstanceOf(ParseError.class);
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
        rewriteRun(
          html(
            """
              <ul>
                <li>a
                <li>b
              </ul>
              """,
            spec -> spec.afterRecipe(doc -> {
                Xml.Tag ul = doc.getRoot();
                assertThat(ul.getChildren()).hasSize(2).allSatisfy(li -> {
                    assertThat(li.getPrefix()).isEqualTo("\n  ");
                    assertThat(li.getContent()).singleElement()
                      .isInstanceOfSatisfying(Xml.CharData.class, text -> assertThat(text.getAfterText()).isEmpty());
                });
                assertThat(ul.getClosing()).isNotNull()
                  .extracting(Xml.Tag.Closing::getPrefix).isEqualTo("\n");
            })
          )
        );
    }

    @Test
    void jspFragmentIsParsedAsHtml() {
        rewriteRun(
          xml(
            """
              <%@ page pageEncoding="UTF-8" %>
              <nav class=main><a href="/">Home</a><br><input disabled></nav>
              """,
            spec -> spec.path("WEB-INF/jspf/header.jspf").afterRecipe(doc -> {
                assertThat(doc.getProlog().getMisc()).singleElement().isInstanceOfSatisfying(Xml.JspDirective.class, directive -> {
                    assertThat(directive.getType()).isEqualTo("page");
                    assertThat(directive.getBeforeTypePrefix()).isEqualTo(" ");
                    assertThat(directive.getAttributes()).singleElement().satisfies(attribute -> {
                        assertThat(attribute.getKeyAsString()).isEqualTo("pageEncoding");
                        assertThat(attribute.getValueAsString()).isEqualTo("UTF-8");
                    });
                    assertThat(directive.getBeforeDirectiveEndPrefix()).isEqualTo(" ");
                });
                assertThat(doc.getRoot().getName()).isEqualTo("nav");
                assertThat(doc.getRoot().getPrefix()).isEqualTo("\n");
            })
          )
        );
    }

    @Test
    void secondTopLevelElementIsReported() {
        assertThat(parse("a.html", "<ul><li>one</ul>\n<p>hidden").getMarkers().findFirst(ParseExceptionResult.class))
          .hasValueSatisfying(e -> assertThat(e.getMessage()).contains("Content after the root element in a.html at line 2, column 1"));
    }

    @Test
    void endTagLeftOverFromMisnestingIsReported() {
        assertThat(parse("a.html", "<html><body><a href=x /></a></body></html>")).isInstanceOf(ParseError.class);
    }

    @Test
    void commentsAndWhitespaceAfterTheRootAreAllowed() {
        rewriteRun(
          html(
            """
              <html></html>
              <!-- trailing -->
              """
          )
        );
    }

    private void assertImpliedClose(Xml.Tag tag) {
        assertThat(tag.getMarkers().findFirst(HtmlImpliedClose.class)).isPresent();
        assertThat(tag.getClosing()).isNull();
    }

    private static SourceFile parse(String path, String source) {
        return XmlParser.builder().build()
          .parseInputs(List.of(Input.fromString(Path.of(path), source)), null, new InMemoryExecutionContext(t -> {
          }))
          .findFirst().orElseThrow();
    }
}
