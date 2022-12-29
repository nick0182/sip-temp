package com.nice.sipservertest.dto;

public enum MediaTransportProfile {

    UNKNOWN,

    UDP,

    RTP_AVP,

    RTP_SAVP;


    public static MediaTransportProfile sdpTransportStringToTransportEnum(String protocolString)
    {
        if (protocolString.equalsIgnoreCase("RTP/AVP"))
        {
            return MediaTransportProfile.RTP_AVP;
        }
        else if (protocolString.equalsIgnoreCase("RTP/SAVP"))
        {
            return MediaTransportProfile.RTP_SAVP;
        }
        else if (protocolString.equalsIgnoreCase("udp"))
        {
            return MediaTransportProfile.UDP;
        }
        else
        {
            return MediaTransportProfile.UNKNOWN;
        }
    }

    public static String getMediaTransportProfileString(MediaTransportProfile mediaTransportProfile)
    {
        switch (mediaTransportProfile)
        {
            case RTP_AVP:
                return "RTP/AVP";

            case RTP_SAVP:
                return "RTP/SAVP";

            case UDP:
                return "UDP";
        }

        throw new SdpParseException(SdpParserErrorTypes.INVALID_TRANSPORT_PROTOCOL,
                "Unknown SDP transport protocol enum - internal error: check that added protocol is fully implemented");
    }
}
