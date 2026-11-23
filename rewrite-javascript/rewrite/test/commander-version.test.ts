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

import * as fs from "fs";
import * as path from "path";
import * as semver from "semver";

describe("commander version", () => {
    test("declared range cannot resolve to 11 or later, so the RPC server stays runnable on Node 14", () => {
        // given
        const packageJson = JSON.parse(fs.readFileSync(path.resolve(__dirname, "../package.json"), "utf8"));
        const range = packageJson.dependencies.commander;

        // when
        const allowsEleven = semver.intersects(range, ">=11.0.0");

        // then
        expect(semver.validRange(range)).not.toBeNull();
        expect(allowsEleven).toBe(false);
    });
});
