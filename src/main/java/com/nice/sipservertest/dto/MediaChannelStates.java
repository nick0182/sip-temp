package com.nice.sipservertest.dto;

public enum MediaChannelStates {

    /**
     * channel is unused and is not available for recording
     * (it is possible for a remote node to change this state to another state)
     */
    UNUSED,

    /**
     * channel has no RTP traffic but remains available for recording
     */
    INACTIVE,

    /**
     * channel is actively sending/receiving RTP audio data
     */
    ACTIVE
}
