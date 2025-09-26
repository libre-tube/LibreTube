/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.libretube.player;

import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.analytics.PlayerId;
import com.github.libretube.player.PlayerEmsgHandler.PlayerTrackEmsgHandler;
import com.github.libretube.player.manifest.SabrManifest;
import androidx.media3.exoplayer.source.chunk.ChunkSource;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.LoaderErrorThrower;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.mp4.Mp4Extractor;
import androidx.media3.extractor.text.SubtitleParser;
import java.util.List;

/** A {@link ChunkSource} for Sabr streams. */
@UnstableApi
public interface SabrChunkSource extends ChunkSource {

  /** Factory for {@link SabrChunkSource}s. */
  interface Factory {

    /**
     * Sets the {@link SubtitleParser.Factory} to use for parsing subtitles during extraction. The
     * default factory value is implementation dependent.
     *
     * @param subtitleParserFactory The {@link SubtitleParser.Factory} for parsing subtitles during
     *     extraction.
     * @return This factory, for convenience.
     */
    default Factory setSubtitleParserFactory(SubtitleParser.Factory subtitleParserFactory) {
      return this;
    }

    /**
     * Sets whether subtitles should be parsed as part of extraction (before being added to the
     * sample queue) or as part of rendering (when being taken from the sample queue). Defaults to
     * {@code false} (i.e. subtitles will be parsed as part of rendering).
     *
     * <p>This method is experimental and will be renamed or removed in a future release.
     *
     * @param parseSubtitlesDuringExtraction Whether to parse subtitles during extraction or
     *     rendering.
     * @return This factory, for convenience.
     */
    default Factory experimentalParseSubtitlesDuringExtraction(
        boolean parseSubtitlesDuringExtraction) {
      return this;
    }

    /**
     * Sets the set of video codecs for which within GOP sample dependency information should be
     * parsed as part of extraction. Defaults to {@code 0} - empty set of codecs.
     *
     * <p>Having access to additional sample dependency information can speed up seeking. See {@link
     * Mp4Extractor#FLAG_READ_WITHIN_GOP_SAMPLE_DEPENDENCIES}.
     *
     * <p>This method is experimental and will be renamed or removed in a future release.
     *
     * @param codecsToParseWithinGopSampleDependencies The set of codecs for which to parse within
     *     GOP sample dependency information.
     * @return This factory, for convenience.
     */
    default Factory experimentalSetCodecsToParseWithinGopSampleDependencies(
        @C.VideoCodecFlags int codecsToParseWithinGopSampleDependencies) {
      return this;
    }

    /**
     * @param manifestLoaderErrorThrower Throws errors affecting loading of manifests.
     * @param manifest The initial manifest.
     * @param baseUrlExclusionList The base URL exclusion list.
     * @param periodIndex The index of the corresponding period in the manifest.
     * @param adaptationSetIndices The indices of the corresponding adaptation sets in the period.
     * @param trackSelection The track selection.
     * @param trackType The {@link C.TrackType track type}.
     * @param elapsedRealtimeOffsetMs If known, an estimate of the instantaneous difference between
     *     server-side unix time and {@link SystemClock#elapsedRealtime()} in milliseconds,
     *     specified as the server's unix time minus the local elapsed time. Or {@link C#TIME_UNSET}
     *     if unknown.
     * @param enableEventMessageTrack Whether to output an event message track.
     * @param closedCaptionFormats The {@link Format Formats} of closed caption tracks to be output.
     * @param playerEmsgHandler The track output to write emsg messages to, or null if emsgs
     *     shouldn't be written.
     * @param transferListener The transfer listener which should be informed of any data transfers.
     *     May be null if no listener is available.
     * @param playerId The {@link PlayerId} of the player using this chunk source.
     * @param cmcdConfiguration The {@link CmcdConfiguration} for this chunk source.
     * @return The created {@link SabrChunkSource}.
     */
    SabrChunkSource createSabrChunkSource(
        LoaderErrorThrower manifestLoaderErrorThrower,
        SabrManifest manifest,
        BaseUrlExclusionList baseUrlExclusionList,
        int periodIndex,
        int[] adaptationSetIndices,
        ExoTrackSelection trackSelection,
        @C.TrackType int trackType,
        long elapsedRealtimeOffsetMs,
        boolean enableEventMessageTrack,
        List<Format> closedCaptionFormats,
        @Nullable PlayerTrackEmsgHandler playerEmsgHandler,
        @Nullable TransferListener transferListener,
        PlayerId playerId,
        @Nullable CmcdConfiguration cmcdConfiguration);

    /**
     * Returns the output {@link Format} of emitted {@linkplain C#TRACK_TYPE_TEXT text samples}
     * which were originally in {@code sourceFormat}.
     *
     * <p>In many cases, where an {@link Extractor} emits samples from the source without mutation,
     * this method simply returns {@code sourceFormat}. In other cases, such as an {@link Extractor}
     * that transcodes subtitles from the {@code sourceFormat} to {@link
     * MimeTypes#APPLICATION_MEDIA3_CUES}, the format is updated to indicate the transcoding that is
     * taking place.
     *
     * <p>Non-text source formats are always returned without mutation.
     *
     * @param sourceFormat The original text-based format.
     * @return The {@link Format} that will be associated with a {@linkplain C#TRACK_TYPE_TEXT text
     *     track}.
     */
    default Format getOutputTextFormat(Format sourceFormat) {
      return sourceFormat;
    }
  }

  /**
   * Updates the manifest.
   *
   * @param newManifest The new manifest.
   * @param newPeriodIndex The index of the period covered by {@code newManifest}.
   */
  void updateManifest(SabrManifest newManifest, int newPeriodIndex);

  /**
   * Updates the track selection.
   *
   * @param trackSelection The new track selection instance. Must be equivalent to the previous one.
   */
  void updateTrackSelection(ExoTrackSelection trackSelection);
}
