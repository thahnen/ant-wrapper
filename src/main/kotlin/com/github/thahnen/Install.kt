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
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Callable
import java.util.zip.ZipFile


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


    companion object {
        internal const val DEFAULT_DISTRIBUTION_PATH = "wrapper/dists"
    }


    /** local variables */
    private val exclusiveFileAccessManager = ExclusiveFileAccessManager(120000, 200)


    /**
     *  Copy data from input to output stream
     *
     *  @param inp input stream
     *  @param out output stream
     *  @throws IOException when working with streams fails
     */
    @Throws(IOException::class)
    private fun copyInputStream(inp: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)

        do {
            val len = inp.read(buffer)
            when {
                len < 0 -> break
            }

            out.write(buffer, 0, len)
        } while (true)

        inp.close()
        out.close()
    }


    /**
     *  Unzip a specific file to a destination provided
     *
     *  @param zip ZIP archive to unzip
     *  @param dest destination to unzip to
     *  @throws IOException when working with ZIP archive did not work
     */
    @Throws(IOException::class)
    private fun unzip(zip: File, dest: File) {
        val zipFile = ZipFile(zip)
        zipFile.use {
            val entries = it.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) {
                    File(dest, entry.name).mkdirs()
                    continue
                }

                val out = BufferedOutputStream(FileOutputStream(File(dest, entry.name)))
                out.use { s ->
                    copyInputStream(it.getInputStream(entry), s)
                }
            }
        }
    }


    /**
     *  Deletes a directory recursively
     *
     *  @param dir directory to be deleted
     *  @return whether deletion was a success or not
     */
    private fun deleteDir(dir: File) : Boolean {
        if (dir.isDirectory) {
            dir.list()?.forEach {
                when {
                    !deleteDir(File(dir, it)) -> return false
                }
            }
        }

        return dir.delete()
    }


    /** Checks if current system is Windows */
    private fun isWindows() : Boolean = System.getProperty("os.name").toLowerCase(Locale.US).indexOf("windows") > -1


    /** Sets execution permissions of ant command if not Windows */
    private fun setExecutionPermissions(antHome: File) {
        when {
            isWindows() -> return
        }

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
            logger.log("Could not set executable permissions for: ${antCommand.absolutePath}")
        }
    }


    /**
     *  List all directories inside a given directory
     *
     *  @param distDir possible directory
     *  @return all sub directories
     */
    private fun listDirs(distDir: File) : List<File> {
        val dirs = ArrayList<File>()
        if (distDir.exists()) {
            distDir.listFiles()?.forEach {
                when {
                    it.isDirectory -> dirs.add(it)
                }
            }
        }

        return dirs
    }


    /**
     *  Check if distribution is correct
     *
     *  @param distDir distribution root directory
     *  @param distributionDescription corresponding distribution description
     *  @return specific installation check
     */
    private fun verifyDistributionRoot(distDir: File, distributionDescription: String) : InstallCheck {
        val dirs = listDirs(distDir)
        when {
            dirs.isEmpty() -> return InstallCheck.failure(
                "Ant distribution $distributionDescription does not contain any directories. Expected to find " +
                "exactly 1 directory!"
            )
            dirs.size != 1 -> return InstallCheck.failure(
                "Ant distribution $distributionDescription contains too many directories. Expected to find exactly " +
                "1 directory!"
            )
        }

        val antHome = dirs[0]
        BootstrapMainStarter.findLauncherJar(antHome) ?: run {
            return InstallCheck.failure(
                "Ant distribution $distributionDescription does not appear to contain an Ant distribution!"
            )
        }

        return InstallCheck.success(antHome)
    }


    /**
     *  Calculate the SHA-256 hash of a file
     *
     *  @param file to get the hash from
     *  @return hash as string
     */
    private fun calculateSha256Sum(file: File) : String {
        val md = MessageDigest.getInstance("SHA-256")
        val fis = FileInputStream(file)

        fis.use {
            var n = 0
            val buffer = ByteArray(4096)
            while (n != -1) {
                n = it.read(buffer)
                if (n > 0) {
                    md.update(buffer, 0, n)
                }
            }
        }

        val byteData = md.digest()
        val hexString = StringBuffer()
        byteData.forEach {
            val hex = Integer.toHexString(0xff and it.toInt())
            when (hex.length) {
                1 -> hexString.append('0')
            }
            hexString.append(hex)
        }

        return hexString.toString()
    }


    @Throws(RuntimeException::class)
    private fun verifyDownloadCheckSum(sourceURL: String, localZipFile: File, expectedSum: String?) {
        expectedSum?.let {
            val actualSum = calculateSha256Sum(localZipFile)
            if (expectedSum != actualSum) {
                localZipFile.delete()
                throw RuntimeException(
                    "Verification of Ant distribution failed\n\nYour Ant distribution may have been tampered with.\n" +
                    "Confirm that the 'distributionSha256Sum' property in your ant-wrapper.properties file is " +
                    "correct and you are downloading the wrapper from a trusted source.\n\n Distribution URL: " +
                    "$sourceURL\nDownload Location: ${localZipFile.absolutePath}\nExpected checksum: $expectedSum\n" +
                    "  Actual checksum: $actualSum\n"
                )
            }
        }
    }


    /**
     *  ...
     */
    fun createDist(configuration: WrapperConfiguration) : File {
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
                logger.log("Downloading $safeDistributionURL")
                download.download(distributionURL, tmpZipFile)
                tmpZipFile.renameTo(localZipFile)
            }

            listDirs(distDir).forEach {
                logger.log("Deleting directory ${it.absolutePath}")
                deleteDir(it)
            }

            verifyDownloadCheckSum(configuration.distribution.toString(), localZipFile, distributionSha256Sum)

            try {
                unzip(localZipFile, distDir)
            } catch (err: IOException) {
                logger.log("Could not unzip ${localZipFile.absolutePath} to ${distDir.absolutePath}.")
                logger.log("Reason: ${err.message}")
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
