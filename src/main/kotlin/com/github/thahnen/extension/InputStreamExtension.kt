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

import java.io.InputStream
import java.io.IOException
import java.io.OutputStream


/**
 *  Copy data from input to output stream
 *
 *  @param out output stream
 *  @throws IOException when working with streams fails
 */
@Throws(IOException::class)
internal fun InputStream.copyToOutputStream(out: OutputStream) {
    val buffer = ByteArray(BUFFER_SIZE)

    do {
        val len = this.read(buffer)
        when {
            len < 0 -> break
            len > 0 -> out.write(buffer, 0, len)
        }
    } while (true)

    this.close()
    out.close()
}
