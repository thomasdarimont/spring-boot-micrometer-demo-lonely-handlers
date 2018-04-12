package demo;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NotCalledController finds controller mappings which were not used since server start.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notcalled")
class NotCalledController {

    private static final String HTTP_SERVER_REQUESTS_METRIC_NAME = "http.server.requests";
    private static final String URI_TAG = "uri";

    // we're only interested in custom @RestMapping handler mappings not built-in ones.
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    private final MeterRegistry meterRegistry;

    @GetMapping
    Object findNotCalledHandlers() {
        return requestMappingEntries() //
                .flatMap(this::toDormantHandlerMappings) //
                .collect(Collectors.toList());
    }

    private Stream<Map.Entry<RequestMappingInfo, HandlerMethod>> requestMappingEntries() {
        return requestMappingHandlerMapping.getHandlerMethods().entrySet().stream();
    }

    private Stream<DormantHandlerMapping> toDormantHandlerMappings(Map.Entry<RequestMappingInfo, HandlerMethod> entry) {

        RequestMappingInfo requestMapping = entry.getKey();
        HandlerMethod handlerMethod = entry.getValue();

        return dormantSearchPatterns(requestMapping) //
                .map(s -> new DormantHandlerMapping(s.getPattern(), handlerMethod.getMethod().toString()));
    }

    private Stream<SearchPattern> dormantSearchPatterns(RequestMappingInfo requestMapping) {
        return createSearchPatternsFromRequestMapping(requestMapping) //
                .filter(this::hasEmptyTimerCount);
    }

    private Stream<SearchPattern> createSearchPatternsFromRequestMapping(RequestMappingInfo requestMapping) {
        return requestMapping.getPatternsCondition().getPatterns().stream() //
                .map(SearchPattern::new);
    }

    private boolean hasEmptyTimerCount(SearchPattern searchPattern) {

        RequiredSearch search = searchPattern.toSearch();
        try {
            Timer counter = search.timer();
            if (counter.count() > 0) {
                return false;
            }
        } catch (MeterNotFoundException ignored) {
        }
        return true;
    }

    @Value
    private class SearchPattern {
        private final String pattern;

        RequiredSearch toSearch() {
            return meterRegistry.get(HTTP_SERVER_REQUESTS_METRIC_NAME).tag(URI_TAG, pattern);
        }
    }

    @Value
    private static class DormantHandlerMapping {
        private final String pattern;
        private final String method;
    }
}
