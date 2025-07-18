/*
 * Copyright 2025 the original author or authors.
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


function readFileToString(relativePath: string): string {
    return fs.readFileSync(path.resolve(__dirname + "/../../src/", relativePath), 'utf8');
}

function uppercaseFirstChar(str: string): string {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

describe('JS LST element tree', () => {
    const treeTSContent = readFileToString('javascript/tree.ts').split("export namespace JSX")[0];
    const tsInterfaces = Array.from(treeTSContent.matchAll(/export interface (\w+) extends JS/gm), m => m[1])
        .sort();
    const propertiesInEachInterface =
        new Map<string, string[]>(treeTSContent.split(/export interface /gm).slice(1)
            .map((interfaceDefinition, i) => {
                const interfaceName = interfaceDefinition.match(/(\w+) extends JS/)?.[1]!;
                return [
                    interfaceName,
                    Array.from(interfaceDefinition.matchAll(/readonly (\w+):/gm), m => m[1])
                        .filter(property => property !== "kind")
                ]
            }));

    test("should have a .java class counterpart for every JS LST element type", () => {
        const javaClasses = Array.from(readFileToString('../../src/main/java/org/openrewrite/javascript/tree/JS.java')
            .matchAll(/class (\w+) implements JS/gm), m => m[1])
            .sort();

        expect(tsInterfaces).toEqual(javaClasses);
    })

    const visitorJavascriptMethods = Array.from(
        readFileToString('javascript/visitor.ts').matchAll(/protected async (visit\w+)[(<]/gm), m => m[1])
        .sort();
    const visitorJavaMethods = Array.from(
        readFileToString('java/visitor.ts').matchAll(/protected async (visit\w+)[(<]/gm), m => m[1])
        .sort();
    const excusedVisitorMethods = ["visitContainer", "visitExpression", "visitStatement", "visitSpace",
                                "visitLeftPadded", "visitRightPadded",
                                "visitOptionalContainer", "visitOptionalLeftPadded", "visitOptionalRightPadded",
                                "visitType", "visitTypeName"];
    const visitorMethods = visitorJavascriptMethods.concat(visitorJavaMethods)
        .filter(m => !excusedVisitorMethods.includes(m))
        .sort();

    test("comparator.ts", () => {
        const comparatorTsMethods = Array.from(
            readFileToString('javascript/comparator.ts').matchAll(/override async (visit\w+)[(<]/gm), m => m[1])
            .sort();

        expect(comparatorTsMethods).toEqual(visitorMethods);
    })

    test("JavaScriptIsoVisitor.java", () => {
        const javaVisitorMethods = Array.from(readFileToString('../../src/main/java/org/openrewrite/javascript/JavaScriptIsoVisitor.java')
            .matchAll(/public [^ ]+ (visit\w+)(?:<[^(]+>)?[(]JSX?[.]/gm), m => m[1])
            .sort();

        expect(visitorJavascriptMethods).toEqual(javaVisitorMethods);
    })

    test.each([
        "JavaScriptVisitor.java",
        "internal/rpc/JavaScriptValidator.java",
        "internal/rpc/JavaScriptReceiver.java",
        "internal/rpc/JavaScriptSender.java",
    ])("%s", (path) => {
        const classContent = readFileToString('../../src/main/java/org/openrewrite/javascript/' + path);
        const javaVisitorMethods = Array.from(classContent
            .matchAll(/public [^ ]+ (visit\w+)(?:<[^(]+>)?[(]JSX?[.]/gm), m => m[1])
            .sort();

        expect(visitorJavascriptMethods).toEqual(javaVisitorMethods);

        classContent.split(/ +public /).slice(1)
            .filter(methodDefinition => methodDefinition.includes(" visit") && methodDefinition.includes("(JS."))
            .forEach((methodDefinition, i) => {
                const element = methodDefinition.match(/ visit\w+[(]JS(?:[.].*)?[.]([^,. ]+) /)?.[1]!;
                if (!element) {
                    console.log(methodDefinition);
                }
                const props = propertiesInEachInterface.get(element);
                if (!props || props.length === 0) {
                    throw new Error("No properties found for class " + element + " in " + path);
                }
                for (const propertyName of props || []) {
                    const upperCase = uppercaseFirstChar(propertyName);
                    expect(methodDefinition).toMatch(new RegExp(`(?:get|with|is|visit)${upperCase}`));
                }
            });
    })

    const rpcTsContentByClass = new Map(readFileToString('javascript/rpc.ts').split("class ")
        .map(classCode =>
            [classCode.split(" ")[0], classCode]
        ));

    test.each([
        "JavaScriptReceiver",
        "JavaScriptSender",
    ])("rpc.ts / %s", (className) => {
        const rpcTsContentForClass = rpcTsContentByClass.get(className)!;
        const rpcTsMethods = Array.from(rpcTsContentForClass
            .matchAll(/override async (visit\w+)[(<]/gm), m => m[1])
            .filter(m => !excusedVisitorMethods.includes(m))
            .sort();

        expect(rpcTsMethods).toEqual(visitorJavascriptMethods);

        rpcTsContentForClass.split("override async ").slice(1)
            .filter(m => m.startsWith("visit") && !m.startsWith("visit<") && !m.includes("JSX."))
            .filter(m => !excusedVisitorMethods.includes(m.substring(0, m.search(/[<(]/))))
            .forEach((methodDefinition, i) => {
                const element = methodDefinition.match(/visit\w+.*[(][^:]+: JS(?:[.].*)?[.]([^,.]+), q/)?.[1]!;
                const props = propertiesInEachInterface.get(element);
                if (!element) {
                    console.log(methodDefinition);
                }
                if (!props || props.length === 0) {
                    throw new Error("No properties found for class " + element + " in " + className);
                }
                for (const propertyName of props || []) {
                    expect(methodDefinition).toContain("." + propertyName);
                }
            });
    })
});
