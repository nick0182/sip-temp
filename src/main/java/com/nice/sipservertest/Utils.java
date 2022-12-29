package com.nice.sipservertest;

import com.nice.sipservertest.dto.MediaCodecTypes;
import com.nice.sipservertest.dto.SdpMediaCodecs;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static final Map<SdpMediaCodecs, MediaCodecTypes> codecConversionTable;
    static {
        codecConversionTable = new HashMap<>();
        codecConversionTable.put(SdpMediaCodecs.G729, MediaCodecTypes.G729);
        codecConversionTable.put(SdpMediaCodecs.PCMU, MediaCodecTypes.G711U);
        codecConversionTable.put(SdpMediaCodecs.PCMA, MediaCodecTypes.G711A);
        codecConversionTable.put(SdpMediaCodecs.TELEPHONE_EVENT, MediaCodecTypes.TELEPHONY_EVENT);
    }
}

