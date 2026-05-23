package com.whut.training.controller;

import com.whut.training.domain.dto.HomeOverview;
import com.whut.training.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public HomeOverview overview(@RequestParam(name = "top", defaultValue = "10") int top) {
        return homeService.getOverview(top);
    }
}
