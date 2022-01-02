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
import java.net.URLClassLoader


/**
 *  Class to work with and start the Ant launcher
 *
 *  @author Tobias Hahnen
 */
internal class BootstrapMainStarter {
    companion object {
        /**
         *  Find the specific launcher JAR
         *
         *  @param antHome Ant installation
         *  @return launcher JAR when found, otherwise null
         */
        internal fun findLauncherJar(antHome: File) : File? {
            with (File(antHome, "lib")) {
                if (this.exists() && this.isDirectory) {
                    this.listFiles()?.filter { it.name == "ant-launcher.jar" }?.let {
                        if (it.isNotEmpty()) {
                            return it[0]
                        }
                    }
                }
            }

            return null
        }


        /**
         *  Run Ant launcher with command line arguments provided
         *
         *  @param args list of arguments passed from Ant wrapper to Ant launcher
         *  @param antHome Ant installation directory
         */
        internal fun start(args: Array<String>, antHome: File) {
            val antJar = findLauncherJar(antHome)
            antJar ?: throw RuntimeException("Could not locate the Ant launcher JAR in Ant distribution $antHome!")

            val contextClassLoader = URLClassLoader(
                arrayOf(antJar.toURI().toURL()), ClassLoader.getSystemClassLoader().parent
            )
            Thread.currentThread().contextClassLoader = contextClassLoader

            contextClassLoader.loadClass(
                "org.apache.tools.ant.launch.Launcher"
            ).getMethod(
                "main", Array<String>::class.java
            ).invoke(null, args)

            contextClassLoader.close()
        }
    }
}
