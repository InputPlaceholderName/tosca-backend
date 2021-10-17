package com.github.insertplaceholdername.tosca.auth

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.authentication
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext
import io.ktor.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.serialization.Serializable

@Serializable
data class ApiUser(val id: Int, val userId: String, val groups: List<Group>) {
    fun hasGroup(group: Group): Boolean = groups.contains(group)
    fun notHasGroup(group: Group): Boolean = !hasGroup(group)
}

class AuthorizationException(override val message: String, val code: HttpStatusCode) : Exception(message)

val CurrentUser = AttributeKey<ApiUser>("CurrentUser")

class GroupAuthorization(config: Configuration) {
    private val getCurrentUser = config.currentUser

    class Configuration {
        internal var currentUser: (Principal) -> ApiUser = { throw NotImplementedError("Current user is not implemented") }

        fun extractCurrentUser(getCurrentUserFunc: (Principal) -> ApiUser) {
            currentUser = getCurrentUserFunc
        }
    }

    fun interceptPipeline(pipeline: ApplicationCallPipeline, requiredGroups: Set<Group>) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Features, Authentication.ChallengePhase)
        pipeline.insertPhaseAfter(Authentication.ChallengePhase, AuthorizationPhase)
        pipeline.intercept(AuthorizationPhase) {
            val principal = call.authentication.principal<Principal>() ?: throw AuthorizationException("Missing credentials", HttpStatusCode.Unauthorized)
            val groups = getCurrentUser(principal).groups
            val denyReasons = mutableListOf<String>()
            if (requiredGroups.isNotEmpty() && groups.none { it in requiredGroups }) {
                denyReasons += "Missing any of the roles: ${groups.joinToString(", ")}"
            }

            if (denyReasons.isNotEmpty()) {
                call.respond(HttpStatusCode.Forbidden, denyReasons.joinToString(". "))
                finish()
            } else {
                call.attributes.put(CurrentUser, getCurrentUser(principal))
            }
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, GroupAuthorization> {
        override val key: AttributeKey<GroupAuthorization> = AttributeKey("GroupAuthorization")
        val AuthorizationPhase = PipelinePhase("Authorization")
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): GroupAuthorization {
            val configuration = Configuration().apply(configure)
            return GroupAuthorization(configuration)
        }
    }
}

class AuthorizedRouteSelector(private val description: String) : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Constant

    override fun toString() = "(authorize $description)"
}

/**
 * Allows any logged-in user to access the routes.
 * Sets the current user in the CurrentUser attribute of the call
 */
fun Route.anyone(build: Route.() -> Unit) = authorizeRoute(setOf(), build)

/**
 * Only allows logged-in users with a specific group to access the routes.
 * Sets the current user in the CurrentUser attribute of the call
 *
 * @param group The required group
 */
fun Route.withGroup(group: Group, build: Route.() -> Unit) = authorizeRoute(setOf(group), build)

/**
 * Only allows logged-in users with at least one of the specified groups to access the routes.
 * Sets the current user in the CurrentUser attribute of the call
 *
 * @param groups The required groups
 */
fun Route.withAnyGroup(vararg groups: Group, build: Route.() -> Unit) = authorizeRoute(groups.toSet(), build)

private fun Route.authorizeRoute(groups: Set<Group>, build: Route.() -> Unit): Route {
    val description = "Groups: ${groups.joinToString(", ")}"
    val authorizedRoute = createChild(AuthorizedRouteSelector(description))
    application.feature(GroupAuthorization).interceptPipeline(authorizedRoute, groups)
    authorizedRoute.build()
    return authorizedRoute
}
