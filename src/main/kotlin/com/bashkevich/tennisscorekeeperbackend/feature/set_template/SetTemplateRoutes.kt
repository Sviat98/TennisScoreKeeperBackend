package com.bashkevich.tennisscorekeeperbackend.feature.set_template

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateDto
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTypeFilter
import com.bashkevich.tennisscorekeeperbackend.plugins.parseEnumSafe
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalKtorApi::class)
fun Route.setTemplateRoutes() {
    val setTemplateService by application.inject<SetTemplateService>()

    route("/set-templates") {
        /**
         * Tag: Set Template
         * Get set templates. Optionally filter by type (ALL, REGULAR, DECIDER).
         */
        get {
            val setTemplateType = call.queryParameters["type"]?.uppercase() ?: SetTemplateTypeFilter.ALL.name

            val setTemplateTypeFilter = setTemplateType.parseEnumSafe<SetTemplateTypeFilter>()

            val setTemplates = setTemplateService.getSetTemplates(setTemplateTypeFilter)

            call.respond(setTemplates)
        }.describe {
            responses {
                HttpStatusCode.OK {
                    description = "Successfully retrieved set templates"
                    schema = jsonSchema<List<SetTemplateDto>>()
                }
                HttpStatusCode.BadRequest {
                    description = "Invalid type filter value. Allowed: ALL, REGULAR, DECIDER"
                    ContentType.Text.Plain()
                }
            }
        }
    }
}
