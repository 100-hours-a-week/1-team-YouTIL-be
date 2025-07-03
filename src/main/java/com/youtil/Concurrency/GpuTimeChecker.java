package com.youtil.Concurrency;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class GpuTimeChecker {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime GPU_TIME_START = LocalTime.of(15, 0);

    public static boolean isGpuTimeNow() {
        return ZonedDateTime.now(ZONE).toLocalTime().isAfter(GPU_TIME_START);
    }
}
