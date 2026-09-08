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

package rpc

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func drainList(after, before []any) []RpcObjectData {
	var got []RpcObjectData
	q := NewSendQueue(1000, func(batch []RpcObjectData) { got = append(got, batch...) }, NewReferenceMap())
	q.sendList(after, before, func(v any) any { return v }, nil, false)
	q.Flush()
	return got
}

func states(batch []RpcObjectData) []State {
	s := make([]State, len(batch))
	for i, d := range batch {
		s[i] = d.State
	}
	return s
}

// The positions array is what lets a reorder cost one integer per element instead of
// re-sending the elements themselves; an event stream without a move event cannot.
func TestSendList_ReorderedElementsAreRepositionedNotResent(t *testing.T) {
	got := drainList([]any{"C", "A", "B"}, []any{"A", "B", "C"})

	require.Equal(t, []State{Change, Change, NoChange, NoChange, NoChange}, states(got))
	require.Equal(t, []any{2, 0, 1}, got[1].Value)
}

func TestSendList_EveryElementIsAddedWhenTheBeforeListIsEmpty(t *testing.T) {
	got := drainList([]any{"A", "B"}, []any{})

	require.Equal(t, []State{Change, Change, Add, Add}, states(got))
	require.Equal(t, []any{AddedListItem, AddedListItem}, got[1].Value)
	require.Equal(t, "A", got[2].Value)
	require.Equal(t, "B", got[3].Value)
}

func TestSendList_MixedAddsAndRemovals(t *testing.T) {
	got := drainList([]any{"A", "E", "F", "C"}, []any{"A", "B", "C", "D"})

	require.Equal(t, []State{Change, Change, NoChange, Add, Add, NoChange}, states(got))
	require.Equal(t, []any{0, AddedListItem, AddedListItem, 2}, got[1].Value)
}

func TestSendList_UnchangedListIsNoChange(t *testing.T) {
	before := []any{"A", "B"}

	require.Equal(t, []State{NoChange}, states(drainList(before, before)))
}
