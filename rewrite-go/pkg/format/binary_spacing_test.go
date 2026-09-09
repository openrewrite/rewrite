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
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
)

func TestBinaryOperandMeetsUnaryOperator(t *testing.T) {
	// Each operand sits deep enough to be written tight, which puts the binary
	// operator against the unary operator to its right.
	sources := []string{
		"package A\n\nvar A = 0%0&&0& &0\n",
		"package A\n\nfunc F(p *int) { _ = 0%0&&0/ *p }\n",
		"package A\n\nfunc F(p *int) { _ = 0%0&&0* *p }\n",
		"package A\n\nfunc F(c chan int) { _ = 0%0&&0- <-c }\n",
		"package A\n\nvar A = 0%0&&0+ +0\n",
		"package A\n\nvar A = 0%0&&0& ^0\n",
	}
	for _, src := range sources {
		t.Run(src, func(t *testing.T) {
			p := parser.NewGoParser()
			p.ParseOnly = true
			cu, err := p.Parse("t.go", src)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			want, err := gofmtSource("t.go", src)
			if err != nil {
				t.Fatalf("gofmt: %v", err)
			}
			if got := runAutoFormat(cu); got != want {
				t.Errorf("got  %q\nwant %q", got, want)
			}
		})
	}
}
