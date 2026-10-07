package com.example.pro1;

import org.bson.Document;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.function.Function;

@Controller
public class PlotController {

    @Value("classpath:plot.R")
    private Resource rSource;

    @Autowired
    private Function<DataHolder, String> plotFunction;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static int index = 0;   // sıradaki satır (0-99)

    @Bean
    Function<DataHolder, String> getPlotFunction(@Autowired Context ctx)
            throws IOException {
        Source source = Source.newBuilder("R", rSource.getURL()).build();
        return ctx.eval(source).as(Function.class);
    }

    @Bean
    public Context getGraalVMContext() {
        return Context.newBuilder().allowAllAccess(true).build();
    }

    @RequestMapping(value = "/plot", produces = "image/svg+xml")
    public ResponseEntity<String> load() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Refresh", "1");

        String svg = "";
        synchronized (plotFunction) {
            // Java, MongoDB'den sıradaki satırı okur
            Document doc = mongoTemplate.getCollection("data")
                    .find()
                    .sort(new Document("_id", 1))
                    .skip(index)
                    .limit(1)
                    .first();

            if (doc == null) {          // index veri sayısını aşmışsa başa dön
                index = 0;
                doc = mongoTemplate.getCollection("data")
                        .find()
                        .sort(new Document("_id", 1))
                        .limit(1)
                        .first();
            }

            double value = ((Number) doc.get("Col-16")).doubleValue();
            index = (index + 1) % 100;

            svg = plotFunction.apply(new DataHolder(value));
        }
        return new ResponseEntity<>(svg, responseHeaders, HttpStatus.OK);
    }
}