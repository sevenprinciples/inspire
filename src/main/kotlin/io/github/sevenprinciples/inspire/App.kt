package io.github.sevenprinciples.inspire

import com.google.gson.GsonBuilder
import com.mashape.unirest.http.Unirest
import io.github.vyo.twig.logger.Level
import io.github.vyo.twig.logger.Logger
import spark.Spark.*
import java.io.InputStreamReader
import java.time.Year
import java.util.*

/**
 * Created by juan.romero on 02.07.17.
 */

val MATTERMOST_TOKEN = System.getenv("MATTERMOST_TOKEN")
val BASE_PATH = System.getenv("BASE_PATH")
val PORT = System.getenv("PORT")

fun getQuotes(path: String): List<String>  {
    val stream = App.javaClass.classLoader.getResourceAsStream(path)
    return InputStreamReader(stream).readLines().map { org.apache.commons.text.StringEscapeUtils.unescapeHtml4(it) }
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

        port(PORT.toInt())

        staticFileLocation("/public")
        logger.level = Level.DEBUG

        before { request, _ ->
            if (request.queryParams("token") != MATTERMOST_TOKEN) {
                throw Exception("forbidden")
            }
        }
        before { request, _ -> logger.debug("request: $request") }
        after { _, response -> logger.debug("response: $response") }

        exception(Exception::class.java, { exc, _, res ->

            if ( exc.message == "forbidden") {
                res.body("forbidden")
                res.status(403)
            } else {
                res.body("internal server error")
                res.status(500)
                logger.error(exc)
            }

        })

        post("/api/inspire", { _, res ->

            res.type("application/json")

            MattermostPost("![](${Unirest.get("http://inspirobot.me/api?generate=true").asString().body})${System.lineSeparator()}> ${chooseQuote(regularQuotes)}${System.lineSeparator()}~ [InspiroBot](http://inspirobot.me/), ${Year.now().value}",
                    "$BASE_PATH/static/inspirobot.png",
                    "in_channel")

        }, { obj -> gson.toJson(obj) })
    }
}