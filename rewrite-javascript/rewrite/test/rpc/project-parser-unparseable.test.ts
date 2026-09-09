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
import * as fs from "fs";
import * as path from "path";
import {dir} from "tmp-promise";
import {ProjectParser} from "../../src/javascript/project-parser";

describe("ProjectParser unparseable file discovery", () => {
    it("routes JS/TS files over the size cap to unparseableFiles, keeps small ones in jsFiles", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            fs.writeFileSync(path.join(tmpDir.path, "small.js"), "export const x = 1;\n");
            // 2 MB > the 1 MB cap; a JSON-like blob rather than a minified single line.
            fs.writeFileSync(path.join(tmpDir.path, "big.js"),
                "export default {\n" + "  a: 1,\n".repeat(300_000) + "};\n");

            const discovered = await new ProjectParser(tmpDir.path, {useGit: false}).discoverFiles();

            expect(discovered.jsFiles.map(f => path.basename(f))).toEqual(["small.js"]);
            expect(discovered.unparseableFiles.map(f => path.basename(f))).toEqual(["big.js"]);
        } finally {
            await tmpDir.cleanup();
        }
    });
});
