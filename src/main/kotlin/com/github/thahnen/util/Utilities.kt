/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thahnen.util

import java.util.*


/**
 *  Checks on multiple parameters if they are null instead of using something like this:
 *
 *  param1?.let { p1 ->
 *      param2?.let { p2 ->
 *          ...
 *      }
 *  }
 */
internal inline fun <T: Any> multipleLet(vararg elements: T?, closure: (List<T>) -> Unit) {
    if (elements.all { it != null}) {
        closure(elements.filterNotNull())
    }
}


/** Checks if current system is Windows */
internal fun isWindows() : Boolean = System.getProperty("os.name").toLowerCase(Locale.US).indexOf("windows") > -1
