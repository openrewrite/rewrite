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
import {ExecutionContext, Option, Recipe} from "@openrewrite/rewrite";
import {escapeXml, Xml, XmlVisitor} from "@openrewrite/rewrite/xml";

export class ChangeXmlAttribute extends Recipe {
    name = "org.openrewrite.example.xml.change-attribute";
    displayName = "Change an XML attribute value";
    description = "Changes the value of every attribute with the given key.";

    @Option({
        displayName: "Key",
        description: "The key of the attribute to change."
    })
    key!: string

    @Option({
        displayName: "Value",
        description: "The value to change to."
    })
    value!: string

    constructor(options: { key: string, value: string }) {
        super(options);
    }

    async editor(): Promise<XmlVisitor<ExecutionContext>> {
        const {key, value} = this;
        return new class extends XmlVisitor<ExecutionContext> {
            protected async visitAttribute(attribute: Xml.Attribute, p: ExecutionContext): Promise<Xml | undefined> {
                const a = await super.visitAttribute(attribute, p) as Xml.Attribute;
                const escaped = escapeXml(value, a.value.quote);
                if (a.key.name !== key || a.value.value === escaped) {
                    return a;
                }
                const changed: Xml.Attribute = {...a, value: {...a.value, value: escaped}};
                return changed;
            }
        };
    }
}
