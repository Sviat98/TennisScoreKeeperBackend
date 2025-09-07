package com.bashkevich.tennisscorekeeperbackend.feature.set_template

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTypeFilter
import com.bashkevich.tennisscorekeeperbackend.plugins.parseEnumSafe
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.setTemplateRoutes() {
    val setTemplateService by application.inject<SetTemplateService>()

    route("/set-templates") {
        get {
            val setTemplateType = call.queryParameters["type"]?.uppercase() ?: SetTemplateTypeFilter.ALL.name

            val setTemplateTypeFilter = setTemplateType.parseEnumSafe<SetTemplateTypeFilter>()

            val setTemplates = setTemplateService.getSetTemplates(setTemplateTypeFilter)

            call.respond(setTemplates)
        }
    }
}