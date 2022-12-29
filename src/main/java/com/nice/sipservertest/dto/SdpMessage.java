package com.nice.sipservertest.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// target support for RFCs 4566 (SDP), 3264 (offer/answer), 4574 (label attribute), 3605 (rtcp media attribute), and 4568 (crypto key exchange)
// NOTE:  SdpMessage does not currently support the following items
//   non-IP origin or connection fields since it's unlikely we'll need non-IP RTP
//   bandwidth (b=) support since it's not widely used and probably not needed for voip recording (although may need to be added)
//   timing (t=) support since this is only used for broadcast/multicast of scheduled media broadcasts
//   multiple contiguous origin/connection IP addresses for hierarchial or layered encoding schemes since it's unlikely it's used anywhere in voip telephony
//   time zones (z=) (RFC 4566 section 5.11) since it's very unlikely it'll be used
//   sdplang attribute (a=sdplang:) in session or media description since it's very unlikely this will be used
//   lang attribute (a=lang:) in session or media description since it's very unlikely this will be used
//   encryption keys (k=) (section 5.12) since it has been replaced by RFC 4568
//   keywords session attribute (a=keywrds:) since it is not used in voip telephony and is poorly documented in RFC 4566
//   multiple session descriptions per message since it's not really used in voip and is not allowed by RFC 3264 (offer/answer)

@Getter
@Setter
public class SdpMessage {

    public final static String SDP_EOL = "\r\n";


    /// <summary>
    /// SDP version (always zero for RFC 4566) - see RFC 4566 section 5.1
    /// </summary>
    @Setter(AccessLevel.PROTECTED)
    public int version;

    /// <summary>
    /// SDP origin user name - see RFC 4566 section 5.2
    /// default is to use a -
    /// </summary>
    public String originUsername;

    /// <summary>
    /// SDP origin globally unique session ID - see RFC 4566 section 5.2
    /// default is to use the system timestamp
    /// </summary>
    public long originSessionId;

    /// <summary>
    /// SDP origin session version - see RFC 4566 section 5.2
    /// default is to use the system timestamp
    /// </summary>
    public long originSessionVersion;

    /// <summary>
    /// SDP origin transport address type (required), typically IPv4 or IPv6 - see RFC 4566 section 5.2
    /// default is IPv4
    /// </summary>
    public SdpAddressTypes originAddressType;

    /// <summary>
    /// SDP origin host unicast address (required) - FQDN is preferred but IP address is ok - see RFC 4566 section 5.2
    /// </summary>
    public String originUnicastAddress;

    /// <summary>
    /// a globally unique string ID of an SDP session - see RFC 4566 section 5.2
    /// NOTE:  at the SDP session level only the remote session ID is used (at this level the local and remote session IDs do not match)
    /// </summary>
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    public String sessionIdGloballyUnique;

    public String getSessionIdGloballyUnique() {
        if (originUsername != null && originUnicastAddress != null) {
            return originUsername + "_" + originUnicastAddress + "_" + originSessionId;
        }
        else {
            return null;
        }
    }

    /// <summary>
    /// SDP session name (required) - see RFC 4566 section 5.3
    /// default is ' '
    /// </summary>
    public String sessionName;

    /// <summary>
    /// SDP session description (optional) - free form description of session - see RFC 4566 section 5.4
    /// </summary>
    public String sessionInformation;

    /// <summary>
    /// SDP URI (optional) - see RFC 4566 section 5.5
    /// </summary>
    public URI sessionUri;

    /// <summary>
    /// SDP session email address(es) (optional) - add with AddSessionEmailAddress method - see RFC 4566 section 5.6
    /// </summary>
    @Setter(AccessLevel.PROTECTED)
    public List<String> sessionEmailAddresses;

    /// <summary>
    /// SDP session phone number(s) (optional) - add with AddSessionPhoneNumber - see RFC 4566 section 5.6
    /// </summary>
    @Setter(AccessLevel.PROTECTED)
    public List<String> sessionPhoneNumbers;

    /// <summary>
    /// SDP connection address - see RFC 4566 section 5.7
    /// </summary>
    public InetAddress connectionAddress;

    /// <summary>
    /// SDP category session attribute 'cat:' - see RFC 4566 section 6
    /// </summary>
    public String category;

    /// <summary>
    /// SDP tool session attribute 'tool:' - see RFC 4566 section 6
    /// </summary>
    public String tool;

    /// <summary>
    /// SDP character set to use for session information in attribute 'charset:' - see RFC 4566 section 6
    /// </summary>
    public SdpCharsets charset;

    /// <summary>
    /// direction of media flow - see RFC 4566 section 6
    /// default is SendReceive
    /// </summary>
    public SdpMediaDirection direction;

    /// <summary>
    /// specifies the type of the conference ('type:' attribute) - see RFC 4566 section 6
    /// </summary>
    public SdpMediaConferenceTypes conferenceType;

