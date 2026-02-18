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
import {json} from "../../src/json";

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

    test('parses JSON with empty arrays containing whitespace', () => spec.rewriteRun(
        //language=json
        json(
            `{
  "parameterConfig": [

  ]
}`
        )
    ));

    test('parses JSON with empty objects containing whitespace', () => spec.rewriteRun(
        //language=json
        json(
            `{
  "healthChecker": {
  }
}`
        )
    ));

    test('parses JSON with decimal numbers', () => spec.rewriteRun(
        //language=json
        json(
            `{
  "pi": 3.14159,
  "negative": -2.5,
  "scientific": 1.23e10,
  "scientificNeg": 6.022e-23
}`
        )
    ));

    test('parses package.json with author containing angle brackets', () => spec.rewriteRun(
        //language=json
        json(
            `{
  "name": "react-native-app-clip",
  "version": "0.6.1",
  "description": "Config plugin to add an App Clip to a React Native iOS app",
  "main": "build/index.js",
  "types": "build/index.d.ts",
  "files": [
    "ios",
    "plugin/build",
    "build",
    "README.md",
    "expo-module.config.json",
    "app.plugin.js"
  ],
  "scripts": {
    "build": "expo-module build",
    "build:plugin": "expo-module build plugin",
    "clean": "expo-module clean",
    "lint": "expo-module lint",
    "test": "expo-module test",
    "prepare": "expo-module prepare",
    "prepublishOnly": "expo-module prepublishOnly",
    "expo-module": "expo-module",
    "open:ios": "xed example/ios",
    "open:android": "open -a \\"Android Studio\\" example/android"
  },
  "keywords": [
    "react-native",
    "expo",
    "react-native-app-clip",
    "ReactNativeAppClip"
  ],
  "repository": "https://github.com/bndkt/react-native-app-clip",
  "bugs": {
    "url": "https://github.com/bndkt/react-native-app-clip/issues"
  },
  "author": "Benedikt Müller <91166910+bndkt@users.noreply.github.com> (https://github.com/bndkt)",
  "license": "MIT",
  "homepage": "https://github.com/bndkt/react-native-app-clip#readme",
  "dependencies": {
    "@expo/config-plugins": "~10.0.0",
    "@expo/plist": "~0.3.0"
  },
  "devDependencies": {
    "expo-module-scripts": "^4.0.0",
    "expo-modules-core": "~2.3.0"
  },
  "peerDependencies": {
    "expo": "*",
    "react": "*",
    "react-native": "*"
  }
}
`
        )
    ));

    test('parses JSONC with line comments', () => spec.rewriteRun(
        json(
            `{
            // This is a comment
            "name": "test"
        }`
        )
    ));

    test('parses JSONC with block comments', () => spec.rewriteRun(
        json(
            `{
            /* This is a block comment */
            "name": "test",
            "value": /* inline comment */ 42
        }`
        )
    ));

    test('parses JSONC with trailing commas in objects', () => spec.rewriteRun(
        json(
            `{
            "name": "test",
            "value": 42,
        }`
        )
    ));

    test('parses JSONC with trailing commas in arrays', () => spec.rewriteRun(
        json(
            `{
            "items": [1, 2, 3,]
        }`
        )
    ));

    // Note: JSON5-specific features (unquoted keys, single quotes, hex numbers,
    // leading/trailing decimals, Infinity, NaN) are NOT supported.
    // We use jsonc-parser which only supports JSONC (JSON with Comments + trailing commas).

    test('parses boolean literal', () => spec.rewriteRun(
        json(`true`)
    ));

    test('parses null literal', () => spec.rewriteRun(
        json(`null`)
    ));

    // Note: Empty document parsing works but can't be tested with the current test framework
    // due to how it handles empty strings (falsy in JS). The Java test uses the same pattern.

    test('parses double literal with signed exponent', () => spec.rewriteRun(
        json(`-1e3`)
    ));

    test('parses double literal with uppercase exponent', () => spec.rewriteRun(
        json(`1E-3`)
    ));

    test('parses big integer', () => spec.rewriteRun(
        json(`-10000000000000000999`)
    ));

    test('parses array with trailing comma and spaces', () => spec.rewriteRun(
        json(`[ 1 , 2 , 3 , ]`)
    ));

    test('parses multi-byte Unicode', () => spec.rewriteRun(
        json(
            `{
  "🤖"    : "robot",
  "robot" : "🤖",
  "நடித்த" : 3 /* 🇩🇪 */
}`
        )
    ));

    test('parses unicode escapes', () => spec.rewriteRun(
        json(
            `{
  "nul": "\\u0000",
  "reverse-solidus": "\\u005c",
}`
        )
    ));

    test('parses multiline comment with URL', () => spec.rewriteRun(
        json(
            `{
  /* https://foo.bar */
}`
        )
    ));

    test('parses long number value', () => spec.rewriteRun(
        json(
            `{
    "timestamp": 1577000812973
}`
        )
    ));
});
