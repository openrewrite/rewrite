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
import {capture, pattern} from "../../../src/javascript";

describe('Pattern Matching', () => {
    describe('Pattern', () => {
        test('creates a pattern with correct template parts and captures', () => {
            const p = pattern`${capture()} + ${capture()}`;
            
            expect(p.templateParts).toBeDefined();
            expect(p.captures).toBeDefined();
            expect(p.captures.length).toBe(2);
        });
    });
});
