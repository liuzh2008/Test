package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.DicInput;
import com.example.medaiassistant.repository.DicInputRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DicInputController {

    private static final Logger logger = LoggerFactory.getLogger(DicInputController.class);
    private final DicInputRepository dicInputRepository;

    public DicInputController(DicInputRepository dicInputRepository) {
        this.dicInputRepository = dicInputRepository;
    }

    @GetMapping("/dicinput")
    public List<DicInput> getAllDicInput() {
        logger.info("Accessing /api/dicinput endpoint");
        return dicInputRepository.findAll();
    }
}
