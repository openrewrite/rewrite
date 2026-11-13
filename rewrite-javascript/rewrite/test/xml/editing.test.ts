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
import {fromVisitor, RecipeSpec} from "../../src/test";
import {
    addAttribute,
    escapeXml,
    getAttributeValue,
    removeAttribute,
    unescapeXml,
    withAttributeValue,
    withName,
    xml,
    Xml,
    XmlVisitor
} from "../../src/xml";

function tagEditor(edit: (tag: Xml.Tag) => Xml.Tag | undefined) {
    return fromVisitor(new class extends XmlVisitor<ExecutionContext> {
        protected async visitTag(tag: Xml.Tag, ctx: ExecutionContext): Promise<Xml | undefined> {
            const t = await super.visitTag(tag, ctx) as Xml.Tag;
            return edit(t);
        }
    });
}

describe("XML editing helpers", () => {
    const spec = new RecipeSpec();

    test("renaming a tag renames its closing tag", async () => {
        spec.recipe = tagEditor(t => t.name === "item" ? withName(t, "entry") : t);
        await spec.rewriteRun(
            //language=xml
            xml(
                `
                  <list>
                      <item id="0">a</item>
                      <item id="1"/>
                  </list>
                `,
                `
                  <list>
                      <entry id="0">a</entry>
                      <entry id="1"/>
                  </list>
                `
            )
        );
    });

    test("a name with surrounding whitespace is refused", () => {
        // given
        const tag = {kind: Xml.Kind.Tag, name: "a"} as Xml.Tag;

        // when
        const rename = () => withName(tag, " b");

        // then
        expect(rename).toThrow("Tag name must not contain leading or trailing whitespace");
    });

    test("an attribute value is replaced and escaped for its quotes", async () => {
        spec.recipe = tagEditor(t => {
            const press = getAttributeValue(t, "press");
            if (press !== undefined && !press.startsWith(".")) {
                t = withAttributeValue(t, "press", "." + unescapeXml(press));
            }
            return withAttributeValue(t, "text", `Say "hi" & <wave>`);
        });
        await spec.rewriteRun(
            //language=xml
            xml(
                `
                  <View xmlns="sap.m">
                      <Button press="onPress" text="x"/>
                      <Button press='onA&amp;B' text='x'/>
                      <Button press=".onDone"/>
                  </View>
                `,
                `
                  <View xmlns="sap.m">
                      <Button press=".onPress" text="Say &quot;hi&quot; &amp; &lt;wave>"/>
                      <Button press='.onA&amp;B' text='Say "hi" &amp; &lt;wave>'/>
                      <Button press=".onDone"/>
                  </View>
                `
            )
        );
    });

    test("an attribute is added after the last one, lined up with it", async () => {
        spec.recipe = tagEditor(t => t.name === "View" ? t : addAttribute(t, "id", "a'b", "Single"));
        await spec.rewriteRun(
            //language=xml
            xml(
                `
                  <View>
                      <Button/>
                      <Input
                          value="{/name}"
                          width="10rem"/>
                      <Label id='keep'/>
                  </View>
                `,
                `
                  <View>
                      <Button id='a&apos;b'/>
                      <Input
                          value="{/name}"
                          width="10rem"
                          id='a&apos;b'/>
                      <Label id='a&apos;b'/>
                  </View>
                `
            )
        );
    });

    test("an attribute is removed with the whitespace before it", async () => {
        spec.recipe = tagEditor(t => removeAttribute(t, "visible"));
        await spec.rewriteRun(
            //language=xml
            xml(
                `
                  <View>
                      <Button visible="true"/>
                      <Input id="a" visible="{/v}" width="1rem"/>
                      <Label
                          id="b"
                          visible="false"/>
                  </View>
                `,
                `
                  <View>
                      <Button/>
                      <Input id="a" width="1rem"/>
                      <Label
                          id="b"/>
                  </View>
                `
            )
        );
    });

    test("escaping for character data and for each quote style", () => {
        // given
        const text = `a < b && c > "d" 'e'`;

        // when
        const escaped = [escapeXml(text), escapeXml(text, "Double"), escapeXml(text, "Single")];

        // then
        expect(escaped).toEqual([
            `a &lt; b &amp;&amp; c &gt; "d" 'e'`,
            `a &lt; b &amp;&amp; c > &quot;d&quot; 'e'`,
            `a &lt; b &amp;&amp; c > "d" &apos;e&apos;`
        ]);
        expect(escaped.map(e => unescapeXml(e))).toEqual([text, text, text]);
    });
});
