package com.jettech.code.service;

import com.jettech.code.entity.Dependency;
import com.jettech.code.entity.LicenseRule;
import com.jettech.code.mapper.LicenseRuleMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LicenseDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(LicenseDetector.class);
    
    private final LicenseRuleMapper licenseRuleMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private final Map<String, String> licenseCache = new ConcurrentHashMap<>();
    
    private static final List<String> APPROVED_LICENSES = List.of(
        "MIT", "Apache-2.0", "Apache 2.0", "BSD-2-Clause", "BSD-3-Clause",
        "BSD-2-Clause-FreeBSD", "ISC", "0BSD", "Artistic-2.0", "Zlib", 
        "Unlicense", "PostgreSQL", "NCSA", "X11", "W3C", "JSON"
    );
    
    private static final Map<String, Pattern> LICENSE_PATTERNS = new LinkedHashMap<>();
    
    static {
        LICENSE_PATTERNS.put("MIT", Pattern.compile("MIT License|Permission is hereby granted, free of charge", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("Apache-2.0", Pattern.compile("Apache License.*2\\.0|Licensed under the Apache License", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("BSD-3-Clause", Pattern.compile("BSD 3-Clause|Redistribution and use in source and binary forms", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("BSD-2-Clause", Pattern.compile("BSD 2-Clause|Redistribution and use in source and binary forms", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("ISC", Pattern.compile("ISC License|Permission to use, copy, modify, and/or distribute", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("GPL-3.0", Pattern.compile("GNU General Public License.*3|GNU GPL", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("LGPL-3.0", Pattern.compile("GNU Lesser General Public License|GNU LGPL", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("MPL-2.0", Pattern.compile("Mozilla Public License.*2", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("EPL-1.0", Pattern.compile("Eclipse Public License|EPL", Pattern.CASE_INSENSITIVE));
        LICENSE_PATTERNS.put("Unlicense", Pattern.compile("This is free and unencumbered software released into the public domain", Pattern.CASE_INSENSITIVE));
    }

    public LicenseDetector(LicenseRuleMapper licenseRuleMapper) {
        this.licenseRuleMapper = licenseRuleMapper;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public void detectLicense(Dependency dependency, String projectPath) {
        String license = null;
        String source = null;
        
        if (license == null) {
            license = detectFromFile(dependency, projectPath);
            if (license != null) source = "file";
        }
        
        if (license == null) {
            license = detectFromRegistry(dependency);
            if (license != null) source = "registry";
        }
        
        if (license == null) {
            license = detectFromRules(dependency);
            if (license != null) source = "rule";
        }
        
        if (license == null || license.isEmpty()) {
            license = "Unknown";
            source = "default";
        }
        
        dependency.setLicense(license);
        dependency.setLicenseStatus(determineLicenseStatus(license));
    }
    
    private String detectFromFile(Dependency dependency, String projectPath) {
        if (projectPath == null || dependency.getName() == null) {
            return null;
        }
        
        String cacheKey = "file:" + dependency.getName();
        if (licenseCache.containsKey(cacheKey)) {
            return licenseCache.get(cacheKey);
        }
        
        String license = findLicenseInDependencyDir(dependency, projectPath);
        licenseCache.put(cacheKey, license != null ? license : "");
        
        return license;
    }
    
    private String findLicenseInDependencyDir(Dependency dependency, String projectPath) {
        String[] possiblePaths = {
            "node_modules/" + dependency.getName(),
            "vendor/" + dependency.getName(),
            dependency.getName()
        };
        
        for (String subPath : possiblePaths) {
            Path depDir = Path.of(projectPath, subPath);
            if (Files.isDirectory(depDir)) {
                String license = scanForLicense(depDir.toFile());
                if (license != null) {
                    return license;
                }
            }
        }
        
        return null;
    }
    
    private String scanForLicense(File dir) {
        String[] licenseFiles = {"LICENSE", "LICENSE.md", "LICENSE.txt", "license", "LICENCE"};
        
        for (String fileName : licenseFiles) {
            File licenseFile = new File(dir, fileName);
            if (licenseFile.exists()) {
                try {
                    String content = Files.readString(licenseFile.toPath());
                    return identifyLicenseFromContent(content);
                } catch (Exception e) {
                    logger.debug("Failed to read license file: {}", licenseFile.getPath());
                }
            }
        }
        
        File packageJson = new File(dir, "package.json");
        if (packageJson.exists()) {
            try {
                JsonNode root = objectMapper.readTree(packageJson);
                if (root.has("license")) {
                    JsonNode licenseNode = root.get("license");
                    String license = licenseNode.isTextual() ? licenseNode.asText() :
                                    licenseNode.isArray() ? licenseNode.get(0).asText() : null;
                    if (license != null && !license.isEmpty()) {
                        return normalizeLicense(license);
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to read package.json: {}", packageJson.getPath());
            }
        }
        
        return null;
    }
    
    private String identifyLicenseFromContent(String content) {
        for (Map.Entry<String, Pattern> entry : LICENSE_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(content);
            if (matcher.find()) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private String detectFromRegistry(Dependency dependency) {
        String type = dependency.getType();
        if (type == null) return null;
        
        String cacheKey = "registry:" + type + ":" + dependency.getName();
        if (licenseCache.containsKey(cacheKey)) {
            return licenseCache.get(cacheKey);
        }
        
        String license = null;
        
        try {
            switch (type.toLowerCase()) {
                case "npm":
                    license = detectFromNpmRegistry(dependency.getName());
                    break;
                case "pypi":
                    license = detectFromPyPIRegistry(dependency.getName());
                    break;
                case "maven":
                    license = detectFromMavenCentral(dependency.getGroupId(), dependency.getArtifactId());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.debug("Failed to detect license from registry for {}: {}", 
                        dependency.getName(), e.getMessage());
        }
        
        licenseCache.put(cacheKey, license != null ? license : "");
        return license;
    }
    
    private String detectFromNpmRegistry(String packageName) {
        try {
            String url = "https://registry.npmjs.org/" + packageName;
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode licenseNode = root.get("license");
                
                if (licenseNode != null) {
                    String license = licenseNode.isTextual() ? licenseNode.asText() :
                                    licenseNode.has("type") ? licenseNode.get("type").asText() : null;
                    return normalizeLicense(license);
                }
            }
        } catch (Exception e) {
            logger.debug("NPM registry lookup failed for {}: {}", packageName, e.getMessage());
        }
        return null;
    }
    
    private String detectFromPyPIRegistry(String packageName) {
        try {
            String url = "https://pypi.org/pypi/" + packageName + "/json";
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode info = root.get("info");
                if (info != null && info.has("license")) {
                    String license = info.get("license").asText();
                    if (license != null && !license.isEmpty()) {
                        return normalizeLicense(license);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("PyPI registry lookup failed for {}: {}", packageName, e.getMessage());
        }
        return null;
    }
    
    private String detectFromMavenCentral(String groupId, String artifactId) {
        if (groupId == null || artifactId == null) {
            return null;
        }
        
        try {
            String groupPath = groupId.replace('.', '/');
            String url = "https://repo1.maven.org/maven2/" + groupPath + "/" + artifactId + "/maven-metadata.xml";
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                Pattern versionPattern = Pattern.compile("<latest>([^<]+)</latest>");
                Matcher matcher = versionPattern.matcher(response);
                if (matcher.find()) {
                    String latestVersion = matcher.group(1);
                    return fetchMavenLicense(groupId, artifactId, latestVersion);
                }
            }
        } catch (Exception e) {
            logger.debug("Maven Central lookup failed for {}:{}: {}", groupId, artifactId, e.getMessage());
        }
        return null;
    }
    
    private String fetchMavenLicense(String groupId, String artifactId, String version) {
        try {
            String groupPath = groupId.replace('.', '/');
            String url = "https://repo1.maven.org/maven2/" + groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                Pattern licensePattern = Pattern.compile("<license>\\s*<name>([^<]+)</name>", Pattern.DOTALL);
                Matcher matcher = licensePattern.matcher(response);
                if (matcher.find()) {
                    String license = matcher.group(1).trim();
                    return normalizeLicense(license);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to fetch Maven POM for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
        }
        return null;
    }
    
    private String detectFromRules(Dependency dependency) {
        List<LicenseRule> rules = licenseRuleMapper.findAll();
        String packageName = dependency.getName();
        
        for (LicenseRule rule : rules) {
            String pattern = rule.getPackagePattern();
            if (pattern.endsWith("*")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                if (packageName.startsWith(prefix)) {
                    return rule.getLicenseType();
                }
            } else if (pattern.startsWith("*")) {
                String suffix = pattern.substring(1);
                if (packageName.endsWith(suffix)) {
                    return rule.getLicenseType();
                }
            } else if (packageName.equals(pattern)) {
                return rule.getLicenseType();
            }
        }
        
        return detectFromBuiltInRules(packageName, dependency.getType());
    }
    
    private String detectFromBuiltInRules(String packageName, String type) {
        String lowerName = packageName.toLowerCase();
        
        if (lowerName.contains("spring") || lowerName.contains("springframework")) return "Apache-2.0";
        if (lowerName.contains("commons") || lowerName.contains("apache")) return "Apache-2.0";
        if (lowerName.contains("jackson")) return "Apache-2.0";
        if (lowerName.contains("slf4j")) return "MIT";
        if (lowerName.contains("log4j")) return "Apache-2.0";
        if (lowerName.contains("logback")) return "EPL-1.0";
        if (lowerName.contains("junit")) return "EPL-1.0";
        if (lowerName.contains("mockito")) return "MIT";
        if (lowerName.contains("hibernate")) return "LGPL-2.1";
        if (lowerName.contains("tomcat")) return "Apache-2.0";
        if (lowerName.contains("netty")) return "Apache-2.0";
        if (lowerName.contains("guava")) return "Apache-2.0";
        if (lowerName.contains("gson")) return "Apache-2.0";
        if (lowerName.contains("okhttp")) return "Apache-2.0";
        if (lowerName.contains("retrofit")) return "Apache-2.0";
        if (lowerName.contains("dagger")) return "Apache-2.0";
        if (lowerName.contains("jwt")) return "Apache-2.0";
        if (lowerName.contains("mybatis")) return "Apache-2.0";
        if (lowerName.contains("druid")) return "Apache-2.0";
        if (lowerName.contains("hikari")) return "Apache-2.0";
        if (lowerName.contains("flyway")) return "Apache-2.0";
        if (lowerName.contains("liquibase")) return "Apache-2.0";
        if (lowerName.contains("quartz")) return "Apache-2.0";
        if (lowerName.contains("jjwt")) return "Apache-2.0";
        if (lowerName.contains("swagger")) return "Apache-2.0";
        if (lowerName.contains("lombok")) return "MIT";
        if (lowerName.contains("mapstruct")) return "Apache-2.0";
        if (lowerName.contains("poi")) return "Apache-2.0";
        if (lowerName.contains("pdfbox")) return "Apache-2.0";
        if (lowerName.contains("tika")) return "Apache-2.0";
        if (lowerName.contains("zookeeper")) return "Apache-2.0";
        if (lowerName.contains("kafka")) return "Apache-2.0";
        if (lowerName.contains("rocketmq")) return "Apache-2.0";
        if (lowerName.contains("dubbo")) return "Apache-2.0";
        if (lowerName.contains("curator")) return "Apache-2.0";
        if (lowerName.contains("thrift")) return "Apache-2.0";
        if (lowerName.contains("grpc")) return "Apache-2.0";
        if (lowerName.contains("protobuf")) return "BSD-3-Clause";
        if (lowerName.contains("snappy")) return "BSD-3-Clause";
        if (lowerName.contains("lz4")) return "Apache-2.0";
        if (lowerName.contains("jna")) return "Apache-2.0";
        if (lowerName.contains("jni")) return "Apache-2.0";
        if (lowerName.contains("hadoop")) return "Apache-2.0";
        if (lowerName.contains("spark")) return "Apache-2.0";
        if (lowerName.contains("flink")) return "Apache-2.0";
        if (lowerName.contains("elasticsearch")) return "Apache-2.0";
        if (lowerName.contains("lucene")) return "Apache-2.0";
        if (lowerName.contains("solr")) return "Apache-2.0";
        if (lowerName.contains("jedis")) return "MIT";
        if (lowerName.contains("lettuce")) return "Apache-2.0";
        if (lowerName.contains("mongo")) return "Apache-2.0";
        if (lowerName.contains("postgresql")) return "PostgreSQL";
        if (lowerName.contains("mysql")) return "GPL-2.0";
        if (lowerName.contains("mariadb")) return "LGPL-2.1";
        if (lowerName.contains("h2")) return "MPL-2.0";
        if (lowerName.contains("hsqldb")) return "BSD-3-Clause";
        if (lowerName.contains("derby")) return "Apache-2.0";
        if (lowerName.contains("oshi")) return "MIT";
        if (lowerName.contains("javassist")) return "Apache-2.0";
        if (lowerName.contains("cglib")) return "Apache-2.0";
        if (lowerName.contains("asm")) return "BSD-3-Clause";
        if (lowerName.contains("bytebuddy")) return "Apache-2.0";
        if (lowerName.contains("reflections")) return "WTFPL";
        if (lowerName.contains("joda")) return "Apache-2.0";
        if (lowerName.contains("threeten")) return "BSD-3-Clause";
        if (lowerName.contains("icu4j")) return "ICU";
        if (lowerName.contains("bouncycastle") || lowerName.contains("bcprov")) return "MIT";
        if (lowerName.contains("crypto") || lowerName.contains("cipher")) return "Apache-2.0";
        if (lowerName.contains("selenium")) return "Apache-2.0";
        
        if (lowerName.contains("react")) return "MIT";
        if (lowerName.contains("vue")) return "MIT";
        if (lowerName.contains("angular")) return "MIT";
        if (lowerName.contains("express")) return "MIT";
        if (lowerName.contains("lodash")) return "MIT";
        if (lowerName.contains("axios")) return "MIT";
        if (lowerName.contains("typescript")) return "Apache-2.0";
        if (lowerName.contains("webpack")) return "MIT";
        if (lowerName.contains("babel")) return "MIT";
        if (lowerName.contains("eslint")) return "MIT";
        if (lowerName.contains("prettier")) return "MIT";
        if (lowerName.contains("jest")) return "MIT";
        if (lowerName.contains("mocha")) return "MIT";
        if (lowerName.contains("tailwind")) return "MIT";
        if (lowerName.contains("antd") || lowerName.contains("ant-design")) return "MIT";
        
        if (lowerName.contains("numpy")) return "BSD-3-Clause";
        if (lowerName.contains("pandas")) return "BSD-3-Clause";
        if (lowerName.contains("requests")) return "Apache-2.0";
        if (lowerName.contains("django")) return "BSD-3-Clause";
        if (lowerName.contains("flask")) return "BSD-3-Clause";
        if (lowerName.contains("scipy")) return "BSD-3-Clause";
        if (lowerName.contains("pytest")) return "MIT";
        if (lowerName.contains("pillow")) return "PIL";
        if (lowerName.contains("sqlalchemy")) return "MIT";
        if (lowerName.contains("celery")) return "BSD-3-Clause";
        if (lowerName.contains("redis")) return "MIT";
        if (lowerName.contains("psycopg")) return "LGPL-3.0";
        
        if (lowerName.contains("gin")) return "MIT";
        if (lowerName.contains("echo")) return "MIT";
        if (lowerName.contains("fiber")) return "MIT";
        if (lowerName.contains("gorm")) return "MIT";
        
        return null;
    }
    
    private String normalizeLicense(String license) {
        if (license == null || license.isEmpty()) {
            return null;
        }
        
        license = license.trim();
        
        Map<String, String> normalizations = new HashMap<>();
        normalizations.put("MIT License", "MIT");
        normalizations.put("The MIT License", "MIT");
        normalizations.put("Apache License 2.0", "Apache-2.0");
        normalizations.put("Apache License, Version 2.0", "Apache-2.0");
        normalizations.put("The Apache Software License, Version 2.0", "Apache-2.0");
        normalizations.put("Apache-2", "Apache-2.0");
        normalizations.put("BSD 3-Clause", "BSD-3-Clause");
        normalizations.put("BSD 2-Clause", "BSD-2-Clause");
        normalizations.put("The BSD License", "BSD-3-Clause");
        normalizations.put("ISC License", "ISC");
        normalizations.put("GPL-3", "GPL-3.0");
        normalizations.put("LGPL-3", "LGPL-3.0");
        normalizations.put("GNU Lesser General Public License, Version 2.1", "LGPL-2.1");
        normalizations.put("CDDL 1.0", "CDDL-1.0");
        normalizations.put("Common Development and Distribution License 1.0", "CDDL-1.0");
        normalizations.put("EPL 1.0", "EPL-1.0");
        normalizations.put("Eclipse Public License 1.0", "EPL-1.0");
        normalizations.put("Eclipse Public License - v 1.0", "EPL-1.0");
        normalizations.put("Eclipse Public License v2.0", "EPL-2.0");
        normalizations.put("Mozilla Public License 1.1", "MPL-1.1");
        normalizations.put("Mozilla Public License 2.0", "MPL-2.0");
        normalizations.put("PostgreSQL License", "PostgreSQL");
        normalizations.put("The PostgreSQL License", "PostgreSQL");
        
        String result = normalizations.get(license);
        if (result != null) {
            return result;
        }
        
        for (Map.Entry<String, String> entry : normalizations.entrySet()) {
            if (license.contains(entry.getKey()) || entry.getKey().contains(license)) {
                return entry.getValue();
            }
        }
        
        return license;
    }
    
    private String determineLicenseStatus(String license) {
        if (license == null || license.isEmpty() || "Unknown".equals(license)) {
            return "UNKNOWN";
        }
        
        String normalized = normalizeLicense(license);
        
        if (APPROVED_LICENSES.contains(normalized)) {
            return "APPROVED";
        }
        
        for (String approved : APPROVED_LICENSES) {
            if (normalized.contains(approved) || approved.contains(normalized)) {
                return "APPROVED";
            }
        }
        
        return "VIOLATION";
    }
    
    public void clearCache() {
        licenseCache.clear();
    }
}
