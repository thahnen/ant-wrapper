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

import java.io.File
import java.lang.RuntimeException
import java.math.BigInteger
import java.security.MessageDigest


/**
 *  Distribution utilities
 *
 *  @param antUserHome user Ant directory
 *  @param projectDirectory Ant project directory
 */
internal class PathAssembler(private val antUserHome: File, private val projectDirectory: File) {
    /** Local distribution install folder / ZIP file */
    data class LocalDistribution(val distZip: File, val distDir: File)


    companion object {
        const val ANT_USER_HOME_STRING  = "ANT_USER_HOME"
        const val PROJECT_STRING        = "PROJECT"


        /**
         *  Creates a MD5 hash of a string provided
         *
         *  @param string to get the hash from
         *  @return MD5-Hash of parameter provided
         *  @throws RuntimeException when hashing failed
         */
        @Throws(RuntimeException::class)
        private fun getHash(string: String) : String {
            try {
                val messageDigest = MessageDigest.getInstance("MD5")
                messageDigest.update(string.toByteArray())
                return BigInteger(1, messageDigest.digest()).toString(36)
            } catch (err: Exception) {
                throw RuntimeException("Could not hash input string.", err)
            }
        }


        /**
         *  Removes the extension of a filename
         *
         *  @param name filename to strip extension
         *  @return file name without extension
         */
        private fun removeExtension(name: String) : String {
            with (name.lastIndexOf(".")) {
                return when {
                    this < 0    -> name
                    else        -> name.substring(this + 1)
                }
            }
        }


        /**
         *  Removes the folder structure from a URI
         *
         *  @param distUrl distribution URI to get name from
         *  @return name without folder structure
         */
        private fun getDistName(distUrl: String) : String {
            with (distUrl.lastIndexOf("/")) {
                return when {
                    this < 0    -> distUrl
                    else        -> distUrl.substring(this + 1)
                }
            }
        }
    }


    /**
     *  Get base directory of string describing base type
     *
     *  @param base string describing base type
     *  @return specific directory
     *  @throws RuntimeException when base string unknown
     */
    @Throws(RuntimeException::class)
    private fun getBaseDir(base: String) : File = when (base) {
        ANT_USER_HOME_STRING    -> antUserHome
        PROJECT_STRING          -> projectDirectory
        else                    -> throw RuntimeException("Base: $base is unknown.")
    }


    /**
     *  Get root directory name based on distribution name and wrapper configuration
     *
     *  @param distName distribution name
     *  @param configuration Ant wrapper configuration
     *  @return URI safe root directory name
     *  @throws RuntimeException when converting to safe URI fails
     */
    @Throws(RuntimeException::class)
    private fun rootDirName(distName: String, configuration: WrapperConfiguration) : String {
        return "$distName/${getHash(Download.safeUri(configuration.distribution).toString())}"
    }


    /**
     *  Get the local distribution based on an Ant wrapper configuration provided
     *
     *  @param configuration Ant wrapper configuration
     *  @return distribution created using wrapper configuration
     */
    fun getDistribution(configuration: WrapperConfiguration) : LocalDistribution {
        val baseName = getDistName(configuration.distribution.path)
        val rootDirName = rootDirName(removeExtension(baseName), configuration)
        return LocalDistribution(
            File(getBaseDir(configuration.distributionBase), "${configuration.distributionPath}/$rootDirName"),
            File(getBaseDir(configuration.zipBase), "${configuration.zipPath}/$rootDirName/$baseName")
        )
    }
}
