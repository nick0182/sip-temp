package com.nice.sipservertest.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

@Getter
@Setter
public class SdpMediaDescriptor extends SdpMediaDescriptorBase {

    public final static InetAddress allZerosIpAddress;

    static {
        try {
            allZerosIpAddress = InetAddress.getByName("0.0.0.0");
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /// <summary>
    /// transport RTP endpoint (address/port) number used for this media
    /// see RFC 4566 section 5.7, 5.14, and RFC 3605
    /// </summary>
    private InetSocketAddress rtpEndpoint;

    /// <summary>
    /// transport RTCP endpoint (address/port) number used for this media
    /// see RFC 3605
    /// </summary>
    @Getter
    @Setter
    private InetSocketAddress rtcpEndpoint;

    public InetSocketAddress getRtcpEndpoint()
    {
        if (rtcpEndpoint == null && rtpEndpoint != null)
        {
            rtcpEndpoint = new InetSocketAddress(rtpEndpoint.getAddress(), rtpEndpoint.getPort() + 1);
        }

        return rtcpEndpoint;
    }

    public boolean getIsValid()
    {
        return rtpEndpoint != null && (transportProfile != MediaTransportProfile.UNKNOWN || transportProtocolString != null);
    }


    public SdpMediaDescriptor()
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
    }

    /// <summary>
    /// get an encoded version of the message as a string in UTF-8 format
    /// </summary>
    public void appendEncodedMessage(StringBuilder sb, InetAddress connectionAddress, SdpMediaDirection direction)
            throws SdpAssemblyException
    {
        // append the m= line
        int endpointPort = 0;
        if (rtpEndpoint != null) endpointPort = rtpEndpoint.getPort();
        String tp = transportProtocolString;
        if (transportProfile != MediaTransportProfile.UNKNOWN) {

            tp = MediaTransportProfile.getMediaTransportProfileString(transportProfile);
        }
        sb.append("m=").append(mediaType.toString().toLowerCase()).append(" ").
                append(endpointPort).append(" ").append(tp).append(" ").append(getMediaFormatString()).append(SdpMessage.SDP_EOL);

        // append the i= line if it exists
        if (mediaTitle != null)
        {
            sb.append("i=").append(mediaTitle).append(SdpMessage.SDP_EOL);
        }

        // determine if we need a c= line (has a different IP address than the session level connection address)
        if (rtpEndpoint != null && rtpEndpoint.getPort() != 0 && rtpEndpoint.getAddress() != null &&
                !connectionAddress.equals(rtpEndpoint.getAddress()))
        {
            sb.append("c=").append(getSdpConnectionString(rtpEndpoint.getAddress())).append(SdpMessage.SDP_EOL);
        }

        // send/receive/inactive attribute
        SdpMediaDirection dir = this.direction;
        if (rtpEndpoint == null || rtpEndpoint.getAddress() == null ||
                rtpEndpoint.getAddress().equals(allZerosIpAddress) || rtpEndpoint.getPort() == 0)
        {
            dir = SdpMediaDirection.INACTIVE;
        }
        else if (dir == SdpMediaDirection.UNSPECIFIED && direction != SdpMediaDirection.UNSPECIFIED)
        {
            // media descriptor direction is unspecified but parent message is specified so use that
            dir = direction;
        }
        else if (dir == SdpMediaDirection.UNSPECIFIED)
        {
            // no direction specified in media descriptor or parent message so use default
            dir = SdpMediaDirection.SEND_RECEIVE;
        }

        switch(dir)
        {
            case INACTIVE:
                sb.append("a=inactive").append(SdpMessage.SDP_EOL);
                break;

            case RECEIVE_ONLY:
                sb.append("a=recvonly").append(SdpMessage.SDP_EOL);
                break;

            case SEND_ONLY:
                sb.append("a=sendonly").append(SdpMessage.SDP_EOL);
                break;

            case SEND_RECEIVE:
                sb.append("a=sendrecv").append(SdpMessage.SDP_EOL);
                break;
        }

        // a=label: attribute
        if (label != null && !label.isEmpty())
        {
            sb.append("a=label:").append(label).append(SdpMessage.SDP_EOL);
        }

        // add attribute lines
        // a=ptime:
        if (packetTimeInMs != 0)
        {
            sb.append("a=ptime:").append(packetTimeInMs).append(SdpMessage.SDP_EOL);
        }

        // a=maxptime:
        if (maxPacketTimeInMs != 0)
        {
            sb.append("a=maxptime:").append(maxPacketTimeInMs).append(SdpMessage.SDP_EOL);
        }

        // a=orient: attribute
        if (orientation != SdpMediaOrientation.NONE)
        {
            switch(orientation)
            {
                case LANDSCAPE:
                    sb.append("a=orient:landscape").append(SdpMessage.SDP_EOL);
                    break;

                case PORTRAIT:
                    sb.append("a=orient:portrait").append(SdpMessage.SDP_EOL);
                    break;

                case SEASCAPE:
                    sb.append("a=orient:seascape").append(SdpMessage.SDP_EOL);
                    break;
            }
        }

        // a=framerate: attribute
        if (framerate != 0)
        {
            sb.append("a=framerate:").append(framerate).append(SdpMessage.SDP_EOL);
        }

        // a=quality: attribute
        if (quality >= 0)
        {
            sb.append("a=quality:").append(quality).append(SdpMessage.SDP_EOL);
        }

        // a=rtcp: attribute from RFC 3605
        boolean rtcpAddressesEqual = (rtpEndpoint == null && rtcpEndpoint == null) ||
                (rtcpEndpoint != null && rtcpEndpoint.getAddress() != null && rtpEndpoint != null &&
                        rtcpEndpoint.getAddress().equals(rtpEndpoint.getAddress()));
        if (rtcpEndpoint != null && rtcpEndpoint.getAddress() != null && (!rtcpAddressesEqual || rtcpEndpoint.getPort() != rtpEndpoint.getPort() + 1))
        {
            if (rtcpAddressesEqual)
            {
                sb.append("a=rtcp:").append(rtcpEndpoint.getPort()).append(SdpMessage.SDP_EOL);
            }
            else
            {
                String pv = (rtcpEndpoint.getAddress().getAddress().length > 4) ? "IP6 " : "IP4 ";
                sb.append("a=rtcp:").append(rtcpEndpoint.getPort()).append(" IN ").append(pv).append(rtcpEndpoint.getAddress()).append(SdpMessage.SDP_EOL);
            }
        }

        // TODO - add crypto stuff back in if time permits
        /*
        // a=crypto: attributes from RFC 4568
        foreach(var kvp in MediaCrypto)
        {
            kvp.Value.AppendToStringBuilder(sb);
            sb.Append(SdpMessage.SdpEOL);
        }
         */

        // walk through the a=rtpmap: and associated attributes (a=fmtp:)
        for (Map.Entry<Integer, SdpMediaMapping> kvp : mediaMappings.entrySet())
        {
            SdpMediaMapping mm = kvp.getValue();

            mm.appendEncodedMessage(sb);
        }
    }



    public void setRtpTransportPort(int port) {

        setRtpTransportPort(port, null);
    }

    public void setRtpTransportPort(int port, InetAddress connectionAddress)
    {
        if (rtpEndpoint != null)
        {
            rtpEndpoint = new InetSocketAddress(rtpEndpoint.getAddress(), port);
        }
        else if (connectionAddress != null)
        {
            rtpEndpoint = new InetSocketAddress(connectionAddress, port);
        }
        else
        {
            rtpEndpoint = new InetSocketAddress(port);
        }
    }

    public void setRtpTransportIp(InetAddress address)
    {
        if (rtpEndpoint != null)
        {
            rtpEndpoint = new InetSocketAddress(address, rtpEndpoint.getPort());
        }
        else
        {
            rtpEndpoint = new InetSocketAddress(address, 0);
        }
    }

    public void setRtcpTransportPort(int port)
    {
        setRtcpTransportPort(port, null);
    }

    public void setRtcpTransportPort(int port, InetAddress sessionConnectionAddress)
    {
        if (rtcpEndpoint != null && rtcpEndpoint.getAddress() != null)
        {
            // we have a valid RTCP address already
            rtcpEndpoint = new InetSocketAddress(rtcpEndpoint.getAddress(), port);
        }
        else if (rtpEndpoint != null && rtpEndpoint.getAddress() != null)
        {
            // we have a valid RTP endpoint
            rtcpEndpoint = new InetSocketAddress(rtpEndpoint.getAddress(), port);
        }
        else if (sessionConnectionAddress != null)
        {
            // we have a valid session level address
            rtcpEndpoint = new InetSocketAddress(sessionConnectionAddress, port);
        }
        else
        {
            // we don't have any way to provide a valid RTCP endpoint IP address so set to zeros for now
            rtcpEndpoint = new InetSocketAddress(allZerosIpAddress, port);
        }
    }

    public void setRtcpTransportIp(InetAddress address)
    {
        if (rtcpEndpoint == null && rtpEndpoint != null)
        {
            rtcpEndpoint = new InetSocketAddress(rtpEndpoint.getAddress(), rtpEndpoint.getPort() + 1);
        }

        if (rtcpEndpoint != null)
        {
            rtcpEndpoint = new InetSocketAddress(address, rtcpEndpoint.getPort());
        }
        else
        {
            rtcpEndpoint = new InetSocketAddress(address, 0);
        }
    }

    public static String getSdpConnectionString(InetAddress connectionAddress) throws SdpAssemblyException
    {
        // TODO - connection string can use a FQDN so we should probably add support for this

        if (connectionAddress.getAddress().length == 4)
        {
            return "IN IP4 " + connectionAddress.getHostAddress();
        }
        else if (connectionAddress.getAddress().length == 16)
        {
            return "IN IP6 " + connectionAddress.getHostAddress();
        }
        else
        {
            throw new SdpAssemblyException(SdpAssemblyErrorTypes.UNSUPPORTED_INET_TYPE,
                    "Current SIP/SDP framework only supports IPv4 or IPv6 addresses in SDP connection lines");
        }
    }
}
