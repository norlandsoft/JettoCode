package com.jettech.code.provider;

import com.jettech.code.client.OsvApiClient;
import com.jettech.code.client.OsvApiClient.OsvVulnerability;
import com.jettech.code.dto.VulnerabilityInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OsvSource implements VulnerabilitySource {
    
    private final OsvApiClient osvApiClient;
    
    public OsvSource(OsvApiClient osvApiClient) {
        this.osvApiClient = osvApiClient;
    }
    
    @Override
    public String getName() {
        return "OSV";
    }
    
    @Override
    public int getPriority() {
        return 1;
    }
    
    @Override
    public List<VulnerabilityInfo> query(String packageName, String ecosystem, String version) {
        List<VulnerabilityInfo> results = new ArrayList<>();
        
        List<OsvVulnerability> osvVulns = osvApiClient.queryVulnerabilities(packageName, ecosystem, version);
        
        for (OsvVulnerability osvVuln : osvVulns) {
            VulnerabilityInfo info = convertToVulnerabilityInfo(osvVuln, packageName, ecosystem, version);
            if (info != null) {
                results.add(info);
            }
        }
        
        return results;
    }
    
    private VulnerabilityInfo convertToVulnerabilityInfo(OsvVulnerability osvVuln, 
                                                          String packageName, String ecosystem, String version) {
        if (osvVuln == null || osvVuln.id == null) {
            return null;
        }
        
        VulnerabilityInfo info = new VulnerabilityInfo();
        info.setId(osvVuln.id);
        info.setCveId(osvVuln.cveId != null ? osvVuln.cveId : osvVuln.id);
        info.setPackageName(packageName);
        info.setEcosystem(ecosystem);
        info.setVersion(version);
        info.setTitle(osvVuln.summary);
        info.setDescription(osvVuln.details);
        info.setSeverity(osvVuln.getNormalizedSeverity());
        info.setCvssScore(osvVuln.getCvssScore());
        info.setAffectedVersion(osvVuln.introducedVersion != null ? ">= " + osvVuln.introducedVersion : "unknown");
        info.setFixedVersion(osvVuln.fixedVersion != null ? ">= " + osvVuln.fixedVersion : "unknown");
        info.setReferences(osvVuln.references);
        info.setSource("OSV");
        info.setPriority(getPriority());
        
        return info;
    }
}
