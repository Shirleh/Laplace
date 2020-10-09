package com.github.shirleh.extensions

import java.util.*

fun <T> Optional<T>.orElseNull(): T? = this.orElse(null)
