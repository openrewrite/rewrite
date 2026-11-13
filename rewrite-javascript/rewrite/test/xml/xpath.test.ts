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
import {Cursor, ExecutionContext, rootCursor} from "../../src";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {withAttributeValue, xml, Xml, XmlParser, XmlVisitor, XPathMatcher} from "../../src/xml";

const documents = {
    xmlDoc: `<?xml version="1.0" encoding="UTF-8"?>
<dependencies>
    <dependency>
        <groupId>org.openrewrite</groupId>
        <artifactId scope="compile">rewrite-xml</artifactId>
    </dependency>
    <dependency>
        <artifactId scope="test">assertj-core</artifactId>
    </dependency>
</dependencies>
`,
    pomXml1: `<project>
  <groupId>com.mycompany.app</groupId>
  <artifactId>my-app</artifactId>
  <version>1</version>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.0</version>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
`,
    pomXml2: `<project>
  <groupId>com.mycompany.app</groupId>
  <artifactId>my-app</artifactId>
  <version>1</version>
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.8.0</version>
          <configuration>
            <source>1.8</source>
            <target>1.8</target>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
`,
    namespacedXml: `<?xml version="1.0" encoding="UTF-8"?>
<root xmlns="http://www.example.com/namespace1"
      xmlns:ns2="http://www.example.com/namespace2"
      xmlns:ns3="http://www.example.com/namespace3"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.example.com/namespace1 http://www.example.com/namespace1.xsd
                          http://www.example.com/namespace2 http://www.example.com/namespace2.xsd
                          http://www.example.com/namespace3 http://www.example.com/namespace3.xsd">
  <element1 ns3:attribute1="content3">content1</element1>
  <ns2:element2>content2</ns2:element2>
  <parent>
      <element3 ns3:attr='test'>content3</element3>
      <ns2:element4 ns3:attr='test2'>content4</ns2:element4>
  </parent>
</root>
`,
    attributeXml: `<?xml version="1.0" encoding="UTF-8"?>
<root>
  <element1 foo="bar"><foo>baz</foo></element1>
</root>
`,
    nestedElementsXml: `<?xml version="1.0" encoding="UTF-8"?>
<root>
    <element foo="bar">
        <element foo="bar">
            <test>auie</test>
        </element>
        <element qux="quux" />
    </element>
</root>
`,
    conditionsXml: `<?xml version="1.0" encoding="UTF-8"?>
<root>
  <element1 foo="bar">
    <foo>baz</foo>
    <test>asdf</test>
  </element1>
</root>
`,
    textXml: `<?xml version="1.0" encoding="UTF-8"?>
<test>
  <foo>bar</foo>
  <foo>notBar</foo>
  <one>
    <two>two</two>
  </one>
  <one>
    <two>
      <three>three</three>
    </two>
  </one>
  <foo/>
</test>
`,
    webXml: `<?xml version="1.0" encoding="UTF-8"?>
<web-app>
  <listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
  </listener>
  <listener>
    <listener-class>com.example.OtherListener</listener-class>
  </listener>
</web-app>
`,
    pomWithParent: `<project>
  <parent>
    <groupId>com.example</groupId>
  </parent>
  <version>1.0</version>
</project>
`
};

type Expectation = [keyof typeof documents, string, boolean | number];

async function parse(text: string): Promise<Xml.Document> {
    return await new XmlParser().parseOne({text, sourcePath: "file.xml"}) as Xml.Document;
}

async function matchCount(xpath: string, document: Xml.Document): Promise<number> {
    const matcher = new XPathMatcher(xpath);
    let count = 0;
    const countMatch = (cursor: Cursor) => {
        if (matcher.matches(cursor)) {
            count++;
        }
    };
    await new class extends XmlVisitor<ExecutionContext> {
        protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
            countMatch(this.cursor);
            return super.visitTag(tag, ctx);
        }

        protected async visitAttribute(attribute: Xml.Attribute, ctx: ExecutionContext): Promise<Xml | undefined> {
            countMatch(this.cursor);
            return super.visitAttribute(attribute, ctx);
        }
    }().visit(document, new ExecutionContext());
    return count;
}

async function rootTagCursor(text: string): Promise<Cursor> {
    const document = await parse(text);
    return new Cursor(document.root, new Cursor(document, rootCursor()));
}

async function evaluate(expectations: Expectation[]): Promise<Expectation[]> {
    const actual: Expectation[] = [];
    for (const [name, xpath, expected] of expectations) {
        const count = await matchCount(xpath, await parse(documents[name]));
        actual.push([name, xpath, typeof expected === "boolean" ? count > 0 : count]);
    }
    return actual;
}

