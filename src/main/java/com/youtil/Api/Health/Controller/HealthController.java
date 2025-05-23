package com.youtil.Api.Health.Controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "health API 관련")
public class HealthController {

    //헬스체크
    @GetMapping("")
    public String health() {
        return "Health OK";
    }

}
