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

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.security.MessageDigest
import java.util.ArrayList
import java.util.zip.ZipFile


/** Constants used */
internal const val BUFFER_SIZE: Int = 4096


/**
 *  Calculate the SHA-256 hash of a file
 *
 *  @return hash as string
 *  @throws IOException when working with input stream fails
 */
@Throws(IOException::class)
internal fun File.calculateSha256Sum() : String {
    val md = MessageDigest.getInstance("SHA-256")

    FileInputStream(this).use {
        val buffer = ByteArray(BUFFER_SIZE)

        do {
            val n = it.read(buffer)
            when {
                n < 0 -> break
                n > 0 -> md.update(buffer, 0, n)
            }
        } while (true)
    }

    val byteData = md.digest()
    val hexString = StringBuffer()
    byteData.forEach {
        val hex = Integer.toHexString(0xff and it.toInt())
        when (hex.length) {
            1 -> hexString.append('0')
        }
        hexString.append(hex)
    }

    return hexString.toString()
}


/**
 *  Deletes a directory recursively
 *
 *  @return whether deletion was a success or not
 */
internal fun File.deleteDir() : Boolean {
    if (this.isDirectory) {
        this.list()?.forEach {
            when {
                !File(this, it).deleteDir() -> return false
            }
        }
    }

    return this.delete()
}


/**
 *  List all directories inside a given directory
 *
 *  @return all sub directories
 */
internal fun File.listDirs() : List<File> {
    val dirs = ArrayList<File>()
    if (this.exists()) {
        this.listFiles()?.forEach {
            when {
                it.isDirectory -> dirs.add(it)
            }
        }
    }

    return dirs
}


/**
 *  Unzip a specific file to a destination provided
 *
 *  @param dest destination to unzip to
 *  @throws IOException when working with ZIP archive did not work
 */
@Throws(IOException::class)
internal fun File.unzip(dest: File) {
    val zipFile = ZipFile(this)
    zipFile.use {
        val entries = it.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory) {
                File(dest, entry.name).mkdirs()
                continue
            }

            BufferedOutputStream(FileOutputStream(File(dest, entry.name))).use { s ->
                it.getInputStream(entry).copyToOutputStream(s)
            }
        }
    }
}


/**
 *  Writes to a file from given input stream
 *
 *  @param inp stream to read from
 *  @throws IOException when writing to file fails
 */
@Throws(IOException::class)
internal fun File.fromInputStream(inp: InputStream) {
    FileOutputStream(this).use {
        it.channel.use { out ->
            Channels.newChannel(inp).use { ins ->
                val buffer = ByteBuffer.allocate(BUFFER_SIZE)
                while (ins.read(buffer) >= 0 || buffer.position() > 0) {
                    buffer.flip()
                    out.write(buffer)
                    buffer.clear()
                }
            }
        }
    }
}
