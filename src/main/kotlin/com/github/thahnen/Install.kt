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
import java.util.concurrent.Callable

import com.github.thahnen.extension.*
import com.github.thahnen.logging.Logger
import com.github.thahnen.util.*
import com.github.thahnen.wrapper.Configuration


/**
 *  ...
 */
internal class Install(private val logger: Logger, val download: Download, val pathAssembler: PathAssembler) {
    /** Local installation path checker */
    internal class InstallCheck(var antHome: File?, var failureMessage: String?) {
        companion object {
            /** Failed installation */
            internal fun failure(message: String) : InstallCheck = InstallCheck(null, message)

            /** Successfully installation */
            internal fun success(antHome: File) : InstallCheck = InstallCheck(antHome, null)
        }

        /** Check if Ant home was provided */
        fun isVerified() : Boolean = antHome != null
    }


    /** Companion object */
    companion object {
        internal const val DEFAULT_DISTRIBUTION_PATH = "wrapper/dists"
    }


    /** local variables */
    private val exclusiveFileAccessManager = ExclusiveFileAccessManager(120000, 200)


    /** Sets execution permissions of ant command if not Windows */
    private fun setExecutionPermissions(antHome: File) {
        when { isWindows() -> return }

        val antCommand = File(antHome, "bin/ant")
        var errorMessage: String? = null

        try {
            val pb = ProcessBuilder("chmod", "755", antCommand.canonicalPath)
            val p = pb.start()
            if (p.waitFor() != 0) {
                val isr = BufferedReader(InputStreamReader(p.inputStream))
                val stdout = Formatter()

                do {
                    isr.readLine()?.let {
                        stdout.format("%s%n", it)
                    } ?: break
                } while (true)

                errorMessage = stdout.toString()
            }
        } catch (err: IOException) {
            errorMessage = err.message
        } catch (err: InterruptedException) {
            Thread.currentThread().interrupt()
            errorMessage = err.message
        }

        errorMessage?.let {
            logger.log(
                "[${this::class.simpleName}.setExecutionPolicy] Could not set executable permissions for: " +
                antCommand.absolutePath
            )
        }
    }


    /**
     *  Check if distribution is correct
     *
     *  @param distDir distribution root directory
     *  @param distributionDescription corresponding distribution description
     *  @return specific installation check
     */
    private fun verifyDistributionRoot(distDir: File, distributionDescription: String) : InstallCheck {
        val dirs = distDir.listDirs()
        when {
            dirs.isEmpty() -> return InstallCheck.failure(
                "[${this::class.simpleName}.verifyDistributionRoot] Ant distribution $distributionDescription does " +
                "not contain any directories. Expected to find exactly 1 directory!"
            )
            dirs.size != 1 -> return InstallCheck.failure(
                "[${this::class.simpleName}.verifyDistributionRoot] Ant distribution $distributionDescription " +
                "contains too many directories. Expected to find exactly 1 directory!"
            )
        }

        val antHome = dirs[0]
        findLauncherJar(antHome) ?: run {
            return InstallCheck.failure(
                "[${this::class.simpleName}.verifyDistributionRoot] Ant distribution $distributionDescription does " +
                "not appear to contain an Ant distribution!"
            )
        }

        return InstallCheck.success(antHome)
    }


    /**
     *  Verify that downloaded file has the correct checksum
     *
     *  @param sourceURL Ant installation download URL
     *  @param localZipFile file object of downloaded ZIP archive
     *  @param expectedSum optional expected checksum
     */
    @Throws(RuntimeException::class)
    private fun verifyDownloadCheckSum(sourceURL: String, localZipFile: File, expectedSum: String?) {
        expectedSum?.let {
            with (localZipFile.calculateSha256Sum()) {
                if (expectedSum != this) {
                    localZipFile.delete()
                    throw RuntimeException(
                        "[${this::class.simpleName}.verifyDownloadCheckSum] Verification of Ant distribution " +
                        "failed\n\nYour Ant distribution may have been tampered with.\nConfirm that the " +
                        "'distributionSha256Sum' property in your ant-wrapper.properties file is correct and you are " +
                        "downloading the wrapper from a trusted source.\n\n Distribution URL: $sourceURL\nDownload " +
                        "Location: ${localZipFile.absolutePath}\nExpected checksum: $expectedSum\n  Actual " +
                        "checksum: $this\n"
                    )
                }
            }
        }
    }


    /**
     *  ...
     */
    fun createDist(configuration: Configuration) : File {
        val distributionURL = configuration.distribution
        val distributionSha256Sum = configuration.distributionSha256Sum
        val localDistribution = pathAssembler.getDistribution(configuration)
        val distDir = localDistribution.distDir
        val localZipFile = localDistribution.distZip

        return exclusiveFileAccessManager.access(localZipFile, Callable {
            val markerFile = File(localZipFile.parentFile, "${localZipFile.name}.ok")
            if (distDir.isDirectory && markerFile.isFile) {
                val installCheck = verifyDistributionRoot(distDir, distDir.absolutePath)
                when {
                    installCheck.isVerified() -> return@Callable installCheck.antHome!!
                }
                System.err.println(installCheck.failureMessage)
                markerFile.delete()
            }

            val needsDownload = !localZipFile.isFile
            val safeDistributionURL = Download.safeUri(distributionURL)

            if (needsDownload) {
                val tmpZipFile = File(localZipFile.parentFile, "${localZipFile.name}.part")
                tmpZipFile.delete()

                logger.log("[${this::class.simpleName}.createDist] Downloading $safeDistributionURL")

                download.download(distributionURL, tmpZipFile)
                tmpZipFile.renameTo(localZipFile)
            }

            distDir.listDirs().forEach {
                logger.log("[${this::class.simpleName}.createDist] Deleting directory ${it.absolutePath}")

                it.deleteDir()
            }

            verifyDownloadCheckSum(configuration.distribution.toString(), localZipFile, distributionSha256Sum)

            try {
                localZipFile.unzip(distDir)
            } catch (err: IOException) {
                logger.log(
                    "[${this::class.simpleName}.createDist] Could not unzip ${localZipFile.absolutePath} to " +
                    "${distDir.absolutePath}. Reason: ${err.message}"
                )

                throw err
            }

            val installCheck = verifyDistributionRoot(distDir, safeDistributionURL.toString())
            if (installCheck.isVerified()) {
                setExecutionPermissions(installCheck.antHome!!)
                markerFile.createNewFile()
                return@Callable installCheck.antHome!!
            }

            throw RuntimeException(installCheck.failureMessage)
        })
    }
}
