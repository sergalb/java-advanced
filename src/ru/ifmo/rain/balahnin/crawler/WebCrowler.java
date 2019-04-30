package ru.ifmo.rain.balahnin.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;

public class WebCrowler implements Crawler {
    private final Downloader downloader;
    private final int downloaders;
    private final int executors;
    private final int perHost;

    public WebCrowler(Downloader downloader, int downloaders, int executors, int perHost) {
        this.downloader = downloader;
        this.downloaders = downloaders;
        this.executors = executors;
        this.perHost = perHost;
    }

    @Override
    public Result download(String s, int i) {
        return null;
    }

    @Override
    public void close() {

    }
}
