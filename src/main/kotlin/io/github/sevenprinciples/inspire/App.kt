package io.github.sevenprinciples.inspire

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mashape.unirest.http.Unirest
import io.github.vyo.twig.logger.Level
import io.github.vyo.twig.logger.Logger
import spark.Spark.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * Created by juan.romero on 02.07.17.
 */

fun getQuotes(path: String): List<String>  {
    val uri = App.javaClass.classLoader.getResource(path).toURI()
    return Files.readAllLines(Paths.get(uri), Charsets.UTF_8)
}

fun chooseQuote(quotes: List<String>): String {
    val random = Random()
    val index = random.nextInt(quotes.size)
    return quotes[index]
}

object App {

    val logger = Logger("inspire")
    val gson = GsonBuilder().disableHtmlEscaping().create()

    val regularQuotes = getQuotes("quotes/regular.txt")
    val xmasQuotes = getQuotes("quotes/xmas.txt")

    @JvmStatic fun main(args: Array<String>) {

        staticFileLocation("/public")
        logger.level = Level.DEBUG

        before { request, _ -> logger.debug("request: $request") }
        after { _, response -> logger.debug("response: $response") }

        post("/api/inspire", { req, res ->

            MattermostPost("${chooseQuote(regularQuotes)}${System.lineSeparator()}![](${Unirest.get("http://inspirobot.me/api?generate=true").asString().body})",
                    "http://localhost:4567/static/inspirobot.png",
                    "in_channel")

        }, { obj -> gson.toJson(obj) })
    }
}