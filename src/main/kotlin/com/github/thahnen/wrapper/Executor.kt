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
package com.github.thahnen.wrapper

import java.io.*
import java.net.URI
import java.util.*

import com.github.thahnen.*
import com.github.thahnen.util.findLauncherJar
import java.net.URLClassLoader


/**
 *  Executes the Ant wrapper based on properties provided
 *
 *  @author Tobias Hahnen
 */
internal class Executor(private val propertiesFile: File, private val properties: Properties) {
    /** Companion object */
    companion object {
        /** Wrapper property keys */
        const val DISTRIBUTION_URL_PROPERTY     = "distributionUrl"
        const val DISTRIBUTION_BASE_PROPERTY    = "distributionBase"
        const val DISTRIBUTION_PATH_PROPERTY    = "distributionPath"
        const val DISTRIBUTION_SHA_256_SUM      = "distributionSha256Sum"
        const val ZIP_STORE_BASE_PROPERTY       = "zipStoreBase"
        const val ZIP_STORE_PATH_PROPERTY       = "zipStorePath"


        /** Create object for Ant wrapper properties file */
        @Throws(RuntimeException::class)
        internal fun forWrapperPropertiesFile(propertiesFile: File) : Executor {
            when {
                !propertiesFile.exists() -> throw RuntimeException(
                    "[${Executor::class.simpleName} -> forWrapperPropertiesFile] Wrapper properties file " +
                    "$propertiesFile does not exist!"
                )
            }

            return Executor(propertiesFile, Properties())
        }
    }


    /** Ant wrapper configuration */
    private var config: Configuration? = null


    /** Constructor initialization */
    init {
        if (propertiesFile.exists()) {
            try {
                FileInputStream(propertiesFile).use {
                    properties.load(it)
                }

                config = Configuration(
                    prepareDistributionUri(),
                    getProperty(DISTRIBUTION_BASE_PROPERTY, PathAssembler.ANT_USER_HOME_STRING)!!,
                    getProperty(DISTRIBUTION_PATH_PROPERTY, Install.DEFAULT_DISTRIBUTION_PATH)!!,
                    getProperty(DISTRIBUTION_SHA_256_SUM, null, false),
                    getProperty(ZIP_STORE_BASE_PROPERTY, PathAssembler.ANT_USER_HOME_STRING)!!,
                    getProperty(ZIP_STORE_PATH_PROPERTY, Install.DEFAULT_DISTRIBUTION_PATH)!!
                )
            } catch (err: Exception) {
                throw RuntimeException(
                    "[${this::class.simpleName}.init] Could not load wrapper properties from $propertiesFile", err
                )
            }
        }
    }


    /** Get a property by key and fallback to default value if provided or fail if required */
    private fun getProperty(propertyName: String, defaultValue: String? = null, required: Boolean = true) : String? {
        properties.getProperty(propertyName)?.let { return it }
        defaultValue?.let { return it }

        when (required) {
            true -> throw RuntimeException(
                "[${this::class.simpleName}.getProperty] No value with key $propertyName specified in wrapper " +
                "properties file $propertiesFile"
            )
        }

        return null
    }


    /** Prepare the distribution URL */
    private fun prepareDistributionUri() : URI {
        val source = properties.getProperty(DISTRIBUTION_URL_PROPERTY)?.let {
            URI(it)
        } ?: throw RuntimeException(
            "[${this::class.simpleName}.prepareDistributionUri] No value with key $DISTRIBUTION_URL_PROPERTY " +
            "specified in wrapper properties file $propertiesFile"
        )

        return source.scheme?.let { source } ?: File(propertiesFile.parent, source.schemeSpecificPart).toURI()
    }


    /** Executes the actual Ant wrapper */
    @Throws(RuntimeException::class)
    fun execute(args: Array<String>, install: Install) {
        with (install.createDist(config!!)) {
            val antJar = findLauncherJar(this)
            antJar ?: throw RuntimeException(
                "[${this::class.simpleName}.start] Could not locate the Ant launcher JAR in Ant distribution $this!"
            )

            URLClassLoader(arrayOf(antJar.toURI().toURL()), ClassLoader.getSystemClassLoader().parent).use {
                Thread.currentThread().contextClassLoader = it
                it.loadClass("org.apache.tools.ant.launch.Launcher")
                    .getMethod("main", Array<String>::class.java)
                    .invoke(null, args)
            }
        }
    }
}
