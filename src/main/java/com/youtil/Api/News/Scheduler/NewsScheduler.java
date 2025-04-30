package com.youtil.Api.News.Scheduler;

import com.youtil.Api.News.Service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsService newsService;

    @Scheduled(cron = "0 0 15 * * *", zone = "Asia/Seoul")
    public void scheduleCreateNews() {
        newsService.createNewsService();
    }
}
