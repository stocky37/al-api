package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.Config
import info.kurozeropb.azurlane.structures.*
import io.javalin.http.Context
import it.skrape.core.Method
import it.skrape.core.Mode
import it.skrape.extract
import it.skrape.selects.htmlDocument
import it.skrape.skrape

enum class Category(val string: String) {
    RARITY("rarity"),
    TYPE("type"),
    AFFILIATION("affiliation")
}

object ShipsController {

    fun getShips(ctx: Context) {
        val category = ctx.queryParam("category")
        if (category.isNullOrBlank()) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Missing or invalid category query param"
            ))
            return
        }

        val data = try {
            when (category) {
                Category.RARITY.string -> getShipsByRarity(ctx)
                Category.TYPE.string -> getShipsByType(ctx)
                Category.AFFILIATION.string -> getShipsByAffiliation(ctx)
                else -> {
                    ctx.status(400).json(ErrorResponse(
                        statusCode = 400,
                        statusMessage = "Bad Request",
                        message = "Invalid category query param"
                    ))
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ctx.status(500).json(ErrorResponse(
                statusCode = 500,
                statusMessage = "Internal Server Error",
                message = "Something bad happened when fetching the construction data",
                error = e.stackTrace.joinToString("\n")
            ))
            return
        }

        if (data.isNullOrEmpty()) return

        ctx.status(200).json(ShipsResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "The request was successful",
            ships = data
        ))
    }

    private fun getShipsByRarity(ctx: Context): List<SmallShip>? {
        var rarity = ctx.queryParam("rarity")
        if (rarity.isNullOrBlank()) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid rarity query param"
            ))
            return null
        }

        rarity = rarity.split(" ")
            .joinToString(" ") { it.capitalize() }

        val isValid = Config.rarities.contains(rarity)
        if (!isValid) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid rarity \"${rarity}\""
            ))
            return null
        }

        return scrapeHtmlForShips(rarity, Category.RARITY)
    }

    private fun getShipsByType(ctx: Context): List<SmallShip>? {
        var type = ctx.queryParam("type")
        if (type.isNullOrBlank()) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid type query param"
            ))
            return null
        }

        type = type.split(" ")
            .joinToString(" ") { it.capitalize() }

        val isValid = Config.types.contains(type)
        if (!isValid) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid type \"${type}\""
            ))
            return null
        }

        return scrapeHtmlForShips(type, Category.TYPE)
    }

    private fun getShipsByAffiliation(ctx: Context): List<SmallShip>? {
        var affiliation = ctx.queryParam("affiliation")
        if (affiliation.isNullOrBlank()) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid affiliation query param"
            ))
            return null
        }

        affiliation = affiliation.split(" ")
            .joinToString(" ") { it.capitalize() }

        val isValid = Config.affiliations.contains(affiliation)
        if (!isValid) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid affiliation \"${affiliation}\""
            ))
            return null
        }

        return scrapeHtmlForShips(affiliation, Category.AFFILIATION)
    }

    private fun scrapeHtmlForShips(value: String, category: Category) =
        skrape {
            url = "${Config.baseUrl}/List_of_Ships"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    val ships = mutableListOf<SmallShip>()
                    val items = getElementsContainingOwnText(value)
                    when (category) {
                        Category.RARITY -> {
                            val vals = items.filter { it.tagName() == "td" }
                            vals.forEach { el ->
                                val parent = el.parent()
                                val id = parent.child(0).attr("data-sort-value")
                                val name = parent.child(1).child(0).attr("title")
                                ships.add(SmallShip(name, id))
                            }
                        }
                        Category.TYPE,
                        Category.AFFILIATION -> {
                            items.forEach { el ->
                                val id = el.parent().parent().child(0).child(0).text()
                                val name = el.parent().parent().child(1).child(0).text()
                                ships.add(SmallShip(name, id))
                            }
                        }
                    }

                    ships
                }
            }
        }

}