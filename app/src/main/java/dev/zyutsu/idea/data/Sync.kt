
package dev.zyutsu.idea.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Device sync over a tiny HTTP vault endpoint.
 *
 * Protocol: PUT/GET <server>/vault/<deviceName> with a JSON body. Any server that stores one JSON
 * document per device works (a local folder served by python, a LAN box, or your own host).
 * Merge is last-write-wins per note, so it is safe offline-first: the app never blocks on network.
 */
class SyncEngine(private val context: Context) {
    private val repo = Repository(context)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val pushed: Int, val pulled: Int) : Result()
        data class Skipped(val why: String) : Result()
        data class Failed(val why: String) : Result()
    }

    suspend fun syncNow(): Result {
        val url = repo.serverUrl()
        if (url.isBlank()) {
            repo.markSyncResult("Сервер не настроен")
            return Result.Skipped("no-server")
        }
        val device = repo.deviceName().ifBlank { "device" }
        val endpoint = url + "/vault/" + java.net.URLEncoder.encode(device, "UTF-8")
        return try {
            val local = repo.snapshot()
            val payload = json.encodeToString(Vault.serializer(), Vault(local.notes, local.checkins, local.tags, System.currentTimeMillis(), device))
            put(endpoint, payload)
            val remoteText = get(endpoint)
            var pulled = 0
            if (!remoteText.isNullOrBlank()) {
                val remote = json.decodeFromString(Vault.serializer(), remoteText)
                pulled = repo.merge(remote)
            }
            val changed = repo.snapshot().notes.size
            repo.markSyncResult("Синхронизировано: $changed записей")
            Result.Success(pushed = changed, pulled = pulled)
        } catch (e: Exception) {
            repo.markSyncResult("Сбой синхронизации: " + (e.message ?: e.javaClass.simpleName))
            Result.Failed(e.message ?: "network")
        }
    }

    private fun put(endpoint: String, body: String) {
        val req = Request.Builder()
            .url(endpoint)
            .put(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 404) throw java.io.IOException("PUT " + resp.code)
        }
    }

    private fun get(endpoint: String): String? {
        val req = Request.Builder().url(endpoint).get().build()
        http.newCall(req).execute().use { resp ->
            if (resp.code == 404) return null
            if (!resp.isSuccessful) throw java.io.IOException("GET " + resp.code)
            return resp.body?.string()
        }
    }

    companion object {
        const val WORK_NAME = "idea-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun runOnce(context: Context) {
            WorkManager.getInstance(context).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<SyncWorker>().build()
            )
        }
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val outcome = runBlocking { SyncEngine(applicationContext).syncNow() }
        return if (outcome is SyncEngine.Result.Failed) Result.retry() else Result.success()
    }
}

