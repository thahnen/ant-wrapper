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

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.concurrent.Callable

import com.github.thahnen.extension.maybeCloseQuietly


/**
 *  Handle access to specific files using locks
 *
 *  @author Tobias Hahnen
 */
internal class ExclusiveFileAccessManager private constructor() {
    companion object {
        /** Constants used */
        private const val TIMEOUT_MS : Long         = 120000
        private const val POLL_INTERVAL_MS: Long    = 200


        /**
         *  Try to access a file provided and add lock, run task while locked
         *
         *  @param exclusiveFile file object to access
         *  @param task to be run when locked
         *  @return ???
         *  @throws IOException when lock file operations failed
         *  @throws RuntimeException when lock file operations failed
         */
        @Throws(IOException::class, RuntimeException::class)
        fun <T> access(exclusiveFile: File, task: Callable<T>) : T {
            val lockFile = File(exclusiveFile.parentFile, "${exclusiveFile.name}.lck")
            val lockFileDirectory = lockFile.parentFile
            if (!lockFileDirectory.mkdirs() && (!lockFileDirectory.exists() || !lockFileDirectory.isDirectory)) {
                throw RuntimeException(
                    "[${this::class.simpleName} -> access] Could not create parent directory for lock file " +
                    lockFile.absolutePath
                )
            }

            var randomAccessFile: RandomAccessFile? = null
            var channel: FileChannel? = null
            try {
                val expiry = System.currentTimeMillis() + TIMEOUT_MS
                var lock: FileLock? = null
                while (lock == null && System.currentTimeMillis() < expiry) {
                    randomAccessFile = RandomAccessFile(lockFile, "rw")
                    channel = randomAccessFile.channel
                    lock = channel.tryLock()

                    lock ?: run {
                        channel.maybeCloseQuietly()
                        randomAccessFile.maybeCloseQuietly()
                        Thread.sleep(POLL_INTERVAL_MS)
                    }
                }

                lock ?: run {
                    throw RuntimeException(
                        "[${this::class.simpleName} -> access] Timeout of $TIMEOUT_MS reached waiting for exclusive " +
                        "access to file: ${exclusiveFile.absolutePath}"
                    )
                }

                try {
                    return task.call()
                } finally {
                    lock.release()

                    channel.maybeCloseQuietly()
                    channel = null
                    randomAccessFile.maybeCloseQuietly()
                    randomAccessFile = null
                }
            } finally {
                channel.maybeCloseQuietly()
                randomAccessFile.maybeCloseQuietly()
            }
        }
    }
}
