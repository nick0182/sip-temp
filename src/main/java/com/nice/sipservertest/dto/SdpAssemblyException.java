package com.nice.sipservertest.dto;

import lombok.Getter;

@Getter
public class SdpAssemblyException extends Exception {
    private final SdpAssemblyErrorTypes errorType;


    public SdpAssemblyException(SdpAssemblyErrorTypes errorType, String info)
    {
        super(info);
        this.errorType = errorType;
    }
}
