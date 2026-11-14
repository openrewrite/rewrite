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
import {ExecutionContext} from "../../src";
import {
    allNamespaces,
    localName,
    namespacePrefix,
    namespaces,
    namespaceUri,
    Xml,
    XML_NS_URI,
    XMLNS_ATTRIBUTE_NS_URI,
    XmlParser,
    XmlVisitor
} from "../../src/xml";

async function parseDocument(text: string): Promise<Xml.Document> {
    const sourceFile = await new XmlParser().parseOne({text, sourcePath: "Main.view.xml"});
    expect(sourceFile.kind).toEqual(Xml.Kind.Document);
    return sourceFile as Xml.Document;
}

/**
 * Resolves `namespaceUri` at every tag and attribute of a document, keyed by its name.
 */
async function resolveAll(document: Xml.Document, prefix?: string): Promise<Map<string, string | undefined>> {
    const resolved = new Map<string, string | undefined>();
    await new class extends XmlVisitor<ExecutionContext> {
        protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
            resolved.set(tag.name, namespaceUri(this.cursor, prefix));
            return super.visitTag(tag, ctx);
        }

        protected async visitAttribute(attribute: Xml.Attribute, ctx: ExecutionContext): Promise<Xml | undefined> {
            resolved.set(`@${attribute.key.name}`, namespaceUri(this.cursor, prefix));
            return super.visitAttribute(attribute, ctx);
        }
    }().visit(document, new ExecutionContext());
    return resolved;
}

describe("XML namespaces", () => {
    test("local name and prefix of a name", () => {
        // when
        const names = ["Button", "m:Button", "core:require"].map(n => [localName(n), namespacePrefix(n)]);

        // then
        expect(names).toEqual([["Button", ""], ["Button", "m"], ["require", "core"]]);
    });

    test("the namespaces a tag declares", async () => {
        // given
        const document = await parseDocument(
            `<mvc:View xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc" controllerName="a.Main"/>`
        );

        // when
        const declared = namespaces(document.root!);

        // then
        expect([...declared]).toEqual([["", "sap.m"], ["mvc", "sap.ui.core.mvc"]]);
    });

    test("a tag resolves its prefix through the tags around it", async () => {
        // given
        const document = await parseDocument(`
            <mvc:View xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc" xmlns:core="sap.ui.core">
                <Page>
                    <core:Fragment/>
                    <m:Button xmlns:m="sap.m"/>
                    <Input/>
                </Page>
            </mvc:View>
        `);

        // when
        const resolved = await resolveAll(document);

        // then
        expect(resolved.get("mvc:View")).toEqual("sap.ui.core.mvc");
        expect(resolved.get("Page")).toEqual("sap.m");
        expect(resolved.get("core:Fragment")).toEqual("sap.ui.core");
        expect(resolved.get("m:Button")).toEqual("sap.m");
        expect(resolved.get("Input")).toEqual("sap.m");
    });

    test("an unprefixed attribute is in no namespace and a prefixed one in its prefix's", async () => {
        // given
        const document = await parseDocument(
            `<View xmlns="sap.m" xmlns:core="sap.ui.core" core:require="{F: 'a/F'}" press="onPress" xml:lang="en"/>`
        );

        // when
        const resolved = await resolveAll(document);

        // then
        expect(resolved.get("@core:require")).toEqual("sap.ui.core");
        expect(resolved.get("@press")).toBeUndefined();
        expect(resolved.get("@xmlns")).toEqual(XMLNS_ATTRIBUTE_NS_URI);
        expect(resolved.get("@xmlns:core")).toEqual(XMLNS_ATTRIBUTE_NS_URI);
        expect(resolved.get("@xml:lang")).toEqual(XML_NS_URI);
    });

    test("a nearer declaration replaces an outer one and xmlns=\"\" undeclares the default", async () => {
        // given
        const document = await parseDocument(`
            <a xmlns="urn:outer" xmlns:p="urn:p1">
                <p:b xmlns:p="urn:p2"/>
                <c xmlns="">
                    <d/>
                </c>
                <e/>
            </a>
        `);

        // when
        const resolved = await resolveAll(document);

        // then
        expect(resolved.get("p:b")).toEqual("urn:p2");
        expect(resolved.get("c")).toBeUndefined();
        expect(resolved.get("d")).toBeUndefined();
        expect(resolved.get("e")).toEqual("urn:outer");
    });

    test("a given prefix resolves where the cursor is, and an undeclared one does not", async () => {
        // given
        const document = await parseDocument(`
            <mvc:View xmlns:mvc="sap.ui.core.mvc" xmlns:core="sap.ui.core">
                <Page/>
            </mvc:View>
        `);

        // when
        const core = await resolveAll(document, "core");
        const undeclared = await resolveAll(document, "m");

        // then
        expect(core.get("Page")).toEqual("sap.ui.core");
        expect(undeclared.get("Page")).toBeUndefined();
    });

    test("all namespaces in scope at a tag", async () => {
        // given
        const document = await parseDocument(`<a xmlns="urn:a" xmlns:p="urn:p"><b xmlns="urn:b"/></a>`);
        let inScope: Map<string, string> | undefined;

        // when
        await new class extends XmlVisitor<ExecutionContext> {
            protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
                if (tag.name === "b") {
                    inScope = allNamespaces(this.cursor);
                }
                return super.visitTag(tag, ctx);
            }
        }().visit(document, new ExecutionContext());

        // then
        expect(inScope && Object.fromEntries(inScope)).toEqual({"": "urn:b", p: "urn:p"});
    });
});
