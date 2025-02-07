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
@file:JvmName("AntWrapperMain")

package com.github.thahnen

import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Paths

import kotlin.collections.HashMap

import org.gradle.cli.CommandLineArgumentException
import org.gradle.cli.CommandLineParser
import org.gradle.cli.SystemPropertiesCommandLineConverter

import com.github.thahnen.util.addSystemProperties
import com.github.thahnen.util.antUserHome
import com.github.thahnen.util.projectDir
import com.github.thahnen.util.SystemPropertiesHandler
import com.github.thahnen.util.wrapperProperties
import com.github.thahnen.wrapper.Executor


private const val ANT_USER_HOME_OPTION = "a"
private const val ANT_USER_HOME_DETAILED_OPTION = "ant-user-home"


/** Main method to invoke Ant wrapper */
@Throws(CommandLineArgumentException::class, RuntimeException::class)
fun main(args: Array<String>) {
    val wrapperJar = wrapperJar()
    val rootDir = projectDir(wrapperJar)

    val parser = CommandLineParser()
    parser.allowUnknownOptions()
    parser.option(ANT_USER_HOME_OPTION, ANT_USER_HOME_DETAILED_OPTION).hasArgument()

    val converter = SystemPropertiesCommandLineConverter()
    converter.configure(parser)

    val options = parser.parse(*args)

    val properties = System.getProperties()
    properties.putAll(converter.convert(options, HashMap<String, String>()))

    val antUserHome = antUserHome(options, ANT_USER_HOME_OPTION)

    addSystemProperties(properties, antUserHome, rootDir)

    Executor.forWrapperPropertiesFile(wrapperProperties(wrapperJar)).execute(
        args,
        Install(
            Download(SystemPropertiesHandler.convertSystemProperties(System.getProperties())),
            PathAssembler(antUserHome, rootDir)
        )
    )
}


/**
 *  Get Ant wrapper JAR file
 *
 *  @return file object of wrapper
 *  @throws RuntimeException when location can not be resolved
 */
@Throws(RuntimeException::class)
internal fun wrapperJar() : File {
    val location: URI?

    try {
        location = Install::class.java.protectionDomain.codeSource.location.toURI()
    } catch (err: URISyntaxException) {
        throw RuntimeException(err)
    }

    when {
        "file" != location.scheme -> throw RuntimeException(
            "[AntWrapperMain -> wrapperJar] Cannot determine classpath for wrapper Jar from codebase $location"
        )
    }

    return try {
        Paths.get(location!!).toFile()
    } catch (err: NoClassDefFoundError) {
        File(location.path)
    }
}
