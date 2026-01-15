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
import {AfterRecipeText, dedentAfter, SourceSpec} from "../test";
import {YamlParser} from "./parser";
import {Yaml} from "./tree";

export function yaml(before: string | null, after?: AfterRecipeText): SourceSpec<Yaml.Documents> {
    return {
        kind: Yaml.Kind.Documents,
        before: before,
        after: dedentAfter(after),
        ext: 'yaml',
        parser: () => new YamlParser()
    };
}
