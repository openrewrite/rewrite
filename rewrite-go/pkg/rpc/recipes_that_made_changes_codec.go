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

// Field order mirrors Java's RecipesThatMadeChanges and RecipeThatMadeChanges codecs.

func init() {
	RegisterValueType(reflect.TypeOf(java.RecipesThatMadeChanges{}), "org.openrewrite.marker.RecipesThatMadeChanges")
	RegisterValueType(reflect.TypeOf(java.RecipeThatMadeChanges{}), "org.openrewrite.marker.RecipeThatMadeChanges")
	RegisterFactory("org.openrewrite.marker.RecipesThatMadeChanges", func() any { return java.RecipesThatMadeChanges{} })
	RegisterFactory("org.openrewrite.marker.RecipeThatMadeChanges", func() any { return java.RecipeThatMadeChanges{} })
}

// stackKey identifies a recipe stack for the sender's own list diff. It never travels,
// so it only has to be stable within this process.
func stackKey(stack []java.RecipeThatMadeChanges) any {
	names := make([]string, len(stack))
	for i, recipe := range stack {
		names[i] = recipe.Name
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
		func(v any) any { return stackKey(v.([]java.RecipeThatMadeChanges)) },
		func(v any) { sendRecipeThatMadeChangesList(v.([]java.RecipeThatMadeChanges), q) })
}

func sendRecipeThatMadeChangesList(stack []java.RecipeThatMadeChanges, q *SendQueue) {
	q.GetAndSendList(stack,
		func(x any) []any {
			recipes := x.([]java.RecipeThatMadeChanges)
			out := make([]any, len(recipes))
			for i, recipe := range recipes {
				out[i] = recipe
			}
			return out
		},
		func(v any) any { return v.(java.RecipeThatMadeChanges).Name },
		func(v any) { sendRecipeThatMadeChanges(v.(java.RecipeThatMadeChanges), q) })
}

func sendRecipeThatMadeChanges(recipe java.RecipeThatMadeChanges, q *SendQueue) {
	q.GetAndSend(recipe, func(x any) any { return x.(java.RecipeThatMadeChanges).Name }, nil)
	q.GetAndSend(recipe, func(x any) any { return nilableString(x.(java.RecipeThatMadeChanges).DisplayName) }, nil)
	q.GetAndSend(recipe, func(x any) any { return nilableString(x.(java.RecipeThatMadeChanges).InstanceName) }, nil)
	q.GetAndSend(recipe, func(x any) any { return x.(java.RecipeThatMadeChanges).Options }, nil)
	q.GetAndSend(recipe, func(x any) any { return derefInt64(x.(java.RecipeThatMadeChanges).EstimatedEffortPerOccurrenceMillis) }, nil)
}

func receiveRecipesThatMadeChanges(m java.RecipesThatMadeChanges, q *ReceiveQueue) java.RecipesThatMadeChanges {
	if idStr := receiveScalar[string](q, m.Ident.String()); idStr != "" {
		if parsed, err := uuid.Parse(idStr); err == nil {
			m.Ident = parsed
		}
	}
	m.Recipes = receiveTypedList(q, m.Recipes,
		func(v any) any { return receiveRecipeThatMadeChangesList(asStack(v), q) },
		asStack)
	return m
}

func receiveRecipeThatMadeChangesList(before []java.RecipeThatMadeChanges, q *ReceiveQueue) []java.RecipeThatMadeChanges {
	return receiveTypedList(q, before,
		func(v any) any { return receiveRecipeThatMadeChanges(asRecipe(v), q) },
		asRecipe)
}

func asStack(v any) []java.RecipeThatMadeChanges {
	stack, _ := v.([]java.RecipeThatMadeChanges)
	return stack
}

func asRecipe(v any) java.RecipeThatMadeChanges {
	recipe, _ := v.(java.RecipeThatMadeChanges)
	return recipe
}

func receiveRecipeThatMadeChanges(recipe java.RecipeThatMadeChanges, q *ReceiveQueue) java.RecipeThatMadeChanges {
	recipe.Name = receiveScalar[string](q, recipe.Name)
	recipe.DisplayName = receiveNilableString(q, recipe.DisplayName)
	recipe.InstanceName = receiveNilableString(q, recipe.InstanceName)
	recipe.Options = q.Receive(recipe.Options, nil)
	recipe.EstimatedEffortPerOccurrenceMillis = receiveNilableInt64(q, recipe.EstimatedEffortPerOccurrenceMillis)
	return recipe
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
