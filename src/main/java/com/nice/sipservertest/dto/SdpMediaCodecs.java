package com.nice.sipservertest.dto;

public enum SdpMediaCodecs
{
    PCMU(0)
            {
                @Override
                public String getSdpCodecStringIdentifier()
                {
                    return "PCMU";
                }
            },

    G723(4)
            {
                @Override
                public String getSdpCodecStringIdentifier()
                {
                    return "G723";
                }
            },

    PCMA(8)
            {
                @Override
                public String getSdpCodecStringIdentifier()
                {
                    return "PCMA";
                }
            },

    G729(18)
            {
                @Override
                public String getSdpCodecStringIdentifier()
                {
                    return "G729";
                }
            },

    UNKNOWN(256),

    DYNAMIC(SdpMediaCodecs.DYNAMIC_BASE),

    TELEPHONE_EVENT(SdpMediaCodecs.DYNAMIC_BASE + 1)
            {
                @Override
                public String getSdpCodecStringIdentifier()
                {
                    return "telephone-event";
                }
            };


    private final int rtpPayloadNumber;

    private static final int DYNAMIC_BASE = 1000;


    SdpMediaCodecs(int rtpPayloadNumber)
    {
        this.rtpPayloadNumber = rtpPayloadNumber;
    }

    public int getStandardRtpPayloadNumber()
    {
        return rtpPayloadNumber;
    }

    public boolean isStaticPayloadType()
    {
        return rtpPayloadNumber < DYNAMIC_BASE;
    }

    public String getSdpCodecStringIdentifier()
    {
        // this is typically overloaded above, so we'll just return unknown here
        return "UNKNOWN";
    }

    public static SdpMediaCodecs getSdpMediaCodecFromSdpName(String codecName)
    {
        SdpMediaCodecs retVal;

        if (codecName == null) return null;

        if (codecName.equalsIgnoreCase("PCMU"))
        {
            retVal = PCMU;
        }
        else if (codecName.equalsIgnoreCase("G723"))
        {
            retVal = G723;
        }
        else if (codecName.equalsIgnoreCase("PCMA"))
        {
            retVal = PCMA;
        }
        else if (codecName.equalsIgnoreCase("G729"))
        {
            retVal = G729;
        }
        else if (codecName.equalsIgnoreCase("telephone-event"))
        {
            retVal = TELEPHONE_EVENT;
        }
        else
        {
            retVal = UNKNOWN;
        }

        return retVal;
    }

    public static SdpMediaCodecs getSdpMediaCodecFromPayloadNumber(int standardPayloadNumber)
    {
        SdpMediaCodecs retVal;

        if (standardPayloadNumber >= 96 && standardPayloadNumber <= 127)
        {
            return DYNAMIC;
        }

        switch(standardPayloadNumber)
        {
            case 0:
                retVal = PCMU;
                break;

            case 4:
                retVal = G723;
                break;

            case 8:
                retVal = PCMA;
                break;

            case 18:
                retVal = G729;
                break;

            default:
                retVal = UNKNOWN;
        }

        return retVal;
    }
}
