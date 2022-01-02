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
@file:JvmName("AntWrapper")

package com.github.thahnen

import java.io.File
import java.net.*
import java.nio.file.Paths
import java.util.*

import org.gradle.cli.*
import kotlin.collections.HashMap


internal const val ANT_USER_HOME_OPTION = "a"
internal const val ANT_USER_HOME_DETAILED_OPTION = "ant-user-home"
internal const val ANT_QUIET_OPTION = "q"
internal const val ANT_QUIET_DETAILED_OPTION = "quiet"


/**
 *  Main method to invoke Ant wrapper
 */
fun main(args: Array<String>) {
    val wrapperJar = wrapperJar()
    val propertiesFile = wrapperProperties(wrapperJar)
    val rootDir = projectDir(wrapperJar)

    val parser = CommandLineParser()
    parser.allowUnknownOptions()
    parser.option(ANT_USER_HOME_OPTION, ANT_USER_HOME_DETAILED_OPTION).hasArgument()
    parser.option(ANT_QUIET_OPTION, ANT_QUIET_DETAILED_OPTION)

    val converter = SystemPropertiesCommandLineConverter()
    converter.configure(parser)

    val options = parser.parse(*args)

    val properties = System.getProperties()
    properties.putAll(converter.convert(options, HashMap<String, String>()))

    val antUserHome = antUserHome(options)

    addSystemProperties(properties, antUserHome, rootDir)
}


/**
 *  Get Ant wrapper JAR file
 *
 *  @return file object of wrapper
 *  @throws RuntimeException when location can not be resolved
 */
@Throws(RuntimeException::class)
internal fun wrapperJar() : File {
    var location: URI? = null

    try {
        location = Logger::class.java.protectionDomain.codeSource.location.toURI()
    } catch (err: URISyntaxException) {
        throw RuntimeException(err)
    }

    when {
        !location.scheme.equals("file") -> throw RuntimeException(
            "Cannot determine classpath for wrapper Jar from codebase $location"
        )
    }

    return try {
        Paths.get(location!!).toFile()
    } catch (err: NoClassDefFoundError) {
        File(location.path)
    }
}


/** Get Ant wrapper properties file */
internal fun wrapperProperties(wrapperJar: File) : File =  File(
    wrapperJar.parent, wrapperJar.name.replaceFirst("\\jar$", ".properties")
)


/** Get Ant project directory */
internal fun projectDir(wrapperJar: File) : File = wrapperJar.parentFile.parentFile.parentFile


/**
 *  Adds the system properties from gradle.properties file
 *
 *  @param systemProperties properties which should be updated
 *  @param antUserHome Ant user directory
 *  @param rootDir Ant project directory
 *  @throws RuntimeException when loading properties file fails
 */
internal fun addSystemProperties(systemProperties: Properties, antUserHome: File, rootDir: File) {
    systemProperties.putAll(SystemPropertiesHandler.getSystemProperties(File(rootDir, "gradle.properties")))
    systemProperties.putAll(SystemPropertiesHandler.getSystemProperties(File(antUserHome, "gradle.properties")))
}


/** Get Ant user directory */
internal fun antUserHome(options: ParsedCommandLine) : File {
    return when (options.hasOption(ANT_USER_HOME_OPTION)) {
        true    -> File(options.option(ANT_USER_HOME_OPTION).value)
        false   -> AntUserHomeLookup.antUserHome()
    }
}
