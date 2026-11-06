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
import {Cursor} from "../tree";
import {isAttribute, isTag, isXml, Xml} from "./tree";

export const XML_NS_URI = "http://www.w3.org/XML/1998/namespace";
export const XMLNS_ATTRIBUTE_NS_URI = "http://www.w3.org/2000/xmlns/";

/**
 * The part of a tag or attribute name after its namespace prefix, as `Namespaced.extractLocalName` returns it:
 * `Button` for both `Button` and `m:Button`.
 */
export function localName(name: string): string {
    const colon = name.indexOf(":");
    return colon === -1 ? name : name.substring(colon + 1);
}

/**
 * The namespace prefix of a tag or attribute name, as `Namespaced.extractNamespacePrefix` returns it:
 * `m` for `m:Button`, and the empty string for an unprefixed name.
 */
export function namespacePrefix(name: string): string {
    const colon = name.indexOf(":");
    return colon === -1 ? "" : name.substring(0, colon);
}

export function isNamespaceDeclaration(attributeName: string): boolean {
    return attributeName === "xmlns" || attributeName.startsWith("xmlns:");
}

/**
 * The namespaces a tag declares itself, keyed by prefix, with the empty string for the default namespace.
 * They map to the URIs as written, so `xmlns=""` maps the empty prefix to the empty string.
 */
export function namespaces(tag: Xml.Tag): Map<string, string> {
    const declared = new Map<string, string>();
    for (const attribute of tag.attributes) {
        const name = attribute.key.name;
        if (isNamespaceDeclaration(name)) {
            declared.set(name === "xmlns" ? "" : localName(name), attribute.value.value);
        }
    }
    return declared;
}

/**
 * The namespaces in scope at the cursor: those of every tag from the root down to the cursor's own tag,
 * where a declaration replaces one of the same prefix on an enclosing tag.
 */
export function allNamespaces(cursor: Cursor): Map<string, string> {
    const tags = cursor.asArray().filter((v): v is Xml.Tag => isXml(v) && isTag(v));
    const inScope = new Map<string, string>();
    for (let i = tags.length - 1; i >= 0; i--) {
        for (const [prefix, uri] of namespaces(tags[i])) {
            inScope.set(prefix, uri);
        }
    }
    return inScope;
}

/**
 * Resolves a namespace URI at the cursor, as `Namespaced.getNamespaceUri()` does.
 *
 * Given a `prefix`, this is the URI bound to it where the cursor is, with the empty string standing for the
 * default namespace. Without one, it is the namespace of the tag or attribute at the cursor: an unprefixed tag
 * is in the default namespace, an unprefixed attribute in none, and an `xmlns` declaration in the xmlns
 * namespace. The `xml` prefix is bound without a declaration, and `xmlns=""` leaves the default namespace
 * unbound. An unbound prefix resolves to `undefined`.
 */
export function namespaceUri(cursor: Cursor, prefix?: string): string | undefined {
    if (prefix === undefined) {
        const value = cursor.value;
        if (!isXml(value)) {
            return undefined;
        }
        if (isTag(value)) {
            prefix = namespacePrefix(value.name);
        } else if (isAttribute(value)) {
            const name = value.key.name;
            if (isNamespaceDeclaration(name)) {
                return XMLNS_ATTRIBUTE_NS_URI;
            }
            prefix = namespacePrefix(name);
            if (prefix === "") {
                return undefined;
            }
        } else {
            return undefined;
        }
    }
    const uri = allNamespaces(cursor).get(prefix);
    if (uri === undefined && prefix === "xml") {
        return XML_NS_URI;
    }
    return uri === "" ? undefined : uri;
}
