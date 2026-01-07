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
import * as crypto from "crypto";

export type UUID = string;

export function randomId(): UUID {
    // Use native randomUUID if available (Node 14.17.0+)
    if (typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }

    // Fallback for older Node versions using randomBytes
    const bytes = crypto.randomBytes(16);

    // Set version 4 bits (0100xxxx in byte 6)
    bytes[6] = (bytes[6] & 0x0f) | 0x40;

    // Set variant bits (10xxxxxx in byte 8)
    bytes[8] = (bytes[8] & 0x3f) | 0x80;

    // Convert to hex string with dashes
    const hex = bytes.toString('hex');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
