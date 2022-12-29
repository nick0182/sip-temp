package com.nice.sipservertest.parser;

import com.nice.sipservertest.dto.*;
import com.nice.sipservertest.factory.SdpMediaDescriptorFactory;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SdpMessageParser {

    public final static String SdpEOLPattern = "\\r\\n";

    static final Pattern sdpLineRegex = Pattern.compile("^([^=])=(.+)$");

    static final Pattern sdpConnectLineRegex = Pattern.compile("^IN[ \\t]+(IP4|IP6)[ \\t]+(.+)$");

    static final Pattern sdpOriginLineRegex = Pattern.compile("^([^ \\t]+)[ \\t]+([^ \\t]+)[ \\t]+([^ \\t]+)[ \\t]+([^ \\t]+)[ \\t]+([^ \\t]+)[ \\t]+([^ \\t]+)$");

    static final Pattern sdpAttributeLineRegex = Pattern.compile("^([^:]+)(:(.+))?$");

    public static SdpMessage parse(String messageString)
    {
        SdpMessage msg = new SdpMessage();

        String[] msgLines = messageString.split(SdpEOLPattern, 0);

        SdpMessageStates state = SdpMessageStates.SESSION;
        List<String> currentMediaObject = new ArrayList<>();
        for (String msgLine : msgLines)
        {
            Matcher m = sdpLineRegex.matcher(msgLine);
            if (!m.find() || m.groupCount() < 2) continue;  // TXDO - might want to provide feedback on the skip for troubleshooting

            char cmdCode = m.group(1).charAt(0);
            String msgLineBody = m.group(2);

            switch(cmdCode)
            {
                case 'v':       // session only - SDP version
                    // if the 'v' command is in the session info and not zero, then fail to parse
                    // otherwise, just ignore it
                    if (state == SdpMessageStates.SESSION && !msgLineBody.equals("0")) {
                        throw new SdpParseException(SdpParserErrorTypes.INVALID_SDP_VERSION,
                                "Invalid SDP session version number (only zero allowed)");
                    }

                    msg.setHasVersion(true);
                    break;

                case 'o':       // session only - originator info
                    Matcher mo = sdpOriginLineRegex.matcher(msgLineBody);

                    if (!mo.find() || mo.groupCount() < 6) break;

                    if (state == SdpMessageStates.SESSION)
                    {
                        String user = mo.group(1);
                        if (user == null || user.isEmpty() || user.equals("-")) {
                            msg.originUsername = null;
                        } else {
                            msg.originUsername = user;
                        }

                        try {
                            msg.originSessionId = Long.parseLong(mo.group(2));
                        } catch (Exception e) {
                        }

                        try {
                            msg.originSessionVersion = Long.parseLong(mo.group(3));
                        } catch (Exception e) {
                        }

                        if (mo.group(5).equals("IP4")) {

                            msg.originAddressType = SdpAddressTypes.IPV4;
                        } else {

                            msg.originAddressType = SdpAddressTypes.IPV6;
                        }

                        msg.originUnicastAddress = mo.group(6);

                        msg.setHasOriginator(true);
                    }
                    break;

                case 's':       // session only - session name
                    // if we're in session state and the session isn't set to the default ('-'), then update it
                    if (state == SdpMessageStates.SESSION)
                    {
                        if (!msgLineBody.equals("-")) msg.sessionName = msgLineBody;

                        msg.setHasSessionName(true);
                    }
                    break;

                case 'c':       // session and media - session IP info
                    if (state == SdpMessageStates.SESSION)
                    {
                        msg.connectionAddress = parseSdpConnectionString(msgLineBody);
                    }
                    else
                    {
                        currentMediaObject.add(msgLine);
                    }
                    break;

                case 't':       // session only - session time (only support t=0 0)
                    // basically ignore this in the parser - hopefully it's a '0 0', but we wouldn't do anything if it wasn't anyway...
                    if (state == SdpMessageStates.SESSION)
                    {
                        msg.setHasTimeDescription(true);
                    }

                    break;

                case 'a':       // session and media - attirbute
                    if (state == SdpMessageStates.SESSION)
                    {
                        // parse session level attributes
                        parseSessionAttributes(msgLineBody, msg);
                    }
                    else
                    {
                        currentMediaObject.add(msgLine);
                    }
                    break;

                case 'm':       // media only - main media info line for media descriptor
                    // if there's a previous media descriptor, then parse it and add it to the message collection
                    if (currentMediaObject.stream().count() > 0)
                    {
                        try {

                            msg.addMediaDescription(SdpMediaDescriptorFactory.parse(currentMediaObject, msg.connectionAddress));
                        } catch(Exception e) {
                        }
                    }

                    state = SdpMessageStates.MEDIA;
                    currentMediaObject.clear();

                    currentMediaObject.add(msgLine);
                    break;

                case 'i':       // session and media - info
                    if (state == SdpMessageStates.SESSION)
                    {
                        msg.sessionInformation = msgLineBody;
                    }
                    else
                    {
                        currentMediaObject.add(msgLine);
                    }
                    break;

                case 'u':       // session only - URL
                    if (state == SdpMessageStates.SESSION)
                    {
                        try
                        {
                            msg.sessionUri = URI.create(msgLineBody);
                        } catch(Exception e) {

                            // if we have an error in parsing, then just move on
                            // we will almost certainly not use this option so no point in error handling
                        }
                    }
                    break;

                case 'e':       // session only - email
                    if (state == SdpMessageStates.SESSION)
                    {
                        msg.addSessionEmailAddress(msgLineBody);
                    }
                    break;

                case 'p':       // session only - phone number
                    if (state == SdpMessageStates.SESSION)
                    {
                        msg.addSessionPhoneNumber(msgLineBody);
                    }
                    break;
            }
        }

        // if there's a previous media descriptor, then parse it and add it to the message collection
        if (currentMediaObject.size() > 0)
        {
            try {
                msg.addMediaDescription(SdpMediaDescriptorFactory.parse(currentMediaObject, msg.connectionAddress));
            } catch(Exception e) {

            }
        }

        if (msg.isHasVersion() && msg.isHasOriginator() && msg.isHasSessionName() && msg.isHasTimeDescription())
        {
            msg.setValid(true);
        }

        return msg;
    }

    public static InetAddress parseSdpConnectionString(String messageLineBody)
    {
        Matcher m = sdpConnectLineRegex.matcher(messageLineBody);

        if (!m.find() || m.groupCount() < 2)
        {
            return null;
        }

        InetAddress ip;

        try {

            ip = InetAddress.getByName(m.group(2));
        } catch(Exception e) {

            return null;
        }

        return ip;
    }

    private static void parseSessionAttributes(String msgLineBody, SdpMessage msg)
    {
        Matcher m = sdpAttributeLineRegex.matcher(msgLineBody);

        if (!m.find()) return;

        if (m.groupCount() == 2)
        {
            // flag type attributes
            String flag = m.group(1);

            switch(flag)
            {
                case SdpMediaDescriptorFactory.SDP_ATTRIB_RECVONLY:
                    msg.direction = SdpMediaDirection.RECEIVE_ONLY;
                    break;

                case SdpMediaDescriptorFactory.SDP_ATTRIB_SENDRECV:
                    msg.direction = SdpMediaDirection.SEND_RECEIVE;
                    break;

                case SdpMediaDescriptorFactory.SDP_ATTRIB_SENDONLY:
                    msg.direction = SdpMediaDirection.SEND_ONLY;
                    break;

                case SdpMediaDescriptorFactory.SDP_ATTRIB_INACTIVE:
                    msg.direction = SdpMediaDirection.INACTIVE;
                    break;

            }
        }
        else if (m.groupCount() == 3)
        {
            // type/value attributes
            String cmd = m.group(1);

            switch(cmd)
            {
                case "cat":
                    msg.category = m.group(3);
                    break;

                case "tool":
                    msg.tool = m.group(3);
                    break;

                case "type":
                    switch (m.group(3).toLowerCase())
                    {
                        case "broadcast":
                            msg.conferenceType = SdpMediaConferenceTypes.BROADCAST;
                            break;

                        case "h332":
                            msg.conferenceType = SdpMediaConferenceTypes.H332;
                            break;

                        case "meeting":
                            msg.conferenceType = SdpMediaConferenceTypes.MEETING;
                            break;

                        case "moderated":
                            msg.conferenceType = SdpMediaConferenceTypes.MODERATED;
                            break;

                        case "test":
                            msg.conferenceType = SdpMediaConferenceTypes.TEST;
                            break;
                    }
                    break;

                case "charset":
                    switch (m.group(3).toUpperCase())
                    {
                        case "ISO-10646":
                            // leaving this here since the default could change in the future
                            msg.charset = SdpCharsets.ISO_10646;
                            break;

                        case "ISO-8859-1":
                            msg.charset = SdpCharsets.ISO_8859_1;
                            break;

                        default:
                            // nothing was specified in the SDP message so use default per RFC 4566
                            msg.charset = SdpCharsets.ISO_10646;
                            break;
                    }
                    break;
            }
        }
    }
}
