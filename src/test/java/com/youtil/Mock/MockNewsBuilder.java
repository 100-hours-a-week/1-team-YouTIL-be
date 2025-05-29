package com.youtil.Mock;

import static com.youtil.Constants.MockNewsConstants.NEWS_CONTENT;
import static com.youtil.Constants.MockNewsConstants.NEWS_ID;
import static com.youtil.Constants.MockNewsConstants.NEWS_ORIGINAL_URL;
import static com.youtil.Constants.MockNewsConstants.NEWS_THUMBNAIL;
import static com.youtil.Constants.MockNewsConstants.NEWS_TITLE;
import com.youtil.Model.News;
import java.time.OffsetDateTime;

public class MockNewsBuilder {

    public static News createNews() {
        News news = News.builder()
                .id(NEWS_ID)
                .title(NEWS_TITLE)
                .content(NEWS_CONTENT)
                .originUrl(NEWS_ORIGINAL_URL)
                .thumbnail(NEWS_THUMBNAIL)
                .createdAt(OffsetDateTime.now())
                .build();
        return news;
    }
}
