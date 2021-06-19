package com.github.insertplaceholdername.tosca.oidc

import com.okta.jwt.AccessTokenVerifier
import com.okta.jwt.IdTokenVerifier
import com.okta.jwt.JwtVerifiers
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.oauth
import io.ktor.client.HttpClient

internal fun config(): OidcConfig =
     oidcConfigReader(ConfigFactory.load() ?: throw Exception("Could not load config"))

fun Application.setupAuth() {
    val oidcConfig = config()
    install(Authentication) {
       oauth {
           urlProvider = {
               "http://localhost:8080/authorization-code/callback"
           }
           providerLookup = {
               oidcConfig.asOAuth2Config()
           }
           client = HttpClient()
       }
    }
}

fun accessTokenVerifier(): AccessTokenVerifier {
    val oidcConfig = config()

    return JwtVerifiers.accessTokenVerifierBuilder()
        .setIssuer(oidcConfig.url)
        .setAudience(oidcConfig.audience)
        .build()
}

fun idTokenVerifier(): IdTokenVerifier {
    val oidcConfig = config()

    return JwtVerifiers.idTokenVerifierBuilder()
        .setClientId(oidcConfig.clientId)
        .setIssuer(oidcConfig.url)
        .build()
}