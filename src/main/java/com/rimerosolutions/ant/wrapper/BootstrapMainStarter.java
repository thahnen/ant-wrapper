/*
 * Copyright 2010 the original author or authors.
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
package com.rimerosolutions.ant.wrapper;

import java.io.File;
import java.net.*;
import java.util.*;


/**
 * @author Hans Dockter
 * @author Yves Zoundi
 */
public class BootstrapMainStarter {
        private static final String JAVA_HOME_VARIABLE_NAME = "JAVA_HOME";
        private static final String LIB_FOLDER_NAME = "lib";
        private static final String JAR_FILE_EXTENSION = ".jar";
        private static final String MAIN_METHOD_NAME = "main";
        private static final String ANT_MAIN_CLASSNAME = "org.apache.tools.ant.Main";
        private static final String TOOLS_JAR_PATH_GENERAL = "lib/tools.jar";
        private static final String TOOLS_JAR_PATH_OSX = "lib/classes.jar";


        public void start(String[] args, File antHome) throws Exception {
                File[] antJars = findBootstrapJars(antHome);

                URL[] jarUrls = new URL[antJars.length];
                
                for (int i = 0; i < antJars.length; i++) {
                        jarUrls[i] = antJars[i].toURI().toURL();
                }

                URLClassLoader contextClassLoader = new URLClassLoader(jarUrls, ClassLoader.getSystemClassLoader().getParent());
                Thread.currentThread().setContextClassLoader(contextClassLoader);

                Class<?> mainClass = contextClassLoader.loadClass(ANT_MAIN_CLASSNAME);
                mainClass.getMethod(MAIN_METHOD_NAME, String[].class).invoke(null, new Object[] { args });
        }


        private File[] findBootstrapJars(File antHome) {
                List<File> bootstrapJars = new ArrayList<File>();

                for (File file : new File(antHome, LIB_FOLDER_NAME).listFiles()) {
                        if (file.getName().endsWith(JAR_FILE_EXTENSION)) {
                                bootstrapJars.add(file);
                        }
                }

                String javaHome = System.getenv(JAVA_HOME_VARIABLE_NAME);
                
                if (javaHome != null) {
                        File toolsJar = new File(javaHome, TOOLS_JAR_PATH_GENERAL);
                        File toolsJarOsx = new File(javaHome, TOOLS_JAR_PATH_OSX);
                        if (toolsJar.exists()) {
                                bootstrapJars.add(toolsJar);
                        }

                        if (toolsJarOsx.exists()) {
                                bootstrapJars.add(toolsJarOsx);
                        }
                }

                return bootstrapJars.toArray(new File[bootstrapJars.size()]);
        }
}
