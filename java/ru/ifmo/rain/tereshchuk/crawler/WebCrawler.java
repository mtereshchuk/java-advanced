package ru.ifmo.rain.tereshchuk.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Future;

public class WebCrawler implements Crawler {

    private Downloader downloader;
    private ExecutorService downloaders;
    private ExecutorService extractors;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) {

        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        Queue<String> queueUrls = new ConcurrentLinkedQueue<>();

        downloaded.add(url);
        queueUrls.add(url);
        for (int d = 0; d < depth; d++) {

            final int finalD = d;
            final Queue<String> finalQueueUrls = queueUrls;
            final Queue<String> nextQueueUrls = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<Future> futures = new ConcurrentLinkedQueue<>();

            while (!finalQueueUrls.isEmpty()) {
                futures.add(downloaders.submit(() -> {
                    final String curUrl = finalQueueUrls.poll();
                    try {
                        Document document = downloader.download(curUrl);
                        if (finalD + 1 < depth) {
                            futures.add(extractors.submit(() -> {
                                try {
                                    List<String> links = document.extractLinks();
                                    for (String link : links) {
                                        if (!downloaded.contains(link)) {
                                            downloaded.add(link);
                                            nextQueueUrls.add(link);
                                        }
                                    }
                                } catch (IOException e) {
                                    errors.put(curUrl, e);
                                }
                            }));
                        }
                    } catch (IOException e) {
                        errors.put(curUrl, e);
                    }
                }));
            }
            while (!futures.isEmpty()) {
                Future f = futures.poll();
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException ignore) {
                }
            }
            queueUrls = nextQueueUrls;
        }
        downloaded.removeAll(errors.keySet());
        return new Result(new ArrayList<>(downloaded), errors);
    }

    @Override
    public void close() {
        downloaders.shutdown();
        extractors.shutdown();
    }

    public static void main(String[] args) {
        try {
            Crawler crawler = new WebCrawler(new CachingDownloader(),
                    args.length >= 3 ? Integer.parseInt(args[2]) : 5,
                    args.length >= 4 ? Integer.parseInt(args[3]) : 5,
                    args.length >= 5 ? Integer.parseInt(args[4]) : 5
            );
            crawler.download(args[0], 5);
        } catch (IOException ignored) {
        }
    }
}
