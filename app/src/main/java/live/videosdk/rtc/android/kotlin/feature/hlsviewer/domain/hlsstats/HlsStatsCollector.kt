package live.videosdk.rtc.android.kotlin.feature.hlsviewer.domain.hlsstats

import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import live.videosdk.rtc.android.kotlin.feature.hlsviewer.domain.hlsstats.internal.ExoPlayerStatsAdapter
import live.videosdk.rtc.android.hlsstats.models.HlsPlaybackStats

/**
 * Main class for collecting HLS playback statistics from an ExoPlayer instance.
 * 
 * Usage:
 * ```kotlin
 * val exoPlayer = ExoPlayer.Builder(context).build()
 * val statsCollector = HlsStatsCollector(exoPlayer)
 * 
 * statsCollector.addListener(object : HlsStatsListener {
 *     override fun onStatsUpdate(stats: HlsPlaybackStats) {
 *         // Handle stats update
 *     }
 *     
 *     override fun onSessionEnded(finalStats: HlsPlaybackStats?) {
 *         // Handle session end
 *     }
 * })
 * ```
 * 
 * **Important**: Users must add ExoPlayer dependencies to their app:
 * ```gradle
 * implementation 'androidx.media3:media3-exoplayer:1.5.0'
 * implementation 'androidx.media3:media3-ui:1.5.0'
 * ```
 */
@UnstableApi
class HlsStatsCollector(
    private val exoPlayer: ExoPlayer,
    private val config: HlsStatsConfig = HlsStatsConfig()
) {
    private val listeners = mutableListOf<HlsStatsListener>()
    private val statsAdapter: ExoPlayerStatsAdapter = ExoPlayerStatsAdapter(exoPlayer, config.keepHistory)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isReleased = false
    
    // Runnable for periodic stats updates on main thread
    private val statsUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isReleased) {
                // This now runs on main thread, safe to access ExoPlayer
                val stats = statsAdapter.getCurrentStats()
                if (stats != null) {
                    notifyListeners(stats)
                }
                // Schedule next update
                mainHandler.postDelayed(this, config.updateIntervalMs)
            }
        }
    }
    
    // Player listener to detect session end
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                notifySessionEnded()
            }
        }
        
        override fun onPlayerError(error: PlaybackException) {
            statsAdapter.trackError(
                code = error.errorCode,
                message = error.message ?: "Unknown error"
            )
        }
    }
    
    init {
        exoPlayer.addListener(playerListener)
        startPeriodicUpdates()
    }
    
    /**
     * Add a listener for stats updates
     * @param listener The listener to add
     */
    fun addListener(listener: HlsStatsListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }
    
    /**
     * Remove a specific listener
     * @param listener The listener to remove
     */
    fun removeListener(listener: HlsStatsListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
    
    /**
     * Remove all listeners
     */
    fun removeAllListeners() {
        synchronized(listeners) {
            listeners.clear()
        }
    }
    
    /**
     * Get current stats on-demand (non-periodic)
     * @return Current playback stats, or null if no active session
     */
    fun getCurrentStats(): HlsPlaybackStats? {
        return statsAdapter.getCurrentStats()
    }
    
    /**
     * Get combined stats for entire lifecycle of this collector
     * @return Combined playback stats across all sessions
     */
    fun getCombinedStats(): HlsPlaybackStats {
        return statsAdapter.getCombinedStats()
    }
    
    /**
     * Release resources - must be called when done with the collector
     * This stops periodic updates and removes listeners from the player
     */
    fun release() {
        if (isReleased) return
        
        isReleased = true
        stopPeriodicUpdates()
        exoPlayer.removeListener(playerListener)
        statsAdapter.release()
        removeAllListeners()
    }
    
    /**
     * Start periodic stats updates based on configured interval
     * Uses Handler with main Looper to ensure ExoPlayer access on main thread
     */
    private fun startPeriodicUpdates() {
        stopPeriodicUpdates() // Stop any existing updates
        
        // Post first update immediately
        mainHandler.post(statsUpdateRunnable)
    }
    
    /**
     * Stop periodic updates
     */
    private fun stopPeriodicUpdates() {
        mainHandler.removeCallbacks(statsUpdateRunnable)
    }
    
    /**
     * Notify all listeners of stats update
     */
    private fun notifyListeners(stats: HlsPlaybackStats) {
        val listenersCopy = synchronized(listeners) {
            listeners.toList()
        }
        
        listenersCopy.forEach { listener ->
            try {
                listener.onStatsUpdate(stats)
            } catch (e: Exception) {
                // Prevent one listener's exception from affecting others
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notify all listeners of session end
     */
    private fun notifySessionEnded() {
        val finalStats = statsAdapter.getCurrentStats()
        
        val listenersCopy = synchronized(listeners) {
            listeners.toList()
        }
        
        listenersCopy.forEach { listener ->
            try {
                listener.onSessionEnded(finalStats)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
