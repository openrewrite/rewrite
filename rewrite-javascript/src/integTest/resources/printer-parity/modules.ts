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
import defaultExport, * as everything from "./module-a";
import type { OnlyType } from "./module-b";
import { type Mixed, value as renamed, other , } from "./module-c";
import data from "./data.json" with { type: "json" };
import legacy from "./legacy.json" assert { type: "json" };
import fs = require("fs");
import "./side-effect";
import surrogate from './e\uD83D\uDE00m';

export * from "./module-d";
export * as ns from "./module-e";
export { renamed as again, type Mixed };
export type { OnlyType } from "./module-b";
export type * as B from "B" /*a*/ with /*b*/ { type: "json" }/*c*/;
export default everything;

export const lazy = import("./module-f");
export type Imported = import("./module-g").Thing<string>;
export type Queried = typeof import("./module-h");
export type Attributed = import("pkg", { with: { "resolution-mode": "import" } }).ImportInterface;

export namespace Outer.Inner {
    export const value = 1;
}

declare module "ambient" {
    export function ambient(): void;
}

declare global {
    interface Window {
        custom: string;
    }
}

@dec export import decorated = everything.first;
declare import ambient from "./module-a";
export import reexported from "./module-a";
/*1*/ @dec /*2*/ declare /*3*/ import /*4*/ commentedDefault from "./module-a";

declare export * from "./module-d";
/*1*/ @dec /*2*/ declare /*3*/ export /*4*/ * from "./module-d";

@dec export as namespace Decorated;
declare export as namespace Ambient;
/*1*/ @dec /*2*/ declare /*3*/ export /*4*/ as /*5*/ namespace /*6*/ Commented;

@dec declare module "decorated" { }
@dec declare global { }
/*1*/ @dec /*2*/ export /*3*/ @other /*4*/ declare /*5*/ namespace /*6*/ Commented.Nested { }

@dec export @other declare type AmbientDecorated = string;
