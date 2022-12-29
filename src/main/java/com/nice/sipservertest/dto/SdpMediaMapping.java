package com.nice.sipservertest.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


/**
 * represents a single SDP media codec mapping (RTP payload to codec name) in a single SDP media descriptor
 * includes RTP payload number, codec name, channels (for audio), sampling frequency, and generic codec format information
 */
@Getter
public class SdpMediaMapping
{
    /**
     * SDP codec/payload type
     */
    @Setter(AccessLevel.PACKAGE)
    private SdpMediaCodecs mediaCodec;

    /**
     * original SDP payload type string in case it's an unknown type
     */
    @Setter(AccessLevel.PACKAGE)
    private String mediaEncodingString;

    /**
     * RTP payload type number used in RTP session (may be static or dynamic type number)
     */
    @Setter(AccessLevel.PACKAGE)
    private int rtpPayloadType;

    /**
     * sampling frequency typically limited to audio media
     * default: 8000
     */
    @Setter(AccessLevel.PACKAGE)
    private int samplingFrequency;

    /**
     * number of media channels
     * default: 1
     */
    @Setter(AccessLevel.PACKAGE)
    private int channels;

    /**
     * the media format specific parameters in a generic string
     */
    @Setter(AccessLevel.PACKAGE)
    private String genericFormatSpecificParameters;


    public SdpMediaMapping()
    {
        initialize();
    }

    public SdpMediaMapping(SdpMediaCodecs payloadType)
    {
        initialize();

        mediaCodec = payloadType;

        if (payloadType == SdpMediaCodecs.UNKNOWN)
        {
            // just set some needed parameters for the unknown type
            mediaEncodingString = null;
        }
        else
        {
            mediaEncodingString = payloadType.getSdpCodecStringIdentifier();
        }

        rtpPayloadType =  payloadType.getStandardRtpPayloadNumber();
    }

    public SdpMediaMapping(SdpMediaCodecs payloadType, int rtpPayloadTypeNumber)
    {
        initialize();

        mediaCodec = payloadType;

        if (payloadType == SdpMediaCodecs.UNKNOWN)
        {
            // just set some needed parameters for the unknown type
            mediaEncodingString = null;
        }
        else
        {
            mediaEncodingString = SdpMediaCodecs.getSdpMediaCodecFromPayloadNumber(rtpPayloadTypeNumber).
                    getSdpCodecStringIdentifier();
        }

        rtpPayloadType =  rtpPayloadTypeNumber;
    }

    public SdpMediaMapping(String payloadType, int rtpPayloadTypeNumber)
    {
        initialize();

        mediaCodec = SdpMediaCodecs.getSdpMediaCodecFromSdpName(payloadType);
        mediaEncodingString = payloadType;

        rtpPayloadType = rtpPayloadTypeNumber;
    }

    private void initialize()
    {
        samplingFrequency = 8000;
        channels = 1;
        mediaCodec = SdpMediaCodecs.UNKNOWN;
    }

    public String getPayloadString()
    {
        if (mediaCodec == SdpMediaCodecs.UNKNOWN)
        {
            return mediaEncodingString;
        }

        return mediaCodec.getSdpCodecStringIdentifier();
    }

    public void appendEncodedMessage(StringBuilder sb)
    {
        // append the rtpmap attribute
        sb.append("a=rtpmap:").append(rtpPayloadType).append(" ");
        if (mediaCodec == SdpMediaCodecs.UNKNOWN)
            sb.append(mediaEncodingString);
        else
            sb.append(mediaCodec.getSdpCodecStringIdentifier());
        sb.append("/").append(samplingFrequency);
        if (channels != 1)
            sb.append("/").append(channels);
        sb.append("\r\n");

        // append the ftmp attribute
        appendFmtp(sb);
    }

    protected void appendFmtp(StringBuilder sb)
    {
        if (genericFormatSpecificParameters != null && !genericFormatSpecificParameters.isEmpty())
        {
            sb.append("a=fmtp:").append(rtpPayloadType).append(" ").append(genericFormatSpecificParameters).append("\r\n");
        }
    }
}