describe("XPathMatcher", () => {
    test("match absolute", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "/dependencies/dependency", 2],
            ["xmlDoc", "/dependencies/*/artifactId", 2],
            ["xmlDoc", "/dependencies/*", 2],
            ["xmlDoc", "/dependencies//dependency", 2],
            ["xmlDoc", "/dependencies//dependency//groupId", 1],
            ["xmlDoc", "/dependencies/dne", 0],
            ["xmlDoc", "/dependencies//dne", 0],
            ["xmlDoc", "/dependencies//dependency//dne", 0],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match absolute attribute", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "/dependencies/dependency/artifactId/@scope", 2],
            ["xmlDoc", "/dependencies/dependency/artifactId/@*", 2],
            ["xmlDoc", "/dependencies/dependency/groupId/@*", 0],
            ["xmlDoc", "/dependencies//dependency//@scope", 2],
            ["xmlDoc", "/dependencies//dependency//artifactId//@scope", 2],
            ["xmlDoc", "/dependencies//dependency//@*", 2],
            ["xmlDoc", "/dependencies//dependency//artifactId//@*", 2],
            ["xmlDoc", "/dependencies/dependency/artifactId/@dne", 0],
            ["xmlDoc", "/dependencies//dependency//@dne", 0],
            ["xmlDoc", "/dependencies//dependency//artifactId//@dne", 0],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match relative", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "dependencies", 1],
            ["xmlDoc", "dependency", 2],
            ["xmlDoc", "//dependency", 2],
            ["xmlDoc", "dependency/*", 3],
            ["xmlDoc", "dne", 0],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match relative attribute", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "dependency/artifactId/@scope", 2],
            ["xmlDoc", "dependency/artifactId/@*", 2],
            ["xmlDoc", "//dependency/artifactId/@scope", 2],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match nested elements with same name", async () => {
        // given
        const expected: Expectation[] = [
            ["nestedElementsXml", "element/test", true],
            ["nestedElementsXml", "element[local-name(..)='root']", true],
            ["nestedElementsXml", "element[@foo='baz']/test", false],
            ["nestedElementsXml", "element/@qux", true],
            ["nestedElementsXml", "dne[@foo='bar']/test", false],
            ["nestedElementsXml", "//element/test", true],
            ["nestedElementsXml", "//element[@foo='bar']/test", true],
            ["nestedElementsXml", "//element[@foo='baz']/test", false],
            ["nestedElementsXml", "//element/@qux", true],
            ["nestedElementsXml", "//dne[@foo='bar']/test", false],
            ["nestedElementsXml", "root//element/test", true],
            ["nestedElementsXml", "root//element[@foo='bar']/test", true],
            ["nestedElementsXml", "root//element[@foo='baz']/test", false],
            ["nestedElementsXml", "root//element/@qux", true],
            ["nestedElementsXml", "root//dne[@foo='bar']/test", false],
            ["nestedElementsXml", "/root//element/test", true],
            ["nestedElementsXml", "/root//element[@foo='bar']/test", true],
            ["nestedElementsXml", "/root//element[@foo='baz']/test", false],
            ["nestedElementsXml", "/root//element/@qux", true],
            ["nestedElementsXml", "/root//dne[@foo='bar']/test", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match pom", async () => {
        // given
        const expected: Expectation[] = [
            ["pomXml1", "/project/build/plugins/plugin/configuration/source", true],
            ["pomXml1", "/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration/source", true],
            ["pomXml1", "//plugin[artifactId='maven-compiler-plugin']/configuration/source", true],
            ["pomXml1", "/project/build/plugins/plugin[groupId='org.apache.maven.plugins']/configuration/source", true],
            ["pomXml1", "/project/build/plugins/plugin[artifactId='somethingElse']/configuration/source", false],
            ["pomXml1", "/project/build//plugins/plugin/configuration/source", true],
            ["pomXml2", "/project/build//plugins/plugin/configuration/source", true],
            ["pomXml2", "/project/build//plugin/configuration/source", true],
            ["pomXml2", "/project//configuration/source", true],
            ["pomXml2", "/project//plugin//source", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("attribute predicate", async () => {
        // given
        const expected: Expectation[] = [
            ["attributeXml", "/root/element1[@foo='bar']", true],
            ["attributeXml", "/root/element1[@foo='baz']", false],
            ["attributeXml", "/root/element1[foo='bar']", false],
            ["attributeXml", "/root/element1[foo='baz']", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("wildcards", async () => {
        // given
        const expected: Expectation[] = [
            ["attributeXml", "/root/element1[@*='bar']", true],
            ["attributeXml", "/root/element1[@*='baz']", false],
            ["attributeXml", "/root/element1[*='bar']", false],
            ["attributeXml", "/root/element1[*='baz']", true],
            ["attributeXml", "/root/*[@foo='bar']", true],
            ["attributeXml", "/root/*[@*='bar']", true],
            ["attributeXml", "/root/*[@foo='baz']", false],
            ["attributeXml", "/root/*[@*='baz']", false],
            ["attributeXml", "//*[@foo='bar']", true],
            ["attributeXml", "//*[@foo='baz']", false],
            ["attributeXml", "//*[foo='bar']", false],
            ["attributeXml", "//*[foo='baz']", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("relative paths with conditions", async () => {
        // given
        const expected: Expectation[] = [
            ["conditionsXml", "//element1[foo='bar']", false],
            ["conditionsXml", "//element1[foo='baz']", true],
            ["conditionsXml", "//element1[@foo='bar']", true],
            ["conditionsXml", "//element1[foo='baz']/test", true],
            ["conditionsXml", "//element1[foo='baz']/baz", false],
            ["conditionsXml", "//element1[foo='bar']/test", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("namespace match functions", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "/root/element1", true],
            ["namespacedXml", "/root/ns2:element2", true],
            ["namespacedXml", "/root/dne", false],
            ["namespacedXml", "/*[local-name()='element1']", false],
            ["namespacedXml", "//*[local-name()='element1']", true],
            ["namespacedXml", "/root/*[local-name()='element1']", true],
            ["namespacedXml", "/root/*[namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "/*[namespace-uri()='http://www.example.com/namespace2']", false],
            ["namespacedXml", "//*[namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//@*[namespace-uri()='http://www.example.com/namespace3']", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match text node type test", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "/root/element1/text()", true],
            ["namespacedXml", "//element1/text()", true],
            ["namespacedXml", "/root/ns2:element2/text()", true],
            ["namespacedXml", "/root/parent/text()", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match contains function", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "contains(/root/element1, 'content1')", true],
            ["namespacedXml", "contains(/root/element1, 'content')", true],
            ["namespacedXml", "contains(/root/element1, '1')", true],
            ["namespacedXml", "contains(/root/element1, 'ent')", true],
            ["namespacedXml", "contains(/root/element1, 'notfound')", false],
            ["namespacedXml", "contains(/root/element1, 'CONTENT1')", false],
            ["namespacedXml", "contains(/root/element1, '')", true],
            ["namespacedXml", "contains(/root/ns2:element2, 'content2')", true],
            ["namespacedXml", "contains(/root/parent/element3, 'content3')", true],
            ["namespacedXml", "contains(/root/nonexistent, 'anything')", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match contains in predicate", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "/dependencies/dependency[contains(groupId, 'openrewrite')]", true],
            ["xmlDoc", "/dependencies/dependency[contains(groupId, 'rewrite')]", true],
            ["xmlDoc", "/dependencies/dependency[contains(artifactId, 'rewrite')]", true],
            ["xmlDoc", "/dependencies/dependency[contains(groupId, 'notfound')]", false],
            ["xmlDoc", "/dependencies/dependency[contains(artifactId, 'notfound')]", false],
            ["xmlDoc", "//dependency[contains(groupId, 'openrewrite')]", true],
            ["xmlDoc", "//dependency[contains(artifactId, 'xml')]", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match not function", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "not(contains(/root/element1, 'content1'))", false],
            ["namespacedXml", "not(contains(/root/element1, 'notfound'))", true],
            ["namespacedXml", "not(starts-with(/root/element1, 'content'))", false],
            ["namespacedXml", "not(starts-with(/root/element1, 'xyz'))", true],
            ["namespacedXml", "not(not(contains(/root/element1, 'content1')))", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match string length function", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "string-length(/root/element1) > 0", true],
            ["namespacedXml", "string-length(/root/element1) > 2", true],
            ["namespacedXml", "string-length(/root/element1) > 100", false],
            ["namespacedXml", "string-length(/root/element1) < 100", true],
            ["namespacedXml", "string-length(/root/element1) < 5", false],
            ["namespacedXml", "string-length(/root/element1) = 8", true],
            ["namespacedXml", "string-length(/root/element1) = 7", false],
            ["namespacedXml", "string-length(/root/element1) >= 8", true],
            ["namespacedXml", "string-length(/root/element1) >= 9", false],
            ["namespacedXml", "string-length(/root/element1) <= 8", true],
            ["namespacedXml", "string-length(/root/element1) <= 7", false],
            ["namespacedXml", "string-length(/root/element1) != 7", true],
            ["namespacedXml", "string-length(/root/element1) != 8", false],
            ["namespacedXml", "string-length(/root/nonexistent) = 0", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match starts with function", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "starts-with(/root/element1, 'content')", true],
            ["namespacedXml", "starts-with(/root/element1, 'con')", true],
            ["namespacedXml", "starts-with(/root/element1, 'c')", true],
            ["namespacedXml", "starts-with(/root/element1, '')", true],
            ["namespacedXml", "starts-with(/root/element1, 'content1')", true],
            ["namespacedXml", "starts-with(/root/element1, 'ontent')", false],
            ["namespacedXml", "starts-with(/root/element1, '1')", false],
            ["namespacedXml", "starts-with(/root/element1, 'Content')", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match ends with function", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "ends-with(/root/element1, '1')", true],
            ["namespacedXml", "ends-with(/root/element1, 'ent1')", true],
            ["namespacedXml", "ends-with(/root/element1, 'content1')", true],
            ["namespacedXml", "ends-with(/root/element1, '')", true],
            ["namespacedXml", "ends-with(/root/element1, 'content')", false],
            ["namespacedXml", "ends-with(/root/element1, '2')", false],
            ["namespacedXml", "ends-with(/root/element1, 'Content1')", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match substring functions", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "substring-before(/root/element1, '1') = 'content'", true],
            ["namespacedXml", "substring-before(/root/element1, 'tent') = 'con'", true],
            ["namespacedXml", "substring-before(/root/element1, 'c') = ''", true],
            ["namespacedXml", "substring-before(/root/element1, 'notfound') = ''", true],
            ["namespacedXml", "substring-after(/root/element1, 'content') = '1'", true],
            ["namespacedXml", "substring-after(/root/element1, 'con') = 'tent1'", true],
            ["namespacedXml", "substring-after(/root/element1, '1') = ''", true],
            ["namespacedXml", "substring-after(/root/element1, 'notfound') = ''", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match count function", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "count(/root/element1)", true],
            ["namespacedXml", "count(/root/element1) > 0", true],
            ["namespacedXml", "count(/root/nonexistent) = 0", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match nested function calls", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "not(not(contains(/root/element1, 'content1')))", true],
            ["namespacedXml", "not(not(not(contains(/root/element1, 'content1'))))", false],
            ["namespacedXml", "contains(substring-after(/root/element1, 'con'), 'tent')", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match function with descendant path", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "contains(//element1, 'content1')", true],
            ["namespacedXml", "contains(//element3, 'content3')", true],
            ["namespacedXml", "string-length(//element1) = 8", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match boolean expression with relative path", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "contains(element1, 'content1')", 1],
            ["xmlDoc", "contains(groupId, 'openrewrite')", 1],
            ["xmlDoc", "contains(groupId, 'notfound')", 0],
            ["xmlDoc", "contains(/dependencies/dependency/groupId, 'openrewrite')", 1],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match local name function condition", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "/*[local-name()='root']", true],
            ["namespacedXml", "/*[local-name()='element1']", false],
            ["namespacedXml", "/*[local-name()='element2']", false],
            ["namespacedXml", "//*[local-name()='element1']", true],
            ["namespacedXml", "//*[local-name()='element2']", true],
            ["namespacedXml", "//*[local-name()='dne']", false],
            ["namespacedXml", "/root[local-name()='root']", true],
            ["namespacedXml", "//element1[local-name()='element1']", true],
            ["namespacedXml", "//element2[local-name()='element2']", false],
            ["namespacedXml", "//ns2:element2[local-name()='element2']", true],
            ["namespacedXml", "//dne[local-name()='dne']", false],
            ["namespacedXml", "/root//element1[local-name()='element1']", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match namespace uri function condition", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "/root/*[namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "/*[namespace-uri()='http://www.example.com/namespace2']", false],
            ["namespacedXml", "//*[namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//ns2:element2[namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//element2[namespace-uri()='http://www.example.com/namespace2']", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match attribute condition", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "//*[@*='content3']", true],
            ["namespacedXml", "//*[@*='content2']", false],
            ["namespacedXml", "//*[@ns3:attribute1='content3']", true],
            ["namespacedXml", "//*[@attribute1='content3']", false],
            ["namespacedXml", "//element1[@ns3:attribute1='content3']", true],
            ["namespacedXml", "//element1[@attribute1='content3']", false],
            ["namespacedXml", "//element1[@*='content3']", true],
            ["namespacedXml", "//element1[@*='dne']", false],
            ["namespacedXml", "/root/element1[@*='content3']", true],
            ["namespacedXml", "/root/element1[@*='dne']", false],
            ["namespacedXml", "/root//element1[@*='content3']", true],
            ["namespacedXml", "/root//element1[@*='dne']", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match attribute element", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "//@ns3:attribute1", true],
            ["namespacedXml", "//@attribute1", false],
            ["namespacedXml", "//@*", true],
            ["namespacedXml", "//@*[local-name()='attribute1']", true],
            ["namespacedXml", "//@*[local-name()='attribute2']", false],
            ["namespacedXml", "//@*[namespace-uri()='http://www.example.com/namespace3']", true],
            ["namespacedXml", "//@*[namespace-uri()='http://www.example.com/namespace2']", false],
            ["namespacedXml", "//element1/@*", true],
            ["namespacedXml", "/root/element1/@*", true],
            ["namespacedXml", "//element1/@*[namespace-uri()='http://www.example.com/namespace3']", true],
            ["namespacedXml", "//element1/@*[namespace-uri()='http://www.example.com/namespace2']", false],
            ["namespacedXml", "//ns2:element2/@*", false],
            ["namespacedXml", "/root/ns2:element2/@*", false],
            ["namespacedXml", "/root/parent/element3/@attr", false],
            ["namespacedXml", "/root/parent/element3/@ns3:attr", true],
            ["namespacedXml", "/root/parent/element3/@ns3:attr[namespace-uri()='http://www.example.com/namespace3']", true],
            ["namespacedXml", "//element3/@ns3:attr[namespace-uri()='http://www.example.com/namespace3']", true],
            ["namespacedXml", "/root//element1/@*", true],
            ["namespacedXml", "/root//element1/@*[namespace-uri()='http://www.example.com/namespace3']", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match multiple conditions", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "//*[namespace-uri()='http://www.example.com/namespace2'][local-name()='element2']", true],
            ["namespacedXml", "//*[local-name()='element2'][namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//*[namespace-uri()='http://www.example.com/namespace2'][local-name()='dne']", false],
            ["namespacedXml", "//*[local-name()='dne'][namespace-uri()='http://www.example.com/namespace2']", false],
            ["namespacedXml", "//*[local-name()='element1'][@ns3:attribute1='content3']", true],
            ["namespacedXml", "//@*[namespace-uri()='http://www.example.com/namespace3'][local-name()='attribute1']", true],
            ["namespacedXml", "//@*[namespace-uri()='http://www.example.com/namespace3'][local-name()='dne']", false],
            ["namespacedXml", "//*[@ns3:attr='test'][local-name()='element3']", true],
            ["namespacedXml", "//*[@ns3:attr='test'][local-name()='elementX']", false],
            ["namespacedXml", "//*[@ns3:attr='test2'][local-name()='element4'][namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//*[@ns3:attr='testX'][local-name()='element4'][namespace-uri()='http://www.example.com/namespace2']", false],
            ["namespacedXml", "//*[@ns3:attr='test2'][local-name()='elementX'][namespace-uri()='http://www.example.com/namespace2']", false],
            ["namespacedXml", "//*[@ns3:attr='test2'][local-name()='element4'][namespace-uri()='http://www.example.com/namespaceX']", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match text function condition", async () => {
        // given
        const expected: Expectation[] = [
            ["textXml", "/test/one[two/three/text()='three']", true],
            ["textXml", "/test/one[two/three/text()='notthree']", false],
            ["textXml", "/test/one[two/text()='two']", true],
            ["textXml", "/test/one[two/text()='nottwo']", false],
            ["textXml", "/test/foo[text()='bar']", true],
            ["textXml", "/test/foo[text()='notBar']", true],
            ["textXml", "/test/foo[text()='nonexistent']", false],
            ["textXml", "//foo[text()='bar']", true],
            ["textXml", "//foo[text()='notBar']", true],
            ["textXml", "//foo[text()='nonexistent']", false],
            ["textXml", "/test/*[text()='bar']", true],
            ["textXml", "//*[text()='bar']", true],
            ["textXml", "//*[text()='notBar']", true],
            ["textXml", "//*[local-name()='foo' and text()='bar']", true],
            ["textXml", "//*[local-name()='foo' and text()='notBar']", true],
            ["textXml", "//*[local-name()='foo' and text()='nonexistent']", false],
            ["textXml", "/test/foo[text()='bar' or text()='notBar']", true],
            ["textXml", "/test/foo[text()='nonexistent' or text()='bar']", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match conditions with conjunctions", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "//*[local-name()='element3' and @ns3:attr='test']", true],
            ["namespacedXml", "//*[local-name()='element3' and @ns3:attr='dne']", false],
            ["namespacedXml", "//*[local-name()='dne' and @ns3:attr='test']", false],
            ["namespacedXml", "//*[local-name()='dne' and @ns3:attr='dne']", false],
            ["namespacedXml", "//*[local-name()='element2' or namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//*[local-name()='element2' or namespace-uri()='dne']", true],
            ["namespacedXml", "//*[local-name()='dne' or local-name()='element2']", true],
            ["namespacedXml", "//*[local-name()='dne' or local-name()='dne2']", false],
            ["namespacedXml", "//@*[namespace-uri()='dne' or namespace-uri()='http://www.example.com/namespace3']", true],
            ["namespacedXml", "//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2' and @ns3:attr='test2']", true],
            ["namespacedXml", "//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2' and @ns3:attr='dne']", false],
            ["namespacedXml", "//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2' or @ns3:attr='dne']", true],
            ["namespacedXml", "//*[local-name()='element4' and @ns3:attr='dne' or namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//*[local-name()='element4' and @ns3:attr='dne' or namespace-uri()='http://www.example.com/namespaceX']", false],
            ["namespacedXml", "//*[local-name()='dne' or local-name()='dne2' or local-name()='element2']", true],
            ["namespacedXml", "//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2'][@ns3:attr='test2']", true],
            ["namespacedXml", "//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2'][@ns3:attr='dne']", false],
            ["namespacedXml", "//*[local-name()='dne' and namespace-uri()='http://www.example.com/namespace2'][@ns3:attr='test2']", false],
            ["namespacedXml", "//*[local-name()='dne' or local-name()='element4'][namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//*[local-name()='dne' or local-name()='element4'][namespace-uri()='http://www.example.com/namespaceX']", false],
            ["namespacedXml", "//*[local-name()='dne' or local-name()='element4' and namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//*[local-name()='dne' or local-name()='element4' and namespace-uri()='http://www.example.com/namespaceX']", false],
            ["namespacedXml", "//*[local-name()='dne' or namespace-uri()='http://www.example.com/namespaceX' and local-name()='element4']", false],
            ["namespacedXml", "//*[local-name()='element4' or local-name()='dne' and namespace-uri()='http://www.example.com/namespace2']", true],
            ["namespacedXml", "//*[local-name()='element4' or local-name()='dne' and namespace-uri()='http://www.example.com/namespaceX']", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match not with comparison argument", async () => {
        // given
        const expected: Expectation[] = [
            ["webXml", "/web-app/listener[not(listener-class/text() = 'org.springframework.web.context.ContextLoaderListener')]", 1],
            ["webXml", "/web-app/listener[listener-class/text() = 'org.springframework.web.context.ContextLoaderListener']", 1],
            ["webXml", "/web-app/listener[not(listener-class/text() = 'com.example.OtherListener')]", 1],
            ["webXml", "/web-app/listener", 2],
            ["webXml", "/web-app/listener[not(listener-class/text() != 'com.example.OtherListener')]", 1],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match node type tests", async () => {
        // given
        const expected: Expectation[] = [
            ["namespacedXml", "/root/element1/text()", true],
            ["namespacedXml", "//element1/text()", true],
            ["namespacedXml", "/root/parent/text()", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match positional predicates", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "/dependencies/dependency[1]", 1],
            ["xmlDoc", "/dependencies/dependency[2]", 1],
            ["xmlDoc", "/dependencies/dependency[3]", 0],
            ["xmlDoc", "/dependencies/dependency[last()]", 1],
            ["xmlDoc", "/dependencies/dependency[position()=1]", 1],
            ["xmlDoc", "/dependencies/dependency[position()=2]", 1],
            ["xmlDoc", "/dependencies/dependency[position()=3]", 0],
            ["xmlDoc", "/dependencies/dependency[1]/groupId", 1],
            ["xmlDoc", "/dependencies/dependency[2]/groupId", 0],
            ["xmlDoc", "/dependencies/dependency[position()>1]", 1],
            ["xmlDoc", "/dependencies/dependency[position()<2]", 1],
            ["xmlDoc", "/dependencies/dependency[position()>=1]", 2],
            ["xmlDoc", "/dependencies/dependency[position()<=2]", 2],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match positional with other conditions", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "/dependencies/dependency[1]/artifactId[@scope='compile']", 1],
            ["xmlDoc", "/dependencies/dependency[2]/artifactId[@scope='test']", 1],
            ["xmlDoc", "/dependencies/dependency[1]/artifactId[@scope='test']", 0],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match parenthesized path expressions", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "(/dependencies/dependency)[1]", 1],
            ["xmlDoc", "(/dependencies/dependency)[2]", 1],
            ["xmlDoc", "(/dependencies/dependency)[3]", 0],
            ["xmlDoc", "(/dependencies/dependency)[last()]", 1],
            ["xmlDoc", "(/dependencies/dependency)[position()=1]", 1],
            ["xmlDoc", "(/dependencies/dependency)[position()=2]", 1],
            ["xmlDoc", "(//dependency)[1]", 1],
            ["xmlDoc", "(//dependency)[last()]", 1],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match advanced filter expressions", async () => {
        // given
        const expected: Expectation[] = [
            ["pomXml1", "(/project/build/plugins/plugin)[1]/groupId", 1],
            ["pomXml1", "(/project/build/plugins/plugin)[1]/artifactId", 1],
            ["pomXml1", "(/project/build/plugins/plugin)[1]/configuration", 1],
            ["pomXml1", "(/project/build/plugins/plugin)[1]/configuration/source", 1],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match abbreviated syntax", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "/dependencies/./dependency", 2],
            ["xmlDoc", "/dependencies/dependency/.", 2],
            ["xmlDoc", "/dependencies/dependency/groupId/..", 1],
            ["xmlDoc", "/dependencies/dependency/groupId/../artifactId", 1],
            ["xmlDoc", "/dependencies/dependency/groupId/../../dependency", 2],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match parent axis", async () => {
        // given
        const expected: Expectation[] = [
            ["xmlDoc", "/dependencies/dependency/groupId/parent::node()", true],
            ["xmlDoc", "/dependencies/dependency/groupId/parent::dependency", true],
            ["xmlDoc", "/dependencies/dependency/parent::dependencies", true],
            ["xmlDoc", "/dependencies/self::node()", true],
            ["xmlDoc", "/dependencies/self::dependencies", true],
            ["xmlDoc", "/dependencies/dependency/groupId/parent::dependency/artifactId", true],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match descendant with child path", async () => {
        // given
        const expected: Expectation[] = [
            ["pomXml1", "//plugins/plugin", true],
            ["pomXml1", "//plugins/plugin/groupId", true],
            ["pomXml1", "//plugins/plugin/artifactId", true],
            ["pomXml1", "//plugins/plugin/configuration", true],
            ["pomXml1", "//plugins/plugin/configuration/source", true],
            ["pomXml2", "//plugins/plugin", true],
            ["pomXml2", "//pluginManagement/plugins/plugin", true],
            ["pomXml1", "//plugins/nonexistent", false],
            ["pomXml1", "//nonexistent/plugin", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match descendant starting with root element", async () => {
        // given
        const expected: Expectation[] = [
            ["pomXml1", "//project/build", true],
            ["pomXml1", "//project/build/plugins", true],
            ["pomXml1", "//project/build/plugins/plugin", true],
            ["pomXml2", "//project/build/pluginManagement/plugins/plugin", true],
            ["pomXml1", "//project/build/pluginManagement", false],
            ["pomXml1", "//project/nonexistent", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match root element", async () => {
        // given
        const expected: Expectation[] = [
            ["pomXml1", "/project", 1],
            ["xmlDoc", "/dependencies", 1],
            ["pomWithParent", "/project/parent", 1],
            ["pomWithParent", "/project", 1],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("match relative path from context", async () => {
        // given
        const expected: Expectation[] = [
            ["pomXml1", "version", true],
            ["pomXml1", "groupId", true],
            ["pomXml1", "artifactId", true],
            ["pomXml1", "configuration/source", true],
            ["pomXml1", "plugin/configuration", true],
            ["pomXml1", "plugins/plugin", true],
            ["pomXml1", "nonexistent", false],
            ["pomXml1", "configuration/nonexistent", false],
        ];

        // when
        const actual = await evaluate(expected);

        // then
        expect(actual).toEqual(expected);
    });

    test("node type tests match the nodes they name", async () => {
        // given
        const document = await parse(`<root><a>x</a><!--c--><?pi d?><b><c/></b></root>`);
        const expressions = ["/root/a/text()", "/root/comment()", "/root/processing-instruction()", "/root/b/node()"];

        // when
        const matched: { [xpath: string]: string[] } = {};
        for (const xpath of expressions) {
            const matcher = new XPathMatcher(xpath);
            matched[xpath] = [];
            await new class extends XmlVisitor<ExecutionContext> {
                protected async preVisit(tree: Xml, p: ExecutionContext): Promise<Xml | undefined> {
                    if (matcher.matches(this.cursor)) {
                        matched[xpath].push(tree.kind.substring(tree.kind.lastIndexOf("$") + 1));
                    }
                    return super.preVisit(tree, p);
                }
            }().visit(document, new ExecutionContext());
        }

        // then
        expect(matched).toEqual({
            "/root/a/text()": ["Tag", "CharData"],
            "/root/comment()": ["Comment"],
            "/root/processing-instruction()": ["ProcessingInstruction"],
            "/root/b/node()": ["Tag", "Closing"]
        });
    });

    test.each([
        ["0x1p3", "7", ">", true],
        ["-0X1.CP+2D", "-6", "<", true],
        ["0x.8p4f", "8", ">=", true],
        ["0x1p1024", "Infinity", ">=", true],
        ["-0x1p1024", "-Infinity", "<=", true],
        ["0x1.fffffffffffffp1023", "1.7976931348623157e308", ">=", true],
        ["0x1.fffffffffffffp1023", "1.7976931348623157e308", ">", false],
        ["0x1p-1074", "4.9e-324", ">=", true],
        ["0x1p-1075", "0", ">", false],
        ["0x1.0000000000001p-1075", "0", ">", true],
        ["0x1.00000000000008p0", "1", ">", false],
        ["0x1.000000000000080001p0", "1", ">", true],
        ["0x1.00000000000018p0", "1.0000000000000004", ">=", true],
        ["0x0p999999", "0", ">", false],
        ["0x1p-999999", "0", ">", false],
        ["0x1p3", "7", "<", false],
        ["0x1p3", "9", ">", false],
        ["0x1", "7", ">", false],
    ])("compare Java hexadecimal double %s %s %s", async (value, bound, operator, expected) => {
        // given
        const document = await parse(`<root><n>${value}</n></root>`);

        // when
        const count = await matchCount(`/root/n[text() ${operator} '${bound}']`, document);

        // then
        expect(count).toBe(expected ? 1 : 0);
    });

    test.each([
        ["self::node()", 0],
        ["self::comment()", 0],
        ["self::processing-instruction()", 0],
        ["self::text()", 1],
        ["child::text()", 1],
        ["b/node()", 1],
    ])("resolve the value of %s in a predicate", async (path, expected) => {
        // given
        const document = await parse(`<root><a>x</a><a><b>x</b></a></root>`);

        // when
        const count = await matchCount(`/root/a[${path}='x']`, document);

        // then
        expect(count).toBe(expected);
    });

    test("an expression that does not parse throws when it is first matched", async () => {
        // given
        const cursor = await rootTagCursor(documents.xmlDoc);
        const matcher = new XPathMatcher("/dependencies/dependency[");

        // when
        const match = () => matcher.matches(cursor);

        // then
        expect(match).toThrow("Syntax error at offset 25");
    });

    test("an unsupported construct throws", async () => {
        // given
        const cursor = await rootTagCursor(documents.xmlDoc);
        const match = (xpath: string) => () => new XPathMatcher(xpath).matches(cursor);

        // when
        const union = match("//groupId | //artifactId");
        const unknownFunction = match("//dependency[lower-case(groupId) = 'x']");
        const decimal = match("//dependency[1.5]");
        const trailing = match("//dependency groupId");

        // then
        expect(union).toThrow("token recognition error at: '|'");
        expect(unknownFunction).toThrow("Unsupported XPath function: lower-case()");
        expect(decimal).toThrow("Unsupported XPath expression: invalid number: 1.5");
        expect(trailing).toThrow("extraneous input 'groupId'");
    });

    test("a recipe edits the tags an expression selects", async () => {
        const matcher = new XPathMatcher("//headerContent/Button");
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends XmlVisitor<ExecutionContext> {
            protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
                const t = await super.visitTag(tag, ctx) as Xml.Tag;
                return matcher.matches(this.cursor) ? withAttributeValue(t, "type", "Emphasized") : t;
            }
        });

        await spec.rewriteRun(
            //language=xml
            xml(
                `
                  <mvc:View xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc">
                      <Page>
                          <headerContent>
                              <Button type="Default" text="Save"/>
                              <Toolbar><Button type="Default"/></Toolbar>
                          </headerContent>
                          <Button type="Default"/>
                      </Page>
                  </mvc:View>
                `,
                `
                  <mvc:View xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc">
                      <Page>
                          <headerContent>
                              <Button type="Emphasized" text="Save"/>
                              <Toolbar><Button type="Default"/></Toolbar>
                          </headerContent>
                          <Button type="Default"/>
                      </Page>
                  </mvc:View>
                `
            )
        );
    });
});
