package com.github.libretube.api.poToken

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import com.github.libretube.BuildConfig
import com.github.libretube.LibreTubeApp
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper

class PoTokenGenerator : PoTokenProvider {
    private val TAG = PoTokenGenerator::class.simpleName
    private val supportsWebView by lazy { runCatching { CookieManager.getInstance() }.isSuccess }

    private object WebPoTokenGenLock
    private var webPoTokenVisitorData: String? = null
    private var webPoTokenGenerator: PoTokenWebView? = null

    private var poToken: PoTokenResult? = null

    fun getCachedWebClientPoToken(): PoTokenResult? = poToken

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        if (!supportsWebView) {
            return null
        }

        return getWebClientPoToken(videoId, false)
            .also { poToken = it }
    }

    /**
     * @param forceRecreate whether to force the recreation of [webPoTokenGenerator], to be used in
     * case the current [webPoTokenGenerator] threw an error last time
     * [PoTokenGenerator.getWebClientPoToken] was called
     */
    private fun getWebClientPoToken(videoId: String, forceRecreate: Boolean): PoTokenResult {
        val (poTokenGenerator, visitorData, hasBeenRecreated) =
            synchronized(WebPoTokenGenLock) {
                val shouldRecreate = webPoTokenGenerator == null || forceRecreate || webPoTokenGenerator!!.isExpired()

                if (shouldRecreate) {
                    val innertubeClientRequestInfo = InnertubeClientRequestInfo.ofWebClient()
                    innertubeClientRequestInfo.clientInfo.clientVersion =
                        YoutubeParsingHelper.getClientVersion()

                    webPoTokenVisitorData = YoutubeParsingHelper.getVisitorDataFromInnertube(
                        innertubeClientRequestInfo,
                        NewPipe.getPreferredLocalization(),
                        NewPipe.getPreferredContentCountry(),
                        YoutubeParsingHelper.getYouTubeHeaders(),
                        YoutubeParsingHelper.YOUTUBEI_V1_URL,
                        null,
                        false
                    )

                    runBlocking {
                        // close the current webPoTokenGenerator on the main thread
                        webPoTokenGenerator?.let { Handler(Looper.getMainLooper()).post { it.close() } }

                        // create a new webPoTokenGenerator
                        webPoTokenGenerator = PoTokenWebView
                            .newPoTokenGenerator(LibreTubeApp.instance)
                    }
                }

                return@synchronized Triple(
                    webPoTokenGenerator!!,
                    webPoTokenVisitorData!!,
                    shouldRecreate
                )
            }

        val poToken = try {
            // Not using synchronized here, since poTokenGenerator would be able to generate
            // multiple poTokens in parallel if needed. The only important thing is for exactly one
            // visitorData/streaming poToken to be generated before anything else.
            runBlocking {
                poTokenGenerator.generatePoToken(videoId)
            }
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                // the poTokenGenerator has just been recreated (and possibly this is already the
                // second time we try), so there is likely nothing we can do
                throw throwable
            } else {
                // retry, this time recreating the [webPoTokenGenerator] from scratch;
                // this might happen for example if NewPipe goes in the background and the WebView
                // content is lost
                Log.e(TAG, "Failed to obtain poToken, retrying", throwable)
                return getWebClientPoToken(videoId = videoId, forceRecreate = true)
            }
        }


        if (BuildConfig.DEBUG) {
            Log.d(
                TAG, "poToken for $videoId: $poToken, visitor_data=$visitorData"
            )
        }

        return PoTokenResult(visitorData, poToken, poToken)
    }

    override fun getWebEmbedClientPoToken(videoId: String?): PoTokenResult? = null

    override fun getAndroidClientPoToken(videoId: String?): PoTokenResult? = null

    override fun getIosClientPoToken(videoId: String?): PoTokenResult? = null
}

