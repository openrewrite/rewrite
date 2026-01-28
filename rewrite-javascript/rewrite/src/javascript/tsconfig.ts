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
import * as fs from 'fs';
import * as path from 'path';
import {parse as parseShellCommand} from 'shell-quote';
import ts from 'typescript';
import {Marker} from "../markers";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {updateIfChanged} from "../util";
import {randomId, UUID} from "../uuid";

export type EcmaScriptTarget =
    "ES3" | "ES5" | "ES2015" | "ES2016" | "ES2017" | "ES2018" | "ES2019" |
    "ES2020" | "ES2021" | "ES2022" | "ES2023" | "ES2024" | "ESNext" | "Latest";

export type ModuleKind =
    "None" | "CommonJS" | "AMD" | "UMD" | "System" | "ES2015" | "ES2020" |
    "ES2022" | "ESNext" | "Node16" | "Node18" | "Node20" | "NodeNext" | "Preserve";

export type ModuleResolutionKind =
    "Classic" | "Node" | "NodeJs" | "Node10" | "Node16" | "NodeNext" | "Bundler";

export type JsxEmit = "None" | "Preserve" | "React" | "ReactNative" | "ReactJSX" | "ReactJSXDev";

export const TsConfigKind = "org.openrewrite.javascript.marker.TsConfig" as const;

export interface TsConfig extends Marker {
    readonly kind: typeof TsConfigKind;
    readonly configPath?: string;
    readonly target: EcmaScriptTarget;
    readonly module: ModuleKind;
    readonly moduleResolution: ModuleResolutionKind;
    readonly jsx: JsxEmit;
    readonly strict: boolean;
    readonly esModuleInterop: boolean;
    readonly baseUrl?: string;
    readonly paths?: Record<string, string[]>;
    readonly lib?: string[];
}

const TARGET_VERSION: Record<string, number> = {
    ES3: 1999, ES5: 2009, ES2015: 2015, ES2016: 2016, ES2017: 2017, ES2018: 2018,
    ES2019: 2019, ES2020: 2020, ES2021: 2021, ES2022: 2022, ES2023: 2023, ES2024: 2024,
    ESNext: 9999, Latest: 9999
};

const ESM_MODULES = new Set<ModuleKind>([
    "ES2015", "ES2020", "ES2022", "ESNext", "Node16", "Node18", "Node20", "NodeNext", "Preserve"
]);

export const getTargetVersion = (config: TsConfig) => TARGET_VERSION[config.target] ?? -1;
export const supportsVersion = (config: TsConfig, version: number) => getTargetVersion(config) >= version;
export const isEsModule = (config: TsConfig) => ESM_MODULES.has(config.module);
export const hasJsx = (config: TsConfig) => config.jsx !== "None";
export const usesNewJsxTransform = (config: TsConfig) => config.jsx === "ReactJSX" || config.jsx === "ReactJSXDev";

export interface TsConfigOptions {
    id?: UUID;
    configPath?: string;
    target?: EcmaScriptTarget;
    module?: ModuleKind;
    moduleResolution?: ModuleResolutionKind;
    jsx?: JsxEmit;
    strict?: boolean;
    esModuleInterop?: boolean;
    baseUrl?: string;
    paths?: Record<string, string[]>;
    lib?: string[];
}

export function createTsConfig(options: TsConfigOptions = {}): TsConfig {
    return {
        kind: TsConfigKind,
        id: options.id ?? randomId(),
        configPath: options.configPath,
        target: options.target ?? "ES2020",
        module: options.module ?? "CommonJS",
        moduleResolution: options.moduleResolution ?? "Node",
        jsx: options.jsx ?? "None",
        strict: options.strict ?? false,
        esModuleInterop: options.esModuleInterop ?? true,
        baseUrl: options.baseUrl,
        paths: options.paths,
        lib: options.lib
    };
}

export const createModernEsmConfig = (options: Partial<TsConfigOptions> = {}) => createTsConfig({
    target: "ES2022", module: "ESNext", moduleResolution: "Bundler", strict: true, esModuleInterop: true, ...options
});

export const createNodeCommonJsConfig = (options: Partial<TsConfigOptions> = {}) => createTsConfig({
    target: "ES2020", module: "CommonJS", moduleResolution: "Node", strict: true, esModuleInterop: true, ...options
});

