package com.github.insertplaceholdername.tosca.oidc

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.okta.jwt.AccessTokenVerifier
import com.okta.jwt.IdTokenVerifier
import com.okta.jwt.JwtVerifiers
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import java.time.Instant
import java.util.Date

internal fun oidcConfig(): OidcConfig =
     oidcConfigReader(ConfigFactory.load() ?: throw Exception("Could not load config"))

object JwtConfig {
    val config = ConfigFactory.load() ?: throw Exception("Could not load config")
    val realm = config.getString("jwt.realm")
    val audience = config.getString("jwt.realm")
    val issuer = config.getString("jwt.issuer")
    private val key = config.getString("jwt.key")
    const val claimKey = "user_id"
    val algorithm = Algorithm.HMAC256(key)
    val expirySeconds = config.getInt("jwt.expirySeconds")
}

fun Application.setupAuth() {
    val oidcConfig = oidcConfig()

    install(Authentication) {
        oauth("oidc") {
           urlProvider = {
               "http://localhost:8080/authorization-code/callback"
           }
           providerLookup = {
               oidcConfig.asOAuth2Config()
           }
           client = HttpClient()
       }

       jwt("jwt") {
           realm = JwtConfig.realm
           verifier(JWT
               .require(JwtConfig.algorithm)
               .withAudience(JwtConfig.audience)
               .withIssuer(JwtConfig.issuer)
               .withClaimPresence(JwtConfig.claimKey)
               .build())
           validate { credential ->
               if (credential.payload.audience.contains(JwtConfig.audience)) {
                   JWTPrincipal(credential.payload)
               } else {
                   null
               }
           }
       }
    }

}


fun createJwt(userId: String): String = JWT.create()
    .withIssuer(JwtConfig.issuer)
    .withAudience(JwtConfig.audience)
    .withClaim(JwtConfig.claimKey, userId)
    .withExpiresAt(expiry())
    .sign(JwtConfig.algorithm)

internal fun expiry() = Date.from(Instant.now().plusSeconds(JwtConfig.expirySeconds.toLong()))

fun accessTokenVerifier(): AccessTokenVerifier {
    val oidcConfig = oidcConfig()

    return JwtVerifiers.accessTokenVerifierBuilder()
        .setIssuer(oidcConfig.url)
        .setAudience(oidcConfig.audience)
        .build()
}

fun idTokenVerifier(): IdTokenVerifier {
    val oidcConfig = oidcConfig()

    return JwtVerifiers.idTokenVerifierBuilder()
        .setClientId(oidcConfig.clientId)
        .setIssuer(oidcConfig.url)
        .build()
}