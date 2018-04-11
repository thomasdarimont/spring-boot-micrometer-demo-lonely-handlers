package demo;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

@RestController
@RequestMapping("/api")
class SomeEndpoints {

    @GetMapping({"/op1", "/op1-alternative"})
    Object op1() {
        return Collections.singletonMap("data", System.currentTimeMillis());
    }

    @GetMapping("/op2")
    Object op2() {
        return Collections.singletonMap("data", System.currentTimeMillis());
    }
}

/**
 * NotCalledController finds controller mappings which were not used since server start.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notcalled")
class NotCalledController {

    private static final String HTTP_SERVER_REQUESTS_METRIC_NAME = "http.server.requests";
    private static final String URI_TAG = "uri";

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    private final MeterRegistry meterRegistry;

    @GetMapping
    Object findNotCalledHandlers() {
        return requestMappingHandlerMapping.getHandlerMethods().entrySet().stream() //
                .flatMap(this::toDormantHandlerMappingsIfPresent) //
                .collect(Collectors.toList());
    }

    private Stream<DormantHandlerMapping> toDormantHandlerMappingsIfPresent(Map.Entry<RequestMappingInfo, HandlerMethod> entry) {

        List<DormantHandlerMapping> results = new ArrayList<>();

        for (String pattern : entry.getKey().getPatternsCondition().getPatterns()) {
            RequiredSearch search = meterRegistry.get(HTTP_SERVER_REQUESTS_METRIC_NAME).tag(URI_TAG, pattern);

            try {
                Timer counter = search.timer();
                if (counter.count() > 0) {
                    continue;
                }
            } catch (MeterNotFoundException ignore) {
            }

            results.add(new DormantHandlerMapping(pattern, entry.getValue().getMethod().toString()));
        }

        return results.stream();
    }

    @Value
    static class DormantHandlerMapping {
        private final String pattern;
        private final String method;
    }
}