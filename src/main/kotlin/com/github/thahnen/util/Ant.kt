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
import java.util.*

import org.gradle.cli.ParsedCommandLine


/** Ant constants */
private val DEFAULT_ANT_USER_HOME               = "${System.getProperty("user.home")}/.ant"
private const val ANT_USER_HOME_PROPERTY_KEY    = "ant.user.home"
private const val ANT_USER_HOME_ENV_KEY         = "ANT_USER_HOME"
private const val ANT_PROPERTIES_NAME           = "ant.properties"
private const val ANT_LAUNCHER_JAR              = "ant-launcher.jar"


/**
 *  Adds the system properties from gradle.properties file
 *
 *  @param systemProperties properties which should be updated
 *  @param antUserHome Ant user directory
 *  @param rootDir Ant project directory
 *  @throws RuntimeException when loading properties file fails
 */
internal fun addSystemProperties(systemProperties: Properties, antUserHome: File, rootDir: File) {
    listOf(rootDir, antUserHome).forEach {
        systemProperties.putAll(SystemPropertiesHandler.getSystemProperties(File(it, ANT_PROPERTIES_NAME)))
    }
}


/**
 *  Get Ant user directory in the following order:
 *  - command line argument
 *  - system property
 *  - environment variable
 *  - default
 */
internal fun antUserHome(options: ParsedCommandLine, option: String) : File {
    return when (options.hasOption(option)) {
        true    -> File(options.option(option).value)
        false   -> run {
            System.getProperty(ANT_USER_HOME_PROPERTY_KEY)?.let { return File(it) }
            System.getenv(ANT_USER_HOME_ENV_KEY)?.let { return File(it) }
            return File(DEFAULT_ANT_USER_HOME)
        }
    }
}


/**
 *  Find the specific launcher JAR
 *  -> $installation/lib/ant-launcher.jar
 *
 *  @param antHome Ant installation
 *  @return launcher JAR when found, otherwise null
 */
internal fun findLauncherJar(antHome: File) : File? {
    with (File(antHome, "lib")) {
        if (this.exists() && this.isDirectory) {
            return this.listFiles()?.firstOrNull { it.name == ANT_LAUNCHER_JAR }
        }
    }

    return null
}


/** Get Ant project directory */
internal fun projectDir(wrapperJar: File) : File = wrapperJar.parentFile.parentFile.parentFile


/** Get Ant wrapper properties file */
internal fun wrapperProperties(wrapperJar: File) : File =  File(
    wrapperJar.parent, wrapperJar.name.replaceFirst("\\jar$", ".properties")
)
