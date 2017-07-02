package io.github.sevenprinciples.inspire

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mashape.unirest.http.Unirest
import io.github.vyo.twig.logger.Level
import io.github.vyo.twig.logger.Logger
import spark.Spark.*
import java.awt.PageAttributes
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * Created by juan.romero on 02.07.17.
 */

val MATTERMOST_TOKEN = System.getenv("MATTERMOST_TOKEN")
val BASE_PATH = System.getenv("BASE_PATH")

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

        before { request, _ ->
            if (request.queryParams("token") != MATTERMOST_TOKEN) {
                throw Exception("forbidden")
            }
        }
        before { request, _ -> logger.debug("request: $request") }
        after { _, response -> logger.debug("response: $response") }

        exception(Exception::class.java, { exc, req, res ->

            if ( exc.message == "forbidden") {
                res.body("forbidden")
                res.status(403)
            } else {
                res.body("internal server error")
                res.status(500)
                logger.error(exc)
            }

        })

        post("/api/inspire", { req, res ->

            res.type("application/json")

            MattermostPost("${chooseQuote(regularQuotes)}${System.lineSeparator()}![](${Unirest.get("http://inspirobot.me/api?generate=true").asString().body})",
                    "$BASE_PATH/static/inspirobot.png",
                    "in_channel")

        }, { obj -> gson.toJson(obj) })
    }
}