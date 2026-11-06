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
	"os"
	"reflect"
	"regexp"
	"slices"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

var javaTreeSources = map[string]string{
	"org.openrewrite.java.tree.J":    "../../../rewrite-java/src/main/java/org/openrewrite/java/tree/J.java",
	"org.openrewrite.golang.tree.Go": "../../src/main/java/org/openrewrite/golang/tree/Go.java",
}

var javaClassDeclaration = regexp.MustCompile(
	`^( *)(?:(?:public|protected|private|static|final|abstract|sealed|non-sealed) )*(?:class|interface|enum) (\w+)[^{]*?(?:implements ([^{]+))?\{`)

var javaSuperInterfaces = map[string][]string{"Loop": {"Statement"}, "MethodCall": {"Expression"}}

func readJavaSource(t *testing.T, path string) string {
	source, err := os.ReadFile(path)
	if err != nil {
		t.Skipf("Java sources are not alongside this module: %v", err)
	}
	return string(source)
}

// javaInterfaces maps each nested class of J.java and Go.java, by its binary
// name, to every interface it implements.
func javaInterfaces(t *testing.T) map[string]map[string]bool {
	implemented := map[string]map[string]bool{}
	for outer, path := range javaTreeSources {
		source := readJavaSource(t, path)
		var enclosing []string
		for _, line := range strings.Split(source, "\n") {
			m := javaClassDeclaration.FindStringSubmatch(line)
			if m == nil {
				continue
			}
			depth := len(m[1]) / 4
			enclosing = append(enclosing[:min(depth, len(enclosing))], m[2])
			if depth == 0 {
				continue
			}
			closure := map[string]bool{}
			var pending []string
			for _, name := range strings.Split(m[3], ",") {
				if name = strings.TrimSpace(strings.SplitN(name, "<", 2)[0]); name != "" {
					pending = append(pending, name)
				}
			}
			for len(pending) > 0 {
				next := pending[len(pending)-1]
				pending = append(pending[:len(pending)-1], javaSuperInterfaces[next]...)
				closure[next] = true
			}
			implemented[outer+"$"+strings.Join(enclosing[1:], "$")] = closure
		}
	}
	return implemented
}

func TestStatementAndExpressionMatchTheJavaModel(t *testing.T) {
	// given
	javaClasses := javaInterfaces(t)
	markers := map[string]reflect.Type{
		"Statement":  reflect.TypeOf((*java.Statement)(nil)).Elem(),
		"Expression": reflect.TypeOf((*java.Expression)(nil)).Elem(),
	}
	jType := reflect.TypeOf((*java.J)(nil)).Elem()

	// when
	var mismatches []string
	for goType, javaClassName := range valueTypeMap {
		if !goType.Implements(jType) {
			continue
		}
		interfaces, ok := javaClasses[javaClassName]
		if !ok {
			if strings.HasPrefix(javaClassName, "org.openrewrite.java.tree.J$") ||
				strings.HasPrefix(javaClassName, "org.openrewrite.golang.tree.Go$") {
				mismatches = append(mismatches, javaClassName+": class declaration not found")
			}
			continue
		}
		for name, marker := range markers {
			if goType.Implements(marker) != interfaces[name] {
				mismatches = append(mismatches, goType.String()+" vs "+javaClassName+": "+name+
					map[bool]string{true: " only in Go", false: " only in Java"}[goType.Implements(marker)])
			}
		}
	}
	slices.Sort(mismatches)

	// then
	assert.Empty(t, mismatches)
}
