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
import java.util.*
import kotlin.collections.HashMap


/**
 *  ...
 */
internal class SystemPropertiesHandler {
    companion object {
        private const val SYSTEM_PROP_PREFIX = "systemProp."


        /**
         *  Resolves the system properties
         *
         *  @param propertiesFile properties file
         *  @return object containing all system properties
         *  @throws RuntimeException when properties file could not be loaded
         */
        @Throws(RuntimeException::class)
        internal fun getSystemProperties(propertiesFile: File) : Map<String, String> {
            val propertyMap = HashMap<String, String>()

            if (propertiesFile.isFile) {
                val properties = Properties()
                try {
                    FileInputStream(propertiesFile).use { properties.load(it) }
                } catch (err: IOException) {
                    throw RuntimeException("Error when loading properties file=$propertiesFile", err)
                }

                properties.keys.forEach {
                    if (it.toString().startsWith(SYSTEM_PROP_PREFIX)) {
                        val key = it.toString().substring(SYSTEM_PROP_PREFIX.length)
                        when {
                            key.isNotEmpty() -> propertyMap[key] = properties[it].toString()
                        }
                    }
                }
            }

            return propertyMap
        }
    }
}
