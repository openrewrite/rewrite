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
import * as os from 'os';

/**
 * Utility functions for cross-platform shell command execution.
 */

/**
 * Commands that are implemented as .cmd scripts on Windows.
 * These need a .cmd suffix when spawned without `shell: true`.
 */
const CMD_SCRIPT_COMMANDS = new Set(['npm', 'npx', 'yarn', 'pnpm', 'pnpx']);

/**
 * Returns the correct command name for the current platform.
 * On Windows, Node.js package manager commands (npm, npx, yarn, pnpm) are
 * implemented as .cmd scripts and require the .cmd extension when spawned
 * without `shell: true`.
 *
 * Note: `bun` uses a native executable (bun.exe) on Windows, not a .cmd script,
 * so it doesn't need special handling.
 *
 * @param command The base command name (e.g., 'npm', 'yarn', 'pnpm')
 * @returns The platform-appropriate command name
 */
export function getPlatformCommand(command: string): string {
    if (os.platform() === 'win32' && CMD_SCRIPT_COMMANDS.has(command)) {
        return `${command}.cmd`;
    }
    return command;
}
