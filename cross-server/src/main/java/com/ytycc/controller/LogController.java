package com.ytycc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@RestController
@RequestMapping("log")
public class LogController {

    @GetMapping("/ip")
    public String ip(HttpServletRequest request) {
        return request.getRemoteHost();
    }
}
