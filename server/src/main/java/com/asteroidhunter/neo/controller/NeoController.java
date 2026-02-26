package com.asteroidhunter.neo.controller;

import com.asteroidhunter.neo.model.NeoSummary;
import com.asteroidhunter.neo.service.NeoTodayService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/neos")
public class NeoController {

    private final NeoTodayService neoTodayService;

    public NeoController(NeoTodayService neoTodayService) {
        this.neoTodayService = neoTodayService;
    }

    @GetMapping("/today")
    public List<NeoSummary> getTodayNeos() {
        return neoTodayService.getTodayNeos();
    }
}