export const createReactConfig = (options: Partial<TsConfigOptions> = {}) => createTsConfig({
    target: "ES2020", module: "ESNext", moduleResolution: "Bundler", jsx: "ReactJSX",
    strict: true, esModuleInterop: true, lib: ["DOM", "DOM.Iterable", "ES2020"], ...options
});

const TSC_COMMANDS = new Set(['tsc', 'vue-tsc', 'ttsc', 'tspc']);
const SCRIPT_PRIORITY = ['build', 'compile', 'tsc', 'typecheck', 'type-check', 'check'];

function priorityCompare(a: string, b: string): number {
    const ai = SCRIPT_PRIORITY.indexOf(a);
    const bi = SCRIPT_PRIORITY.indexOf(b);
    if (ai >= 0 && bi >= 0) return ai - bi;
    if (ai >= 0) return -1;
    if (bi >= 0) return 1;
    return 0;
}

export function detectTsConfigPath(pkg: { scripts?: Record<string, string> }): string | undefined {
    if (!pkg.scripts) return undefined;

    const sorted = Object.entries(pkg.scripts).sort(([a], [b]) => priorityCompare(a, b));

    for (const [, script] of sorted) {
        const configPath = parseTscCommand(script);
        if (configPath !== undefined) return configPath;
    }
    return undefined;
}

function parseTscCommand(script: string): string | undefined {
    const tokens = parseShellCommand(script);
    let foundTsc = false;

    for (let i = 0; i < tokens.length; i++) {
        const token = tokens[i];
        if (typeof token !== 'string') continue;

        const cmd = token.split('/').pop()?.split('\\').pop() ?? token;
        if (!TSC_COMMANDS.has(cmd)) continue;
        foundTsc = true;

        for (let j = i + 1; j < tokens.length; j++) {
            const arg = tokens[j];
            if (typeof arg !== 'string') break;
            if (arg.startsWith('-p=')) return arg.slice(3);
            if (arg.startsWith('--project=')) return arg.slice(10);
            if (arg === '-p' || arg === '--project') {
                const next = tokens[j + 1];
                if (typeof next === 'string') return next;
            }
            if (arg === '--build' || arg === '-b') {
                const next = tokens[j + 1];
                if (typeof next === 'string' && next.endsWith('.json')) return next;
            }
        }
    }
    return foundTsc ? 'tsconfig.json' : undefined;
}

export interface ReadTsConfigOptions {
    searchPath?: string;
    relativeTo?: string;
}

export function readTsConfigFromPath(configPath: string, relativeTo?: string): TsConfig | undefined {
    if (!fs.existsSync(configPath)) return undefined;

    const configFile = ts.readConfigFile(configPath, ts.sys.readFile);
    if (configFile.error) return undefined;

    const parsed = ts.parseJsonConfigFileContent(configFile.config, ts.sys, path.dirname(configPath));
    const opts = parsed.options;

    return createTsConfig({
        configPath: relativeTo ? path.relative(relativeTo, configPath) : configPath,
        target: mapTarget(opts.target),
        module: mapModule(opts.module),
        moduleResolution: mapModuleRes(opts.moduleResolution),
        jsx: mapJsx(opts.jsx),
        strict: opts.strict ?? false,
        esModuleInterop: opts.esModuleInterop ?? false,
        baseUrl: opts.baseUrl,
        paths: opts.paths as Record<string, string[]>,
        lib: opts.lib
    });
}

export function readTsConfigFile(options: ReadTsConfigOptions = {}): TsConfig | undefined {
    const configPath = ts.findConfigFile(options.searchPath ?? process.cwd(), ts.sys.fileExists, 'tsconfig.json');
    return configPath ? readTsConfigFromPath(configPath, options.relativeTo) : undefined;
}

const targetMap: Record<number, EcmaScriptTarget> = {
    [ts.ScriptTarget.ES3]: "ES3", [ts.ScriptTarget.ES5]: "ES5", [ts.ScriptTarget.ES2015]: "ES2015",
    [ts.ScriptTarget.ES2016]: "ES2016", [ts.ScriptTarget.ES2017]: "ES2017", [ts.ScriptTarget.ES2018]: "ES2018",
    [ts.ScriptTarget.ES2019]: "ES2019", [ts.ScriptTarget.ES2020]: "ES2020", [ts.ScriptTarget.ES2021]: "ES2021",
    [ts.ScriptTarget.ES2022]: "ES2022", [ts.ScriptTarget.ES2023]: "ES2023", [ts.ScriptTarget.ESNext]: "ESNext",
};

