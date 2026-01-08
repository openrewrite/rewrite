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

import {describe} from "@jest/globals";
import {randomId} from "../src/uuid";

describe("randomId", () => {
    test("generates valid UUID v4 format", () => {
        const uuid = randomId();
        // UUID v4 format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        // where y is one of 8, 9, a, or b
        const uuidV4Regex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
        expect(uuid).toMatch(uuidV4Regex);
    });

    test("generates unique IDs on each call", () => {
        const ids = new Set<string>();
        const iterations = 1000;

        for (let i = 0; i < iterations; i++) {
            ids.add(randomId());
        }

        expect(ids.size).toBe(iterations);
    });

    test("returns string type", () => {
        const uuid = randomId();
        expect(typeof uuid).toBe("string");
    });

    test("returns 36 character string", () => {
        const uuid = randomId();
        expect(uuid.length).toBe(36);
    });
});
