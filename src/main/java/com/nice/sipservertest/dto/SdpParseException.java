package com.nice.sipservertest.dto;

import lombok.Getter;

@Getter
public class SdpParseException extends Error {

    private SdpParserErrorTypes errorType;


    public SdpParseException(SdpParserErrorTypes errorType, String info)
    {
        super(info);
        this.errorType = errorType;
    }
}
