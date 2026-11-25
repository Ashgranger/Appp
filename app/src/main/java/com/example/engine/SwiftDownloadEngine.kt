package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import com.example.data.DownloadDao
import com.example.data.DownloadEntity
import com.example.data.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ActiveDownloadTelemetry(
    val speedBytesPerSec: Long = 0L
)

/**
 * Robust, zero-permission OkHttp streaming download engine with true HTTP Range
 * Pause / Resume / Cancel / Retry support and live byte-level progress & speed telemetry.
 */
class SwiftDownloadEngine(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val pausedByUser = ConcurrentHashMap.newKeySet<Long>()
    private val cancelledByUser = ConcurrentHashMap.newKeySet<Long>()

    private val _liveSpeedById = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val liveSpeedById: StateFlow<Map<Long, Long>> = _liveSpeedById.asStateFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private fun downloadsDir(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun allocateUniqueFile(desiredName: String): File {
        val safeName = desiredName.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").ifBlank { "download.bin" }
        val dir = downloadsDir()
        var candidate = File(dir, safeName)
        if (!candidate.exists()) return candidate

        val dotIdx = safeName.lastIndexOf('.')
        val base = if (dotIdx > 0) safeName.substring(0, dotIdx) else safeName
        val ext = if (dotIdx > 0) safeName.substring(dotIdx) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($counter)$ext")
            counter++
        }
        return candidate
    }

    fun startDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        onStarted: ((String) -> Unit)? = null
    ) {
        scope.launch {
            val cleanUrl = url.trim()
            if (cleanUrl.isBlank()) return@launch
            val guessedName = URLUtil.guessFileName(cleanUrl, contentDisposition, mimeType)
            val targetFile = allocateUniqueFile(guessedName)
            val entity = DownloadEntity(
                fileName = targetFile.name,
                url = cleanUrl,
                mimeType = mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream",
                status = DownloadStatus.DOWNLOADING.name,
                bytesDownloaded = 0L,
                totalBytes = -1L,
                localPath = targetFile.absolutePath,
                timestamp = System.currentTimeMillis()
            )
            val id = downloadDao.insert(entity)
            onStarted?.invoke(targetFile.name)
            launchTransfer(id = id, url = cleanUrl, localPath = targetFile.absolutePath, userAgent = userAgent)
        }
    }

    fun pauseDownload(id: Long) {
        pausedByUser.add(id)
        activeJobs.remove(id)?.cancel()
        clearSpeed(id)
        scope.launch {
            val current = downloadDao.getById(id) ?: return@launch
            if (current.downloadStatus == DownloadStatus.DOWNLOADING) {
                downloadDao.updateStatus(id, DownloadStatus.PAUSED.name)
            }
        }
    }

    fun resumeDownload(id: Long) {
        pausedByUser.remove(id)
        cancelledByUser.remove(id)
        scope.launch {
            val current = downloadDao.getById(id) ?: return@launch
            if (current.downloadStatus == DownloadStatus.COMPLETED) return@launch
            downloadDao.updateStatus(id, DownloadStatus.DOWNLOADING.name, "")
            launchTransfer(
                id = current.id,
                url = current.url,
                localPath = current.localPath,
                userAgent = null
            )
        }
    }

    fun cancelDownload(id: Long) {
        cancelledByUser.add(id)
        pausedByUser.remove(id)
        activeJobs.remove(id)?.cancel()
        clearSpeed(id)
        scope.launch {
            val current = downloadDao.getById(id) ?: return@launch
            runCatching { File(current.localPath).delete() }
            downloadDao.updateProgress(
                id = id,
                bytesDownloaded = 0L,
                totalBytes = current.totalBytes,
                status = DownloadStatus.CANCELLED.name,
                errorMessage = "Cancelled"
            )
        }
    }

    fun deleteDownload(id: Long) {
        cancelledByUser.add(id)
        activeJobs.remove(id)?.cancel()
        clearSpeed(id)
        scope.launch {
            val current = downloadDao.getById(id)
            if (current != null && current.localPath.isNotBlank()) {
                runCatching { File(current.localPath).delete() }
            }
            downloadDao.deleteById(id)
        }
    }

    fun clearFinishedDownloads() {
        scope.launch {
            downloadDao.clearAll()
        }
    }

    private fun launchTransfer(id: Long, url: String, localPath: String, userAgent: String?) {
        activeJobs[id]?.cancel()
        val job = scope.launch {
            val file = File(localPath)
            val existingBytes = if (file.exists()) file.length() else 0L

            try {
                val cookies = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent ?: WebViewSessionManager.MOBILE_USER_AGENT)
                if (!cookies.isNullOrBlank()) {
                    reqBuilder.header("Cookie", cookies)
                }
                if (existingBytes > 0L) {
                    reqBuilder.header("Range", "bytes=$existingBytes-")
                }

                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        downloadDao.updateStatus(id, DownloadStatus.FAILED.name, "HTTP ${response.code}")
                        clearSpeed(id)
                        return@launch
                    }

                    val body = response.body
                    if (body == null) {
                        downloadDao.updateStatus(id, DownloadStatus.FAILED.name, "Empty response")
                        clearSpeed(id)
                        return@launch
                    }

                    val isResumedPartial = response.code == 206 && existingBytes > 0L
                    val startOffset = if (isResumedPartial) existingBytes else 0L
                    val contentLength = body.contentLength()
                    val totalBytes = if (contentLength > 0L) startOffset + contentLength else -1L

                    var downloaded = startOffset
                    downloadDao.updateProgress(id, downloaded, totalBytes, DownloadStatus.DOWNLOADING.name)

                    val buffer = ByteArray(16 * 1024)
                    var lastEmitTime = System.currentTimeMillis()
                    var bytesSinceLastEmit = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(file, isResumedPartial).use { output ->
                            while (isActive) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                bytesSinceLastEmit += read

                                val now = System.currentTimeMillis()
                                val elapsedMs = now - lastEmitTime
                                if (elapsedMs >= 250L) {
                                    val speed = (bytesSinceLastEmit * 1000L) / elapsedMs.coerceAtLeast(1L)
                                    updateSpeed(id, speed)
                                    downloadDao.updateProgress(
                                        id = id,
                                        bytesDownloaded = downloaded,
                                        totalBytes = totalBytes,
                                        status = DownloadStatus.DOWNLOADING.name
                                    )
                                    lastEmitTime = now
                                    bytesSinceLastEmit = 0L
                                }
                            }
                            output.flush()
                        }
                    }

                    if (!isActive) {
                        val nextStatus = when {
                            cancelledByUser.remove(id) -> DownloadStatus.CANCELLED.name
                            pausedByUser.remove(id) -> DownloadStatus.PAUSED.name
                            else -> DownloadStatus.PAUSED.name
                        }
                        downloadDao.updateProgress(id, downloaded, totalBytes, nextStatus)
                        clearSpeed(id)
                        return@launch
                    }

                    val finalTotal = if (totalBytes > 0L) totalBytes else downloaded
                    downloadDao.updateProgress(
                        id = id,
                        bytesDownloaded = downloaded,
                        totalBytes = finalTotal,
                        status = DownloadStatus.COMPLETED.name
                    )
                    clearSpeed(id)
                }
            } catch (ce: CancellationException) {
                val nextStatus = when {
                    cancelledByUser.remove(id) -> DownloadStatus.CANCELLED.name
                    pausedByUser.remove(id) -> DownloadStatus.PAUSED.name
                    else -> DownloadStatus.PAUSED.name
                }
                val currentLen = if (file.exists()) file.length() else existingBytes
                downloadDao.updateProgress(
                    id = id,
                    bytesDownloaded = if (nextStatus == DownloadStatus.CANCELLED.name) 0L else currentLen,
                    totalBytes = downloadDao.getById(id)?.totalBytes ?: -1L,
                    status = nextStatus
                )
                clearSpeed(id)
            } catch (e: Exception) {
                downloadDao.updateStatus(
                    id = id,
                    status = DownloadStatus.FAILED.name,
                    errorMessage = e.localizedMessage?.take(120) ?: "Network error"
                )
                clearSpeed(id)
            } finally {
                activeJobs.remove(id)
            }
        }
        activeJobs[id] = job
    }

    private fun updateSpeed(id: Long, bytesPerSec: Long) {
        _liveSpeedById.update { it + (id to bytesPerSec) }
    }

    private fun clearSpeed(id: Long) {
        _liveSpeedById.update { it - id }
    }

    fun openDownloadedFile(activityContext: Context, item: DownloadEntity): Boolean {
        val file = File(item.localPath)
        if (!file.exists()) return false
        return runCatching {
            val uri = FileProvider.getUriForFile(
                activityContext,
                "${activityContext.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activityContext.startActivity(Intent.createChooser(intent, "Open ${item.fileName}"))
            true
        }.getOrDefault(false)
    }

    fun shareDownloadedFile(activityContext: Context, item: DownloadEntity): Boolean {
        val file = File(item.localPath)
        if (!file.exists()) return false
        return runCatching {
            val uri = FileProvider.getUriForFile(
                activityContext,
                "${activityContext.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activityContext.startActivity(Intent.createChooser(intent, "Share ${item.fileName}"))
            true
        }.getOrDefault(false)
    }
}
