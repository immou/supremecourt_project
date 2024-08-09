package com.cases.info.service;

import com.cases.info.dto.VolumeDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Service

public interface CaseDataService {
    public void fetchCaseVolumePerYear(List<String> years) throws URISyntaxException, IOException, InterruptedException;
}
