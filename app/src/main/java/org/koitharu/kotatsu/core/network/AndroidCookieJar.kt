package org.koitharu.kotatsu.core.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidCookieJar : CookieJar {

	private val cookieManager = CookieManager.getInstance()

	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		val rawCookie = cookieManager.getCookie(url.toString()) ?: return emptyList()
		return rawCookie.split(';').mapNotNull {
			Cookie.parse(url, it)
		}
	}

	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		if (cookies.isEmpty()) {
			return
		}
		val urlString = url.toString()
		for (cookie in cookies) {
			cookieManager.setCookie(urlString, cookie.toString())
		}
	}

	fun remove(url: String, vararg names: String) {
		val cookies = cookieManager.getCookie(url) ?: return
		val newCookies = cookies.split(";")
			.filterNot { cookie ->
				names.any { cookie.startsWith("$it=") }
			}.joinToString(";")
		cookieManager.setCookie(url, newCookies)
	}

	fun clearAsync() {
		cookieManager.removeAllCookies(null)
	}

	suspend fun clear() = suspendCoroutine<Boolean> { continuation ->
		cookieManager.removeAllCookies(continuation::resume)
	}
}