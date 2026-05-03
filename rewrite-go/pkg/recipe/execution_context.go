/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package recipe

import "sync"

// ExecutionContext carries state through a recipe run.
// It holds messages (key-value pairs) that recipes and visitors can use
// to communicate and accumulate data during execution.
type ExecutionContext struct {
	mu       sync.RWMutex
	messages map[string]any
}

// NewExecutionContext creates a new ExecutionContext.
func NewExecutionContext() *ExecutionContext {
	return &ExecutionContext{
		messages: make(map[string]any),
	}
}

// PutMessage stores a value by key.
func (ctx *ExecutionContext) PutMessage(key string, value any) {
	ctx.mu.Lock()
	defer ctx.mu.Unlock()
	ctx.messages[key] = value
}

// GetMessage retrieves a value by key.
func (ctx *ExecutionContext) GetMessage(key string) (any, bool) {
	ctx.mu.RLock()
	defer ctx.mu.RUnlock()
	v, ok := ctx.messages[key]
	return v, ok
}

// GetMessageOrDefault retrieves a value by key, or returns the default if not found.
func (ctx *ExecutionContext) GetMessageOrDefault(key string, defaultValue any) any {
	ctx.mu.RLock()
	defer ctx.mu.RUnlock()
	if v, ok := ctx.messages[key]; ok {
		return v
	}
	return defaultValue
}

// MessageKeys returns a snapshot of the current message keys. Used by the
// BatchVisit handler to detect whether a visitor pass added new keys
// (`hasNewMessages` in the per-visitor result).
func (ctx *ExecutionContext) MessageKeys() []string {
	ctx.mu.RLock()
	defer ctx.mu.RUnlock()
	keys := make([]string, 0, len(ctx.messages))
	for k := range ctx.messages {
		keys = append(keys, k)
	}
	return keys
}
