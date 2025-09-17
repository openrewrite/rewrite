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
import * as v8 from 'v8';
import * as inspector from 'inspector';

export class ChromeProfiler {
    private traceEvents: any[] = [];
    private memoryInterval?: NodeJS.Timeout;
    private saveInterval?: NodeJS.Timeout;
    private session?: inspector.Session;
    private readonly pid = process.pid;
    private readonly tid = 1; // Main thread
    private readonly tracePath: string;
    private exitHandlersRegistered = false;
    private startTime: number = 0;
    private ttsCounter: number = 0;
    private lastProfileTime: number = 0;
    private profileNodes = new Map();

    constructor(outputPath?: string) {
        this.tracePath = outputPath || path.join(process.cwd(), 'rewrite.json');
    }

    async start() {
        this.startTime = Date.now() * 1000; // Convert to microseconds
        this.lastProfileTime = this.startTime;

        // Add initial metadata events
        this.addMetadataEvents();

        // Start V8 Inspector session for CPU profiling
        this.session = new inspector.Session();
        this.session.connect();

        // Enable and start CPU profiling
        await this.enableCpuProfiling();

        // Start collecting memory data
        this.startMemoryTracking();

        // Save trace periodically and collect CPU profile samples
        this.saveInterval = setInterval(async () => {
            await this.collectCpuProfile();
            this.saveTrace();
        }, 10000); // Save every 10 seconds

        // Register exit handlers
        if (!this.exitHandlersRegistered) {
            this.registerExitHandlers();
            this.exitHandlersRegistered = true;
        }
    }

    private async enableCpuProfiling() {
        if (!this.session) return;

        // Enable profiler
        await new Promise<void>((resolve, reject) => {
            this.session!.post('Profiler.enable', (err) => {
                if (err) reject(err);
                else resolve();
            });
        });

        // Start sampling
        await new Promise<void>((resolve, reject) => {
            this.session!.post('Profiler.start', {
                // Sample at high frequency for detailed profiling
                samplingInterval: 100
            }, (err) => {
                if (err) reject(err);
                else resolve();
            });
        });

        // Subscribe to console profile events
        this.session.on('Profiler.consoleProfileStarted', (params: any) => {
            this.traceEvents.push({
                cat: 'disabled-by-default-devtools.timeline',
                name: 'Profile',
                ph: 'P',
                id: params.id || '1',
                pid: this.pid,
                tid: this.tid,
                ts: Date.now() * 1000
            });
        });
    }

    private addMetadataEvents() {
        // Thread name metadata
        this.traceEvents.push({
            args: { name: 'CrRendererMain' },
            cat: '__metadata',
            name: 'thread_name',
            ph: 'M',
            pid: this.pid,
            tid: this.tid,
            ts: 0
        });

        // Process name metadata
        this.traceEvents.push({
            args: { name: 'Node.js' },
            cat: '__metadata',
            name: 'process_name',
            ph: 'M',
            pid: this.pid,
            tid: 0,
            ts: 0
        });

        // TracingStartedInBrowser event - required for Chrome DevTools
        this.traceEvents.push({
            args: {
                data: {
                    frameTreeNodeId: 1,
                    frames: [{
                        frame: '0x1',
                        name: '',
                        processId: this.pid,
                        url: 'node://process'
                    }],
                    persistentIds: true
                }
            },
            cat: 'disabled-by-default-devtools.timeline',
            name: 'TracingStartedInBrowser',
            ph: 'I',
            pid: this.pid,
            s: 't',
            tid: this.tid,
            ts: this.startTime,
            tts: this.ttsCounter++
        });
    }

    private startMemoryTracking() {
        // Collect memory data every 50ms (matching Chrome's frequency)
        this.memoryInterval = setInterval(() => {
            const memStats = v8.getHeapStatistics();
            const timestamp = Date.now() * 1000;

            // UpdateCounters event with correct format
            this.traceEvents.push({
                args: {
                    data: {
                        jsHeapSizeUsed: memStats.used_heap_size,
                        jsEventListeners: 0,
                        documents: 1,
                        nodes: 0
                    }
                },
                cat: 'disabled-by-default-devtools.timeline',
                name: 'UpdateCounters',
                ph: 'I', // Instant event, not Counter
                pid: this.pid,
                s: 't', // Required for instant events
                tid: this.tid,
                ts: timestamp,
                tts: this.ttsCounter++
            });
        }, 50); // Every 50ms
    }

