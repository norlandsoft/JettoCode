package com.jettech.code.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jettech.code.dto.VulnerabilityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class NvdSource implements VulnerabilitySource {
    
    private static final Logger logger = LoggerFactory.getLogger(NvdSource.class);
    
    @Value("${nvd.api-key:}")
    private String apiKey;
    
    @Value("${nvd.api-url:https://services.nvd.nist.gov/restjson/cves/2.0}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public NvdSource(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "NVD";
    }
    
    @Override
    public int getPriority() {
        return 2;
    }
    
    @Override
    public List<VulnerabilityInfo> query(String packageName, String ecosystem, String version) {
        List<VulnerabilityInfo> results = new ArrayList<>();
        
        try {
            String cpeId = buildCpeId(packageName, ecosystem);
            String url = apiUrl + "?cpeId=" + URLEncoder.encode(cpeId, StandardCharsets.UTF_8) + "&resultsPerPage=20";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("apiKey", apiKey);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            logger.info("Querying NVD for package: {}", packageName);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<VulnerabilityInfo> vulns = parseVulnerabilities(response.getBody(), packageName, ecosystem, version);
                results.addAll(vulns);
            }
        } catch (Exception e) {
            logger.error("Failed to query NVD API for {}@{}: {}", packageName, version, e.getMessage());
        }
        
        return results;
    }
    
    private String buildCpeId(String packageName, String ecosystem) {
        String vendor = "*";
        String product = packageName;
        
        if (packageName.contains(":")) {
            String[] parts = packageName.split(":");
            if (parts.length >= 2) {
                vendor = parts[0];
                product = parts[1];
            }
        }
        
        return String.format("cpe:2.3:a:%s:%s:*:*:*:*:*:*:*", vendor, product);
    }
    
    private List<VulnerabilityInfo> parseVulnerabilities(String responseBody, 
                                                          String packageName, String ecosystem, String version) {
        List<VulnerabilityInfo> results = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode vulnsArray = root.path("vulnerabilities");
            
            if (vulnsArray.isArray()) {
                for (JsonNode vulnNode : vulnsArray) {
                    VulnerabilityInfo info = parseVulnerability(vulnNode, packageName, ecosystem, version);
                    if (info != null) {
                        results.add(info);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse NVD response: {}", e.getMessage());
        }
        
        return results;
    }
    
    private VulnerabilityInfo parseVulnerability(JsonNode vulnNode, 
                                                  String packageName, String ecosystem, String version) {
        try {
            JsonNode cveNode = vulnNode.path("cve");
            if (cveNode.isMissingNode()) {
                return null;
            }
            
            VulnerabilityInfo info = new VulnerabilityInfo();
            
            String cveId = cveNode.path("id").asText();
            info.setId(cveId);
            info.setCveId(cveId);
            info.setPackageName(packageName);
            info.setEcosystem(ecosystem);
            info.setVersion(version);
            info.setSource("NVD");
            info.setPriority(getPriority());
            
            JsonNode descriptions = cveNode.path("descriptions");
            if (descriptions.isArray()) {
                for (JsonNode desc : descriptions) {
                    if ("en".equals(desc.path("lang").asText())) {
                        info.setDescription(desc.path("value").asText());
                        break;
                    }
                }
            }
            
            JsonNode metrics = cveNode.path("metrics");
            if (metrics.isArray() && metrics.size() > 0) {
                JsonNode metric = metrics.get(0);
                JsonNode cvssData = metric.path("cvssData");
                if (!cvssData.isMissingNode()) {
                    Double score = cvssData.path("baseScore").asDouble(0.0);
                    info.setCvssScore(score);
                    info.setSeverity(normalizeSeverity(score));
                } else {
                    Double score = metric.path("baseScore").asDouble(0.0);
                    info.setCvssScore(score);
                    info.setSeverity(normalizeSeverity(score));
                }
            } else {
                info.setCvssScore(5.0);
                info.setSeverity("MEDIUM");
            }
            
            info.setTitle(info.getCveId());
            info.setAffectedVersion("unknown");
            info.setFixedVersion("unknown");
            
            String published = cveNode.path("published").asText();
            if (published != null && !published.isEmpty()) {
                try {
                    info.setPublishedAt(LocalDateTime.parse(published, DateTimeFormatter.ISO_DATE_TIME));
                } catch (Exception e) {
                    logger.debug("Failed to parse published date: {}", published);
                }
            }
            
            return info;
        } catch (Exception e) {
            logger.debug("Failed to parse NVD vulnerability: {}", e.getMessage());
            return null;
        }
    }
    
    private String normalizeSeverity(Double score) {
        if (score == null) return "UNKNOWN";
        if (score >= 9.0) return "CRITICAL";
        if (score >= 7.0) return "HIGH";
        if (score >= 4.0) return "MEDIUM";
        return "LOW";
    }
}