const mapTarget = (target?: ts.ScriptTarget): EcmaScriptTarget =>
    target === undefined ? "ES2020" : targetMap[target] ?? ((ts.ScriptTarget as any).ES2024 === target ? "ES2024" : "ESNext");

const moduleMap: Record<number, ModuleKind> = {
    [ts.ModuleKind.None]: "None", [ts.ModuleKind.CommonJS]: "CommonJS", [ts.ModuleKind.AMD]: "AMD",
    [ts.ModuleKind.UMD]: "UMD", [ts.ModuleKind.System]: "System", [ts.ModuleKind.ES2015]: "ES2015",
    [ts.ModuleKind.ES2020]: "ES2020", [ts.ModuleKind.ES2022]: "ES2022", [ts.ModuleKind.ESNext]: "ESNext",
    [ts.ModuleKind.Node16]: "Node16", [ts.ModuleKind.NodeNext]: "NodeNext",
};

const mapModule = (module?: ts.ModuleKind): ModuleKind => {
    if (module === undefined) return "CommonJS";
    if (moduleMap[module]) return moduleMap[module];
    const mk = ts.ModuleKind as any;
    if (mk.Node18 === module) return "Node18";
    if (mk.Node20 === module) return "Node20";
    if (mk.Preserve === module) return "Preserve";
    return "CommonJS";
};

const moduleResMap: Record<number, ModuleResolutionKind> = {
    [ts.ModuleResolutionKind.Classic]: "Classic", [ts.ModuleResolutionKind.NodeJs]: "Node",
    [ts.ModuleResolutionKind.Node16]: "Node16", [ts.ModuleResolutionKind.NodeNext]: "NodeNext",
    [ts.ModuleResolutionKind.Bundler]: "Bundler",
};

const mapModuleRes = (resolution?: ts.ModuleResolutionKind): ModuleResolutionKind =>
    resolution === undefined ? "Node" : moduleResMap[resolution] ?? ((ts.ModuleResolutionKind as any).Node10 === resolution ? "Node10" : "Node");

const jsxMap: Record<number, JsxEmit> = {
    [ts.JsxEmit.None]: "None", [ts.JsxEmit.Preserve]: "Preserve", [ts.JsxEmit.React]: "React",
    [ts.JsxEmit.ReactNative]: "ReactNative", [ts.JsxEmit.ReactJSX]: "ReactJSX", [ts.JsxEmit.ReactJSXDev]: "ReactJSXDev",
};

const mapJsx = (jsx?: ts.JsxEmit): JsxEmit => jsx === undefined ? "None" : jsxMap[jsx] ?? "None";

RpcCodecs.registerCodec(TsConfigKind, {
    async rpcReceive(before: TsConfig, queue: RpcReceiveQueue): Promise<TsConfig> {
        return updateIfChanged(before, {
            id: await queue.receive(before.id),
            configPath: await queue.receive(before.configPath),
            target: await queue.receive(before.target),
            module: await queue.receive(before.module),
            moduleResolution: await queue.receive(before.moduleResolution),
            jsx: await queue.receive(before.jsx),
            strict: await queue.receive(before.strict),
            esModuleInterop: await queue.receive(before.esModuleInterop),
            baseUrl: await queue.receive(before.baseUrl),
            paths: await queue.receive(before.paths),
            lib: await queue.receive(before.lib),
        });
    },
    async rpcSend(after: TsConfig, queue: RpcSendQueue): Promise<void> {
        await queue.getAndSend(after, a => a.id);
        await queue.getAndSend(after, a => a.configPath);
        await queue.getAndSend(after, a => a.target);
        await queue.getAndSend(after, a => a.module);
        await queue.getAndSend(after, a => a.moduleResolution);
        await queue.getAndSend(after, a => a.jsx);
        await queue.getAndSend(after, a => a.strict);
        await queue.getAndSend(after, a => a.esModuleInterop);
        await queue.getAndSend(after, a => a.baseUrl);
        await queue.getAndSend(after, a => a.paths);
        await queue.getAndSend(after, a => a.lib);
    }
});
