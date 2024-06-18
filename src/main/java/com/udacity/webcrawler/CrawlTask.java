package com.udacity.webcrawler;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.udacity.webcrawler.parser.PageParserFactory;
import com.udacity.webcrawler.parser.PageParser;

class CrawlTask extends RecursiveAction {
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final PageParserFactory parserFactory;
    private final Clock clock;
    private final List<Pattern> ignoredUrls;

    public CrawlTask(String url, Instant deadline, int maxDepth, Map<String, Integer> counts,
                     Set<String> visitedUrls, PageParserFactory parserFactory,
                     Clock clock, List<Pattern> ignoredUrls) {
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.parserFactory = parserFactory;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        if (visitedUrls.contains(url)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        visitedUrls.add(url);
        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        List<CrawlTask> tasks = result.getLinks().stream()
                .map(link -> new CrawlTask(link, deadline, maxDepth - 1, counts, visitedUrls, parserFactory, clock, ignoredUrls))
                .collect(Collectors.toList());
        invokeAll(tasks);
    }
}

