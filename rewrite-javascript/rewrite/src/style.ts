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
import {Marker, MarkersKind} from "./markers";
import {SourceFile} from "./tree";

export interface Style {
    readonly kind: string
}

export interface NamedStyles<K extends string = typeof MarkersKind.NamedStyles> extends Marker {
    readonly kind: K
    readonly name: string;
    readonly displayName: string;
    readonly description?: string;

    readonly tags: string[];
    readonly styles: Style[];
}
