package com.nice.sipservertest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MediaCodec {

    public static final List<MediaCodec> defaultCodecList = new ArrayList<>();
    static {
        defaultCodecList.add(new MediaCodec(0, MediaCodecTypes.G711U, ""));
    }

    private int rtpPayloadType;

    /**
     * media codec available on a media descriptor (ex. G.711 a-law or G.729)
     */
    private MediaCodecTypes mediaCodecType;

    /**
     * contains a codec specific string (currently taken from the SDP fmtp attribute)
     */
    private String fmtp;
}

