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
import {RecipeSpec} from "../../src/test";
import {getAttributeValue, getChildren, getValue, isTag, unescapeXml, withValue, xml, Xml, XmlParser} from "../../src/xml";
import {ParseErrorKind} from "../../src/parse-error";
import {TreePrinters} from "../../src/print";
import {SourceFile} from "../../src/tree";

async function parse(text: string, sourcePath: string = "file.xml"): Promise<SourceFile> {
    return new XmlParser().parseOne({text, sourcePath});
}

async function parseDocument(text: string, sourcePath?: string): Promise<Xml.Document> {
    const sourceFile = await parse(text, sourcePath);
    expect(sourceFile.kind).toEqual(Xml.Kind.Document);
    return sourceFile as Xml.Document;
}

describe("XML parsing", () => {
    const spec = new RecipeSpec();

    test("xml declaration", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
              <root/>
            `
        )
    ));

    test("doctype with an internal subset", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <?xml version="1.0"?>
              <!DOCTYPE note SYSTEM "note.dtd" [
                  <!ELEMENT note (to,from,body)>
                  <!ATTLIST note id CDATA #REQUIRED>
                  <!ENTITY writer "Writer: &quot;Donald&quot; > Duck">
                  <!-- a comment in the subset -->
                  <?pi in subset?>
                  %parameterEntity;
              ]>
              <note id="1"><to>Tove</to><from>&writer;</from><body/></note>
            `
        )
    ));

    test("doctype with a public identifier", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd' >
              <html/>
            `
        )
    ));

    test("lower case doctype", () => spec.rewriteRun(
        xml(`<!doctype html><html></html>`)
    ));

    test("comments", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <!-- before the root -->
              <root>
                  <!-- inside -->
                  <a/><!--adjacent--><!---->
              </root>
            `
        )
    ));

    test("processing instructions", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <?xml version="1.0"?>
              <?xml-stylesheet type="text/xsl" href="style.xsl"?>
              <?empty?>
              <?single ?>
              <?spaced   data with spaces   ?>
              <root>
                  <?php echo "hi"; ?>
              </root>
            `
        )
    ));

    test("CDATA", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <root>
                  <script><![CDATA[ if (a < b && c > d) { return "]]"; } ]]></script>
                  <![CDATA[]]>
                  text <![CDATA[<raw>]]> more text
              </root>
            `
        )
    ));

    test("entity and character references", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <root a="&lt;&amp;&#x41;&#65;">
                  &lt;tag&gt; &amp; &#169; &#xA9; &custom;
                  <b>x&amp;y</b>
              </root>
            `
        )
    ));

    test("attributes with single and double quotes and whitespace around =", () => spec.rewriteRun(
        xml(
            `<root a="1" b='2' c = "3"  d	=	'4'\n  e\n=\n"5" f='say "hi"' g="it's"   ></root>`
        )
    ));

    test("self-closing tags", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <root>
                  <a/>
                  <b />
                  <c attr="1"/>
                  <d attr="1"   />
                  <e></e>
              </root>
            `
        )
    ));

    test("closing tag with whitespace before >", () => spec.rewriteRun(
        xml(`<root><a></a  ></root >`)
    ));

    test("namespaces", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                             xmlns:m="https://www.example.org/stock" xmlns="urn:default">
                  <soap:Body>
                      <m:GetStockPrice m:currency="EUR">
                          <m:StockName>T</m:StockName>
                      </m:GetStockPrice>
                  </soap:Body>
              </soap:Envelope>
            `
        )
    ));

    test("mixed content", () => spec.rewriteRun(
        //language=xml
        xml(
            `
              <p>
                  Some <b>bold</b> and <i>italic</i> text, a <br/> break,
                  and   trailing   spaces
              </p>
            `
        )
    ));

    test("unicode names and text", () => spec.rewriteRun(
        xml(`<données clé="valeur">こんにちは 👋 <ü·x/></données>`)
    ));

    test("whitespace before the root and at the end of the file", () => spec.rewriteRun(
        xml(`\n\n  <root/>\n\n`)
    ));

    test("windows line endings", () => spec.rewriteRun(
        xml(`<?xml version="1.0"?>\r\n<root>\r\n  <a b="c"/>\r\n  text\r\n</root>\r\n`)
    ));

    test("JSP", () => spec.rewriteRun(
        {
            ...xml(
                `
                  <%@ page contentType="text/html" pageEncoding="UTF-8" %>
                  <%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
                  <html>
                      <%-- a JSP comment --%>
                      <%! int count = 0; %>
                      <% count++; %>
                      <p>Count: <%= count %></p>
                      <br>
                      <img src="a.png">
                  </html>
                `
            ),
            path: "index.jsp"
        }
    ));

    test("a realistic UI5 view", () => spec.rewriteRun(
        {
            ...xml(
                //language=xml
                `
                  <mvc:View
                      controllerName="sap.ui.demo.walkthrough.controller.App"
                      xmlns="sap.m"
                      xmlns:core="sap.ui.core"
                      xmlns:mvc="sap.ui.core.mvc"
                      displayBlock="true">
                      <Shell>
                          <App class="myAppDemoWT">
                              <pages>
                                  <Page title="{i18n>homePageTitle}">
                                      <headerContent>
                                          <Button
                                              icon="sap-icon://hello-world"
                                              press=".onOpenDialog"/>
                                      </headerContent>
                                      <content>
                                          <!-- the panel -->
                                          <Panel headerText="{i18n>helloPanelTitle}" class="sapUiResponsiveMargin" width="auto">
                                              <content>
                                                  <Input value="{/recipient/name}" valueLiveUpdate="true" width="60%"/>
                                                  <Text text="Hello {/recipient/name}" class="sapUiSmallMargin"/>
                                                  <core:Fragment fragmentName="sap.ui.demo.walkthrough.view.HelloDialog" type="XML"/>
                                              </content>
                                          </Panel>
                                      </content>
                                  </Page>
                              </pages>
                          </App>
                      </Shell>
                  </mvc:View>
                `
            ),
            path: "webapp/view/App.view.xml"
        }
    ));

    test("byte order mark", async () => {
        // given
        const source = "\uFEFF<?xml version=\"1.0\"?>\n<root/>\n";

        // when
        const document = await parseDocument(source);

        // then
        expect(document.charsetBomMarked).toBe(true);
        expect(document.prolog.xmlDecl?.prefix).toEqual("");
        expect(await TreePrinters.print(document)).toEqual(source);
    });

    test("whitespace goes where the Java parser puts it", async () => {
        // given
        const source = `  <?xml version="1.0"?>\n<!-- c -->\n<root a = "1"  >\n  text\n  <b/>\n  &amp; more \n</root>\n`;

        // when
        const document = await parseDocument(source);

        // then
        expect(document.prefix).toEqual("  ");
        expect(document.prolog.misc[0].prefix).toEqual("\n");
        const root = document.root!;
        expect(root.prefix).toEqual("\n");
        expect(root.attributes[0]).toMatchObject({prefix: " ", beforeEquals: " ", value: {prefix: " ", value: "1"}});
        expect(root.beforeTagDelimiterPrefix).toEqual("  ");
        expect(root.content!.map(c => [c.kind, c.prefix, (c as Xml.CharData).text, (c as Xml.CharData).afterText]))
            .toEqual([
                [Xml.Kind.CharData, "\n  ", "text", "\n  "],
                [Xml.Kind.Tag, "", undefined, undefined],
                [Xml.Kind.CharData, "\n  ", "&amp;", ""],
                [Xml.Kind.CharData, " ", "more", " \n"],
            ]);
        expect(root.closing!.prefix).toEqual("");
        expect(document.eof).toEqual("\n");
    });

    test("self-closing tag has no content while an empty one does", async () => {
        // when
        const document = await parseDocument(`<root><a/><b></b></root>`);

        // then
        const [a, b] = getChildren(document.root!);
        expect(a.content).toBeUndefined();
        expect(a.closing).toBeUndefined();
        expect(b.content).toEqual([]);
        expect(b.closing?.name).toEqual("b");
    });

    test("tag helpers", async () => {
        // when
        const document = await parseDocument(`<root a="x&amp;y"><v>1&lt;2</v><v/><w>t</w></root>`);

        // then
        expect(getAttributeValue(document.root!, "a")).toEqual("x&amp;y");
        expect(getChildren(document.root!, "v")).toHaveLength(2);
        expect(getValue(getChildren(document.root!, "v")[0])).toEqual("1<2");
        expect(getValue(getChildren(document.root!, "w")[0])).toEqual("t");
        expect(document.root!.content!.every(isTag)).toBe(true);
    });

    test("HTML void elements only in HTML-like files", async () => {
        // when
        const jsp = await parseDocument(`<p><br></p>`, "index.jsp");
        const notJsp = await parse(`<p><br></p>`, "index.xml");

        // then
        const br = getChildren(jsp.root!)[0];
        expect(br.markers.markers.map(m => m.kind)).toEqual([Xml.MarkersKind.HtmlVoidElement]);
        expect(await TreePrinters.print(jsp)).toEqual(`<p><br></p>`);
        expect(notJsp.kind).toEqual(ParseErrorKind);
    });

    test("a value given to an HTML void element prints", async () => {
        // given
        const jsp = await parseDocument(`<p><br></p>`, "index.jsp");

        // when
        const br = withValue(getChildren(jsp.root!)[0], "x");

        // then
        expect(await TreePrinters.print({...jsp, root: {...jsp.root, content: [br]}} as Xml.Document))
            .toEqual(`<p><br>x</br></p>`);
    });

    test("a character reference beyond Unicode stays as written", () => {
        expect(unescapeXml("a&#x110000;b&#169;")).toEqual("a&#x110000;b©");
    });

    test("a document without a root element puts the whitespace where the Java parser does", async () => {
        // given
        const sources = [``, `   \n`, `<!-- c -->\n`, `  <?xml version="1.0"?>  \n  <!-- c -->\n\n`];

        // when
        const documents = await Promise.all(sources.map(s => parseDocument(s)));

        // then
        expect(documents.map(d => [d.root, d.prefix, d.prolog.misc.map(m => m.prefix), d.eof])).toEqual([
            [undefined, "", [], ""],
            [undefined, "   \n", [], ""],
            [undefined, "", [""], "\n"],
            [undefined, "  ", ["  \n  "], "\n\n"],
        ]);
        expect(await Promise.all(documents.map(d => TreePrinters.print(d)))).toEqual(sources);
    });

    test("a JSP file without HTML", () => spec.rewriteRun(
        {
            //language=html
            ...xml(
                `
                  <%-- This is a JSP comment that won't appear in the HTML output --%>
                `
            ),
            path: "noHtmlContent.jsp"
        }
    ));

    test.each([
        ["an unclosed element", `<root><a></root>`],
        ["a mismatched closing tag", `<root></toor>`],
        ["an unquoted attribute value", `<root a=1/>`],
        ["a '<' in an attribute value", `<root a="<"/>`],
        ["an attribute without a value", `<root a/>`],
        ["an invalid reference", `<root>& </root>`],
        ["text without a root element", `<!-- c -->text`],
    ])("reports %s as a parse error", async (_, source) => {
        // when
        const sourceFile = await parse(source);

        // then
        expect(sourceFile.kind).toEqual(ParseErrorKind);
    });

    test("accepts the paths the Java parser accepts", () => {
        expect(XmlParser.accept("pom.xml")).toBe(true);
        expect(XmlParser.accept("webapp/view/App.view.xml")).toBe(true);
        expect(XmlParser.accept("schema.XSD")).toBe(true);
        expect(XmlParser.accept("src/App.csproj")).toBe(true);
        expect(XmlParser.accept("Web.Release.config")).toBe(true);
        expect(XmlParser.accept("nuget.config")).toBe(true);
        expect(XmlParser.accept("index.html")).toBe(false);
        expect(XmlParser.accept("other.config")).toBe(false);
        expect(XmlParser.accept(".xml")).toBe(false);
    });
});
