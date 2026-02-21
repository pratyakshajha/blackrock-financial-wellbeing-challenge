package com.example.controller;

import com.example.domain.ReturnType;
import com.example.dto.*;
import com.example.service.ReturnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReturnController {
    private static final Logger logger = LoggerFactory.getLogger(ReturnController.class.getName());

    @Autowired
    private ReturnService returnService;

    @PostMapping("/returns:nps")
    public ReturnResponse parseNps(@RequestBody ReturnRequest returnRequest) {
        return returnService.returns(returnRequest, ReturnType.NPS);
    }

    @PostMapping("/returns:index")
    public ReturnResponse parseIndex(@RequestBody ReturnRequest returnRequest) {
        return returnService.returns(returnRequest, ReturnType.INDEX_FUND);
    }

}