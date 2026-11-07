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
import {describeJavaRpc, testJavaRpc} from "../../src/test/java-rpc";
import {RecipeSpec} from "../../src/test";
import {prepareJavaRecipe} from "../../src/rpc";
import {ExecutionContext} from "../../src/execution";
import {TreePrinters} from "../../src/print";
import {xml, Xml, XmlParser, XmlVisitor} from "../../src/xml";

/**
 * Sources whose trees the Java and TypeScript parsers have to agree on, down to where each run of whitespace goes.
 */
const samples: { [name: string]: { text: string, sourcePath?: string } } = {
    "declaration and doctype": {
        text: `<?xml version="1.0" encoding="UTF-8" standalone='yes' ?>\n` +
            `<!DOCTYPE note SYSTEM "note.dtd" [\n  <!ELEMENT note (to)>\n  <!ENTITY w "a > b">\n  <!-- c -->\n  %pe;\n]>\n` +
            `<note/>\n`
    },
    "doctype with public id": {
        text: `<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd' >\n<html></html>`
    },
    "comments and processing instructions": {
        text: `  <!-- first -->\n<?xml-stylesheet href="a.xsl"?>\n<?empty?>\n<?single ?>\n<?spaced   data  ?>\n` +
            `<root><!--in--><?php echo 1; ?>\n  <!---->\n</root>\n<!-- trailing -->\n`
    },
    "attributes": {
        text: `<root a="1" b='2' c = "3"  d\t=\t'4'\n  e\n=\n"5" f='say "hi"' g="it's"   ><a x="1"/><b  /></root  >`
    },
    "mixed content and references": {
        text: `<p>\n  Some <b>bold</b>, &lt;tag&gt; &amp; &#169;&#xA9; text \n  <![CDATA[ <raw> ]]> tail  \n</p>\n`
    },
    "namespaces": {
        text: `<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns="urn:d">\n` +
            `  <soap:Body><m:Get xmlns:m="urn:m" m:c="EUR"/></soap:Body>\n</soap:Envelope>`
    },
    "windows line endings": {
        text: `<?xml version="1.0"?>\r\n<root>\r\n  <a b="c"/>\r\n  text\r\n</root>\r\n`
    },
    "byte order mark": {
        text: `\uFEFF<?xml version="1.0"?>\n<root/>\n`
    },
    "JSP": {
        text: `<%@ page contentType="text/html" %>\n<%@taglib prefix="c" uri="u"%>\n<html>\n  <%-- c --%>\n  <%! int i = 0; %>\n` +
            `  <% i++; %>\n  <p>Count: <%= i %></p>\n  <br>\n  <img src="a.png">\n</html>\n`,
        sourcePath: "index.jsp"
    },
    "UI5 view": {
        text: `<mvc:View\n    controllerName="sap.ui.demo.walkthrough.controller.App"\n    xmlns="sap.m"\n` +
            `    xmlns:mvc="sap.ui.core.mvc"\n    displayBlock="true">\n  <Shell>\n    <App class="myAppDemoWT">\n` +
            `      <Page title="{i18n>homePageTitle}">\n        <Button icon="sap-icon://hello-world" press=".onOpenDialog"/>\n` +
            `      </Page>\n    </App>\n  </Shell>\n</mvc:View>\n`,
        sourcePath: "webapp/view/App.view.xml"
    },
    "an empty file": {
        text: ``
    },
    "a file of whitespace": {
        text: `   \n`
    },
    "a prolog without a root element": {
        text: `  <?xml version="1.0"?>  \n  <!-- c -->\n\n`
    },
    "a JSP directive without a root element": {
        text: `<%@ page x="1" %>\n`,
        sourcePath: "index.jsp"
    },
};

/**
 * A tree without what differs between two parses of the same source: ids, and absent values that one side
 * holds as `undefined` and the other as `null`.
 */
function shape(tree: any): any {
    if (Array.isArray(tree)) {
        return tree.map(shape);
    } else if (tree !== null && typeof tree === "object") {
        const result: any = {};
        for (const [key, value] of Object.entries(tree)) {
            if (key !== "id" && value !== null && value !== undefined) {
                result[key] = shape(value);
            }
        }
        return result;
    }
    return tree;
}

function syntax(document: Xml.Document): any {
    return shape({prefix: document.prefix, prolog: document.prolog, root: document.root, eof: document.eof});
}

describeJavaRpc("XML across the Java RPC boundary", () => {

    for (const [name, {text, sourcePath = "file.xml"}] of Object.entries(samples)) {
        testJavaRpc(`Java and TypeScript parse ${name} into the same tree`, async ({javaRpc}) => {
            // when
            const [fromJava] = await javaRpc.rpc.parse([{text, sourcePath}], Xml.Kind.Document);
            const fromTypeScript = await new XmlParser().parseOne({text, sourcePath});

            // then
            expect(fromJava.kind).toEqual(Xml.Kind.Document);
            expect(fromTypeScript.kind).toEqual(Xml.Kind.Document);
            expect(syntax(fromTypeScript as Xml.Document)).toEqual(syntax(fromJava as Xml.Document));
            expect((fromTypeScript as Xml.Document).charsetBomMarked).toEqual((fromJava as Xml.Document).charsetBomMarked);
            expect(await TreePrinters.print(fromJava)).toEqual(text);
            expect(await javaRpc.rpc.print(fromTypeScript)).toEqual(text);
        });
    }

    testJavaRpc("a TypeScript recipe edits a document the Java parser produced", async ({javaRpc}) => {
        // given
        const text = samples["UI5 view"].text;
        const [fromJava] = await javaRpc.rpc.parse([{text, sourcePath: "webapp/view/App.view.xml"}], Xml.Kind.Document);
        const visitor = new class extends XmlVisitor<ExecutionContext> {
            protected async visitAttribute(attribute: Xml.Attribute, p: ExecutionContext): Promise<Xml | undefined> {
                const a = await super.visitAttribute(attribute, p) as Xml.Attribute;
                const changed: Xml.Attribute = {...a, value: {...a.value, value: "my.app.Home"}};
                return a.key.name === "controllerName" ? changed : a;
            }
        };

        // when
        const after = await visitor.visit<Xml.Document>(fromJava, new ExecutionContext());

        // then
        expect(after).not.toBe(fromJava);
        expect(await javaRpc.rpc.print(after!)).toEqual(text.replace("sap.ui.demo.walkthrough.controller.App", "my.app.Home"));
    });

    testJavaRpc("a Java recipe edits a document the TypeScript parser produced", async ({javaRpc: _javaRpc}) => {
        const spec = new RecipeSpec();
        spec.recipe = await prepareJavaRecipe("org.openrewrite.xml.ChangeTagAttribute", {
            elementName: "//Button",
            attributeName: "icon",
            newValue: "sap-icon://home"
        });
        await spec.rewriteRun(
            //language=xml
            xml(
                `
                  <mvc:View xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc">
                      <!-- toolbar -->
                      <Button icon="sap-icon://hello-world" press=".onPress"/>
                      <Text text="&lt;hi&gt;"/>
                  </mvc:View>
                `,
                `
                  <mvc:View xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc">
                      <!-- toolbar -->
                      <Button icon="sap-icon://home" press=".onPress"/>
                      <Text text="&lt;hi&gt;"/>
                  </mvc:View>
                `
            )
        );
    });
});
