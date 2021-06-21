package com.github.insertplaceholdername.tosca.auth

import com.typesafe.config.Config
import io.ktor.auth.OAuthServerSettings
import io.ktor.http.HttpMethod.Companion.Post

data class OidcConfig(
    val url: String,
    val clientId: String,
    val clientSecret: String,
    val audience: String,
    val clientPublicHost: String,
    val accessTokenUrl: String,
    val authorizeUrl: String,
    val logoutUrl: String,
)

fun oidcConfigReader(config: Config): OidcConfig {
    val url = config.getString("oidc.url")
    return OidcConfig(
        url = url,
        clientId = config.getString("oidc.clientId"),
        clientSecret = config.getString("oidc.clientSecret"),
        audience = config.getString("oidc.audience"),
        clientPublicHost = config.getString("oidc.clientPublicHost"),
        accessTokenUrl = "$url${config.getString("oidc.accessTokenUrl")}",
        authorizeUrl = "$url${config.getString("oidc.authorizeUrl")}",
        logoutUrl = "$url${config.getString("oidc.logoutUrl")}"
    )
}

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
