package com.brentvatne.exoplayer;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer2.extractor.ts.Ac3Extractor;
import com.google.android.exoplayer2.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory;
import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.google.android.exoplayer2.source.hls.HlsExtractorFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.TimestampAdjuster;

import java.util.Collections;
import java.util.List;

/**
 * Created by adnaan on 30/01/18.
 */



/**
 * Default {@link HlsExtractorFactory} implementation.
 */
public final class CustomHlsExtractorFactory implements HlsExtractorFactory {

    public static final String AAC_FILE_EXTENSION = ".aac";
    public static final String AC3_FILE_EXTENSION = ".ac3";
    public static final String EC3_FILE_EXTENSION = ".ec3";
    public static final String MP3_FILE_EXTENSION = ".mp3";
    public static final String MP4_FILE_EXTENSION = ".mp4";
    public static final String M4_FILE_EXTENSION_PREFIX = ".m4";
    public static final String MP4_FILE_EXTENSION_PREFIX = ".mp4";

    @Override
    public Pair<Extractor, Boolean> createExtractor(Extractor previousExtractor, Uri uri,
                                                    Format format, List<Format> muxedCaptionFormats, DrmInitData drmInitData,
                                                    TimestampAdjuster timestampAdjuster) {
        String lastPathSegment = uri.getLastPathSegment();
        boolean isPackedAudioExtractor = false;
        Extractor extractor;
         if (lastPathSegment.endsWith(AAC_FILE_EXTENSION)) {
            isPackedAudioExtractor = true;
            extractor = new AdtsExtractor();
        } else if (lastPathSegment.endsWith(AC3_FILE_EXTENSION)
                || lastPathSegment.endsWith(EC3_FILE_EXTENSION)) {
            isPackedAudioExtractor = true;
            extractor = new Ac3Extractor();
        } else if (lastPathSegment.endsWith(MP3_FILE_EXTENSION)) {
            isPackedAudioExtractor = true;
            extractor = new Mp3Extractor(0, 0);
        } else if (previousExtractor != null) {
            // Only reuse TS and fMP4 extractors.
            extractor = previousExtractor;
        } else if (lastPathSegment.endsWith(MP4_FILE_EXTENSION)
                || lastPathSegment.startsWith(M4_FILE_EXTENSION_PREFIX, lastPathSegment.length() - 4)
                || lastPathSegment.startsWith(MP4_FILE_EXTENSION_PREFIX, lastPathSegment.length() - 5)) {
            extractor = new FragmentedMp4Extractor(0, timestampAdjuster, null, drmInitData,
                    muxedCaptionFormats != null ? muxedCaptionFormats : Collections.<Format>emptyList());
        } else {
            // For any other file extension, we assume TS format.
            @DefaultTsPayloadReaderFactory.Flags
            int esReaderFactoryFlags = DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM;
            if (muxedCaptionFormats != null) {
                // The playlist declares closed caption renditions, we should ignore descriptors.
                esReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_OVERRIDE_CAPTION_DESCRIPTORS;
            } else {
                muxedCaptionFormats = Collections.emptyList();
            }
            String codecs = format.codecs;
            Log.d("CustomHlsExFctry","uri: " + uri.toString());
           Log.d("CustomHlsExFctry","segment: " + lastPathSegment + " codecs: " + codecs + " containerMimeType: " + format.containerMimeType + " format: " + format.toString() );
           Log.d("CustomHlsExFctry","esr : " + esReaderFactoryFlags);
            if (!TextUtils.isEmpty(codecs)) {
                // Sometimes AAC and H264 streams are declared in TS chunks even though they don't really
                // exist. If we know from the codec attribute that they don't exist, then we can
                // explicitly ignore them even if they're declared.
                if (!MimeTypes.AUDIO_AAC.equals(MimeTypes.getAudioMediaMimeType(codecs))) {
                    Log.d("CustomHlsExFctry","ignore aac " + MimeTypes.getAudioMediaMimeType(codecs));
                    esReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_IGNORE_AAC_STREAM;
                }
                if (!MimeTypes.VIDEO_H264.equals(MimeTypes.getVideoMediaMimeType(codecs))) {
                    Log.d("CustomHlsExFctry","ignore h264");
                    esReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_IGNORE_H264_STREAM;
                }
            }

             Log.d("CustomHlsExtractFactory", "format.id " + format.id);

             if (MimeTypes.VIDEO_H264.equals(MimeTypes.getVideoMediaMimeType(codecs))) {
                 Log.d("CustomHlsExFctry","ignore aac " + MimeTypes.getAudioMediaMimeType(codecs));
                 esReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_IGNORE_AAC_STREAM;
             }


            if ((codecs == null && format.containerMimeType == "application/x-mpegURL") ){

                Log.d("CustomHlsExFctry","ignore h264 " + MimeTypes.getAudioMediaMimeType(codecs));
                esReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_IGNORE_H264_STREAM;
            }



            Log.d("CustomHlsExtractFactory","esr : " + esReaderFactoryFlags);
            extractor = new TsExtractor(TsExtractor.MODE_HLS, timestampAdjuster,
                    new DefaultTsPayloadReaderFactory(esReaderFactoryFlags, muxedCaptionFormats));
        }
        return Pair.create(extractor, isPackedAudioExtractor);
    }

}

