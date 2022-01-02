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

import java.net.URI

import com.github.thahnen.*


/**
 *  Ant Wrapper configuration
 *
 *  @author Tobias Hahnen
 */
internal data class Configuration(var distribution: URI,
                                  var distributionBase: String = PathAssembler.ANT_USER_HOME_STRING,
                                  var distributionPath: String = Install.DEFAULT_DISTRIBUTION_PATH,
                                  var distributionSha256Sum: String?,
                                  var zipBase: String = PathAssembler.ANT_USER_HOME_STRING,
                                  var zipPath: String = Install.DEFAULT_DISTRIBUTION_PATH
)
