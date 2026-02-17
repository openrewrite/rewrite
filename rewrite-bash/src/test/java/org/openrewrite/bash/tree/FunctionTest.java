/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.bash.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.bash.Assertions.bash;

class FunctionTest implements RewriteTest {

    @Test
    void simpleFunction() {
        rewriteRun(
          bash(
            "foo() {\n  echo hello\n}\n"
          )
        );
    }

    @Test
    void functionKeyword() {
        rewriteRun(
          bash(
            "function foo() {\n  echo bar\n}\n"
          )
        );
    }

    @Test
    void functionKeywordNoParens() {
        rewriteRun(
          bash(
            "function foo {\n  echo bar\n}\n"
          )
        );
    }

    @Test
    void functionWithLocal() {
        rewriteRun(
          bash(
            "function greet() {\n  local name=\"$1\"\n  echo \"Hello, $name\"\n}\n"
          )
        );
    }

    @Test
    void functionWithReturn() {
        rewriteRun(
          bash(
            "check() {\n  if [ -f \"$1\" ]; then\n    return 0\n  fi\n  return 1\n}\n"
          )
        );
    }

    @Test
    void functionWithLocalArray() {
        rewriteRun(
          bash(
            "process() {\n  local -a items=()\n  items+=(one)\n  items+=(two)\n  echo \"${items[@]}\"\n}\n"
          )
        );
    }

    @Test
    void functionWithCompoundBody() {
        rewriteRun(
          bash(
            "cleanup() {\n  rm -f /tmp/lock\n  echo \"cleaned up\"\n}\n"
          )
        );
    }

    @Test
    void functionCalledInPipeline() {
        rewriteRun(
          bash(
            "get_data() {\n  echo hello\n}\nget_data | grep h\n"
          )
        );
    }

    @Test
    void functionNameNoSpace() {
        rewriteRun(
          bash(
            "generate_random_id(){\n  echo $RANDOM\n}\n"
          )
        );
    }

    @Test
    void functionWithWhileLoopAndRegex() {
        rewriteRun(
          bash(
            """
            expand_env() {
                local value="$1"
                while [[ "$value" =~ \\$\\{([^}]+)\\} ]]; do
                    local var="${BASH_REMATCH[1]}"
                    local val="${!var:-}"
                    value="${value/\\$\\{$var\\}/$val}"
                done
                echo "$value"
            }
            """
          )
        );
    }

    @Test
    void functionWithForLoopAndArrays() {
        rewriteRun(
          bash(
            """
            run_test() {
                local base_url="$1"
                local -a latencies=()
                local count=0

                for i in {1..10}; do
                    local start=$(($(date +%s%N) / 1000000))
                    local code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "$base_url" 2>/dev/null)
                    local end=$(($(date +%s%N) / 1000000))
                    local latency=$((end - start))

                    if [[ "$code" =~ ^[2-3] ]]; then
                        ((count++))
                        latencies+=($latency)
                    fi
                done

                if [[ ${#latencies[@]} -eq 0 ]]; then
                    echo "failed"
                    return 1
                fi

                local sorted=($(printf '%s\\n' "${latencies[@]}" | sort -n))
                echo "${sorted[0]} ${sorted[$((${#sorted[@]} - 1))]}"
            }
            """
          )
        );
    }

    @Test
    void functionWithSubshellBody() {
        rewriteRun(
          bash(
            "f() (\n  echo subshell\n)\n"
          )
        );
    }

    @Test
    void functionWithArithmetic() {
        rewriteRun(
          bash(
            "f() {\n  x=$((1+2))\n}\n"
          )
        );
    }

    @Test
    void functionWithTripleParenArithmetic() {
        rewriteRun(
          bash(
            "f() {\n  x=$(((1)))\n}\n"
          )
        );
    }

    @Test
    void functionWithDeeplyNestedSubshells() {
        rewriteRun(
          bash(
            "f() {\n\t((((( cmd 2>&1; echo $? >&3) | cat >&4) 3>&1) | (read xs; exit $xs)) 4>>log && echo ok) || echo fail\n}\n"
          )
        );
    }

    @Test
    void functionWithComplexCommandSub() {
        rewriteRun(
          bash(
            "f() {\n\t[[ true ]] && {\n\t\texit 1\n\t}\n}\ng() {\n\tERRMSG=$((echo $$ > cgroup.procs) |& cat)\n}\n"
          )
        );
    }

    @Test
    void functionWithArithmeticAfterTripleParens() {
        rewriteRun(
          bash(
            """
            f() {
              echo $(((t1 - t0) / interval))
            }
            g() {
              local order="$((1 << 40))"
            }
            """
          )
        );
    }

    @Test
    void functionWithEvalIfAndSubshells() {
        rewriteRun(
          bash(
            """
            f() {
              eval x="\\$${ref:-}"
              if [ $(head -n 1 "$f") = "#!" ]; then
                echo found
              fi
              ((((( cmd 2>&1; echo $? >&3) | cat >&4) 3>&1) | (read xs; exit $xs)) 4>>log && echo ok) || echo fail
            }
            """
          )
        );
    }
}
