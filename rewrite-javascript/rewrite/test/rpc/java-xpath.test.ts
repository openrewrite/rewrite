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
import {describeJavaRpc, testJavaRpc} from "../../src/test/java-rpc.mjs";
import {ExecutionContext} from "../../src/execution";
import {findMarker, MarkersKind} from "../../src/markers";
import {Xml, XmlParser, XmlVisitor, XPathMatcher} from "../../src/xml";

const documents: { [name: string]: string } = {
    pom: `<project>
  <groupId>com.example</groupId>
  <version>1</version>
  <dependencies>
    <dependency>
      <groupId>org.openrewrite</groupId>
      <artifactId scope="compile">rewrite-xml</artifactId>
    </dependency>
    <dependency>
      <artifactId scope="test">assertj-core</artifactId>
    </dependency>
  </dependencies>
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <artifactId>maven-compiler-plugin</artifactId>
          <configuration><source>1.8</source><target>1.8</target></configuration>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
`,
    namespaced: `<?xml version="1.0" encoding="UTF-8"?>
<root xmlns="http://www.example.com/namespace1" xmlns:ns2="http://www.example.com/namespace2"
      xmlns:ns3="http://www.example.com/namespace3">
  <element1 ns3:attribute1="content3">content1</element1>
  <ns2:element2>content2</ns2:element2>
  <parent>
    <element3 ns3:attr='test'>content3</element3>
    <ns2:element4 ns3:attr='test2'>content4</ns2:element4>
  </parent>
</root>
`,
    view: `<mvc:View controllerName="a.Main" xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc" xmlns:m="sap.m">
  <Page title="{i18n>title}">
    <headerContent>
      <Button press="onSave" text="Save"/>
      <m:Button press=".onCancel"/>
    </headerContent>
    <content>
      <Button press="onOther"/>
    </content>
  </Page>
</mvc:View>
`,
    numbers: `<root><n>0x1p3</n><n>-0X1.CP+2D</n><n>0x.8p4f</n><n>0x1p1024</n><n>-0x1p1024</n>
<n>0x1.fffffffffffffp1023</n><n>0x1p-1074</n><n>0x1p-1075</n><n>0x1.0000000000001p-1075</n>
<n>0x1.00000000000008p0</n><n>0x1.000000000000080001p0</n><n>0x1.00000000000018p0</n>
<n>0x0p999999</n><n>0x1p-999999</n><n>0x1</n></root>`,
    mixed: `<root>text<a>1</a>more<!--c--><?pi x?><a>2</a><b x="1" y='2'>&lt;e&gt;</b><b><![CDATA[cd]]></b><c> </c><n>10</n><n>9</n><n>abc</n><a x="3"/><d><a>4</a></d></root>
`
};

