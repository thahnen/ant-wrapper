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
import java.net.*
import java.util.*
import kotlin.collections.HashMap


/**
 *  ...
 */
internal class Download constructor(private val logger: Logger, private val appName: String,
                                    private val appVersion: String, private val systemProperties: Map<String, String>) {
    /** Custom authenticator */
    private class ProxyAuthenticator : Authenticator() {
        /** Override getPasswordAuthentication method */
        override fun getPasswordAuthentication(): PasswordAuthentication {
            if (requestorType == RequestorType.PROXY) {
                val protocol = requestingURL.protocol
                System.getProperty("$protocol.proxyUser")?.let {
                    PasswordAuthentication(
                        it, (System.getProperty("$protocol.proxyPassword") ?: "").toCharArray()
                    )
                }
            }

            return super.getPasswordAuthentication()
        }
    }


    /** Initialization */
    init {
        systemProperties["http.proxyUser"]?.let {
            systemProperties["https.proxyUser"]?.let {
                Authenticator.setDefault(ProxyAuthenticator())
            }
        }
    }


    companion object {
        const val UNKNOWN_VERSION = "0"
        const val BUFFER_SIZE = 10 * 1024
        const val CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000
        const val READ_TIMEOUT_MILLISECONDS = 10 * 1000


        /**
         *  ...
         */
        private fun convertSystemProperties(properties: Properties) : Map<String, String?> {
            val result = HashMap<String, String?>()
            properties.keys.forEach {
                result[it.toString()] = properties[it.toString()]?.toString()
            }

            return result
        }


        /**
         *  ...
         */
        @Throws(RuntimeException::class)
        internal fun safeUri(uri: URI) : URI {
            try {
                return URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, uri.fragment)
            } catch (err: URISyntaxException) {
                throw RuntimeException("Failed to parse URI", err)
            }
        }


        @Throws(RuntimeException::class)
        internal fun base64Encode(userInfo: String) : String {
            with (Download::class.java.classLoader) {
                try {
                    val encoder = this.loadClass("java.util.Base64").getMethod("getEncoder").invoke(null)
                    return this.loadClass(
                        "java.util.Base64\$Encoder"
                    ).getMethod(
                        "encodeToString", ByteArray::class.java
                    ).invoke(
                        encoder, arrayOf(userInfo.toByteArray())
                    ) as String
                } catch (java70rOrEarlier: Exception) {
                    return this.loadClass(
                        "javax.xml.bind.DatatypeConverter"
                    ).getMethod(
                        "printBase64Binary", ByteArray::class.java
                    ).invoke(
                        null, arrayOf(userInfo.toByteArray())
                    ) as String
                } catch (java50rEarlier: Exception) {
                    throw RuntimeException(
                        "Downloading Ant distributions with HTTP Basic Authentication is not supported on your JVM",
                        java50rEarlier
                    )
                }
            }
        }
    }


    /**
     *  Resolve user information
     *
     *  @param uri necessary for user information
     *  @return username - password combination
     */
    private fun calculateUserInfo(uri: URI) : String? {
        systemProperties["ant.wrapperUser"]?.let { user ->
            systemProperties["ant.wrapperPassword"]?.let { password ->
                return "$user:$password"
            }
        }

        return uri.userInfo
    }


    /**
     *  Resolve user agent
     *
     *  @return string passed as user agent
     */
    private fun calculateUserAgent() : String {
        with (systemProperties) {
            return "$appName/$appVersion (${this["os.name"]};${this["os.version"]};${this["os.arch"]}) " +
                    "(${this["java.vendor"]};${this["java.version"]};${this["java.vm.version"]})"
        }
    }


    /**
     *  Set basic authentication for connection
     *
     *  @param address
     *  @param connection
     *  @return
     */
    private fun addBasicAuthentication(address: URI, connection: URLConnection) {
        val userInfo = calculateUserInfo(address)
        userInfo ?: return

        when {
            "https" != address.scheme -> logger.log(
                "WARNING - Using HTTP Basic Authentication over an insecure connection to download the Ant " +
                "distribution. Please consider using HTTPS."
            )
        }

        connection.setRequestProperty("Authorization", "Basic ${base64Encode(userInfo)}")
    }


    @Throws(IOException::class)
    private fun downloadInternal(address: URI, destination: File) {
        var out: OutputStream? = null
        var inp: InputStream? = null
        val safeUrl = safeUri(address).toURL()

        try {
            out = BufferedOutputStream(FileOutputStream(destination))

            val conn = safeUrl.openConnection()

            addBasicAuthentication(address, conn)

            conn.setRequestProperty("User-Agent", calculateUserAgent())
            conn.connectTimeout = CONNECTION_TIMEOUT_MILLISECONDS
            conn.readTimeout = READ_TIMEOUT_MILLISECONDS
            inp = conn.getInputStream()

            val buffer = ByteArray(BUFFER_SIZE)

            var downloadedLength: Long = 0

            do {
                val numRead = inp.read(buffer)
                when {
                    numRead < 0                             -> break
                    Thread.currentThread().isInterrupted    -> throw IOException("Download was interrupted!")
                }

                downloadedLength += numRead

                out.write(buffer, 0, numRead)
            } while (true)
        } catch (err: SocketTimeoutException) {
            throw IOException("Downloading from $safeUrl failed: timeout", err)
        } finally {
            inp?.let { inp.close() }
            out?.let { out.close() }
        }
    }


    fun download(address: URI, destination: File) {
        destination.parentFile.mkdirs()
        downloadInternal(address, destination)
    }
}
