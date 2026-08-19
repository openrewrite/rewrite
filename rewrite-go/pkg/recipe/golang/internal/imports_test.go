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

package internal

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestPackageNameForPath(t *testing.T) {
	for path, want := range map[string]string{
		"strings":                   "strings",
		"encoding/json":             "json",
		"github.com/pkg/errors":     "errors",
		"encoding/json/v2":          "json",
		"encoding/json/jsontext":    "jsontext",
		"github.com/x/y/v2":         "y",
		"github.com/x/y/v10":        "y",
		"gopkg.in/yaml.v3":          "yaml",
		"gopkg.in/check.v1":         "check",
		"example.com/shipped/mathx": "mathx",
		"v2":                        "v2",
		"github.com/x/version":      "version",
		"github.com/x/v2x":          "v2x",
	} {
		assert.Equal(t, want, packageNameForPath(path), "path %q", path)
	}
}

func TestResolvedQualifiers(t *testing.T) {
	blank := "_"
	imports := []*java.Import{
		NewImport("math/rand", nil),
		NewImport("math/rand/v2", nil),
		NewImport("fmt", nil),
		NewImport("net/http/pprof", &blank),
	}

	assert.Equal(t, map[string]bool{"rand": true},
		ResolvedQualifiers(imports, map[string]bool{"math/rand/v2": true, "net/http/pprof": true}))
	assert.Empty(t, ResolvedQualifiers(imports, map[string]bool{}))
}
