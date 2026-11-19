package io.flagship.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards HTML5 History API client routes to index.html for Single-Page Application routing.
 */
@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/login",
            "/flags",
            "/flags/**",
            "/change-requests",
            "/change-requests/**",
            "/experiments",
            "/experiments/**",
            "/hygiene",
            "/hygiene/**",
            "/segments",
            "/segments/**"
    })
    public String forwardSpa() {
        return "forward:/index.html";
    }
}
