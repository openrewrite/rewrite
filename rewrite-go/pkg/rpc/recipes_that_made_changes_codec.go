/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
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
	"reflect"
	"strings"

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// Field order mirrors Java's RecipesThatMadeChanges and RecipeIdentity codecs.

func init() {
	RegisterValueType(reflect.TypeOf(java.RecipesThatMadeChanges{}), "org.openrewrite.marker.RecipesThatMadeChanges")
	RegisterValueType(reflect.TypeOf(java.RecipeIdentity{}), "org.openrewrite.marker.RecipeIdentity")
	RegisterFactory("org.openrewrite.marker.RecipesThatMadeChanges", func() any { return java.RecipesThatMadeChanges{} })
	RegisterFactory("org.openrewrite.marker.RecipeIdentity", func() any { return java.RecipeIdentity{} })
}

// stackKey identifies a recipe stack for the sender's own list diff. It never travels,
// so it only has to be stable within this process.
func stackKey(stack []java.RecipeIdentity) any {
	names := make([]string, len(stack))
	for i, identity := range stack {
		names[i] = identity.Name
	}
	return strings.Join(names, "\x00")
}

func sendRecipesThatMadeChanges(m java.RecipesThatMadeChanges, q *SendQueue) {
	q.GetAndSend(m, func(x any) any { return x.(java.RecipesThatMadeChanges).Ident.String() }, nil)
	q.GetAndSendList(m,
		func(x any) []any {
			stacks := x.(java.RecipesThatMadeChanges).Recipes
			out := make([]any, len(stacks))
			for i, stack := range stacks {
				out[i] = stack
			}
			return out
		},
		func(v any) any { return stackKey(v.([]java.RecipeIdentity)) },
		func(v any) { sendRecipeIdentityList(v.([]java.RecipeIdentity), q) })
}

func sendRecipeIdentityList(stack []java.RecipeIdentity, q *SendQueue) {
	q.GetAndSendList(stack,
		func(x any) []any {
			identities := x.([]java.RecipeIdentity)
			out := make([]any, len(identities))
			for i, identity := range identities {
				out[i] = identity
			}
			return out
		},
		func(v any) any { return v.(java.RecipeIdentity).Name },
		func(v any) { sendRecipeIdentity(v.(java.RecipeIdentity), q) })
}

func sendRecipeIdentity(identity java.RecipeIdentity, q *SendQueue) {
	q.GetAndSend(identity, func(x any) any { return x.(java.RecipeIdentity).Name }, nil)
	q.GetAndSend(identity, func(x any) any { return nilableString(x.(java.RecipeIdentity).DisplayName) }, nil)
	q.GetAndSend(identity, func(x any) any { return nilableString(x.(java.RecipeIdentity).InstanceName) }, nil)
	q.GetAndSend(identity, func(x any) any { return x.(java.RecipeIdentity).Options }, nil)
	q.GetAndSend(identity, func(x any) any { return derefInt64(x.(java.RecipeIdentity).EstimatedEffortPerOccurrenceMillis) }, nil)
}

func receiveRecipesThatMadeChanges(m java.RecipesThatMadeChanges, q *ReceiveQueue) java.RecipesThatMadeChanges {
	if idStr := receiveScalar[string](q, m.Ident.String()); idStr != "" {
		if parsed, err := uuid.Parse(idStr); err == nil {
			m.Ident = parsed
		}
	}
	m.Recipes = receiveTypedList(q, m.Recipes,
		func(v any) any { return receiveRecipeIdentityList(asStack(v), q) },
		asStack)
	return m
}

func receiveRecipeIdentityList(before []java.RecipeIdentity, q *ReceiveQueue) []java.RecipeIdentity {
	return receiveTypedList(q, before,
		func(v any) any { return receiveRecipeIdentity(asIdentity(v), q) },
		asIdentity)
}

func asStack(v any) []java.RecipeIdentity {
	stack, _ := v.([]java.RecipeIdentity)
	return stack
}

func asIdentity(v any) java.RecipeIdentity {
	identity, _ := v.(java.RecipeIdentity)
	return identity
}

func receiveRecipeIdentity(identity java.RecipeIdentity, q *ReceiveQueue) java.RecipeIdentity {
	identity.Name = receiveScalar[string](q, identity.Name)
	identity.DisplayName = receiveNilableString(q, identity.DisplayName)
	identity.InstanceName = receiveNilableString(q, identity.InstanceName)
	identity.Options = q.Receive(identity.Options, nil)
	identity.EstimatedEffortPerOccurrenceMillis = receiveNilableInt64(q, identity.EstimatedEffortPerOccurrenceMillis)
	return identity
}

func derefInt64(i *int64) any {
	if i == nil {
		return nil
	}
	return *i
}

func receiveNilableString(q *ReceiveQueue, before *string) *string {
	v := q.Receive(nilableString(before), nil)
	if v == nil {
		return nil
	}
	s, ok := v.(string)
	if !ok {
		return nil
	}
	return &s
}

func receiveNilableInt64(q *ReceiveQueue, before *int64) *int64 {
	v := q.Receive(derefInt64(before), nil)
	if v == nil {
		return nil
	}
	i := convertTo[int64](v)
	return &i
}
