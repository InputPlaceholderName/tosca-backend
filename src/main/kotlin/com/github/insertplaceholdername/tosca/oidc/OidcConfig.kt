package com.github.insertplaceholdername.tosca.oidc

import com.typesafe.config.Config
import io.ktor.auth.OAuthServerSettings
import io.ktor.config.tryGetString
import io.ktor.http.HttpMethod.Companion.Post

data class OidcConfig(
    val url: String,
    val clientId: String,
    val clientSecret: String,
    val audience: String
    ) {

    val accessTokenUrl = "$url/v1/token"
    val authorizeUrl = "$url/v1/authorize"
    val logoutUrl = "$url/v1/logout"
}

fun oidcConfigReader(config: Config) = OidcConfig(
    url = config.getString("oidc.url"),
    clientId = config.getString("oidc.clientId"),
    clientSecret = config.getString("oidc.clientSecret"),
    audience = config.tryGetString("oidc.audience") ?: "api://default"
)

fun OidcConfig.asOAuth2Config(): OAuthServerSettings.OAuth2ServerSettings =
    OAuthServerSettings.OAuth2ServerSettings(
        name = "oidc",
        authorizeUrl = authorizeUrl,
        accessTokenUrl = accessTokenUrl,
        clientId = clientId,
        clientSecret = clientSecret,
        defaultScopes = listOf("openid"),
        requestMethod = Post
    )