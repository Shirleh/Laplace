package com.github.shirleh

import java.util.*

fun <T> Optional<T>.orElseNull(): T? = this.orElse(null)
