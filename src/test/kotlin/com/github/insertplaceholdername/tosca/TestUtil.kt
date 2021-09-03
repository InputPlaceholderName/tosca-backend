package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.auth.ApiUser
import com.github.insertplaceholdername.tosca.auth.GroupAuthorization
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.basic
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication

class MockJWTPrincipal : Principal

fun runTest(user: ApiUser?, routes: Application.() -> Unit, test: TestApplicationEngine.() -> Unit) {
    withTestApplication(
        app@{
            install(Authentication) {
                basic("basic") {
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
                routes(this@app)
            }
        }
    ) {
        test(this)
    }
}