    /// <summary>
    /// list of media descriptions - set with AddMediaDescription
    /// </summary>
    @Setter(AccessLevel.PROTECTED)
    public List<SdpMediaDescriptor> mediaDescriptions;

    // used to provide an SDP label attribute indexed collection
    // note: this can't be the only SdpMessageMedia collection since the label is not required
    /// <summary>
    /// RFC 4574 label-attribute indexed collection of media descriptions
    /// note:  will not include media descriptions that do not have an RFC 4574 Label attribute
    /// </summary>
    public Map<String, SdpMediaDescriptor> getMediaMappingWithSdpLabelKey()
    {
        return Collections.unmodifiableMap(mediaDescriptionsLabelIndexed);
    }

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Map<String, SdpMediaDescriptor> mediaDescriptionsLabelIndexed;

    @Setter
    private boolean isValid;

    @Getter
    @Setter
    private boolean hasVersion;

    @Getter
    @Setter
    private boolean hasOriginator;

    @Getter
    @Setter
    private boolean hasSessionName;

    @Getter
    @Setter
    private boolean hasTimeDescription;


    /// <summary>
    /// constructor to be used with the parser
    /// </summary>
    public SdpMessage()
    {
        initialize();
    }

    public SdpMessage(String originUnicastAddress, InetAddress connectionAddress)
    {
        this(originUnicastAddress, connectionAddress, null);
    }

    public SdpMessage(String originUnicastAddress, InetAddress connectionAddress, String originUsername)
    {
        initialize();

        this.originUnicastAddress = originUnicastAddress;
        this.connectionAddress = connectionAddress;
        if (originUsername != null) this.originUsername = originUsername;

        // this constructor is not used for parsing (it is for messages originated here), so it's always valid
        isValid = true;
    }

    private void initialize()
    {
        version = 0;
        originUsername = null;
        originSessionId = 0;
        originSessionVersion = 0;
        originAddressType = SdpAddressTypes.IPV4;
        connectionAddress = null;
        sessionName = null;
        sessionInformation = null;
        sessionUri = null;
        sessionEmailAddresses = null;
        sessionPhoneNumbers = null;
        conferenceType = SdpMediaConferenceTypes.NONE;
        charset = SdpCharsets.ISO_10646;

        mediaDescriptions = new ArrayList<>();
        mediaDescriptionsLabelIndexed = new HashMap<>();
    }

    /// <summary>
    /// add an email address to the SDP session info - see RFC 4566 section 5.6
    /// </summary>
    /// <param name="emailAddress">email to include in the SDP session info</param>
    /// <returns>this SdpMessage object</returns>
    public void addSessionEmailAddress(String emailAddress)
    {
        if (sessionEmailAddresses == null)
        {
            sessionEmailAddresses = new ArrayList<>();
        }

        sessionEmailAddresses.add(emailAddress);
    }

    /// <summary>
    /// add a phone number to this SDP session info - see RFC 4566 section 5.6
    /// </summary>
    /// <param name="phoneNumber"></param>
    /// <returns></returns>
    public void addSessionPhoneNumber(String phoneNumber)
    {
        if (sessionPhoneNumbers == null)
        {
            sessionPhoneNumbers = new ArrayList<>();
        }

        sessionPhoneNumbers.add(phoneNumber);
    }

    public void addMediaDescriptor(SdpMediaDescriptor descriptor)
    {
        mediaDescriptions.add(descriptor);
        if (descriptor.label != null) mediaDescriptionsLabelIndexed.put(descriptor.label, descriptor);
    }

    /// <summary>
    /// gets a media description object (<c>SdpMessageMedia</c>) for a specified label
    /// </summary>
    /// <param name="label"></param>
    /// <returns>returns SDP media descriptor or null if not available</returns>
    public SdpMediaDescriptor getMediaDescriptionFromLabel(String label)
    {
        try {

            return mediaDescriptionsLabelIndexed.get(label);
        } catch(Exception e) {

            return null;
        }
    }

    /// <summary>
    /// add a media description to the SDP message
    /// </summary>
    /// <param name="mediaDescription"></param>
    /// <returns>this SdpMessage object</returns>
    public void addMediaDescription(SdpMediaDescriptor mediaDescription)
    {
        mediaDescriptions.add(mediaDescription);

        if (mediaDescription.label != null)
        {
            mediaDescriptionsLabelIndexed.put(mediaDescription.label, mediaDescription);
        }
    }

    public SdpMediaDescriptor addMediaDescription(SdpMediaTypes mediaType, MediaTransportProfile rtpTransportMechanism,
                                                  int transportPort, String label, SdpMediaDirection direction,
                                                  InetAddress rtpEndpointAddress)
    {
        return addMediaDescription(mediaType, rtpTransportMechanism, transportPort, label, direction, rtpEndpointAddress,
                -1, null);
    }