    private saveTrace() {
        const trace = {
            metadata: {
                'command-line': process.argv.join(' '),
                'cpu-brand': 'Node.js V8',
                'dataOrigin': 'TraceEvents',
                'highres-ticks': true,
                'hostname': 'localhost',
                'num-cpus': require('os').cpus().length,
                'physical-memory': require('os').totalmem(),
                'platform': process.platform,
                'process-uptime': process.uptime(),
                'product-version': `Node.js ${process.version}`,
                'protocol-version': '1.0',
                'source': 'NodeProfiler',
                'startTime': new Date(this.startTime / 1000).toISOString(),
                'trace-config': '',
                'user-agent': `Node.js/${process.version}`,
                'v8-version': process.versions.v8
            },
            traceEvents: this.traceEvents
        };

        try {
            fs.writeFileSync(this.tracePath, JSON.stringify(trace, null, 2));
        } catch (e) {
            // Ignore write errors
        }
    }

    private async collectCpuProfile() {
        if (!this.session) return;

        try {
            // Stop current profiling to get samples
            const profile = await new Promise<any>((resolve, reject) => {
                this.session!.post('Profiler.stop', (err, params) => {
                    if (err) reject(err);
                    else resolve(params.profile);
                });
            });

            // Convert CPU profile samples to trace events
            if (profile && profile.samples) {
                this.addCpuProfileSamples(profile);
            }

            // Restart profiling for the next interval
            await new Promise<void>((resolve, reject) => {
                this.session!.post('Profiler.start', {
                    samplingInterval: 100
                }, (err) => {
                    if (err) reject(err);
                    else resolve();
                });
            });
        } catch (e) {
            // Ignore errors and try to restart
            try {
                await new Promise<void>((resolve, reject) => {
                    this.session!.post('Profiler.start', {
                        samplingInterval: 100
                    }, (err) => {
                        if (err) reject(err);
                        else resolve();
                    });
                });
            } catch (e2) {
                // Ignore restart errors
            }
        }
    }

    async stop() {
        // Clear intervals
        if (this.memoryInterval) {
            clearInterval(this.memoryInterval);
            this.memoryInterval = undefined;
        }
        if (this.saveInterval) {
            clearInterval(this.saveInterval);
            this.saveInterval = undefined;
        }

        // Collect final CPU profile
        await this.collectCpuProfile();

        // Disconnect session
        if (this.session) {
            this.session.disconnect();
            this.session = undefined;
        }

        // Save final trace
        this.saveTrace();
    }

    private addCpuProfileSamples(profile: any) {
        if (!profile.samples || !profile.timeDeltas || !profile.nodes) return;

        // Use the last profile time as starting point to maintain continuity
        let currentTime = this.lastProfileTime;

        // Update nodes map with new nodes
        profile.nodes.forEach((node: any) => {
            this.profileNodes.set(node.id, node);
        });

        // Convert samples to trace events with actual function names
        profile.samples.forEach((nodeId: number, index: number) => {
            const node = this.profileNodes.get(nodeId);
            if (!node) return;

            currentTime += (profile.timeDeltas[index] || 0);

            const callFrame = node.callFrame;
            if (callFrame) {
                // Clean up function name for display
                let functionName = callFrame.functionName || '(anonymous)';
                if (functionName === '' || functionName === '(root)') {
                    functionName = '(program)';
                }

                // Extract filename from URL or use a meaningful default
                let url = callFrame.url || '';
                let fileName: string;

                if (url) {
                    // Clean up the URL for display
                    if (url.startsWith('file://')) {
                        url = url.substring(7);
                    }
                    const parts = url.split('/');
                    fileName = parts[parts.length - 1] || url;

                    // Special handling for node internals
                    if (url.startsWith('node:')) {
                        fileName = url;
                    }
                } else {
                    // No URL - try to provide context from function name
                    if (functionName === '(garbage collector)') {
                        fileName = 'v8::gc';
                    } else if (functionName === '(idle)') {
                        fileName = 'v8::idle';
                    } else if (functionName === '(program)') {
                        fileName = 'main';
                    } else if (functionName.includes('::')) {
                        // C++ internal function
                        fileName = 'native';
                    } else {
                        // JavaScript code without source mapping
                        fileName = 'javascript';
                    }
                }

                this.traceEvents.push({
                    args: {
                        data: {
                            columnNumber: callFrame.columnNumber || 0,
                            frame: `0x${nodeId.toString(16)}`,
                            functionName: functionName,
                            lineNumber: callFrame.lineNumber || 0,
                            scriptId: String(callFrame.scriptId || 0),
                            url: url || fileName  // Use fileName as URL if no real URL
                        }
                    },
                    cat: 'devtools.timeline',
                    dur: Math.max(profile.timeDeltas[index] || 1, 1),
                    name: 'FunctionCall',
                    ph: 'X',
                    pid: this.pid,
                    tid: this.tid,
                    ts: Math.floor(currentTime),
                    tts: this.ttsCounter++
                });
            }
        });

        // Update lastProfileTime to maintain continuity for the next batch
        this.lastProfileTime = currentTime;
    }

    private registerExitHandlers() {
        const cleanup = async () => {
            await this.stop();
        };

        process.once('beforeExit', cleanup);
        process.once('SIGINT', cleanup);
        process.once('SIGTERM', cleanup);
    }
}
