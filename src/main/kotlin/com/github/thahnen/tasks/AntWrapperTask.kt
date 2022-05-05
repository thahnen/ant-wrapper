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
package com.github.thahnen.tasks

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.URISyntaxException
import java.util.Properties

import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Task

import com.github.thahnen.extension.fromInputStream
import com.github.thahnen.extension.getFileName


/**
 *  Ant task to generate wrapper resources
 */
open class AntWrapperTask : Task() {
    /** Task attributes */
    private var antVersion: String? = null
    private var baseDistributionUrl: String? = null


    /** Necessary setters for task attributes */
    @Suppress("UNUSED") fun setAntVersion(nAntVersion: String) { this.antVersion = nAntVersion }
    @Suppress("UNUSED") fun setBaseDistributionUrl(nBaseDistributionUrl: String) {
        this.baseDistributionUrl = nBaseDistributionUrl
    }


    /** Override getDescription method */
    override fun getDescription(): String = "Generate Ant wrapper"


    /** Override execute method */
    @Throws(BuildException::class)
    override fun execute() {
        // store scripts in file system
        val classLoader = this::class.java.classLoader
        listOf("scripts/antw", "scripts/antw.bat").forEach {
            val scriptStream = classLoader.getResourceAsStream(it)
            val scriptFile = File(getProject().baseDir, it.getFileName())
            try {
                scriptFile.fromInputStream(scriptStream!!)
            } catch (err: Exception) {
                throw BuildException(
                    "[${this::class.simpleName}.execute] Writing script '$it' to actual file '$scriptFile' failed " +
                    "due to an exception", err
                )
            }

            when {
                !scriptFile.setExecutable(true) -> log(
                    "[${this::class.simpleName}.execute] Setting executable flag on script file '$scriptFile' " +
                    "failed!"
                )
            }
        }

        // store wrapper jar in file system
        val wrapperDir = File(getProject().baseDir, "ant/wrapper")
        when {
            !wrapperDir.exists() -> wrapperDir.mkdirs()
        }

        try {
            File(wrapperDir, "ant-wrapper.jar").fromInputStream(getWrapperJarAsStream())
        } catch (err: IOException) {
            throw BuildException(
                "[${this::class.simpleName}.execute] Writing wrapper Jar " +
                "'${File(wrapperDir, "ant-wrapper.jar")}' failed due to an exception", err
            )
        }

        // store wrapper properties in file system
        try {
            val properties = Properties()
            properties[
                "distributionUrl"
            ] = baseDistributionUrl
                ?: "http://archive.apache.org/dist/ant/binaries/apache-ant-${antVersion ?: getAntVersion()}-bin.zip"
            properties.store(
                FileOutputStream(File(wrapperDir, "ant-wrapper.properties")), "Ant wrapper properties"
            )
        } catch (err: Exception) {
            throw BuildException(
                "[${this::class.simpleName}.execute] Writing wrapper properties " +
                "'${File(wrapperDir, "ant-wrapper.properties")}' failed due to an exception", err
            )
        }
    }


    /**
     *  Tries to retrieve this current wrapper Jar as stream
     *
     *  @return stream of this wrapper Jar
     *  @throws BuildException when getting Jar as stream failed
     *  @throws FileNotFoundException when code source location is not a valid file
     *  @throws URISyntaxException when code source location is not a correct URI
     */
    @Throws(BuildException::class, FileNotFoundException::class, URISyntaxException::class)
    private fun getWrapperJarAsStream() : FileInputStream {
        val location = this::class.java.protectionDomain.codeSource.location.toURI()

        try {
            when {
                location.scheme != "file" -> throw BuildException(
                    "[${this::class.simpleName}.getWrapperJarAsStream] Cannot determine classpath for wrapper Jar " +
                    "from location $location"
                )
            }

            return FileInputStream(File(location))
        } catch (err: FileNotFoundException) {
            throw BuildException(
                "[${this::class.simpleName}.getWrapperJarAsStream] Loading Jar (at location '$location') as stream " +
                "failed due tue an exception", err
            )
        }
    }


    /**
     *  Tries to retrieve the current Ant version
     *
     *  @return Ant version
     *  @throws BuildException when an IO error occurred loading the file holding the Ant version
     */
    @Throws(BuildException::class)
    private fun getAntVersion() : String {
        this::class.java.getResourceAsStream("/org/apache/tools/ant/version.txt").use {
            val properties = Properties()
            try {
                properties.load(it)
                return properties["VERSION"] as String
            } catch (err: IOException) {
                throw BuildException(
                    "[${this::class.simpleName}.getAntVersion] Retrieving Ant version from properties located at " +
                    "classpath failed due to an exception", err
                )
            }
        }
    }
}
