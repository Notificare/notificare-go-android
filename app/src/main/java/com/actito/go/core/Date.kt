package com.actito.go.core

import java.util.*

fun today(): Date = Calendar.getInstance().apply {
    time = Date()
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.time

fun yesterday(): Date = Calendar.getInstance().apply {
    time = today()
    add(Calendar.DAY_OF_YEAR, -1)
}.time

fun sevenDaysAgo(): Date = Calendar.getInstance().apply {
    time = today()
    add(Calendar.DAY_OF_YEAR, -7)
}.time
