package com.example.customalarm.data.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetingParserTest {

    @Test
    fun parse_name_artist_fields() {
        val body = """[{"id":"1","name":"SongA","artist":"ArtistA","url":"http://x/a.mp3"}]"""
        val songs = MetingParser.parseSongs(body)
        assertEquals(1, songs.size)
        assertEquals("SongA", songs[0].title)
        assertEquals("ArtistA", songs[0].artist)
        assertEquals("http://x/a.mp3", songs[0].url)
    }

    @Test
    fun parse_title_author_fields() {
        val body = """[{"id":"2","title":"SongB","author":"AuthorB","url":null}]"""
        val songs = MetingParser.parseSongs(body)
        assertEquals(1, songs.size)
        assertEquals("SongB", songs[0].title)
        assertEquals("AuthorB", songs[0].artist)
        assertNull(songs[0].url)
    }

    @Test
    fun parse_numeric_id() {
        val body = """[{"id":186016,"name":"X","artist":"Y"}]"""
        val songs = MetingParser.parseSongs(body)
        assertEquals("186016", songs[0].songId)
    }

    @Test
    fun parse_id_from_url_query_when_no_top_level_id() {
        // i-meto / injahow / meting-ve 的 type=song / type=playlist 无顶层 id，id 嵌在 url 查询串
        val body = """[{"title":"晴天","author":"周杰伦","url":"https://api.i-meto.com/meting/api?server=netease&type=url&id=186016&auth=abc"}]"""
        val songs = MetingParser.parseSongs(body)
        assertEquals(1, songs.size)
        assertEquals("186016", songs[0].songId)
        assertEquals("晴天", songs[0].title)
        assertEquals("周杰伦", songs[0].artist)
    }

    @Test
    fun parse_playlist_without_top_level_id() {
        val body = """[{"name":"A","artist":"B","url":"https://api.injahow.cn/meting/?server=netease&type=url&id=2755496359"}]"""
        val songs = MetingParser.parseSongs(body)
        assertEquals("2755496359", songs[0].songId)
    }

    @Test
    fun parse_nonArray_returnsEmpty() {
        assertTrue(MetingParser.parseSongs("""{"id":"4"}""").isEmpty())
        assertTrue(MetingParser.parseSongs("garbage").isEmpty())
    }

    @Test
    fun parseFirstUrl() {
        val body = """[{"url":null},{"url":"http://x/b.mp3"}]"""
        assertEquals("http://x/b.mp3", MetingParser.parseFirstUrl(body))
    }
}
