package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.auth.ApiUser
import com.github.insertplaceholdername.tosca.auth.GroupAuthorization
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.ContentNegotiation
import io.ktor.routing.Route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.withTestApplication

class MockJWTPrincipal : Principal

fun runTest(user: ApiUser?, routes: Route.() -> Unit, test: TestApplicationEngine.() -> Unit) {
    withTestApplication(
        app@{
            install(Authentication) {
                basic {
                    realm = "test-realm"
                    validate {
                        MockJWTPrincipal()
                    }
                }
            }
            user?.let {
                install(GroupAuthorization) {
                    extractCurrentUser {
                        user
                    }
                }
            }
            routing {
                install(ContentNegotiation) {
                    json()
                }

                authenticate {
                    routes(this)
                }
            }
        }
    ) {
        test(this)
    }
}

fun TestApplicationRequest.withValidLogin(): TestApplicationRequest {
    addHeader("Authorization", "Basic amV0YnJhaW5zOmZvb2Jhcg")
    return this
}

fun TestApplicationRequest.setJsonContentType() {
    addHeader("Content-Type", "application/json")
}
