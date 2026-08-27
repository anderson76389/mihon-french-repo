package eu.kanade.tachiyomi.extension.fr.scanmanga

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

class ScanManga : ParsedHttpSource() {

    override val name = "Scan-Manga"
    override val baseUrl = "https://www.scan-manga.com"
    override val lang = "fr"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Referer", "$baseUrl/")
                .build()
            chain.proceed(request)
        }
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/top-mangas.html?page=$page", headers)
    override fun popularMangaSelector(): String = "div.content_manga div.element, div.listing_manga div.manga_item, div.manga"
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        val link = element.select("div.title a, h3 a, a.titre_manga, a.manga_title").first()!!
        title = link.text().trim()
        setUrlWithoutDomain(link.attr("href"))
        thumbnail_url = element.select("div.image img, img.manga_img, img.cover").attr("abs:src")
    }
    override fun popularMangaNextPageSelector(): String? = "div.pagination a.next, a:contains(Suivant)"

    override fun latestUpdatesRequest(page: Int): Request = GET(baseUrl, headers)
    override fun latestUpdatesSelector(): String = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/recherche?q=${URLEncoder.encode(query, "UTF-8")}", headers)
    override fun searchMangaSelector(): String = popularMangaSelector()
    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        description = document.select("div.description, div.synopsis, div.texte_detail").text().trim()
        genre = document.select("div.genres a, div.tags a, span.genre a").joinToString { it.text().trim() }
        val statusText = document.select("div.status, span.statut").text().lowercase()
        status = when {
            statusText.contains("en cours") -> SManga.ONGOING
            statusText.contains("terminé") || statusText.contains("complete") -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        thumbnail_url = document.select("div.cover img, div.image_manga img, div.manga_image img").attr("abs:src")
    }

    override fun chapterListSelector(): String = "div.chapitres_list div.chapitre, ul.chapters_list li, div.element_chapitre"
    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val link = element.select("a").first()!!
        setUrlWithoutDomain(link.attr("href"))
        name = link.text().trim()
        date_upload = runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).parse(element.select("span.date, span.time").text().trim())?.time ?: 0L
        }.getOrDefault(0L)
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        val html = document.html()

        val scriptPattern = Pattern.compile("var\\s+(?:images|pages|lstImages)\\s*=\\s*\\[(.*?)\\];", Pattern.DOTALL)
        val matcher = scriptPattern.matcher(html)

        if (matcher.find()) {
            val rawContent = matcher.group(1) ?: ""
            val urlPattern = Pattern.compile("['\"](https?://[^'\"]+|/[^'\"]+)['\"]")
            val urlMatcher = urlPattern.matcher(rawContent)
            var index = 0
            while (urlMatcher.find()) {
                var url = urlMatcher.group(1)
                if (url.startsWith("/")) url = baseUrl + url
                pages.add(Page(index++, "", url))
            }
        }

        if (pages.isEmpty()) {
            document.select("div.reader-images img, div#lecture img, div.image_scan img").forEachIndexed { index, element ->
                val src = element.attr("data-src").ifEmpty { element.attr("src") }.trim()
                if (src.isNotBlank()) {
                    pages.add(Page(index, "", if (src.startsWith("http")) src else baseUrl + src))
                }
            }
        }

        if (pages.isEmpty()) throw Exception("Structure de lecture non reconnue.")
        return pages
    }

    override fun imageUrlParse(document: Document): String = ""
}
