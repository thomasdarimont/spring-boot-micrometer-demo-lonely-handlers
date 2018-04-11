package demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

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
