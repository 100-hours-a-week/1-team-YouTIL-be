package com.youtil.Config;

import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Constants.FilteringServiceConstants;
import com.youtil.Common.Constants.InterviewServiceConstants;
import com.youtil.Common.Constants.TilServiceConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConstantsConfig {

    @Bean(name = "tilServiceConstants")
    public AiServiceConstants tilServiceConstants() {
        return new TilServiceConstants();
    }


    @Bean(name = "interviewServiceConstants")
    public AiServiceConstants interviewServiceConstants() {
        return new InterviewServiceConstants();
    }

    @Bean(name = "filterServiceConstants")
    public AiServiceConstants filterServiceConstants() {
        return new FilteringServiceConstants();
    }
}
