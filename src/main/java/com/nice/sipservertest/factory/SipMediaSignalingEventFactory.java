package com.nice.sipservertest.factory;

import com.nice.sipservertest.Utils;
import com.nice.sipservertest.dto.*;
import com.nice.sipservertest.parser.SdpMessageParser;
import com.nice.sipservertest.util.SipBodyHelpers;

import javax.sip.message.Message;
import javax.sip.message.Request;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SipMediaSignalingEventFactory {

    public static MediaSignalingEvent createMediaSignalingEvent(Message message, SdpMessage sdpMessage){
        if(message == null){
            return null;
        }

        MediaSignalingEvent mediaSignalingEvent = null;

        List<MediaChannel> mediaChannels = (sdpMessage == null) ? null : getMediaChannels(sdpMessage);
        mediaSignalingEvent = new MediaSignalingEvent(mediaChannels);

        return mediaSignalingEvent;
    }

    private static List<MediaChannel> getMediaChannels(SdpMessage sdpMessage) {

        List<MediaChannel> channels = new ArrayList<>();

        List<SdpMediaDescriptor> mediaDescriptors = sdpMessage.getMediaDescriptions();

        for (SdpMediaDescriptor md : mediaDescriptors) {
            InetSocketAddress rtpEndpoint = md.getRtpEndpoint();

            MediaChannelStates mediaChannelState = MediaChannelStates.ACTIVE;
            if (rtpEndpoint.getPort() == 0) {
                mediaChannelState = MediaChannelStates.UNUSED;
            } else if (rtpEndpoint.getAddress().equals(SdpMediaDescriptor.allZerosIpAddress) ||
                    md.getDirection() == SdpMediaDirection.INACTIVE) {
                mediaChannelState = MediaChannelStates.INACTIVE;
            }

            // make the list of codecs in provided order
            List<MediaCodec> codecs = new ArrayList<>();
            for (int rtpPayloadNumber : md.getRtpPayloadTypeList()) {
                SdpMediaMapping mm = md.getMediaMappings().get(rtpPayloadNumber);

                MediaCodec mCodec = new MediaCodec();
                mCodec.setRtpPayloadType(rtpPayloadNumber);
                mCodec.setMediaCodecType(getMediaCodecTypeFromSdpMediaCodec(mm.getMediaCodec()));
                mCodec.setFmtp(mm.getGenericFormatSpecificParameters());
                codecs.add(mCodec);
            }
            // create media channel and add to list
            channels.add(new MediaChannel(
                    mediaChannelState,
                    (md.getMediaType() == SdpMediaTypes.AUDIO) ? MediaChannelTypes.AUDIO : MediaChannelTypes.UNKNOWN,
                    rtpEndpoint,
                    md.getRtcpEndpoint(),
                    codecs
            ));
        }

        return channels;
    }

    public static MediaCodecTypes getMediaCodecTypeFromSdpMediaCodec(SdpMediaCodecs codec) {

        MediaCodecTypes codecType = Utils.codecConversionTable.get(codec);

        if (codecType == null) {
            codecType = MediaCodecTypes.UNKNOWN;
        }

        return codecType;
    }
}
