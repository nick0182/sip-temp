package com.nice.sipservertest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;

import java.nio.file.Paths;

import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

//@Configuration
@Slf4j
public class AWSConfig {

    @Bean
    Route53Client route53Client() {
        return Route53Client.builder().credentialsProvider((ProfileCredentialsProvider.builder().profileFile(ProfileFile
                        .aggregator()
                        .applyMutation(aggregator -> aggregator.addFile(ProfileFile
                                .builder()
                                .content(Paths.get(userHomeDirectory(), ".aws", "nikolai", "credentials"))
                                .type(ProfileFile.Type.CREDENTIALS)
                                .build()))
                        .applyMutation(aggregator -> aggregator.addFile(ProfileFile
                                .builder()
                                .content(Paths.get(userHomeDirectory(), ".aws", "nikolai", "config"))
                                .type(ProfileFile.Type.CONFIGURATION)
                                .build()))
                        .build()).profileName("default").build()))
                .region(Region.US_WEST_2)
                .build();
    }
}
