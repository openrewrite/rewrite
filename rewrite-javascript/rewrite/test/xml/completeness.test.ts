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
import fs = require('fs');
import path = require('path');
import {Xml} from "../../src/xml";

const rewriteXml = "../../../../rewrite-xml/src/main/java/org/openrewrite/xml/";

function read(relativePath: string): string {
    return fs.readFileSync(path.resolve(__dirname, relativePath), 'utf8');
}

/**
 * The fields each method of a sender writes, in the order it writes them.
 */
function fieldsSent(source: string, methodPattern: RegExp, fieldPattern: RegExp): Map<string, string[]> {
    const fields = new Map<string, string[]>();
    const methods = source.split(methodPattern);
    for (let i = 1; i < methods.length; i += 2) {
        const body = methods[i + 1].split(/\n    }\n/)[0];
        fields.set(methods[i], Array.from(body.matchAll(fieldPattern), m => m[1]));
    }
    return fields;
}

const uncapitalize = (s: string) => s.charAt(0).toLowerCase() + s.substring(1);

describe("XML LST", () => {
    test("has a kind for every class of Xml.java", () => {
        const javaKinds: string[] = [];
        const enclosing: string[] = [];
        for (const line of read(rewriteXml + "tree/Xml.java").split("\n")) {
            const m = line.match(/^( *)(?:public |static )*(?:class|interface) (\w+)/);
            if (m) {
                const depth = m[1].length / 4;
                enclosing.splice(depth, enclosing.length, m[2]);
                if (depth > 0) {
                    javaKinds.push(`org.openrewrite.xml.tree.${enclosing.join("$")}`);
                }
            }
        }

        expect(Object.values(Xml.Kind).sort()).toEqual(javaKinds.sort());
    });

    const javaSent = fieldsSent(read(rewriteXml + "internal/rpc/XmlSender.java"),
        /public Xml (\w+)\(/,
        /q\.getAndSend(?:List)?\(\w+, (?:\([^)]*\) )?[^,]*?(?:::|\.)(?:get|is)(\w+)/g);
    const tsRpc = read("../../src/xml/rpc.ts");
    const [tsSender, tsReceiver] = tsRpc.split("class XmlReceiver");
    const tsSent = fieldsSent(tsSender, /protected (\w+)\(/, /q\.getAndSend(?:List)?\(\w+, \w+ => (?:asRef\()?\w+\.(\w+)/g);
    const tsReceived = fieldsSent(tsReceiver, /protected (\w+)\(/, /^ {12}(\w+): q\.receive/gm);
    const tsMethod = (javaMethod: string) => /visitJsp(Scriptlet|Expression|Declaration|Comment)/.test(javaMethod) ?
        "visitJspContent" : javaMethod;
    const tsField = (javaField: string) => javaField === "Charset" ? "charsetName" : uncapitalize(javaField);

    test.for([...javaSent.keys()])("rpc.ts sends the fields of XmlSender.%s in its order", javaMethod => {
        const expected = javaSent.get(javaMethod)!.map(tsField);

        expect(tsSent.get(tsMethod(javaMethod))).toEqual(expected);
        expect(tsReceived.get(tsMethod(javaMethod))).toEqual(expected);
    });
});
