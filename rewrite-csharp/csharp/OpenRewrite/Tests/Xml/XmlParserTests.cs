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
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Xml;

public class XmlParserTests : XmlRewriteTest
{
    [Fact]
    public void SimpleTag()
    {
        RewriteRun(
            Xml("<root/>")
        );
    }

    [Fact]
    public void SelfClosingTagWithAttributes()
    {
        RewriteRun(
            Xml("""<root attr="value"/>""")
        );
    }

    [Fact]
    public void EmptyTag()
    {
        RewriteRun(
            Xml("<root></root>")
        );
    }

    [Fact]
    public void TagWithTextContent()
    {
        RewriteRun(
            Xml("<root>hello</root>")
        );
    }

    [Fact]
    public void NestedTags()
    {
        RewriteRun(
            Xml(
                """
                <root>
                    <child>text</child>
                </root>
                """
            )
        );
    }

    [Fact]
    public void ParseXmlDocument()
    {
        RewriteRun(
            Xml(
                """
                <?xml
                    version="1.0" encoding="UTF-8"?>
                <?xml-stylesheet href="mystyle.css" type="text/css"?>
                <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN"
                    "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
                <beans >
                    <bean id="myBean"/>
                </beans>
                """
            )
        );
    }

    [Fact]
    public void ParsePomDocument()
    {
        RewriteRun(
            Xml(
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
                """
            )
        );
    }

    [Fact]
    public void SpecialCharacters()
    {
        RewriteRun(
            Xml("<project>Some &#39;Example&#39;</project>")
        );
    }

    [Fact]
    public void CommentBeforeContent()
    {
        RewriteRun(
            Xml(
                """
                <foo>
                    <a><!-- comment -->a</a>
                </foo>
                """
            )
        );
    }

    [Fact]
    public void CommentBeforeContentNewline()
    {
        RewriteRun(
            Xml(
                """
                <foo>
                    <a>
                        <!-- comment -->
                        a
                    </a>
                </foo>
                """
            )
        );
    }

    [Fact]
    public void CommentAfterContent()
    {
        RewriteRun(
            Xml(
                """
                <foo>
                    <a>a<!-- comment --></a>
                </foo>
                """
            )
        );
    }

    [Fact]
    public void CommentAfterContentNewline()
    {
        RewriteRun(
            Xml(
                """
                <foo>
                    <a>
                        a
                        <!-- comment -->
                    </a>
                </foo>
                """
            )
        );
    }

    [Fact]
    public void CdataTag()
    {
        RewriteRun(
            Xml(
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
                """
            )
        );
    }

    [Fact]
    public void PreserveSpaceBeforeAttributeAssignment()
    {
        RewriteRun(
            Xml(
                """
                <?xml version = "1.0" encoding    =   "UTF-8" standalone = "no" ?><blah></blah>
                """
            )
        );
    }

    [Fact]
    public void ProcessingInstructions()
    {
        RewriteRun(
            Xml(
                """
                <?xml-stylesheet href="mystyle.css" type="text/css"?>
                <execution>
                    <?m2e execute onConfiguration,onIncremental?>
                </execution>
                """
            )
        );
    }

