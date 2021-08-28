package com.github.insertplaceholdername.tosca.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.insertplaceholdername.tosca.UserRepository
import com.okta.jwt.AccessTokenVerifier
import com.okta.jwt.IdTokenVerifier
import com.okta.jwt.JwtVerifiers
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Date

internal fun oidcConfig(): OidcConfig =
    oidcConfigReader(ConfigFactory.load() ?: throw Exception("Could not load config"))

object JwtConfig {
    private val config: Config = ConfigFactory.load() ?: throw Exception("Could not load config")
    private val key: String = config.getString("jwt.key")
    const val claimKey: String = "user"
    val realm: String = config.getString("jwt.realm")
    val audience: String = config.getString("jwt.realm")
    val issuer: String = config.getString("jwt.issuer")
    val algorithm: Algorithm = Algorithm.HMAC256(key)
    val expirySeconds: Int = config.getInt("jwt.expirySeconds")
}

object Oidc {
    val config = oidcConfig()
    val accessTokenVerifier = accessTokenVerifier()
    val idTokenVerifier = idTokenVerifier()
}

fun Application.setupAuth(userRepository: UserRepository) {

    install(Authentication) {
        oauth("oidc") {
            urlProvider = {
                "${Oidc.config.clientPublicHost}/authorization-code/callback"
            }
            providerLookup = {
                Oidc.config.asOAuth2Config()
            }
            client = HttpClient()
        }

        jwt("jwt") {
            realm = JwtConfig.realm
            verifier(
                JWT
                    .require(JwtConfig.algorithm)
                    .withAudience(JwtConfig.audience)
                    .withIssuer(JwtConfig.issuer)
                    .withClaimPresence(JwtConfig.claimKey)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(JwtConfig.audience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    routing {
        authenticate("oidc") {
            get("/login") {
                call.respondRedirect("/")
            }
            get("/authorization-code/callback") {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>() ?: throw Exception("No principal was given")
                val accessToken = Oidc.accessTokenVerifier.decode(principal.accessToken)
                val idTokenString = principal.extraParameters["id_token"] ?: throw Exception("id_token was not returned")
                val idToken = Oidc.idTokenVerifier.decode(idTokenString, null)
                val firstName = idToken.claims["firstName"] ?: throw Exception("id_token did not contain firstName")
                val lastName = idToken.claims["lastName"] ?: throw Exception("id_token did not contain firstName")
                val groups = idToken.claims["groups"] as List<String>
                val id = idToken.claims["id"] ?: throw Exception("id_token did not contain id")

                userRepository.storeUser(id as String, firstName as String, lastName as String)

                val jwt = createJwt(ApiUser(id, groups.map { group -> Groups.fromString(group) }))
                call.respondRedirect("${Oidc.config.afterLoginRedirectUrl}?tosca_token=$jwt")
            }
        }
    }
}

internal fun createJwt(user: ApiUser): String = JWT.create()
    .withIssuer(JwtConfig.issuer)
    .withAudience(JwtConfig.audience)
    .withClaim(JwtConfig.claimKey, Json.encodeToString(user))
    .withExpiresAt(expiry())
    .sign(JwtConfig.algorithm)

internal fun expiry() = Date.from(Instant.now().plusSeconds(JwtConfig.expirySeconds.toLong()))

internal fun accessTokenVerifier(): AccessTokenVerifier {
    val oidcConfig = oidcConfig()

    return JwtVerifiers.accessTokenVerifierBuilder()
        .setIssuer(oidcConfig.url)
        .setAudience(oidcConfig.audience)
        .build()
}

internal fun idTokenVerifier(): IdTokenVerifier {
    val oidcConfig = oidcConfig()

    return JwtVerifiers.idTokenVerifierBuilder()
        .setClientId(oidcConfig.clientId)
        .setIssuer(oidcConfig.url)
        .build()
}
