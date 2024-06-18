package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);

    // Check if a result path is specified and write the result accordingly
    if (config.getResultPath() == null || config.getResultPath().isEmpty()) {
      // If resultPath is empty, print the results to standard output.
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
        resultWriter.write(writer);
      }
    } else {
      // If resultPath is not empty, write the results to the specified file.
      Path path = Path.of(config.getResultPath());
      resultWriter.write(path);
    }

    // Write the profile data
    if (config.getProfileOutputPath() == null || config.getProfileOutputPath().isEmpty()) {
      // If profileOutputPath is empty, print the profiler data to standard output.
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
        profiler.writeData(writer);
      }
    } else {
      // If profileOutputPath is not empty, write the profiler data to the specified file.
      Path profilePath = Path.of(config.getProfileOutputPath());
      try (BufferedWriter writer = Files.newBufferedWriter(profilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
        profiler.writeData(writer);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
