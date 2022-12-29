package com.nice.sipservertest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * media signaling event data when signaling creates new or updates existing signaling for a media endpoint session
 */
@Getter
@AllArgsConstructor
@Setter
public class MediaSignalingEvent {

    private List<MediaChannel> mediaChannels;
}