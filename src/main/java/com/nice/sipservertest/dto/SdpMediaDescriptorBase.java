package com.nice.sipservertest.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Getter
@Setter
public class SdpMediaDescriptorBase {

    /// <summary>
    /// SDP label attribute - see RFC 4574
    /// default is none
    /// </summary>
    public String label;

    /// <summary>
    /// SDP media description title - see RFC 4566 section 5.4
    /// default is none
    /// </summary>
    protected String mediaTitle;

    /// <summary>
    /// type of media - see RFC 4566 section 5.14
    /// default is audio
    /// </summary>
    protected SdpMediaTypes mediaType;

    /// <summary>
    /// transport profile to use for the media - see RFC 4566 section 5.14
    /// default is RTP/AVP
    /// </summary>
    public MediaTransportProfile transportProfile;

    @Getter(AccessLevel.NONE)
    public String transportProtocolString;

    public String getTransportProtocolString()
    {
        if (transportProfile == MediaTransportProfile.UNKNOWN)
        {
            return transportProtocolString;
        }
        else
        {
            return MediaTransportProfile.getMediaTransportProfileString(transportProfile);
        }
    }

    /// <summary>
    /// direction of media flow - see RFC 4566 section 6
    /// default is SendReceive
    /// for SdpMediaDescriptorComplete this is the answer nodes value
    /// </summary>
    public SdpMediaDirection direction;

    /// <summary>
    /// SDP media session description (optional) - free form description of media - see RFC 4566 section 5.4
    /// </summary>
    public String mediaDescriptorInformation;

    /// <summary>
    /// length of media in packet in milliseconds ('ptime:' attribute) - see RFC 4566 section 6
    /// default is none and value should be taken as a recommendation
    /// </summary>
    public int packetTimeInMs;

    /// <summary>
    /// maximum length of media in packet in milliseconds ('maxptime:' attribute) - see RFC 4566 section 6
    /// default is none and value should be taken as a recommendation
    /// </summary>
    public int maxPacketTimeInMs;

    /// <summary>
    /// whiteboard or video orientation ('orient:' attribute) - see RFC 4566 section 6
    /// default is none
    /// </summary>
    public SdpMediaOrientation orientation;

    /// <summary>
    /// provides the maximum video frame rate in attribute 'framerate:' - see RFC 4566 section 6
    /// default is none (0)
    /// </summary>
    public double framerate;

    /// <summary>
    /// quality suggestion from 0 for worst to 10 for best in attribute 'quality:' - see RFC 4566 section 6
    /// default is none (-1)
    /// </summary>
    public int quality;

    /// <summary>
    /// SDP media description media mappings (codecs) in order of preference
    /// </summary>
    @Setter(AccessLevel.NONE)
    public Map<Integer, SdpMediaMapping> mediaMappings;

    // TODO - port this if there is time
    /// <summary>
    /// SDP crypto attributes, key is crypto map index number (since an offer can contain multiple crypto offers)
    /// </summary>
    //@Setter(AccessLevel.NONE)
    //private Map<Integer, SdpMediaCrypto> mediaCrypto;

    @Setter(AccessLevel.NONE)
    public List<Integer> rtpPayloadTypeList;


    public SdpMediaDescriptorBase()
    {
        label = null;
        mediaType = SdpMediaTypes.AUDIO;
        transportProfile = MediaTransportProfile.RTP_AVP;
        direction = SdpMediaDirection.UNSPECIFIED;
        packetTimeInMs = 0;
        maxPacketTimeInMs = 0;
        orientation = SdpMediaOrientation.NONE;
        framerate = 0;
        quality = -1;

        rtpPayloadTypeList = new ArrayList<>();
        mediaMappings = new HashMap<>();
        // mediaCrypto = new Dictionary<int, SdpMediaCrypto>();
    }

    /**
     * add a media codec mapping to the media descriptor
     * @param mapping codec mapping to add
     */
    public SdpMediaDescriptorBase addMediaMappingCodec(SdpMediaMapping mapping)
    {
        mediaMappings.put(mapping.getRtpPayloadType(), mapping);
        rtpPayloadTypeList.add(mapping.getRtpPayloadType());
        return this;
    }

    /**
     * add a media codec mapping to the media descriptor
     * @param codec codec to add (everything else is defaulted)
     */
    public SdpMediaDescriptorBase addMediaMappingCodec(SdpMediaCodecs codec)
    {
        SdpMediaMapping mapping = new SdpMediaMapping(codec);
        addMediaMappingCodec(mapping);
        return this;
    }

    /**
     * add a media codec mapping to the media descriptor
     * @param rtpPayloadTypeNumber RTP payload type number
     * @param codec codec for this codec mapping
     */
    public SdpMediaDescriptorBase addMediaMappingCodec(int rtpPayloadTypeNumber, SdpMediaCodecs codec)
    {
        addMediaMappingCodec( new SdpMediaMapping(codec, rtpPayloadTypeNumber));
        return this;
    }

    /**
     * add a media codec mapping to the media descriptor
     * @param codec codec for this codec mapping
     * @param fmtpString fmtp string specific for this particular codec (see RFCs)
     */
    public SdpMediaDescriptorBase addMediaMappingCodec(SdpMediaCodecs codec, String fmtpString)
    {
        SdpMediaMapping mapping = new SdpMediaMapping(codec);
        mapping.setGenericFormatSpecificParameters(fmtpString);
        addMediaMappingCodec(mapping);

        return this;
    }

    /**
     * add a media codec mapping to the media descriptor
     * @param rtpPayloadTypeNumber RTP payload type number
     * @param codec codec for this codec mapping
     * @param fmtpString fmtp string specific for this particular codec (see RFCs)
     */
    public SdpMediaDescriptorBase addMediaMappingCodec(int rtpPayloadTypeNumber, SdpMediaCodecs codec, String fmtpString)
    {
        SdpMediaMapping mapping = new SdpMediaMapping(codec, rtpPayloadTypeNumber);
        mapping.setGenericFormatSpecificParameters(fmtpString);
        addMediaMappingCodec(mapping);

        return this;
    }

    protected String getMediaFormatString() throws SdpParseException
    {
        switch (transportProfile)
        {
            case RTP_AVP:
            case RTP_SAVP:
                return getListOfRtpPayloadNumbers();

            case UDP:
                return ""; // TODO - need to look into the fmt string for UDP transport instead of UDP/RTP (although this is probably never used in voip telephony)
        }

        throw new SdpParseException(SdpParserErrorTypes.INVALID_TRANSPORT_PROFILE,
                "Unknown SDP transport protocol enum - internal error: check that added protocol is fully implemented");
    }

    private String getListOfRtpPayloadNumbers()
    {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (int t : rtpPayloadTypeList)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(' ');
            }

            sb.append(t);
        }

        return sb.toString();
    }

}
