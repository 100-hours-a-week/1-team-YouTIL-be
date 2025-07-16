package com.youtil.Common.Retry;

import java.util.function.Consumer;

public interface RetryStrategy<Q> {


    void retry(Q requestJson, Long userId, String requestId, int retryCount,
            Consumer<String> onFail);


}
