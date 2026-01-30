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
package org.openrewrite.kotlin.replace;

import org.junit.jupiter.api.Test;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinxCoroutinesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResources("org.jetbrains.kotlinx.ReplaceDeprecatedKotlinxCoroutinesCore1Methods")
          .parser(KotlinParser.builder().classpath("kotlinx-coroutines-core-jvm"))
          .afterTypeValidationOptions(TypeValidation.all().methodInvocations(false));
    }

    @Test
    void replacePollWithTryReceive() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.channels.ReceiveChannel
              
              @Suppress("DEPRECATION")
              fun <T> pollChannel(channel: ReceiveChannel<T>): T? {
                  return channel.poll()
              }
              """,
            """
              import kotlinx.coroutines.channels.ReceiveChannel
              
              @Suppress("DEPRECATION")
              fun <T> pollChannel(channel: ReceiveChannel<T>): T? {
                  return channel.tryReceive().getOrNull()
              }
              """
          )
        );
    }

    @Test
    void replaceOfferWithTrySend() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.channels.SendChannel
              
              @Suppress("DEPRECATION")
              fun <T> offerToChannel(channel: SendChannel<T>, value: T): Boolean {
                  return channel.offer(value)
              }
              """,
            """
              import kotlinx.coroutines.channels.SendChannel
              
              @Suppress("DEPRECATION")
              fun <T> offerToChannel(channel: SendChannel<T>, value: T): Boolean {
                  return channel.trySend(value).isSuccess
              }
              """
          )
        );
    }

    @Test
    void replaceFlattenWithFlattenConcat() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.flatten
              
              @Suppress("DEPRECATION")
              fun <T> flattenFlow(flow: Flow<Flow<T>>): Flow<T> {
                  return flow.flatten()
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.flatten
              
              @Suppress("DEPRECATION")
              fun <T> flattenFlow(flow: Flow<Flow<T>>): Flow<T> {
                  return flow.flattenConcat()
              }
              """
          )
        );
    }

    @Test
    void replaceSkipWithDrop() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.skip
              
              @Suppress("DEPRECATION")
              fun <T> skipItems(flow: Flow<T>, n: Int): Flow<T> {
                  return flow.skip(n)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.skip
              
              @Suppress("DEPRECATION")
              fun <T> skipItems(flow: Flow<T>, n: Int): Flow<T> {
                  return flow.drop(n)
              }
              """
          )
        );
    }

    @Test
    void replaceReceiveOrNullWithReceiveCatching() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.channels.ReceiveChannel
              
              @Suppress("DEPRECATION")
              suspend fun <T> receiveOrNullValue(channel: ReceiveChannel<T>): T? {
                  return channel.receiveOrNull()
              }
              """,
            """
              import kotlinx.coroutines.channels.ReceiveChannel
              
              @Suppress("DEPRECATION")
              suspend fun <T> receiveOrNullValue(channel: ReceiveChannel<T>): T? {
                  return channel.receiveCatching().getOrNull()
              }
              """
          )
        );
    }

    @Test
    void replaceConcatMapWithFlatMapConcat() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.concatMap
              
              @Suppress("DEPRECATION")
              fun <T, R> concatMapFlow(flow: Flow<T>, mapper: (T) -> Flow<R>): Flow<R> {
                  return flow.concatMap(mapper)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.concatMap
              
              @Suppress("DEPRECATION")
              fun <T, R> concatMapFlow(flow: Flow<T>, mapper: (T) -> Flow<R>): Flow<R> {
                  return flow.flatMapConcat(mapper)
              }
              """
          )
        );
    }

    @Test
    void replaceScanReduceWithRunningReduce() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.scanReduce
              
              @Suppress("DEPRECATION")
              fun <T> scanReduceFlow(flow: Flow<T>, op: suspend (T, T) -> T): Flow<T> {
                  return flow.scanReduce(op)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.scanReduce
              
              @Suppress("DEPRECATION")
              fun <T> scanReduceFlow(flow: Flow<T>, op: suspend (T, T) -> T): Flow<T> {
                  return flow.runningReduce(op)
              }
              """
          )
        );
    }

    @Test
    void replaceSwitchMapWithFlatMapLatest() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.switchMap
              
              @Suppress("DEPRECATION")
              fun <T, R> switchMapFlow(flow: Flow<T>, transform: suspend (T) -> Flow<R>): Flow<R> {
                  return flow.switchMap(transform)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.switchMap
              
              @Suppress("DEPRECATION")
              fun <T, R> switchMapFlow(flow: Flow<T>, transform: suspend (T) -> Flow<R>): Flow<R> {
                  return flow.flatMapLatest(transform)
              }
              """
          )
        );
    }

    @Test
    void replaceComposeWithLet() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.compose
              
              @Suppress("DEPRECATION")
              fun <T, R> composeFlow(flow: Flow<T>, transformer: (Flow<T>) -> Flow<R>): Flow<R> {
                  return flow.compose(transformer)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.compose
              
              @Suppress("DEPRECATION")
              fun <T, R> composeFlow(flow: Flow<T>, transformer: (Flow<T>) -> Flow<R>): Flow<R> {
                  return flow.let(transformer)
              }
              """
          )
        );
    }

    @Test
    void replaceDelayEachWithOnEach() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.delayEach
              
              @Suppress("DEPRECATION")
              fun <T> delayEachItem(flow: Flow<T>, delayMs: Long): Flow<T> {
                  return flow.delayEach(delayMs)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.delayEach
              
              @Suppress("DEPRECATION")
              fun <T> delayEachItem(flow: Flow<T>, delayMs: Long): Flow<T> {
                  return flow.onEach { delay(delayMs) }
              }
              """
          )
        );
    }

    @Test
    void replaceDelayFlowWithOnStart() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.delayFlow
              
              @Suppress("DEPRECATION")
              fun <T> delayFlowStart(flow: Flow<T>, delayMs: Long): Flow<T> {
                  return flow.delayFlow(delayMs)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.delayFlow
              
              @Suppress("DEPRECATION")
              fun <T> delayFlowStart(flow: Flow<T>, delayMs: Long): Flow<T> {
                  return flow.onStart { delay(delayMs) }
              }
              """
          )
        );
    }

    @Test
    void replaceCombineLatestWithCombine() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.combineLatest
              
              @Suppress("DEPRECATION")
              fun <T, R> combineFlows(flow1: Flow<T>, flow2: Flow<T>, transform: suspend (T, T) -> R): Flow<R> {
                  return flow1.combineLatest(flow2, transform)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.combineLatest
              
              @Suppress("DEPRECATION")
              fun <T, R> combineFlows(flow1: Flow<T>, flow2: Flow<T>, transform: suspend (T, T) -> R): Flow<R> {
                  return flow1.combine(flow2, transform)
              }
              """
          )
        );
    }

    @Test
    void replaceScanFoldWithScan() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.scanFold
              
              @Suppress("DEPRECATION")
              fun <T, R> scanFoldFlow(flow: Flow<T>, init: R, op: suspend (R, T) -> R): Flow<R> {
                  return flow.scanFold(init, op)
              }
              """,
            """
              import kotlinx.coroutines.flow.Flow
              import kotlinx.coroutines.flow.scanFold
              
              @Suppress("DEPRECATION")
              fun <T, R> scanFoldFlow(flow: Flow<T>, init: R, op: suspend (R, T) -> R): Flow<R> {
                  return flow.scan(init, op)
              }
              """
          )
        );
    }
}
