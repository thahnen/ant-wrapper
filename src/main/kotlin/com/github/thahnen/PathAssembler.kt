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

import com.github.thahnen.extension.*
import com.github.thahnen.wrapper.Configuration


/**
 *  Distribution utilities
 *
 *  @param antUserHome user Ant directory
 *  @param projectDirectory Ant project directory
 */
internal class PathAssembler(private val antUserHome: File, private val projectDirectory: File) {
    /** Local distribution install folder / ZIP file */
    data class LocalDistribution(val distZip: File, val distDir: File)


    /** Companion object */
    companion object {
        const val ANT_USER_HOME_STRING  = "ANT_USER_HOME"
        const val PROJECT_STRING        = "PROJECT"
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
        else                    -> throw RuntimeException(
            "[${this::class.simpleName}.getBaseDir] Base: $base is unknown."
        )
    }


    /**
     *  Get the local distribution based on an Ant wrapper configuration provided
     *
     *  @param configuration Ant wrapper configuration
     *  @return distribution created using wrapper configuration
     *  @throws RuntimeException when parsing URI failed or base unknown
     */
    @Throws(RuntimeException::class)
    fun getDistribution(configuration: Configuration) : LocalDistribution {
        val baseName = configuration.distribution.path.getFileName()
        val rootDirName = baseName.removeExtension() +
                            "/${Download.safeUri(configuration.distribution).toString().getHash()}"

        return LocalDistribution(
            File(getBaseDir(configuration.zipBase), "${configuration.zipPath}/$rootDirName/$baseName"),
            File(getBaseDir(configuration.distributionBase), "${configuration.distributionPath}/$rootDirName")
        )
    }
}
