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

package template_test

import (
	"io/fs"
	"sync"
	"sync/atomic"
	"testing"
	"testing/fstest"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/exportdata"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type invocationCollector struct {
	visitor.GoVisitor
	calls []*java.MethodInvocation
}

func (c *invocationCollector) PreVisit(t java.Tree, p any) java.Tree {
	if mi, ok := t.(*java.MethodInvocation); ok {
		c.calls = append(c.calls, mi)
	}
	return t
}

func applyOne(t *testing.T, code string, sets ...fs.FS) *java.MethodInvocation {
	t.Helper()
	b := template.ExpressionTemplate(code).Imports(test.ShippedPath).ExportData(sets...)
	applied := b.Build().Apply(nil, nil)
	require.NotNil(t, applied, "template did not apply")

	c := &invocationCollector{}
	c.Self = c
	c.Visit(applied, nil)
	require.Len(t, c.calls, 1)
	return c.calls[0]
}

func paramFQNs(mi *java.MethodInvocation) []string {
	var names []string
	for _, p := range mi.MethodType.ParameterTypes {
		names = append(names, matcher.GetFullyQualifiedName(p))
	}
	return names
}

func TestExportDataGivesTemplateOutputRealSignatures(t *testing.T) {
	mi := applyOne(t, `mathx.Clamp(1, 0, 10)`, test.ShippedExportData(t))

	require.True(t, matcher.IsResolved(mi))
	assert.Equal(t, test.ShippedPath, matcher.GetFullyQualifiedName(mi.MethodType.DeclaringType))
	assert.Equal(t, "int", matcher.GetFullyQualifiedName(mi.MethodType.ReturnType))
	assert.Equal(t, []string{"int", "int", "int"}, paramFQNs(mi))
}

func TestExportDataResolvesTypesFromTheShippedPackagesOwnDependencies(t *testing.T) {
	mi := applyOne(t, `mathx.Deadline(0)`, test.ShippedExportData(t))

	require.True(t, matcher.IsResolved(mi))
	// time ships no blob of its own: its types travel inside the one that names them.
	assert.Equal(t, "time.Time", matcher.GetFullyQualifiedName(mi.MethodType.ReturnType))
	assert.Equal(t, []string{"time.Duration"}, paramFQNs(mi))
}

func TestWithoutExportDataTemplateOutputIsUnresolved(t *testing.T) {
	assert.False(t, matcher.IsResolved(applyOne(t, `mathx.Clamp(1, 0, 10)`)))
}

// A recipe module may generate one package per dependency it templates
// against, so the sets a template draws on accumulate rather than replace.
func TestExportDataSetsAccumulate(t *testing.T) {
	mathx := test.ShippedExportDataFor(t, test.ShippedPath)
	strx := test.ShippedExportDataFor(t, test.ShippedPathB)

	both := template.ExpressionTemplate(`mathx.Clamp(strx.Repeat("a", 2) == "" && true, 0, 10)`).
		Imports(test.ShippedPath, test.ShippedPathB).
		ExportData(mathx).
		ExportData(strx).
		Build().
		Apply(nil, nil)
	require.NotNil(t, both)

	c := &invocationCollector{}
	c.Self = c
	c.Visit(both, nil)
	require.Len(t, c.calls, 2)
	for _, mi := range c.calls {
		assert.True(t, matcher.IsResolved(mi), "%s lost attribution when a second set was added", mi.Name.Name)
	}
}

func TestExportDataSetsAccumulateInOneCall(t *testing.T) {
	mi := applyOne(t, `mathx.Clamp(1, 0, 10)`,
		fstest.MapFS{}, test.ShippedExportDataFor(t, test.ShippedPath))
	assert.True(t, matcher.IsResolved(mi))
}

func TestUnreadableExportDataDegradesToNoExportData(t *testing.T) {
	stale := fstest.MapFS{exportdata.BlobName(test.ShippedPath): {Data: []byte("stale")}}
	assert.False(t, matcher.IsResolved(applyOne(t, `mathx.Clamp(1, 0, 10)`, stale)))
}

func TestExportDataLeavesStdlibAttributionIntact(t *testing.T) {
	tmpl := template.ExpressionTemplate(`strings.Contains(a, b)`).
		Imports("strings").
		ExportData(test.ShippedExportData(t)).
		Build()

	c := &invocationCollector{}
	c.Self = c
	applied := tmpl.Apply(nil, nil)
	require.NotNil(t, applied)
	c.Visit(applied, nil)

	require.Len(t, c.calls, 1)
	assert.True(t, matcher.IsResolved(c.calls[0]))
	assert.Equal(t, "boolean", matcher.GetFullyQualifiedName(c.calls[0].MethodType.ReturnType))
}

// countingFS records every blob it serves.
type countingFS struct {
	inner fs.FS
	opens atomic.Int64
}

func (c *countingFS) Open(name string) (fs.File, error) {
	c.opens.Add(1)
	return c.inner.Open(name)
}

func TestExportDataIsDecodedOncePerTemplate(t *testing.T) {
	counting := &countingFS{inner: test.ShippedExportDataFor(t, test.ShippedPath)}
	tmpl := template.ExpressionTemplate(`mathx.Clamp(1, 0, 10)`).
		Imports(test.ShippedPath).
		ExportData(counting).
		Build()

	for i := 0; i < 5; i++ {
		require.NotNil(t, tmpl.Apply(nil, nil))
	}
	assert.Equal(t, int64(1), counting.opens.Load())
}

func TestTemplateIsSafeToApplyConcurrently(t *testing.T) {
	tmpl := template.ExpressionTemplate(`mathx.Clamp(1, 0, 10)`).
		Imports(test.ShippedPath).
		ExportData(test.ShippedExportDataFor(t, test.ShippedPath)).
		Build()

	var wg sync.WaitGroup
	for i := 0; i < 8; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			assert.NotNil(t, tmpl.Apply(nil, nil))
		}()
	}
	wg.Wait()
}

func TestPatternAcceptsExportData(t *testing.T) {
	p := template.Expression(`mathx.Clamp(1, 0, 10)`).
		Imports(test.ShippedPath).
		ExportData(test.ShippedExportData(t)).
		Build()
	assert.True(t, p.Matches(applyOne(t, `mathx.Clamp(1, 0, 10)`, test.ShippedExportData(t)), nil))
}
