package com.jettech.code.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OsvApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OsvApiClient.class);
    
    private static final String OSV_API_URL = "https://api.osv.dev/v1";
    private static final String QUERY_URL = OSV_API_URL + "/query";
    private static final String BATCH_QUERY_URL = OSV_API_URL + "/querybatch";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OsvApiClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public List<OsvVulnerability> queryVulnerabilities(String packageName, String ecosystem, String version) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> packageInfo = new HashMap<>();
            packageInfo.put("name", packageName);
            packageInfo.put("ecosystem", ecosystem);
            requestBody.put("package", packageInfo);
            requestBody.put("version", version);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            logger.debug("Querying OSV for {}@{} in {}", packageName, version, ecosystem);
            
            ResponseEntity<String> response = restTemplate.exchange(
                QUERY_URL, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseVulnerabilities(response.getBody());
            }
        } catch (Exception e) {
            logger.error("Failed to query OSV API for {}@{}: {}", packageName, version, e.getMessage());
        }
        
        return List.of();
    }
    
    public Map<String, List<OsvVulnerability>> batchQueryVulnerabilities(List<PackageQuery> queries) {
        Map<String, List<OsvVulnerability>> results = new HashMap<>();
        
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode queriesArray = requestBody.putArray("queries");
            
            for (PackageQuery query : queries) {
                ObjectNode queryNode = queriesArray.addObject();
                ObjectNode packageNode = queryNode.putObject("package");
                packageNode.put("name", query.packageName);
                packageNode.put("ecosystem", query.ecosystem);
                queryNode.put("version", query.version);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            logger.debug("Batch querying OSV for {} packages", queries.size());
            
            ResponseEntity<String> response = restTemplate.exchange(
                BATCH_QUERY_URL, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode resultsArray = root.get("results");
                
                if (resultsArray != null && resultsArray.isArray()) {
                    for (int i = 0; i < resultsArray.size() && i < queries.size(); i++) {
                        PackageQuery query = queries.get(i);
                        JsonNode resultNode = resultsArray.get(i);
                        String key = query.packageName + "@" + query.version;
                        results.put(key, parseVulnerabilitiesFromResult(resultNode));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to batch query OSV API: {}", e.getMessage());
        }
        
        return results;
    }
    
    private List<OsvVulnerability> parseVulnerabilities(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return parseVulnerabilitiesFromResult(root);
        } catch (Exception e) {
            logger.error("Failed to parse OSV response: {}", e.getMessage());
            return List.of();
        }
    }
    
    private List<OsvVulnerability> parseVulnerabilitiesFromResult(JsonNode resultNode) {
        List<OsvVulnerability> vulnerabilities = new ArrayList<>();
        
        JsonNode vulnsArray = resultNode.get("vulns");
        if (vulnsArray == null || !vulnsArray.isArray()) {
            return vulnerabilities;
        }
        
        for (JsonNode vulnNode : vulnsArray) {
            try {
                OsvVulnerability vuln = parseVulnerability(vulnNode);
                if (vuln != null) {
                    vulnerabilities.add(vuln);
                }
            } catch (Exception e) {
                logger.debug("Failed to parse vulnerability: {}", e.getMessage());
            }
        }
        
        return vulnerabilities;
    }
    
    private OsvVulnerability parseVulnerability(JsonNode vulnNode) {
        OsvVulnerability vuln = new OsvVulnerability();
        
        vuln.id = getTextValue(vulnNode, "id");
        vuln.summary = getTextValue(vulnNode, "summary");
        vuln.details = getTextValue(vulnNode, "details");
        
        JsonNode severityArray = vulnNode.get("severity");
        if (severityArray != null && severityArray.isArray() && severityArray.size() > 0) {
            JsonNode severityNode = severityArray.get(0);
            vuln.severityType = getTextValue(severityNode, "type");
            vuln.severityScore = getTextValue(severityNode, "score");
        }
        
        JsonNode creditsNode = vulnNode.get("credits");
        if (creditsNode != null && creditsNode.isArray() && creditsNode.size() > 0) {
            vuln.credits = getTextValue(creditsNode.get(0), "name");
        }
        
        JsonNode affectedArray = vulnNode.get("affected");
        if (affectedArray != null && affectedArray.isArray() && affectedArray.size() > 0) {
            JsonNode affectedNode = affectedArray.get(0);
            vuln.affectedPackage = getTextValue(affectedNode.get("package"), "name");
            vuln.affectedEcosystem = getTextValue(affectedNode.get("package"), "ecosystem");
            
            JsonNode rangesArray = affectedNode.get("ranges");
            if (rangesArray != null && rangesArray.isArray()) {
                for (JsonNode range : rangesArray) {
                    JsonNode events = range.get("events");
                    if (events != null && events.isArray()) {
                        for (JsonNode event : events) {
                            if (event.has("introduced")) {
                                vuln.introducedVersion = event.get("introduced").asText();
                            }
                            if (event.has("fixed")) {
                                vuln.fixedVersion = event.get("fixed").asText();
                            }
                        }
                    }
                }
            }
        }
        
        JsonNode refsArray = vulnNode.get("references");
        if (refsArray != null && refsArray.isArray()) {
            StringBuilder refs = new StringBuilder();
            for (JsonNode ref : refsArray) {
                if (refs.length() > 0) refs.append("\n");
                refs.append(getTextValue(ref, "url"));
            }
            vuln.references = refs.toString();
        }
        
        if (vuln.id != null && (vuln.id.startsWith("CVE-") || vuln.id.contains("-CVE-"))) {
            vuln.cveId = vuln.id;
        } else {
            JsonNode aliases = vulnNode.get("aliases");
            if (aliases != null && aliases.isArray()) {
                for (JsonNode alias : aliases) {
                    String aliasStr = alias.asText();
                    if (aliasStr.startsWith("CVE-")) {
                        vuln.cveId = aliasStr;
                        break;
                    }
                }
            }
        }
        
        return vuln;
    }
    
    private String getTextValue(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode fieldNode = node.get(field);
        return fieldNode != null ? fieldNode.asText() : null;
    }
    
    public static class PackageQuery {
        public String packageName;
        public String ecosystem;
        public String version;
        
        public PackageQuery(String packageName, String ecosystem, String version) {
            this.packageName = packageName;
            this.ecosystem = ecosystem;
            this.version = version;
        }
    }
    
    public static class OsvVulnerability {
        public String id;
        public String cveId;
        public String summary;
        public String details;
        public String severityType;
        public String severityScore;
        public String affectedPackage;
        public String affectedEcosystem;
        public String introducedVersion;
        public String fixedVersion;
        public String references;
        public String credits;
        
        public String getNormalizedSeverity() {
            if (severityScore != null && severityType != null) {
                if ("CVSS_V3".equals(severityType) || "CVSS_V2".equals(severityType)) {
                    try {
                        double score = Double.parseDouble(severityScore);
                        if (score >= 9.0) return "CRITICAL";
                        if (score >= 7.0) return "HIGH";
                        if (score >= 4.0) return "MEDIUM";
                        return "LOW";
                    } catch (NumberFormatException e) {
                        // Fall through
                    }
                }
            }
            return "MEDIUM";
        }
        
        public Double getCvssScore() {
            if (severityScore != null) {
                try {
                    return Double.parseDouble(severityScore);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }
}
