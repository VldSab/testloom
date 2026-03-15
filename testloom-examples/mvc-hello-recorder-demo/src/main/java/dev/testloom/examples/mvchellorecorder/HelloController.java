package dev.testloom.examples.mvchellorecorder;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/hello", produces = MediaType.APPLICATION_JSON_VALUE)
public class HelloController {

    @GetMapping
    public HelloResponse hello() {
        return new HelloResponse("hello");
    }

    public record HelloResponse(String message) {
    }
}
