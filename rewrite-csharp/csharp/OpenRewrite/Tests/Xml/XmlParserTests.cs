/*
 * Copyright 2026 the original author or authors.
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
using OpenRewrite.Xml;

namespace OpenRewrite.Tests.Xml;

/// <summary>
/// Tests for XmlParser round-trip fidelity.
/// Port of Java's org.openrewrite.xml.XmlParserTest.
/// </summary>
public class XmlParserTests
{
    private readonly XmlParser _parser = new();

    /// <summary>
    /// Parse XML and assert round-trip: print(parse(input)) == input.
    /// </summary>
    private void AssertRoundTrip(string xml)
    {
        var document = _parser.Parse(xml);
        var printed = XmlParser.Print(document);
        Assert.Equal(xml, printed);
    }

    [Fact]
    public void Jsp()
    {
        AssertRoundTrip(
            """
            <!DOCTYPE html>
            <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
            <%@ taglib prefix="s" uri="/struts-tags" %>
            <html lang="en">
              <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <title>Hello World!</title>
              </head>
              <body>
                <h2><s:property value="messageStore.message" /></h2>
              </body>
            </html>
            """);
    }

    [Fact]
    public void JspScriptlet()
    {
        AssertRoundTrip(
            """
            <!DOCTYPE html>
            <html>
              <body>
                <%
                  String name = request.getParameter("name");
                  if (name == null) {
                      name = "Guest";
                  }
                %>
                <h1>Welcome!</h1>
              </body>
            </html>
            """);
    }

    [Fact]
    public void JspExpression()
    {
        AssertRoundTrip(
            """
            <!DOCTYPE html>
            <html>
              <body>
                <h1>Current time: <%= new java.util.Date() %></h1>
                <p>Your name: <%= request.getParameter("name") %></p>
              </body>
            </html>
            """);
    }

    [Fact]
    public void JspDeclaration()
    {
        AssertRoundTrip(
            """
            <!DOCTYPE html>
            <%!
              private int counter = 0;

              public int incrementCounter() {
                  return ++counter;
              }
            %>
            <html>
              <body>
                <h1>Page visits: <%= incrementCounter() %></h1>
              </body>
            </html>
            """);
    }

    [Fact]
    public void JspComment()
    {
        AssertRoundTrip(
            """
            <!DOCTYPE html>
            <html>
              <body>
                <%-- This is a JSP comment that won't appear in the HTML output --%>
                <h1>Hello World</h1>
                <%--
                  Multi-line JSP comment
                  for documenting JSP code
                --%>
              </body>
            </html>
            """);
    }

    [Fact]
    public void JspEmptyFile()
    {
        AssertRoundTrip("\n");
    }

    [Fact]
    public void JspNoHtmlContent()
    {
        AssertRoundTrip(
            """
            <%-- This is a JSP comment that won't appear in the HTML output --%>
            """);
    }

    [Fact]
    public void JspScriptletBeforeHtml()
    {
        AssertRoundTrip(
            """
            <%-- This is a JSP comment that won't appear in the HTML output --%>
            <% String test = "hello"; %>
            <html></html>
            """);
    }

    [Fact]
    public void MixedJspElements()
    {
        AssertRoundTrip(
            """
            <!DOCTYPE html>
            <%@ page language="java" contentType="text/html; charset=UTF-8" %>
            <html>
              <body>
                <%!
                  private String greeting = "Hello";
                %>
                <%-- Display greeting --%>
                <h1><%= greeting %> from JSP!</h1>
                <%
                  for(int i = 1; i <= 3; i++) {
                %>
                  <p>Line <%= i %></p>
                <% } %>
              </body>
            </html>
            """);
    }

    [Fact]
    public void LowerCaseDocType()
    {
        AssertRoundTrip(
            """
            <!doctype html>
            <html lang="en">
              <body>
                <h2><s:property value="messageStore.message" /></h2>
              </body>
            </html>
            """);
    }

    [Fact]
    public void SpecialCharacters()
    {
        AssertRoundTrip("<project>Some &#39;Example&#39;</project>");
    }

    [Fact]
    public void ParseXmlDocument()
    {
        AssertRoundTrip(
            """
            <?xml
                version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet href="mystyle.css" type="text/css"?>
            <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN"
                "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
            <beans >
                <bean id="myBean"/>
            </beans>
            """);
    }

    [Fact]
    public void CdataTagWhitespace()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                <suppress>
                    <notes>
                        <![CDATA[
                      file name: foo.jar
                      ]]>
                    </notes>
                    <gav regex="true">^:foo:.*$</gav>
                    <cve>CVE-2020-000</cve>
                </suppress>
            </suppressions>
            """);
    }

    [Fact]
    public void ParsePomDocument()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- comment -->
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>com.google.guava</groupId>
                <artifactId>guava-parent</artifactId>
                <version>28.2-jre</version>
              </parent>
              <artifactId>guava</artifactId>
              <packaging>bundle</packaging>
              <name>Guava: Google Core Libraries for Java</name>
            </project>
            """);
    }

    [Fact]
    public void CommentBeforeContent()
    {
        AssertRoundTrip(
            """
            <foo>
                <a><!-- comment -->a</a>
            </foo>
            """);
    }

    [Fact]
    public void SingleQuestionMarkContent()
    {
        AssertRoundTrip(
            """
            <foo>
                <a><!-- comment -->a</a>
                <literal>List&lt;?&gt;</literal>
            </foo>
            """);
    }

    [Fact]
    public void CommentBeforeContentNewline()
    {
        AssertRoundTrip(
            """
            <foo>
                <a>
                    <!-- comment -->
                    a
                </a>
            </foo>
            """);
    }

    [Fact]
    public void CommentAfterContent()
    {
        AssertRoundTrip(
            """
            <foo>
                <a>a<!-- comment --></a>
            </foo>
            """);
    }

    [Fact]
    public void CommentAfterContentNewline()
    {
        AssertRoundTrip(
            """
            <foo>
                <a>
                    a
                    <!-- comment -->
                </a>
            </foo>
            """);
    }

    [Fact]
    public void ParseDocTypeWithoutExternalId()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE configuration >

            <configuration scan="true">
                <root>
                    <level>WARN</level>
                    <appender-ref ref="CONSOLE"/>
                </root>
            </configuration>
            """);
    }

    [Fact]
    public void DtdSubsetMarkupDecl()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0"?>
            <!DOCTYPE p [
                <!ELEMENT p ANY>
            ]>
            <p>Hello world!</p>
            """);
    }

    [Fact]
    public void DtdSubsetParamEntityRef()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0"?>
            <!DOCTYPE p [
                %entity;
            ]>
            <p>Hello world!</p>
            """);
    }

    [Fact]
    public void DtdSubsetComment()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0"?>
            <!DOCTYPE p [
                <!-- comment -->
            ]>
            <p>Hello world!</p>
            """);
    }

    [Fact]
    public void ProcessingInstructions()
    {
        AssertRoundTrip(
            """
            <?xml-stylesheet href="mystyle.css" type="text/css"?>
            <execution>
                <?m2e execute onConfiguration,onIncremental?>
            </execution>
            """);
    }

    [Fact]
    public void Utf8BomCharacters()
    {
        AssertRoundTrip(
            "\u00ef\u00bb\u00bf<?xml version=\"1.0\" encoding=\"UTF-8\"?><test></test>");
    }

    [Fact]
    public void Utf8WithBom()
    {
        var xml = "\uFEFF<?xml version=\"1.0\" encoding=\"UTF-8\"?><a />\n";
        var document = _parser.Parse(xml);
        Assert.True(document.CharsetBomMarked);
        var printed = XmlParser.Print(document);
        Assert.Equal(xml, printed);
    }

    [Fact]
    public void Utf8WithoutBom()
    {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a />\n";
        var document = _parser.Parse(xml);
        Assert.False(document.CharsetBomMarked);
        var printed = XmlParser.Print(document);
        Assert.Equal(xml, printed);
    }

    [Fact]
    public void PreserveSpaceBeforeAttributeAssignment()
    {
        AssertRoundTrip(
            """
            <?xml version = "1.0" encoding    =   "UTF-8" standalone = "no" ?><blah></blah>
            """);
    }

    [Fact]
    public void LinkWithQuestionMark()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0" encoding="ISO-8859-1"?>
            <?xml-stylesheet type="text/xsl" href="/name/other?link"?>
            <blah></blah>
            """);
    }

    [Fact]
    public void PreserveWhitespaceOnEntities()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <message><text>&lt;?xml version='1.0' encoding='UTF-8'?&gt;&#13;
            &lt;note&gt;&#13;
                &lt;to&gt;Tove&lt;/to&gt;&#13;
                &lt;from&gt;Jani&lt;/from&gt;&#13;
                &lt;heading&gt;Reminder&lt;/heading&gt;&#13;
                &lt;body&gt;Don't forget me this weekend!&lt;/body&gt;&#13;
            &lt;/note&gt;&#13;
            &#13;
            </text></message>
            """);
    }

    [Fact]
    public void AcceptWithValidPaths()
    {
        Assert.True(_parser.Accept("foo.xml"));
        Assert.True(_parser.Accept("proj.csproj"));
        Assert.True(_parser.Accept("/foo/bar/baz.jsp"));
        Assert.True(_parser.Accept("packages.config"));
    }

    [Fact]
    public void AcceptWithInvalidPaths()
    {
        Assert.False(_parser.Accept(".xml"));
        Assert.False(_parser.Accept("foo.xml."));
        Assert.False(_parser.Accept("file.cpp"));
        Assert.False(_parser.Accept("/foo/bar/baz.xml.txt"));
    }

    [Fact]
    public void CRsWithNoLFs()
    {
        AssertRoundTrip(
            "<?xml version=\"1.0\"?>\r<a>\r</a>".Replace("CR", "\r"));
    }

    [Fact]
    public void Utf8SurrogatePairsInComments()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <!-- 👇 Problem below -->
                <dependency>
                    <groupId>org.example</groupId>
                    <artifactId>example</artifactId>
                </dependency>
                <!-- 👆 Problem above -->
            </project>
            """);
    }

    [Fact]
    public void Utf8SurrogatePairsSimple()
    {
        AssertRoundTrip(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- 👇 -->
            <a></a>
            """);
    }
}
