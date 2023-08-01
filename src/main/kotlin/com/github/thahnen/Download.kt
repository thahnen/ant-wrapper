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

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLConnection
import java.util.Base64

import com.github.thahnen.util.multipleLet


/**
 *  Utilities to download Ant installation
 *
 *  @author Tobias Hahnen
 */
internal class Download(private val systemProperties: Map<String, String>) {
    /** Custom authenticator */
    private class ProxyAuthenticator : Authenticator() {
        /** Override getPasswordAuthentication method */
        override fun getPasswordAuthentication(): PasswordAuthentication {
            if (requestorType == RequestorType.PROXY) {
                val protocol = requestingURL.protocol
                System.getProperty("$protocol.proxyUser")?.let {
                    PasswordAuthentication(it, (System.getProperty("$protocol.proxyPassword") ?: "").toCharArray())
                }
            }

            return super.getPasswordAuthentication()
        }
    }


    /** Companion object */
    companion object {
        // Necessary constants used directly when downloading
        const val BUFFER_SIZE = 10 * 1024
        const val CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000
        const val READ_TIMEOUT_MILLISECONDS = 10 * 1000

        const val PROP_PROXY_USER_HTTP = "http.proxyUser"
        const val PROP_PROXY_USER_HTTPS = "https.proxyUser"
        const val PROP_ANT_WRAPPER_USER = "ant.wrapperUser"
        const val PROP_ANT_WRAPPER_PASSWORD = "ant.wrapperPassword"


        /**
         *  Creates a safe URI from one provided
         *
         *  @param uri possible unsafe URI
         *  @return safe URI
         *  @throws RuntimeException when provided URI could not be parsed
         */
        @Throws(RuntimeException::class)
        internal fun safeUri(uri: URI) : URI {
            try {
                return URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, uri.fragment)
            } catch (err: URISyntaxException) {
                throw RuntimeException("[${Download::class.simpleName} -> safeUri] Failed to parse URI", err)
            }
        }
    }


    /** Initialization */
    init {
        multipleLet(systemProperties[PROP_PROXY_USER_HTTP], systemProperties[PROP_PROXY_USER_HTTPS]) {
            Authenticator.setDefault(ProxyAuthenticator())
        }
    }


    /**
     *  Resolve user information
     *
     *  @param uri necessary for user information
     *  @return username - password combination
     */
    private fun calculateUserInfo(uri: URI) : String? {
        multipleLet(systemProperties[PROP_ANT_WRAPPER_USER], systemProperties[PROP_ANT_WRAPPER_PASSWORD]) { (u, p) ->
            return "$u:$p"
        }

        return uri.userInfo
    }


    /**
     *  Resolve user agent information
     *
     *  @return string passed as user agent
     */
    private fun calculateUserAgent() : String {
        with (systemProperties) {
            return "antw (${this["os.name"]};${this["os.version"]};${this["os.arch"]}) " +
                    "(${this["java.vendor"]};${this["java.version"]};${this["java.vm.version"]})"
        }
    }


    /**
     *  Set basic authentication for connection
     *
     *  @param address web address
     *  @param connection current connection
     */
    private fun addBasicAuthentication(address: URI, connection: URLConnection) {
        val userInfo = calculateUserInfo(address)
        userInfo ?: return

        when {
            "https" != address.scheme -> println(
                "[${this::class.simpleName}.addBasicAuthentication - WARNING] - Using HTTP Basic Authentication over " +
                "an insecure connection to download the Ant distribution. Please consider using HTTPS."
            )
        }

        connection.setRequestProperty(
            "Authorization", "Basic ${Base64.getEncoder().encodeToString(userInfo.toByteArray())}"
        )
    }


    /**
     *  Download a file provided to given destination
     *
     *  @param address to download file from
     *  @param destination output file
     *  @throws IOException when download / file operations fail
     */
    @Throws(IOException::class)
    fun download(address: URI, destination: File) {
        destination.parentFile.mkdirs()

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

            do {
                val numRead = inp.read(buffer)
                when {
                    numRead < 0                             -> break
                    Thread.currentThread().isInterrupted    -> throw IOException(
                        "[${this::class.simpleName}.download] Download was interrupted!"
                    )
                }

                out.write(buffer, 0, numRead)
            } while (true)
        } catch (err: SocketTimeoutException) {
            throw IOException("[${this::class.simpleName}.download] Downloading from $safeUrl failed: timeout", err)
        } finally {
            inp?.let { inp.close() }
            out?.let { out.close() }
        }
    }
}
