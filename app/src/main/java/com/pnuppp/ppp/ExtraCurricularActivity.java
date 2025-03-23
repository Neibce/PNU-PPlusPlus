package com.pnuppp.ppp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ExtraCurricularActivity extends AppCompatActivity {
    public static final int MAX_PAGE = 15;

    private RecyclerView recyclerView;
    private LinearLayout networkErrorLayout;
    private ExtraCurricularAdapter noticeAdapter;
    private List<RSSItem> rssItems = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private int currentPage = 1;
    private boolean isOnDownloading = false;
    private static String sortBy = "approach";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extra_curricular);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noticeAdapter = new ExtraCurricularAdapter(rssItems, this);
        recyclerView.setAdapter(noticeAdapter);

        networkErrorLayout = findViewById(R.id.networkErrorLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.spinner_colors));
        swipeRefreshLayout.setRefreshing(true);

        fetchRSSFeed("https://ppp.jun0.dev/?page=1&sort=approach", true);

        findViewById(R.id.retryButton).setOnClickListener(v -> {
            swipeRefreshLayout.setRefreshing(true);
            fetchRSSFeed("https://ppp.jun0.dev/?page=1&sort=" + sortBy, true);
        });

        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            swipeRefreshLayout.setRefreshing(true);

            if (checkedId == R.id.radioButton1) sortBy = "approach";
            else if (checkedId == R.id.radioButton2) sortBy = "applicant";
            else if (checkedId == R.id.radioButton3) sortBy = "date";
            fetchRSSFeed("https://ppp.jun0.dev/?page=1&sort=" + sortBy, true);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchRSSFeed("https://ppp.jun0.dev/?page=1&sort=" + sortBy, true);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1) && !isOnDownloading && currentPage < MAX_PAGE) {
                    Log.i("NF", "onScrolled: END");

                    fetchRSSFeed("https://ppp.jun0.dev/?page=" + (++currentPage) + "&sort=" + sortBy, false);
                }
            }
        });
    }

    private void hideNetworkError() {
        networkErrorLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showNetworkError() {
        recyclerView.scrollToPosition(0);
        networkErrorLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void fetchRSSFeed(String rssUrl, boolean reset) {
        hideNetworkError();
        if (rssUrl == null || rssUrl.isEmpty()) return;
        isOnDownloading = true;
        if (reset)
            currentPage = 1;

        new Thread(() -> {
            try {
                URL url = new URL(rssUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                String content = readStream(inputStream);
                Log.d("RSSFeed", content);

                parseRSS(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), reset);
            } catch (Exception e) {
                Log.e("RSSFetch", "Error fetching RSS feed", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showNetworkError();
                });
            }

            isOnDownloading = false;
        }).start();
    }

    private String readStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        return out.toString();
    }

    private void parseRSS(InputStream inputStream, boolean reset) throws Exception {
        ArrayList<RSSItem> newRssItems = new ArrayList<>();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        RSSItem currentItem = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName;
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (tagName.equals("item")) {
                        currentItem = new RSSItem();
                    } else if (currentItem != null) {
                        switch (tagName) {
                            case "title":
                                currentItem.setTitle(parser.nextText());
                                break;
                            case "link":
                                currentItem.setLink(parser.nextText());
                                break;
                            case "category":
                                currentItem.setAuthor(parser.nextText());
                                break;
                            case "description":
                                currentItem.setCategory(parser.nextText());
                                break;
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase("item") && currentItem != null) {
                        newRssItems.add(currentItem);
                        currentItem = null;
                    }
                    break;
            }
            eventType = parser.next();
        }

        if (currentPage < MAX_PAGE)
            newRssItems.add(null);

        if (reset) rssItems.clear();
        if (currentPage > 1) rssItems.remove(rssItems.size() - 1);

        rssItems.addAll(newRssItems);

        new Handler(Looper.getMainLooper()).post(() -> {
            noticeAdapter.notifyDataSetChanged();
            if (reset) {
                recyclerView.scrollToPosition(0);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
}