/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {match, capture, backRef, Pattern, Matcher} from "../../../src/javascript/templating2";

describe('templating2 basics', () => {
    describe('capture', () => {
        test('creates a capture with name', () => {
            const c = capture('test');
            expect(c.name).toBe('test');
            expect(c.isBackRef).toBe(false);
            expect(c.typeConstraint).toBeUndefined();
        });

        test('creates a capture with type constraint', () => {
            const c = capture('test', 'number');
            expect(c.name).toBe('test');
            expect(c.isBackRef).toBe(false);
            expect(c.typeConstraint).toBe('number');
        });
    });

    describe('backRef', () => {
        test('creates a back reference', () => {
            const b = backRef('test');
            expect(b.name).toBe('test');
            expect(b.isBackRef).toBe(true);
        });
    });

    describe('match', () => {
        test('creates a pattern with template parts and captures', () => {
            const pattern = match`${capture('left')} + ${capture('right')}`;
            expect(pattern).toBeInstanceOf(Pattern);

            const captures = pattern.getCaptures();
            expect(captures.length).toBe(2);
            expect(captures[0].name).toBe('left');
            expect(captures[1].name).toBe('right');

            const parts = pattern.getTemplateParts();
            expect(parts.length).toBe(3);
            expect(parts[0]).toBe('');
            expect(parts[1]).toBe(' + ');
            expect(parts[2]).toBe('');
        });

        test('creates a pattern with type constraints', () => {
            const pattern = match`${capture('x', 'number')} + ${capture('y', 'number')}`;

            const captures = pattern.getCaptures();
            expect(captures.length).toBe(2);
            expect(captures[0].name).toBe('x');
            expect(captures[0].typeConstraint).toBe('number');
            expect(captures[1].name).toBe('y');
            expect(captures[1].typeConstraint).toBe('number');
        });

        test('creates a pattern with back-references', () => {
            const pattern = match`${capture('expr')} || ${backRef('expr')}`;

            const captures = pattern.getCaptures();
            expect(captures.length).toBe(2);
            expect(captures[0].name).toBe('expr');
            expect(captures[0].isBackRef).toBe(false);
            expect(captures[1].name).toBe('expr');
            expect(captures[1].isBackRef).toBe(true);
        });
    });

    describe('Pattern', () => {
        test('against returns a Matcher', () => {
            const pattern = match`${capture('left')} + ${capture('right')}`;
            const matcher = pattern.against({} as any);
            expect(matcher).toBeInstanceOf(Matcher);
        });
    });

    describe('Matcher', () => {
        test('matches returns false in initial implementation', async () => {
            const pattern = match`${capture('left')} + ${capture('right')}`;
            const matcher = pattern.against({} as any);
            expect(await matcher.matches()).toBe(false);
        });

        test('get returns undefined for non-existent captures', () => {
            const pattern = match`${capture('left')} + ${capture('right')}`;
            const matcher = pattern.against({} as any);
            expect(matcher.get('left')).toBeUndefined();
        });

        test('getAll returns empty map initially', () => {
            const pattern = match`${capture('left')} + ${capture('right')}`;
            const matcher = pattern.against({} as any);
            expect(matcher.getAll().size).toBe(0);
        });
    });
});
