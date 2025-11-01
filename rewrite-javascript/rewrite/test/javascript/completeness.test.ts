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

describe('JS LST element tree', () => {
    test("should have a .java class counterpart for every JS LST element type", () => {
        const treeTSContent = readFileToString('javascript/tree.ts').split("export namespace JSX")[0];
        const tsInterfaces = Array.from(treeTSContent.matchAll(/export interface (\w+) extends JS/gm), m => m[1])
            .sort();

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
    const excused = ["visitContainer", "visitExpression", "visitStatement", "visitSpace",
                                "visitLeftPadded", "visitRightPadded",
                                "visitOptionalContainer", "visitOptionalLeftPadded", "visitOptionalRightPadded",
                                "visitType", "visitTypeName"];
    const visitorMethods = visitorJavascriptMethods.concat(visitorJavaMethods)
        .filter(m => !excused.includes(m))
        .sort();

    test("comparator.ts", () => {
        // Extract all override methods from the file
        const comparatorContent = readFileToString('javascript/comparator.ts');

        // Find the base JavaScriptComparatorVisitor class methods
        // (before the JavaScriptSemanticComparatorVisitor class)
        const semanticComparatorStart = comparatorContent.indexOf('export class JavaScriptSemanticComparatorVisitor');
        const baseComparatorContent = semanticComparatorStart > 0
            ? comparatorContent.substring(0, semanticComparatorStart)
            : comparatorContent;

        const comparatorTsMethods = Array.from(
            baseComparatorContent.matchAll(/override async (visit\w+)[(<]/gm), m => m[1])
            .sort();

        expect(comparatorTsMethods).toEqual(visitorMethods);
    })

    test.each([
        "JavaScriptVisitor.java",
        "internal/rpc/JavaScriptValidator.java",
        "JavaScriptIsoVisitor.java",
        "internal/rpc/JavaScriptReceiver.java",
        "internal/rpc/JavaScriptSender.java",
    ])("%s", (path) => {
        const javaVisitorMethods = Array.from(readFileToString('../../src/main/java/org/openrewrite/javascript/' + path)
            .matchAll(/public [^ ]+ (visit\w+)(?:<[^(]+>)?[(]JSX?[.]/gm), m => m[1])
            .sort();

        expect(visitorJavascriptMethods).toEqual(javaVisitorMethods);
    })

    const rpcTsContentByClass = new Map(readFileToString('javascript/rpc.ts').split("class ")
        .map(classCode =>
            [classCode.split(" ")[0], classCode]
        ));

    test.each([
        "JavaScriptReceiver",
        "JavaScriptSender",
    ])("rpc.ts / %s", (className) => {
        const rpcTsMethods = Array.from(rpcTsContentByClass.get(className)!
            .matchAll(/override async (visit\w+)[(<]/gm), m => m[1])
            .filter(m => !excused.includes(m))
            .sort();

        expect(rpcTsMethods).toEqual(visitorJavascriptMethods);
    })
});
