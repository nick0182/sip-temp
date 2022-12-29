package com.nice.sipservertest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * represents a media descriptor for an RTP session (one channel of an audio signal)
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MediaChannel {

    /**
     * the state of this media session
     * Unused - disabled and will not be used in the future
     * Inactive - not currently active but can be made active if needed
     * Active - currently active media descriptor (RTP should be or will be flowing)
     */
    @Setter
    private MediaChannelStates mediaSessionState;

    /**
     * media descriptor type such as audio, video, etc.
     */
    private MediaChannelTypes mediaChannelType;

    /**
     * endpoint (IP and port) for RTP
     */
    private InetSocketAddress rtpEndpoint;

    /**
     * endpoint (IP and port) for RTCP
     */
    private InetSocketAddress rtcpEndpoint;

    /**
     * list of media codecs supported
     */
    private List<MediaCodec> mediaCodecs;
}
