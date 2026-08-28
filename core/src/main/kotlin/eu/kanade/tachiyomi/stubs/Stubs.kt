package eu.kanade.tachiyomi.source.model

import java.io.Serializable

interface SManga : Serializable {
    var url: String
    var title: String
    var artist: String?
    var author: String?
    var description: String?
    var genre: String?
    var status: Int
    var thumbnail_url: String?
    var update_strategy: Int
    var initialized: Boolean

    fun setUrlWithoutDomain(url: String) {
        this.url = if (url.startsWith("http")) {
            try {
                val uri = java.net.URI(url)
                val out = uri.path + if (uri.query != null) "?" + uri.query else ""
                if (out.isEmpty()) "/" else out
            } catch (e: Exception) {
                url
            }
        } else {
            url
        }
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        fun create(): SManga = SMangaImpl()
    }
}

class SMangaImpl : SManga {
    override var url: String = ""
    override var title: String = ""
    override var artist: String? = null
    override var author: String? = null
    override var description: String? = null
    override var genre: String? = null
    override var status: Int = SManga.UNKNOWN
    override var thumbnail_url: String? = null
    override var update_strategy: Int = 0
    override var initialized: Boolean = false
}

interface SChapter : Serializable {
    var url: String
    var name: String
    var date_upload: Long
    var chapter_number: Float
    var scanlator: String?

    fun setUrlWithoutDomain(url: String) {
        this.url = if (url.startsWith("http")) {
            try {
                val uri = java.net.URI(url)
                val out = uri.path + if (uri.query != null) "?" + uri.query else ""
                if (out.isEmpty()) "/" else out
            } catch (e: Exception) {
                url
            }
        } else {
            url
        }
    }

    companion object {
        fun create(): SChapter = SChapterImpl()
    }
}

class SChapterImpl : SChapter {
    override var url: String = ""
    override var name: String = ""
    override var date_upload: Long = 0L
    override var chapter_number: Float = -1f
    override var scanlator: String? = null
}

class Page(
    val index: Int,
    val url: String = "",
    var imageUrl: String? = null,
    val uri: Any? = null
)

class FilterList(val filters: List<Any> = emptyList()) : List<Any> by filters {
    constructor(vararg filters: Any) : this(filters.toList())
}
