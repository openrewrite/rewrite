/*
 * Copyright 2026 the original author or authors.
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

package format

import (
	"sync"
	"sync/atomic"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// FormattingListener hears about each request to auto-format t, stopping after
// stopAfter when it is not nil, so that a test harness can check that a recipe
// formats only what it changed.
type FormattingListener func(t, stopAfter java.Tree, cursor *visitor.Cursor)

var (
	listening   atomic.Int32
	listenersMu sync.Mutex
	listeners   []*FormattingListener
)

// ListenToFormatting calls l before each auto-format until the returned function
// is called.
func ListenToFormatting(l FormattingListener) (stop func()) {
	listener := &l
	listenersMu.Lock()
	defer listenersMu.Unlock()
	listeners = append(listeners, listener)
	listening.Add(1)
	return func() {
		listenersMu.Lock()
		defer listenersMu.Unlock()
		for i, other := range listeners {
			if other == listener {
				listeners = append(listeners[:i:i], listeners[i+1:]...)
				listening.Add(-1)
				return
			}
		}
	}
}

func beforeFormat(t, stopAfter java.Tree, cursor *visitor.Cursor) {
	// formatting costs one atomic load until something listens
	if listening.Load() == 0 {
		return
	}
	listenersMu.Lock()
	current := append([]*FormattingListener(nil), listeners...)
	listenersMu.Unlock()
	for _, l := range current {
		(*l)(t, stopAfter, cursor)
	}
}
