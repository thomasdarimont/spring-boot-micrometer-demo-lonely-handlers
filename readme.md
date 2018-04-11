# Finding unused Spring MVC handlers with Micrometer

Often it can be a bit hard to tell which Spring MVC handlers are NOT used anymore since most 
of the time only used routes are logged / monitored, this holds especially for long running projects 
that have grown over time. 

This repository demonstrates how to use Micrometer metrics with Spring Boot (1.5.x)
to determine dormant controller mappings that were not called since the application was started.

Those found dormant handler mappings could then be looked up in access-logs or log-monitoring systems
in order to gather evidence that a handler is indeed not used anymore an could be removed.

The `NotCalledController` determines custom handler mappings from the `requestMappingHandlerMapping` bean.
For each handler we check if the controller method was used by trying to find an associated `http.server.requests` metric.
If such a metric was found, we know that the method was used at least once and can continue the search, otherwise it is returned as a dormant handler. 

See [NotCalledController.java](src/main/java/demo/NotCalledController.java)

# Example

Given the following controller `SomeEndpoints`
```java
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
```

The first call to `/notcalled` will yield 
```json
[
{
"pattern": "/notcalled",
"method": "java.lang.Object demo.NotCalledController.findNotCalledHandlers()"
},
{
"pattern": "/api/op1",
"method": "java.lang.Object demo.SomeEndpoints.op1()"
},
{
"pattern": "/api/op1-alternative",
"method": "java.lang.Object demo.SomeEndpoints.op1()"
},
{
"pattern": "/api/op2",
"method": "java.lang.Object demo.SomeEndpoints.op2()"
},
{
"pattern": "/error",
"method": "public org.springframework.http.ResponseEntity org.springframework.boot.autoconfigure.web.BasicErrorController.error(javax.servlet.http.HttpServletRequest)"
},
{
"pattern": "/error",
"method": "public org.springframework.web.servlet.ModelAndView org.springframework.boot.autoconfigure.web.BasicErrorController.errorHtml(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)"
}
]
```

After calling `/op1` we get the following response from `/notcalled`.
```json
[
{
"pattern": "/api/op1-alternative",
"method": "java.lang.Object demo.SomeEndpoints.op1()"
},
{
"pattern": "/api/op2",
"method": "java.lang.Object demo.SomeEndpoints.op2()"
},
{
"pattern": "/error",
"method": "public org.springframework.http.ResponseEntity org.springframework.boot.autoconfigure.web.BasicErrorController.error(javax.servlet.http.HttpServletRequest)"
},
{
"pattern": "/error",
"method": "public org.springframework.web.servlet.ModelAndView org.springframework.boot.autoconfigure.web.BasicErrorController.errorHtml(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)"
}
]
```
Note that the pattern '/op1' and '/notcalled' itself are now gone since they were just called.