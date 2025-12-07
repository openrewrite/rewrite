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
import {RecipeSpec} from "../../src/test";
import {json, JsonParser} from "../../src/json";
import {findMarker, isParseError, MarkersKind, ParseExceptionResult} from "../../src";

describe('JSON parsing', () => {
    const spec = new RecipeSpec();

    test('parses JSON', () => spec.rewriteRun(
        //language=json
        json(
            `
              {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string"
                  }
                }
              }
            `
        )
    ));

    test('parses JSON with escaped quotes, Unicode, and emojis', () => spec.rewriteRun(
        //language=json
        json(
            `
              {
                "emoji": "Hello 👋 World 🌍",
                "japanese": "こんにちは",
                "mixed": "Test 🎉 with \\"quotes\\" and ü"
              }
            `
        )
    ));

    test('parses JSON with booleans and null', () => spec.rewriteRun(
        //language=json
        json(
            `
              {
                "enabled": true,
                "disabled": false,
                "nothing": null,
                "nested": {
                  "flag": true,
                  "empty": null
                }
              }
            `
        )
    ));

    test('parses JSON with empty arrays', () => spec.rewriteRun(
        //language=json
        json(
            `
              {
                "deny": [],
                "ask": []
              }
            `
        )
    ));

    test('returns ParseError for JSONC (JSON with comments)', async () => {
        const parser = new JsonParser();
        const jsonc = `{
            // This is a comment
            "name": "test"
        }`;

        const results: any[] = [];
        for await (const sf of parser.parse({sourcePath: 'test.json', text: jsonc})) {
            results.push(sf);
        }

        expect(results).toHaveLength(1);
        const result = results[0];

        // Should be a ParseError, not a crash
        expect(isParseError(result)).toBe(true);
        expect(result.text).toBe(jsonc);

        // Should have a ParseExceptionResult marker
        const parseException = findMarker<ParseExceptionResult>(result, MarkersKind.ParseExceptionResult);
        expect(parseException).toBeDefined();
        expect(parseException!.parserType).toBe('JsonParser');
        // Error message varies by Node.js version
        expect(parseException!.message).toMatch(/JSON|token/);
    });
});
