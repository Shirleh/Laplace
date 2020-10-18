@file:Suppress("unused")

package com.github.shirleh.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.core.publisher.Mono

suspend fun <T> Mono<T>.await(): T = awaitFirst()

suspend fun <T> Mono<T>.awaitNullable(): T? = awaitFirstOrNull()

suspend fun Mono<*>.awaitComplete(): Unit = awaitNullable().let { Unit }

@JvmName("awaitVoid")
suspend fun Mono<Void>.await(): Unit = awaitComplete()

val <T> Mono<T>.async: Deferred<T> get() = CoroutineScope(Job()).async { await() }

fun <T> Mono<T>.async(scope: CoroutineScope = CoroutineScope(Job())): Deferred<T> = scope.async { await() }

fun <T> Mono<T>.nullableAsync(scope: CoroutineScope = CoroutineScope(Job())): Deferred<T?> = scope.async { awaitNullable() }

fun Mono<*>.completeAsync(scope: CoroutineScope = CoroutineScope(Job())): Deferred<Unit> = scope.async { awaitComplete() }

@JvmName("asyncVoid")
fun Mono<Void>.async(scope: CoroutineScope = CoroutineScope(Job())): Deferred<Unit> = completeAsync(scope)
