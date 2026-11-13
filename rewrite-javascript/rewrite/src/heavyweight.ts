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
import {MarkersKind} from "./markers";

const heavyweightClasses: Function[] = [];
const heavyweightKinds = new Set<string>();

/**
 * Registers a class, or the kind of a plain object such as a marker, as a type too expensive to keep in memory for a
 * whole run, as in a ScanningRecipe's accumulator.
 */
export function registerHeavyweight(type: string | (abstract new (...args: any[]) => unknown)): void {
    if (typeof type === "string") {
        heavyweightKinds.add(type);
    } else if (!heavyweightClasses.includes(type)) {
        heavyweightClasses.push(type);
    }
}

/** The registered kind or class that `value` is of, or the kind of a tree, which is always heavyweight. */
export function heavyweightType(value: object): string | Function | undefined {
    const kind = (value as { kind?: unknown }).kind;
    // a tree has an id and markers, and so do the Markers that hold its markers
    if (typeof kind === "string" && (heavyweightKinds.has(kind) ||
        "id" in value && "markers" in value && kind !== MarkersKind.Markers)) {
        return kind;
    }
    return heavyweightClasses.find(type => value instanceof type);
}