    /// <summary>
    /// add a media descriptor (m= record) to the message
    /// </summary>
    /// <param name="mediaType"></param>
    /// <param name="rtpTransportMechanism"></param>
    /// <param name="transportPort"></param>
    /// <param name="label"></param>
    /// <param name="direction"></param>
    /// <param name="rtpEndpointAddress"></param>
    /// <param name="rtcpPort"></param>
    /// <param name="rtcpAddress"></param>
    /// <returns></returns>
    public SdpMediaDescriptor addMediaDescription(SdpMediaTypes mediaType, MediaTransportProfile rtpTransportMechanism,
                                                  int transportPort, String label, SdpMediaDirection direction,
                                                  InetAddress rtpEndpointAddress, int rtcpPort, InetAddress rtcpAddress)
    {
        SdpMediaDescriptor md = new SdpMediaDescriptor();

        md.mediaType = mediaType;
        md.transportProfile = rtpTransportMechanism;
        if (rtpEndpointAddress != null)
        {
            md.setRtpTransportIp(rtpEndpointAddress);
        }
        md.setRtpTransportPort(transportPort, connectionAddress);
        if (label != null)
        {
            md.label = label;
        }
        if (direction != SdpMediaDirection.UNSPECIFIED)
        {
            md.direction = direction;
        }
        if (rtcpAddress != null)
        {
            md.setRtcpTransportIp(rtcpAddress);
        }
        if (rtcpPort != -1)
        {
            md.setRtcpTransportPort(rtcpPort, connectionAddress);
        }

        addMediaDescription(md);
        return md;
    }

    /// <summary>
    /// get an encoded version of the message as a string in UTF-8 format
    /// </summary>
    /// <returns>'wire' encoded message</returns>
    public String getEncodedMessage() throws SdpAssemblyException
    {
        StringBuilder sb = new StringBuilder();

        // first the required session lines
        sb.append("v=").append(version).append(SDP_EOL);
        String ouser = "-";
        if (originUsername != null && !originUsername.isEmpty()) ouser = originUsername;

        String inetType = "IP4";
        if (originAddressType == SdpAddressTypes.IPV6) inetType = "IP6";
        sb.append("o=").append(ouser).append(" ").append(originSessionId).append(" ").append(originSessionVersion).
                append(" IN ").append(inetType).append(" ").append(originUnicastAddress).append(SDP_EOL);

        String sname = "-";
        if (sessionName != null && !sessionName.isEmpty()) sname = sessionName;
        sb.append("s=").append(sname).append(SDP_EOL);

        // optional sessions lines
        // session information (i=)
        if (sessionInformation != null && !sessionInformation.isEmpty())
        {
            sb.append("i=").append(sessionInformation).append(SDP_EOL);
        }

        // uri of description (u=)
        if (sessionUri != null)
        {
            sb.append("u=").append(sessionUri).append(SDP_EOL);
        }

        // email address(es) (e=)
        if (sessionEmailAddresses != null)
        {
            for (String emailAddress : sessionEmailAddresses)
            {
                sb.append("e=").append(emailAddress).append(SDP_EOL);
            }
        }

        // phone number(s) (p=)
        if (sessionPhoneNumbers != null)
        {
            for (String phoneNumber : sessionPhoneNumbers)
            {
                sb.append("p=").append(phoneNumber).append(SDP_EOL);
            }
        }

        // connection address (c=)
        if (connectionAddress != null)
        {
            sb.append("c=").append(getSdpConnectionString(connectionAddress)).append(SDP_EOL);
        }

        // TXDO - only supporting "t=0 0" since this is only used for scheduled media transmission (broadcast/multicast scheduled streaming)
        sb.append("t=0 0").append(SDP_EOL);

        // category session attribute (a=cat:)
        if (category != null)
        {
            sb.append("a=cat:").append(category).append(SDP_EOL);
        }

        // a=type: conference type attribute
        if (conferenceType != SdpMediaConferenceTypes.NONE)
        {
            switch (conferenceType)
            {
                case BROADCAST:
                    sb.append("a=type:broadcast" + SDP_EOL);
                    break;

                case H332:
                    sb.append("a=type:H332" + SDP_EOL);
                    break;

                case MEETING:
                    sb.append("a=type:meeting" + SDP_EOL);
                    break;

                case MODERATED:
                    sb.append("a=type:moderated" + SDP_EOL);
                    break;

                case TEST:
                    sb.append("a=type:test" + SDP_EOL);
                    break;

            }
        }

        switch (charset)
        {
            case ISO_10646:
                sb.append("a=charset:ISO-10646" + SDP_EOL);
                break;

            case ISO_8859_1:
                sb.append("a=charset:ISO-8859-1" + SDP_EOL);
                break;
        }

        // append media descriptions to the message encoding
        for (SdpMediaDescriptor mm : mediaDescriptions)
        {
            mm.appendEncodedMessage(sb, connectionAddress, direction);
        }

        return sb.toString();
    }

    public static String getSdpConnectionString(InetAddress connectionAddress) throws SdpAssemblyException
    {
        // TODO - connection string can use a FQDN so we should probably add support for this (although rare in practice)

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
