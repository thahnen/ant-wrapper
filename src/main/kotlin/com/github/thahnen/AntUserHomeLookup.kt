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


/**
 *  Get Ant user home for later use
 *
 *  @author Tobias Hahnen
 */
internal class AntUserHomeLookup {
    companion object {
        private val DEFAULT_ANT_USER_HOME               = "${System.getProperty("user.home")}/.ant"
        private const val ANT_USER_HOME_PROPERTY_KEY    = "ant.user.home"
        private const val ANT_USER_HOME_ENV_KEY         = "ANT_USER_HOME"


        /**
         *  Resolve file object of Ant user home in the following order:
         *  - system property
         *  - environment variable
         *  - default value
         *
         *  @return file object of Ant user home
         */
        internal fun antUserHome() : File {
            System.getProperty(ANT_USER_HOME_PROPERTY_KEY)?.let { return File(it) }
            System.getenv(ANT_USER_HOME_ENV_KEY)?.let { return File(it) }
            return File(DEFAULT_ANT_USER_HOME)
        }
    }
}
