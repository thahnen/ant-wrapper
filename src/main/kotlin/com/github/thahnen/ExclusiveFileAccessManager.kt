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
package com.github.thahnen

import java.io.*
import java.nio.channels.*
import java.util.concurrent.Callable


/**
 *  Handle access to specific files using locks
 *
 *  @author Tobias Hahnen
 */
internal class ExclusiveFileAccessManager(private val timeoutMS: Int, private val pollIntervalMs: Int) {
    companion object {
        internal const val LOCK_FILE_SUFFIX = ".lck"

        /** Get current time in milliseconds */
        private fun getTimeMillis() : Long = System.nanoTime() / (1000L * 1000L)

        /** Try to close a closeable */
        private fun maybeCloseQuietly(closable: Closeable?) {
            closable?.let {
                try {
                    closable.close()
                } catch (ignored: Exception) { }
            }
        }
    }


    /**
     *  Try to access a file provided and add lock, run task while locked
     *
     *  @param exclusiveFile file object to access
     *  @param task to be run when locked
     *  @return ???
     *  @throws RuntimeException
     */
    @Throws(RuntimeException::class)
    fun <T> access(exclusiveFile: File, task: Callable<T>) : T {
        val lockFile = File(exclusiveFile.parentFile, "${exclusiveFile.name}$LOCK_FILE_SUFFIX")
        val lockFileDirectory = lockFile.parentFile
        if (!lockFileDirectory.mkdirs() && (!lockFileDirectory.exists() || !lockFileDirectory.isDirectory)) {
            throw RuntimeException("Could not create parent directory for lock file ${lockFile.absolutePath}")
        }

        var randomAccessFile: RandomAccessFile? = null
        var channel: FileChannel? = null
        try {
            val expiry = getTimeMillis() + timeoutMS
            var lock: FileLock? = null
            while (lock == null && getTimeMillis() < expiry) {
                randomAccessFile = RandomAccessFile(lockFile, "rw")
                channel = randomAccessFile.channel
                lock = channel.tryLock()

                lock ?: run {
                    maybeCloseQuietly(channel)
                    maybeCloseQuietly(randomAccessFile)
                    Thread.sleep(pollIntervalMs.toLong())
                }
            }

            lock ?: run {
                throw RuntimeException(
                    "Timeout of $timeoutMS reached waiting for exclusive access to file: ${exclusiveFile.absolutePath}"
                )
            }

            try {
                return task.call()
            } finally {
                lock.release()

                maybeCloseQuietly(channel)
                channel = null
                maybeCloseQuietly(randomAccessFile)
                randomAccessFile = null
            }
        } finally {
            maybeCloseQuietly(channel)
            maybeCloseQuietly(randomAccessFile)
        }
    }
}
