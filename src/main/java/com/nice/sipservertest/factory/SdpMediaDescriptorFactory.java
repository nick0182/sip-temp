package com.nice.sipservertest.factory;

import com.nice.sipservertest.dto.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SdpMediaDescriptorFactory {

    public final static String SDP_ATTRIB_RTPMAP = "rtpmap";

    public final static String SDP_ATTRIB_FMTP = "fmtp";

    public final static String SDP_ATTRIB_LABEL = "label";

    public final static String SDP_ATTRIB_PTIME = "ptime";

    public final static String SDP_ATTRIB_SENDONLY = "sendonly";

    public final static String SDP_ATTRIB_RECVONLY = "recvonly";

    public final static String SDP_ATTRIB_SENDRECV = "sendrecv";

    public final static String SDP_ATTRIB_INACTIVE = "inactive";

    public final static String SDP_ATTRIB_ORIENT = "orient";

    public final static String SDP_ATTRIB_CONFERENCE_TYPE = "type";

    public final static String SDP_ATTRIB_FRAMERATE = "framerate";

    public final static String SDP_ATTRIB_QUALITY = "quality";

    public final static String SDP_ATTRIB_RTCP = "rtcp";

    public final static String SDP_ATTRIB_CRYPTO = "crypto";

    public final static Pattern sdpConnectLinePattern = Pattern.compile("^IN[ \\t]+(IP4|IP6)[ \\t]+(.+)$");

    public final static Pattern mediaLinePattern = Pattern.compile("^m=([a-zA-Z0-9]+)[ \\t]+([0-9]+)[ \\t]+([a-zA-Z0-9/\\-]+)[ \\t]+(.*)$");

    public final static Pattern attribFlagLinePattern = Pattern.compile("^a=([a-zA-Z0-9\\-]+)$");

    public final static Pattern attribLinePattern = Pattern.compile("^a=([a-zA-Z0-9\\-]+):(.*)");

    public final static Pattern attribMappingLinePattern = Pattern.compile("^a=(rtpmap|fmtp):([0-9\\-]+)");

    public final static Pattern attribRtcp = Pattern.compile("^([0-9]+)(?:[ \\t]+IN[ \\t]+(IP4|IP6)[ \\t]+([A-F0-9.:\\-]+))?$");

    public final static Pattern sdpLinePattern = Pattern.compile("^([^=])=(.+)$");


    /// <summary>
    /// parse a SDP media mapping attribute set from a list of line strings
    /// </summary>
    /// <param name="parentMessage">parent <c>SdpMessage</c> that owns this <c>SdpMessageMedia</c> object</param>
    /// <param name="message">multiple lines starting with the m= at message[0] and ending with the last a= line for one media mapping</param>
    /// <param name="sdpMedia"></param>
    /// <returns></returns>
    public static SdpMediaDescriptor parse(List<String> message, InetAddress connectionAddress)
    {
        SdpMediaDescriptor sdpMedia = new SdpMediaDescriptor();

        // parse the m= line
        Matcher m = mediaLinePattern.matcher(message.get(0));
        if (!m.find() || m.groupCount() > 4 || m.groupCount() < 3) return null;

        int rtpEndpointPort;
        try
        {
            rtpEndpointPort = Integer.parseInt(m.group(2));
        }
        catch(Exception e)
        {
            return null;
        }

        sdpMedia.transportProfile = MediaTransportProfile.sdpTransportStringToTransportEnum(m.group(3));
        sdpMedia.transportProtocolString = m.group(3);

        // get the RTP payload type codes from the m= line
        if (m.groupCount() == 4)
        {
            // we have an RTP payload type list
            String[] rtpPtListStrings = m.group(4).split("\\s", 0);

            for(String rtpPtString : rtpPtListStrings)
            {
                try
                {
                    sdpMedia.rtpPayloadTypeList.add(Integer.parseInt(rtpPtString));
                }
                catch(Exception e)
                {
                }
            }
        }

        // walk the lines of the media description body (skip the first m= line)
        for(String msg : message.subList(1, message.size()))
        {
            // determine if this is an rtpmap or fmtp attribute, and if so, process it
            Matcher ma = attribMappingLinePattern.matcher(msg);

            if (ma.find())
            {
                String ptString = ma.group(2);
                int pt;
                try
                {
                    pt = Integer.parseInt(ptString);
                }
                catch(Exception e)
                {
                    continue;
                }

                SdpMediaMapping mm = sdpMedia.mediaMappings.get(pt);
                if (mm == null)
                {
                    mm = new SdpMediaMapping();
                    sdpMedia.mediaMappings.put(pt, mm);
                }

                if (ma.group(1).equalsIgnoreCase(SDP_ATTRIB_RTPMAP))
                {
                    SdpMediaMappingFactory.tryParseRtpMapIntoCurrent(mm, msg);
                }
                else
                {
                    SdpMediaMappingFactory.tryParseFmtpIntoCurrent(mm, msg);
                }

                continue;
            }

            // match for non-rtpmap/fmtp flag-type attributes
            ma = attribFlagLinePattern.matcher(msg);

            if (ma.find() && ma.groupCount() > 0)
            {
                String attribCmd = ma.group(1);

                // this is a valid a= flag-type attribute line so process it
                // note:  RFC 4566 doesn't seem to specify whether or not the attribute name is case-sensitive so going with the most general case
                if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_SENDONLY))
                {
                    sdpMedia.direction = SdpMediaDirection.SEND_ONLY;
                }
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_RECVONLY))
                {
                    sdpMedia.direction = SdpMediaDirection.RECEIVE_ONLY;
                }
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_SENDRECV))
                {
                    sdpMedia.direction = SdpMediaDirection.SEND_RECEIVE;
                }
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_INACTIVE))
                {
                    sdpMedia.direction = SdpMediaDirection.INACTIVE;
                }

                continue;
            }

            // match for non-rtpmap/fmtp value-type attributes
            ma = attribLinePattern.matcher(msg);

            if (ma.find() && ma.groupCount() > 1)
            {
                String attribCmd = ma.group(1);

                // this is a valid a= attribute line so process it
                // note:  RFC 4566 seems to suggest that the token is case-sensitive, but going to take the more cautious route
                // and process as case-insensitive (I can't imagine the IETF would assign another key using case to differentiate them)
                if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_LABEL))
                {
                    sdpMedia.label = ma.group(2);
                }
                // TODO - removing this for now - may add crypto attributes back in if time permits
                /*
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_CRYPTO))
                {
                    if (sdpMediaCrypto.TryParse(ma.Groups[2].Value, out SdpMediaCrypto mcrypto))
                    {
                        sdpMedia.MediaCrypto.Add(mcrypto.Tag, mcrypto);
                    }
                }
                */
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_PTIME))
                {
                    try
                    {
                        sdpMedia.packetTimeInMs = Integer.parseInt(ma.group(2));
                    }
                    catch(Exception e)
                    {
                        // if it fails to parse, there's not much that can be done
                    }
                }
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_ORIENT))
                {
                    switch(ma.group(2).toLowerCase())
                    {
                        case "landscape":
                            sdpMedia.orientation = SdpMediaOrientation.LANDSCAPE;
                            break;

                        case "portrait":
                            sdpMedia.orientation = SdpMediaOrientation.PORTRAIT;
                            break;

                        case "seascape":
                            sdpMedia.orientation = SdpMediaOrientation.SEASCAPE;
                            break;
                    }
                }
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_RTCP))
                {
                    // a=rtcp: attribute
                    Matcher mrtcp = attribRtcp.matcher(ma.group(2));
                    if (mrtcp.find())
                    {
                        InetAddress rtcpIp;
                        InetAddress rtpAddr = (sdpMedia.getRtpEndpoint() == null) ? connectionAddress : sdpMedia.getRtpEndpoint().getAddress();
                        if (mrtcp.groupCount() >= 3)
                        {
                            try
                            {
                                rtcpIp = Inet4Address.getByName(mrtcp.group(3));
                            }
                            catch(Exception e)
                            {
                                rtcpIp = rtpAddr;
                            }
                        }
                        else
                        {
                            rtcpIp = rtpAddr;
                        }

                        if (rtcpIp != null)
                        {
                            try
                            {
                                sdpMedia.setRtcpEndpoint(new InetSocketAddress(rtcpIp, Integer.parseInt(mrtcp.group(1))));
                            }
                            catch(Exception e)
                            {
                            }
                        }
                    }
                }
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_FRAMERATE))
                {
                    try
                    {
                        sdpMedia.framerate = Integer.parseInt(ma.group(2));
                    }
                    catch(Exception e)
                    {
                    }
                }
                else if (attribCmd.equalsIgnoreCase(SDP_ATTRIB_QUALITY))
                {
                    try
                    {
                        int x = Integer.parseInt(ma.group(2));
                        if (x >= 0 && x <= 10) sdpMedia.quality = x;
                    }
                    catch(Exception e)
                    {
                    }
                }

                continue;
            }

            // look for other non-attribute lines
            Matcher mc = sdpLinePattern.matcher(msg);

            if (mc.find() && mc.groupCount() >= 2)
            {
                switch(mc.group(1).charAt(0))
                {
                    case 'c':
                        InetAddress cip = parseSdpConnectionString(mc.group(2));
                        if (cip != null)
                        {
                            sdpMedia.setRtpEndpoint(new InetSocketAddress(cip, rtpEndpointPort));
                        }
                        break;

                    case 'i':
                        sdpMedia.mediaDescriptorInformation = mc.group(2);
                        break;
                }
            }
        }

        // take care of the RTP endpoint if it wasn't already by a media-level "c=" line
        if (sdpMedia.getRtpEndpoint() == null && connectionAddress != null)
        {
            sdpMedia.setRtpEndpoint(new InetSocketAddress(connectionAddress, rtpEndpointPort));
        }

        // check the rtp payload list from the m= line and create defaults for any that are not specified in the message
        for(int pt : sdpMedia.rtpPayloadTypeList)
        {
            if (sdpMedia.mediaMappings.get(pt) == null)
            {
                // no info provided for this RTP payload type in the message so we'll create a default if possible
                switch(pt)
                {
                    case 0:     // PCMU
                        sdpMedia.mediaMappings.put(pt, new SdpMediaMapping(SdpMediaCodecs.PCMU));
                        break;

                    case 4:     // G.723
                        sdpMedia.mediaMappings.put(pt, new SdpMediaMapping(SdpMediaCodecs.G723));
                        break;

                    case 8:     // PCMA
                        sdpMedia.mediaMappings.put(pt, new SdpMediaMapping(SdpMediaCodecs.PCMA));
                        break;

                    case 18:    // G.729
                        sdpMedia.mediaMappings.put(pt, new SdpMediaMapping(SdpMediaCodecs.G729));
                        break;

                    case 101:   // telephone-event (DTMF)
                        sdpMedia.mediaMappings.put(pt, new SdpMediaMapping(SdpMediaCodecs.TELEPHONE_EVENT));
                        break;

                    default:    // an unknown encoding format
                        sdpMedia.mediaMappings.put(pt, new SdpMediaMapping(SdpMediaCodecs.UNKNOWN, pt));
                        break;
                }
            }
        }

        return sdpMedia;
    }

    public static InetAddress parseSdpConnectionString(String messageLineBody)
    {
        Matcher m = sdpConnectLinePattern.matcher(messageLineBody);

        if (!m.find() || m.groupCount() < 2)
        {
            return null;
        }

        InetAddress ip;
        try {

            ip = Inet4Address.getByName(m.group(2));
        } catch(Exception e) {

            return null;
        }

        return ip;
    }
}
