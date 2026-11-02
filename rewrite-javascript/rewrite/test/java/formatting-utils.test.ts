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
import {emptyMarkers} from "../../src";
import {J, spaceContainsNewline, TextComment} from "../../src/java";

describe('spaceContainsNewline', () => {
    const blockComment = (text: string): TextComment =>
        ({kind: J.Kind.TextComment, multiline: true, text, suffix: " ", markers: emptyMarkers});

    test('a block comment spanning lines is a line terminator', () => {
        expect(spaceContainsNewline({kind: J.Kind.Space, whitespace: " ", comments: [blockComment("\n * a\n ")]})).toBe(true);
        expect(spaceContainsNewline({kind: J.Kind.Space, whitespace: " ", comments: [blockComment(" a ")]})).toBe(false);
    });
});