    [Fact]
    public void ParseDocTypeWithoutExternalId()
    {
        RewriteRun(
            Xml(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE configuration >

                <configuration scan="true">
                    <root>
                        <level>WARN</level>
                        <appender-ref ref="CONSOLE"/>
                    </root>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void DtdSubsetMarkupDecl()
    {
        RewriteRun(
            Xml(
                """
                <?xml version="1.0"?>
                <!DOCTYPE p [
                    <!ELEMENT p ANY>
                ]>
                <p>Hello world!</p>
                """
            )
        );
    }

    [Fact]
    public void DtdSubsetParamEntityRef()
    {
        RewriteRun(
            Xml(
                """
                <?xml version="1.0"?>
                <!DOCTYPE p [
                    %entity;
                ]>
                <p>Hello world!</p>
                """
            )
        );
    }

    [Fact]
    public void DtdSubsetComment()
    {
        RewriteRun(
            Xml(
                """
                <?xml version="1.0"?>
                <!DOCTYPE p [
                    <!-- comment -->
                ]>
                <p>Hello world!</p>
                """
            )
        );
    }

    [Fact]
    public void SingleQuoteAttributes()
    {
        RewriteRun(
            Xml("<root attr='value'/>")
        );
    }

    [Fact]
    public void MultipleAttributes()
    {
        RewriteRun(
            Xml("""<root a="1" b="2" c="3"/>""")
        );
    }

    [Fact]
    public void MixedContent()
    {
        RewriteRun(
            Xml(
                """
                <root>
                    text before
                    <child/>
                    text after
                </root>
                """
            )
        );
    }

    [Fact]
    public void NamespacedAttributes()
    {
        RewriteRun(
            Xml("""<root xmlns:ns="http://example.com" ns:attr="value"/>""")
        );
    }

    [Fact]
    public void SelfClosingWithSpaceBeforeSlash()
    {
        RewriteRun(
            Xml("<root />")
        );
    }

    [Fact]
    public void MultipleChildElements()
    {
        RewriteRun(
            Xml(
                """
                <parent>
                    <child1/>
                    <child2>text</child2>
                    <child3 attr="val">
                        <nested/>
                    </child3>
                </parent>
                """
            )
        );
    }

    [Fact]
    public void EntityReferences()
    {
        RewriteRun(
            Xml(
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
                """
            )
        );
    }

    [Fact]
    public void Utf8SurrogatePairsInComments()
    {
        RewriteRun(
            Xml(
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
                """
            )
        );
    }

    [Fact]
    public void Utf8SurrogatePairsSimple()
    {
        RewriteRun(
            Xml(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!-- 👇 -->
                <a></a>
                """
            )
        );
    }

    [Fact]
    public void SingleQuestionMarkContent()
    {
        RewriteRun(
            Xml(
                """
                <foo>
                    <a><!-- comment -->a</a>
                    <literal>List&lt;?&gt;</literal>
                </foo>
                """
            )
        );
    }

    [Fact]
    public void LinkWithQuestionMark()
    {
        RewriteRun(
            Xml(
                """
                <?xml version="1.0" encoding="ISO-8859-1"?>
                <?xml-stylesheet type="text/xsl" href="/name/other?link"?>
                <blah></blah>
                """
            )
        );
    }

    [Fact]
    public void LowerCaseDocType()
    {
        RewriteRun(
            Xml(
                """
                <!doctype html>
                <html lang="en">
                  <body>
                    <h2>hello</h2>
                  </body>
                </html>
                """
            )
        );
    }

    [Fact]
    public void Jsp()
    {
        RewriteRun(
            Xml(
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
                """,
                sourcePath: "index.jsp"
            )
        );
    }

    [Fact]
    public void JspScriptlet()
    {
        RewriteRun(
            Xml(
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
                """,
                sourcePath: "scriptlet.jsp"
            )
        );
    }

    [Fact]
    public void JspExpression()
    {
        RewriteRun(
            Xml(
                """
                <!DOCTYPE html>
                <html>
                  <body>
                    <h1>Current time: <%= new java.util.Date() %></h1>
                    <p>Your name: <%= request.getParameter("name") %></p>
                  </body>
                </html>
                """,
                sourcePath: "expression.jsp"
            )
        );
    }

    [Fact]
    public void JspDeclaration()
    {
        RewriteRun(
            Xml(
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
                """,
                sourcePath: "declaration.jsp"
            )
        );
    }

    [Fact]
    public void JspComment()
    {
        RewriteRun(
            Xml(
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
                """,
                sourcePath: "comment.jsp"
            )
        );
    }

    [Fact]
    public void MixedJspElements()
    {
        RewriteRun(
            Xml(
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
                """,
                sourcePath: "mixed.jsp"
            )
        );
    }

    [Fact]
    public void EmptyDocument()
    {
        RewriteRun(
            Xml(
                """
                <root/>
                """
            )
        );
    }

    [Fact]
    public void CsprojFile()
    {
        RewriteRun(
            Xml(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void AttributeWithEmptyValue()
    {
        RewriteRun(
            Xml("""<root attr=""/>""")
        );
    }

    [Fact]
    public void DeeplyNestedTags()
    {
        RewriteRun(
            Xml(
                """
                <a>
                  <b>
                    <c>
                      <d>deep</d>
                    </c>
                  </b>
                </a>
                """
            )
        );
    }

    [Fact]
    public void WhitespacePreservation()
    {
        RewriteRun(
            Xml(
                """
                <root  attr1 = "val1"   attr2="val2" >
                    <child  />
                </root>
                """
            )
        );
    }

    [Fact]
    public void CommentOnly()
    {
        RewriteRun(
            Xml(
                """
                <!-- just a comment -->
                <root/>
                """
            )
        );
    }

    [Fact]
    public void MultipleCommentsInProlog()
    {
        RewriteRun(
            Xml(
                """
                <?xml version="1.0"?>
                <!-- comment 1 -->
                <!-- comment 2 -->
                <root/>
                """
            )
        );
    }

    [Fact]
    public void XmlDeclWithSingleQuotes()
    {
        RewriteRun(
            Xml(
                """
                <?xml version='1.0' encoding='UTF-8'?>
                <root/>
                """
            )
        );
    }
}
