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
import {ExecutionContext, Recipe, TreeVisitor} from "../../src";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {json} from "../../src/json";
import {getAttributeValue, withValue, xml, Xml, XmlParser, XmlVisitor} from "../../src/xml";

/**
 * Points a UI5 view at a renamed controller.
 */
class ChangeControllerName extends Recipe {
    readonly name = "org.openrewrite.xml.test.ChangeControllerName";
    readonly displayName = "Change controller name";
    readonly description = "Changes the `controllerName` of UI5 XML views.";

    constructor(private readonly from: string, private readonly to: string) {
        super();
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const {from, to} = this;
        return new class extends XmlVisitor<ExecutionContext> {
            protected async visitAttribute(attribute: Xml.Attribute, ctx: ExecutionContext): Promise<Xml | undefined> {
                const a = await super.visitAttribute(attribute, ctx) as Xml.Attribute;
                if (a.key.name !== "controllerName" || a.value.value !== from) {
                    return a;
                }
                return this.produceXml<Xml.Attribute>(a, ctx, draft => {
                    draft.value.value = to;
                });
            }
        };
    }
}

describe("XmlVisitor", () => {
    const spec = new RecipeSpec();

    test("changes an attribute value", async () => {
        spec.recipe = new ChangeControllerName("my.app.controller.Main", "my.app.controller.Home");
        await spec.rewriteRun({
            ...xml(
                //language=xml
                `
                  <mvc:View controllerName="my.app.controller.Main" xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc">
                      <Page title='Main'/>
                  </mvc:View>
                `,
                //language=xml
                `
                  <mvc:View controllerName="my.app.controller.Home" xmlns="sap.m" xmlns:mvc="sap.ui.core.mvc">
                      <Page title='Main'/>
                  </mvc:View>
                `
            ),
            path: "webapp/view/Main.view.xml"
        });
    });

    test("leaves a document without a match unchanged", async () => {
        spec.recipe = new ChangeControllerName("my.app.controller.Main", "my.app.controller.Home");
        await spec.rewriteRun(
            //language=xml
            xml(`<mvc:View controllerName="other.Controller" xmlns:mvc="sap.ui.core.mvc"/>`)
        );
    });

    test("does not visit other languages", async () => {
        spec.recipe = new ChangeControllerName("a", "b");
        await spec.rewriteRun(
            //language=json
            json(`{"controllerName": "a"}`)
        );
    });

    test("changes a tag value and removes a tag", async () => {
        spec.recipe = fromVisitor(new class extends XmlVisitor<ExecutionContext> {
            protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
                const t = await super.visitTag(tag, ctx) as Xml.Tag;
                if (t.name === "version") {
                    return withValue(t, "2.0.0");
                } else if (t.name === "scope" && getAttributeValue(t, "remove") === "true") {
                    return undefined;
                }
                return t;
            }
        });
        await spec.rewriteRun(
            //language=xml
            xml(
                `
                  <dependency>
                      <version>1.0.0</version>
                      <scope remove="true">test</scope>
                      <optional/>
                  </dependency>
                `,
                `
                  <dependency>
                      <version>2.0.0</version>
                      <optional/>
                  </dependency>
                `
            )
        );
    });

    test("adds a child tag", async () => {
        spec.recipe = fromVisitor(new class extends XmlVisitor<ExecutionContext> {
            protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
                const t = await super.visitTag(tag, ctx) as Xml.Tag;
                if (t.name !== "root" || t.content?.some(c => c.kind === Xml.Kind.Tag)) {
                    return t;
                }
                const child = ((await new XmlParser().parseOne({text: "<child/>", sourcePath: "child.xml"})) as Xml.Document).root!;
                const withChild: Xml.Tag = {
                    ...t,
                    content: [{...child, prefix: "\n    "}],
                    closing: t.closing && {...t.closing, prefix: "\n"}
                };
                return withChild;
            }
        });
        await spec.rewriteRun(
            xml(
                `
                  <root></root>
                `,
                `
                  <root>
                      <child/>
                  </root>
                `
            )
        );
    });
});
