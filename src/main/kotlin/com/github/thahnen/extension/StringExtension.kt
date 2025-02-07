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
package com.github.thahnen.extension

import java.math.BigInteger
import java.security.MessageDigest


/**
 *  Creates the MD5 hash from string
 *
 *  @return MD5 hash
 */
internal fun String.getHash() : String {
    val md = MessageDigest.getInstance("MD5")
    md.update(this.toByteArray())
    return BigInteger(1, md.digest()).toString(36)
}


/**
 *  Removes the extension of a filename
 *
 *  @return file name without extension
 */
internal fun String.removeExtension() : String {
    val index = this.lastIndexOf(".")
    return when {
        index < 0   -> this
        else        -> this.substring(0, index)
    }
}


/**
 *  Removes the folder structure from a URI
 *
 *  @return name without folder structure
 */
internal fun String.getFileName() : String {
    val index = this.lastIndexOf("/")
    return when {
        index < 0   -> this
        else        -> this.substring(index + 1)
    }
}