const expressions = [
    "/project/dependencies/dependency",
    "/project//dependency/artifactId",
    "dependency/*",
    "//dependency[groupId]",
    "//dependency[not(groupId)]",
    "//dependency[artifactId/@scope='test']",
    "//dependency[artifactId[@scope='test']]",
    "//dependency[1]",
    "//dependency[last()]/artifactId",
    "//dependency[position() > 1]",
    "//artifactId[@scope='compile']",
    "//*[@scope]",
    "//plugin[artifactId='maven-compiler-plugin']//source",
    "/project/build//plugin/configuration/*[last()]",
    "//target[text() >= '1.8']",
    "//groupId/..",
    "/project/dependencies/dependency/groupId/../artifactId",
    "/project/dependencies/dependency/groupId/../../dependency",
    "//groupId/parent::dependency",
    "/project/self::project",
    "/project/child::version",
    "(//dependency)[2]",
    "(/project/dependencies/dependency)[1]/artifactId",
    "(//plugin)[1]//source",
    "contains(/project/version, '1')",
    "contains(groupId, 'openrewrite')",
    "count(/project/dependencies/dependency) = 2",
    "string-length(//version) = 1",
    "/root/ns2:element2",
    "//*[local-name()='element2']",
    "//*[namespace-uri()='http://www.example.com/namespace2']",
    "//*[local-name(..)='parent']",
    "//*[namespace-uri(..)='http://www.example.com/namespace1']",
    "//*[@ns3:attr='test2'][local-name()='element4']",
    "//*[local-name()='dne' or local-name()='element3' and @ns3:attr='test']",
    "//element1[text()='content1']",
    "//*[starts-with(text(), 'content') and not(ends-with(text(), '4'))]",
    "//*[substring-after(text(), 'content') = '3']",
    "//headerContent/Button",
    "//*[local-name()='Button']",
    "//Button[starts-with(@press, '.')]",
    "//m:Button",
    "//Page[@title='{i18n>title}']",
    "/mvc:View[@controllerName='a.Main']",
    "//content/Button[1]",
    "/root/a[2]",
    "//a[.='1']",
    "//a[self::node()='1']",
    "//a[self::comment()='1']",
    "//a[self::processing-instruction()='1']",
    "//a[self::text()='1']",
    "//a[child::text()='1']",
    "//dependency[artifactId/node()='rewrite-xml']",
    "//a[text()='4']",
    "//b[text()='<e>']",
    "//b[text()='cd']",
    "//c[text()]",
    "//n[text() > '7']",
    "//n[text() < '-6']",
    "//n[text() >= '8']",
    "//n[text() >= 'Infinity']",
    "//n[text() <= '-Infinity']",
    "//n[text() >= '1.7976931348623157e308']",
    "//n[text() > '1.7976931348623157e308']",
    "//n[text() >= '4.9e-324']",
    "//n[text() > '0']",
    "//n[text() > '1']",
    "//n[text() >= '1.0000000000000004']",
    "//n[text() < '7']",
    "//n[text() > '9']",
    "//n[text() > 9]",
    "//n[text() < 'b']",
    "//b[@y > @x]",
    "//*[count(//a) = 4]",
    "/root/node()",
    "/root/a/text()",
    "(//a)[last()]",
    "(//a)[position() > 2]",
    "/*/*[2]",
    "//*[*]"
];

async function foundByJava(document: Xml.Document, xpath: string,
                           prepareRecipe: (id: string, options: any) => Promise<{ editor(): Promise<any> }>): Promise<string[]> {
    const recipe = await prepareRecipe("org.openrewrite.xml.search.FindTags", {xPath: xpath});
    const after = await (await recipe.editor()).visit(document, new ExecutionContext()) as Xml.Document;
    const found: string[] = [];
    await new class extends XmlVisitor<ExecutionContext> {
        protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
            if (findMarker(tag, MarkersKind.SearchResult)) {
                found.push(tag.id);
            }
            return super.visitTag(tag, ctx);
        }
    }().visit(after, new ExecutionContext());
    return found;
}

async function foundByTypeScript(document: Xml.Document, xpath: string): Promise<string[]> {
    const matcher = new XPathMatcher(xpath);
    const found: string[] = [];
    await new class extends XmlVisitor<ExecutionContext> {
        protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
            if (matcher.matches(this.cursor)) {
                found.push(tag.id);
            }
            return super.visitTag(tag, ctx);
        }
    }().visit(document, new ExecutionContext());
    return found;
}

describeJavaRpc("XPathMatcher against the Java XPathMatcher", () => {
    testJavaRpc("both find the same tags", async ({javaRpc}) => {
        // given
        const parsed = await Promise.all(Object.entries(documents).map(async ([name, text]) =>
            [name, await new XmlParser().parseOne({text, sourcePath: `${name}.xml`}) as Xml.Document] as const));

        // when
        const java: string[] = [];
        const typeScript: string[] = [];
        for (const xpath of expressions) {
            for (const [name, document] of parsed) {
                const fromJava = await foundByJava(document, xpath, (id, options) => javaRpc.rpc.prepareRecipe(id, options));
                const fromTypeScript = await foundByTypeScript(document, xpath);
                java.push(`${name} ${xpath}: ${fromJava.length} ${fromJava.join(",")}`);
                typeScript.push(`${name} ${xpath}: ${fromTypeScript.length} ${fromTypeScript.join(",")}`);
            }
        }

        // then
        expect(typeScript).toEqual(java);
        expect(java.filter(f => !f.endsWith(": 0 ")).length).toBeGreaterThan(expressions.length / 2);
    }, 300_000);
});
