package fr.lactea.presets

import fr.lactea.settings.Settings
import java.io.File

/**
 * Preset order with history (so "previous" really goes back), shuffle or sequential,
 * and filtering: blacklist, favourites-only, slow presets. Thread-safe.
 */
class Playlist(private val settings: Settings, private val stats: PresetStats) {

    data class Entry(val key: String, val file: File) {
        val category: String get() = key.substringBefore('/')
        val name: String get() = key.substringAfterLast('/')
    }

    private var all: List<Entry> = emptyList()
    private val history = ArrayList<Entry>()
    private var position = -1

    @Synchronized fun setLibrary(entries: List<Entry>) {
        all = entries
        val keys = entries.mapTo(HashSet()) { it.key }
        history.retainAll { it.key in keys }
        position = position.coerceAtMost(history.lastIndex)
    }

    @Synchronized fun all(): List<Entry> = all

    @Synchronized fun current(): Entry? = history.getOrNull(position)

    @Synchronized fun next(): Entry? {
        if (position < history.lastIndex) return history[++position]
        val pick = pickNext() ?: return null
        history.add(pick)
        if (history.size > MAX_HISTORY) history.removeAt(0)
        position = history.lastIndex
        return pick
    }

    @Synchronized fun previous(): Entry? {
        if (position > 0) position--
        return current()
    }

    @Synchronized fun jumpTo(entry: Entry): Entry {
        while (history.lastIndex > position) history.removeAt(history.lastIndex)
        history.add(entry)
        position = history.lastIndex
        return entry
    }

    /** Presets that failed to load on this device (e.g. shader not supported by the GPU). */
    private val failed = HashSet<String>()

    @Synchronized fun markFailed(key: String) {
        failed.add(key)
    }

    @Synchronized fun eligible(): List<Entry> {
        val blacklist = settings.blacklist + failed
        val favorites = settings.favorites
        val cap = settings.fpsCap
        val base = all.filter { it.key !in blacklist }
        var list = base
        if (settings.favoritesOnly) list = list.filter { it.key in favorites }
        if (settings.excludeSlow) list = list.filter { !stats.isSlow(it.key, cap) }
        return list.ifEmpty { base.ifEmpty { all } }
    }

    private fun pickNext(): Entry? {
        val pool = eligible()
        if (pool.isEmpty()) return null
        val cur = current()
        if (!settings.shuffle) {
            val idx = cur?.let { c -> pool.indexOfFirst { it.key == c.key } } ?: -1
            return pool[(idx + 1) % pool.size]
        }
        val recent = history.takeLast(minOf(RECENT_AVOID, pool.size - 1)).mapTo(HashSet()) { it.key }
        return pool.filter { it.key !in recent }.ifEmpty { pool }.random()
    }

    companion object {
        private const val MAX_HISTORY = 200
        private const val RECENT_AVOID = 20
    }
}
