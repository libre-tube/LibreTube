package com.github.libretube.helpers

import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Streams
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element

// Based off of https://github.com/TeamPiped/Piped/blob/master/src/utils/DashUtils.js

object DashHelper {

    private val builderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    private val transformerFactory: TransformerFactory = TransformerFactory.newInstance()

    private data class AdapSetInfo(
        val mimeType: String,
        val formats: MutableList<PipedStream> = mutableListOf(),
        val audioTrackId: String? = null,
        val audioTrackType: String? = null,
        val audioLocale: String? = null
    )

    fun createManifest(streams: Streams, supportsHdr: Boolean, rewriteUrls: Boolean): String {
        val builder = builderFactory.newDocumentBuilder()

        val doc = builder.newDocument()
        val mpd = doc.createElement("MPD")
        mpd.setAttribute("xmlns", "urn:mpeg:dash:schema:mpd:2011")
        mpd.setAttribute("profiles", "urn:mpeg:dash:profile:full:2011")
        mpd.setAttribute("minBufferTime", "PT1.5S")
        mpd.setAttribute("type", "static")
        mpd.setAttribute("mediaPresentationDuration", "PT${streams.duration}S")

        val period = doc.createElement("Period")

        val adapSetInfos = ArrayList<AdapSetInfo>()

        for (
        stream in streams.videoStreams
            // used to avoid including LBRY HLS inside the streams in the manifest
            .filter { !it.format.orEmpty().contains("HLS") }
            .filter { supportsHdr || !it.quality.orEmpty().uppercase().contains("HDR") }
        ) {
            // ignore dual format and OTF streams
            if (!stream.videoOnly!! || stream.indexEnd!! <= 0) {
                continue
            }

            val adapSetInfo = adapSetInfos.find { it.mimeType == stream.mimeType }
            if (adapSetInfo != null) {
                adapSetInfo.formats.add(stream)
                continue
            }
            adapSetInfos.add(
                AdapSetInfo(
                    stream.mimeType!!,
                    mutableListOf(stream)
                )
            )
        }

        for (stream in streams.audioStreams) {
            val adapSetInfo =
                adapSetInfos.find {
                    it.mimeType == stream.mimeType && it.audioTrackId == stream.audioTrackId
                }
            if (adapSetInfo != null) {
                adapSetInfo.formats.add(stream)
                continue
            }
            adapSetInfos.add(
                AdapSetInfo(
                    stream.mimeType!!,
                    mutableListOf(stream),
                    stream.audioTrackId,
                    stream.audioTrackType,
                    stream.audioTrackLocale
                )
            )
        }

        for (adapSet in adapSetInfos) {
            val adapSetElement = doc.createElement("AdaptationSet")
            adapSetElement.setAttribute("mimeType", adapSet.mimeType)
            adapSetElement.setAttribute("startWithSAP", "1")
            adapSetElement.setAttribute("subsegmentAlignment", "true")

            if (adapSet.audioTrackId != null) {
                adapSetElement.setAttribute("lang", adapSet.audioTrackId.substring(0, 2))
            } else if (adapSet.audioLocale != null) {
                adapSetElement.setAttribute("lang", adapSet.audioLocale)
            }

            // Only add the Role element if there is a track type set
            // This allows distinction between formats marked as original on YouTube and
            // formats without track type info set
            if (adapSet.audioTrackType != null) {
                val roleElement = doc.createElement("Role")
                roleElement.setAttribute("schemeIdUri", "urn:mpeg:dash:role:2011")
                roleElement.setAttribute(
                    "value",
                    getRoleValueFromAudioTrackType(adapSet.audioTrackType)
                )
                adapSetElement.appendChild(roleElement)
            }

            val isVideo = adapSet.mimeType.contains("video")

            if (isVideo) {
                adapSetElement.setAttribute("scanType", "progressive")
            }

            for (stream in adapSet.formats) {
                val rep = let {
                    if (isVideo) {
                        createVideoRepresentation(doc, stream, rewriteUrls)
                    } else {
                        createAudioRepresentation(doc, stream, rewriteUrls)
                    }
                }
                adapSetElement.appendChild(rep)
            }

            period.appendChild(adapSetElement)
        }

        mpd.appendChild(period)

        doc.appendChild(mpd)

        val domSource = DOMSource(doc)
        val writer = StringWriter()

        val transformer = transformerFactory.newTransformer()
        transformer.transform(domSource, StreamResult(writer))

        return writer.toString()
    }

    private fun createAudioRepresentation(
        doc: Document,
        stream: PipedStream,
        rewriteUrls: Boolean
    ): Element {
        val representation = doc.createElement("Representation")
        representation.setAttribute("bandwidth", stream.bitrate.toString())
        representation.setAttribute("codecs", stream.codec!!)
        representation.setAttribute("mimeType", stream.mimeType!!)

        val audioChannelConfiguration = doc.createElement("AudioChannelConfiguration")
        audioChannelConfiguration.setAttribute(
            "schemeIdUri",
            "urn:mpeg:dash:23003:3:audio_channel_configuration:2011"
        )
        audioChannelConfiguration.setAttribute("value", "2")

        val baseUrl = doc.createElement("BaseURL")
        baseUrl.appendChild(doc.createTextNode(ProxyHelper.unwrapUrl(stream.url!!, rewriteUrls)))

        representation.appendChild(audioChannelConfiguration)
        representation.appendChild(baseUrl)
        representation.appendChild(createSegmentBaseElement(doc, stream))

        return representation
    }

    private fun createSegmentBaseElement(document: Document, stream: PipedStream): Element {
        val segmentBase = document.createElement("SegmentBase")
        segmentBase.setAttribute("indexRange", "${stream.indexStart}-${stream.indexEnd}")

        val initialization = document.createElement("Initialization")
        initialization.setAttribute("range", "${stream.initStart}-${stream.initEnd}")
        segmentBase.appendChild(initialization)

        return segmentBase
    }

    private fun getRoleValueFromAudioTrackType(audioTrackType: String): String {
        return when (audioTrackType.lowercase()) {
            "descriptive" -> "description"
            "dubbed" -> "dub"
            "original" -> "main"
            else -> "alternate"
        }
    }

    private fun createVideoRepresentation(
        doc: Document,
        stream: PipedStream,
        rewriteUrls: Boolean
    ): Element {
        val representation = doc.createElement("Representation")
        representation.setAttribute("codecs", stream.codec!!)
        representation.setAttribute("bandwidth", stream.bitrate.toString())
        representation.setAttribute("width", stream.width.toString())
        representation.setAttribute("height", stream.height.toString())
        representation.setAttribute("maxPlayoutRate", "1")
        representation.setAttribute("frameRate", stream.fps.toString())

        val baseUrl = doc.createElement("BaseURL")
        baseUrl.appendChild(doc.createTextNode(ProxyHelper.unwrapUrl(stream.url!!, rewriteUrls)))

        val segmentBase = doc.createElement("SegmentBase")
        segmentBase.setAttribute("indexRange", "${stream.indexStart}-${stream.indexEnd}")

        val initialization = doc.createElement("Initialization")
        initialization.setAttribute("range", "${stream.initStart}-${stream.initEnd}")
        segmentBase.appendChild(initialization)

        representation.appendChild(baseUrl)
        representation.appendChild(segmentBase)

        return representation
    }
}
