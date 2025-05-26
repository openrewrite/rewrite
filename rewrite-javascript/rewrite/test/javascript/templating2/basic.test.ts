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
import {pattern, capture, Pattern, Matcher} from "../../../src/javascript/templating2";

describe('templating2 basics', () => {
    describe('capture', () => {
        test('creates a capture with name', () => {
            const c = capture('test');
            expect(c.name).toBe('test');
        });
    });

    describe('match', () => {
        test('creates a pattern with template parts and captures', () => {
            const p = pattern`${capture('left')} + ${capture('right')}`;
            expect(p).toBeInstanceOf(Pattern);

            const captures = p.captures;
            expect(captures.length).toBe(2);
            expect(captures[0].name).toBe('left');
            expect(captures[1].name).toBe('right');

            const parts = p.templateParts;
            expect(parts.length).toBe(3);
            expect(parts[0]).toBe('');
            expect(parts[1]).toBe(' + ');
            expect(parts[2]).toBe('');
        });
    });

    describe('Pattern', () => {
        test('against returns a Matcher', () => {
            const p = pattern`${capture('left')} + ${capture('right')}`;
            const matcher = p.against({} as any);
            expect(matcher).toBeInstanceOf(Matcher);
        });
    });

    describe('Matcher', () => {
        test('matches returns false in initial implementation', async () => {
            const p = pattern`${capture('left')} + ${capture('right')}`;
            const matcher = p.against({} as any);
            expect(await matcher.matches()).toBe(false);
        });

        test('get returns undefined for non-existent captures', () => {
            const p = pattern`${capture('left')} + ${capture('right')}`;
            const matcher = p.against({} as any);
            expect(matcher.get('left')).toBeUndefined();
        });

        test('getAll returns empty map initially', () => {
            const p = pattern`${capture('left')} + ${capture('right')}`;
            const matcher = p.against({} as any);
            expect(matcher.getAll().size).toBe(0);
        });
    });
});
