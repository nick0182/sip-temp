package com.nice.sipservertest.dto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SdpMediaMappingFactory {

    // TODO - may need to update this regex with the characters for the media codec name
    public final static Pattern rtpmapRegex = Pattern.compile("a=rtpmap\\:([0-9]+)[ \\t]+([a-zA-Z0-9\\-]+)\\/([0-9]+)(\\/([0-9]*))?", Pattern.CASE_INSENSITIVE);

    // regex used to parse the a=fmtp: SDP mapping option
    public final static Pattern fmtpRegex = Pattern.compile("a=fmtp\\:([0-9]+)[ \\t]+(.*)", Pattern.CASE_INSENSITIVE);


    /**
     *  parse the 'a=rtpmap:' attribute into current object - pass full line starting with a=
     * @param rtpMapString string to parse
     * @return true if successful, false otherwise
     */
    public static boolean tryParseRtpMapIntoCurrent(SdpMediaMapping mediaMapping, String rtpMapString)
    {
        Matcher m = rtpmapRegex.matcher(rtpMapString);

        if (!m.find()) return false;

        try
        {
            mediaMapping.setRtpPayloadType(Integer.parseInt(m.group(1)));
        }
        catch(NumberFormatException e)
        {
            return false;
        }

        mediaMapping.setMediaEncodingString(m.group(2));
        mediaMapping.setMediaCodec(SdpMediaCodecs.getSdpMediaCodecFromSdpName(mediaMapping.getMediaEncodingString()));

        try
        {
            mediaMapping.setSamplingFrequency(Integer.parseInt(m.group(3)));
        }
        catch(NumberFormatException e)
        {
            return false;
        }

        try
        {
            mediaMapping.setChannels(Integer.parseInt(m.group(5)));
        }
        catch(NumberFormatException e)
        {
            mediaMapping.setChannels(1);
        }

        return true;
    }

    /**
     * parse the 'a=fmtp:' attribute into current object - pass full line starting with a=
     * @param fmtpString string to parse
     * @return true if successful, false otherwise
     */
    public static boolean tryParseFmtpIntoCurrent(SdpMediaMapping mediaMapping, String fmtpString)
    {
        Matcher m = fmtpRegex.matcher(fmtpString);

        if (!m.find() || m.groupCount() > 3) return false;


        int pt = -1;

        try
        {
            pt = Integer.parseInt(m.group(1));

            if (mediaMapping.getMediaCodec() == SdpMediaCodecs.UNKNOWN &&
                    SdpMediaCodecs.getSdpMediaCodecFromPayloadNumber(pt) != SdpMediaCodecs.UNKNOWN &&
                    SdpMediaCodecs.getSdpMediaCodecFromPayloadNumber(pt) != SdpMediaCodecs.DYNAMIC)
            {
                mediaMapping.setRtpPayloadType(pt);
                mediaMapping.setMediaCodec(SdpMediaCodecs.getSdpMediaCodecFromPayloadNumber(pt));
            }
        }
        catch(NumberFormatException e)
        {
            return false;
        }

        if (pt != mediaMapping.getRtpPayloadType()) return false;

        mediaMapping.setGenericFormatSpecificParameters(m.group(2));

        return true;
    }
}
