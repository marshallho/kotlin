/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.metrics

import java.io.Serializable

data class BuildMetrics(
    val buildTimes: Map<BuildTime, Long>,
    val buildAttributes: Map<BuildAttribute, String>
) : Serializable {
    companion object {
        const val serialVersionUID = 0L
    }
}