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
import org.springframework.http.client.SimpleClientHttpRequestFactory;

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
    private final RestTemplate restTemplateWithTimeout;
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
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplateWithTimeout = new RestTemplate(factory);
    }
    
    public void detectLicense(Dependency dependency, String projectPath) {
        String license = null;
        String source = null;
        
        license = detectFromBuiltInRules(dependency.getName(), dependency.getType());
        if (license != null) {
            source = "builtin";
        }
        
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
        
        if (license == null) {
            license = detectFromDepsDev(dependency);
            if (license != null) source = "depsdev";
        }
        
        if (license == null || license.isEmpty()) {
            license = "Unknown";
            source = "default";
        }
        
        dependency.setLicense(license);
        dependency.setLicenseStatus(determineLicenseStatus(license));
        
        logger.debug("License for {}: {} (source: {})", dependency.getName(), license, source);
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
            String response = restTemplateWithTimeout.getForObject(url, String.class);
            
            if (response != null) {
                Pattern[] licensePatterns = {
                    Pattern.compile("<licenses>\\s*<license>\\s*<name>([^<]+)</name>", Pattern.DOTALL),
                    Pattern.compile("<license>\\s*<name>([^<]+)</name>", Pattern.DOTALL),
                    Pattern.compile("<license>\\s*<url>([^<]+)</url>", Pattern.DOTALL),
                    Pattern.compile("<licenses>.*?<name>([^<]+)</name>.*?</licenses>", Pattern.DOTALL)
                };
                
                for (Pattern pattern : licensePatterns) {
                    Matcher matcher = pattern.matcher(response);
                    if (matcher.find()) {
                        String license = matcher.group(1).trim();
                        if (!license.isEmpty() && !license.startsWith("http")) {
                            return normalizeLicense(license);
                        }
                    }
                }
                
                if (response.contains("<licenses>")) {
                    Pattern namePattern = Pattern.compile("<name>([^<]+)</name>");
                    Matcher nameMatcher = namePattern.matcher(response.substring(response.indexOf("<licenses>")));
                    if (nameMatcher.find()) {
                        String license = nameMatcher.group(1).trim();
                        if (!license.isEmpty()) {
                            return normalizeLicense(license);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to fetch Maven POM for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
        }
        return null;
    }
    
    private String detectFromDepsDev(Dependency dependency) {
        String type = dependency.getType();
        if (type == null) return null;
        
        String ecosystem = switch (type.toLowerCase()) {
            case "maven" -> "maven";
            case "npm" -> "npm";
            case "pypi" -> "pypi";
            case "golang" -> "go";
            default -> null;
        };
        
        if (ecosystem == null) return null;
        
        String packageName;
        if ("maven".equals(ecosystem)) {
            if (dependency.getGroupId() == null || dependency.getArtifactId() == null) return null;
            packageName = dependency.getGroupId() + ":" + dependency.getArtifactId();
        } else {
            if (dependency.getName() == null) return null;
            packageName = dependency.getName();
        }
        
        String cacheKey = "depsdev:" + ecosystem + ":" + packageName;
        if (licenseCache.containsKey(cacheKey)) {
            return licenseCache.get(cacheKey);
        }
        
        try {
            String url = "https://api.deps.dev/v3/systems/" + ecosystem + "/packages/" + 
                        java.net.URLEncoder.encode(packageName, "UTF-8");
            String response = restTemplateWithTimeout.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode version = root.path("version");
                if (version.has("licenses")) {
                    JsonNode licenses = version.get("licenses");
                    if (licenses.isArray() && licenses.size() > 0) {
                        String license = licenses.get(0).asText();
                        licenseCache.put(cacheKey, normalizeLicense(license));
                        return normalizeLicense(license);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Deps.dev lookup failed for {}: {}", packageName, e.getMessage());
        }
        
        licenseCache.put(cacheKey, "");
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
        if (packageName == null) return null;
        
        String lowerName = packageName.toLowerCase();
        String fullName = packageName;
        
        if (lowerName.startsWith("org.springframework") || lowerName.contains("springframework") ||
            lowerName.contains("spring-boot") || lowerName.contains("springcloud") ||
            lowerName.contains("spring-security") || lowerName.contains("spring-data")) {
            return "Apache-2.0";
        }
        
        if (lowerName.startsWith("org.apache.commons") || lowerName.startsWith("commons-") ||
            lowerName.contains("apache-http") || lowerName.startsWith("org.apache.http")) {
            return "Apache-2.0";
        }
        
        if (lowerName.startsWith("com.fasterxml.jackson") || lowerName.contains("jackson-")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("slf4j") || lowerName.startsWith("org.slf4j")) {
            return "MIT";
        }
        
        if (lowerName.contains("log4j") || lowerName.startsWith("org.apache.logging.log4j")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("logback") || lowerName.startsWith("ch.qos.logback")) {
            return "EPL-1.0";
        }
        
        if (lowerName.contains("junit") || lowerName.startsWith("org.junit")) {
            return "EPL-2.0";
        }
        
        if (lowerName.contains("mockito") || lowerName.startsWith("org.mockito")) {
            return "MIT";
        }
        
        if (lowerName.contains("hibernate") || lowerName.startsWith("org.hibernate")) {
            return "LGPL-2.1";
        }
        
        if (lowerName.contains("tomcat") || lowerName.startsWith("org.apache.tomcat")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("netty") || lowerName.startsWith("io.netty")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("guava") || lowerName.startsWith("com.google.guava")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("gson") || lowerName.startsWith("com.google.gson") ||
            lowerName.contains("google-gson")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("okhttp") || lowerName.contains("okio") || 
            lowerName.startsWith("com.squareup.okhttp") || lowerName.startsWith("com.squareup.okio")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("retrofit") || lowerName.startsWith("com.squareup.retrofit")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("dagger") || lowerName.startsWith("com.google.dagger") ||
            lowerName.startsWith("dagger")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("jjwt") || lowerName.startsWith("io.jsonwebtoken")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("mybatis") || lowerName.contains("mybatis-plus") ||
            lowerName.startsWith("org.mybatis") || lowerName.startsWith("com.baomidou")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("druid") || lowerName.startsWith("com.alibaba.druid")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("hikari") || lowerName.startsWith("com.zaxxer.hikari")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("flyway") || lowerName.startsWith("org.flywaydb")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("liquibase") || lowerName.startsWith("org.liquibase")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("quartz") || lowerName.startsWith("org.quartz-scheduler")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("swagger") || lowerName.contains("openapi") ||
            lowerName.startsWith("io.swagger") || lowerName.startsWith("org.springdoc")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("lombok") || lowerName.startsWith("org.projectlombok")) {
            return "MIT";
        }
        
        if (lowerName.contains("mapstruct") || lowerName.startsWith("org.mapstruct")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("poi") || lowerName.startsWith("org.apache.poi")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("pdfbox") || lowerName.startsWith("org.apache.pdfbox")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("tika") || lowerName.startsWith("org.apache.tika")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("zookeeper") || lowerName.startsWith("org.apache.zookeeper")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("kafka") || lowerName.startsWith("org.apache.kafka")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("rocketmq") || lowerName.startsWith("org.apache.rocketmq")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("dubbo") || lowerName.startsWith("org.apache.dubbo")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("curator") || lowerName.startsWith("org.apache.curator")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("thrift") || lowerName.startsWith("org.apache.thrift")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("grpc") || lowerName.startsWith("io.grpc")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("protobuf") || lowerName.startsWith("com.google.protobuf")) {
            return "BSD-3-Clause";
        }
        
        if (lowerName.contains("snappy") || lowerName.startsWith("org.xerial.snappy")) {
            return "BSD-3-Clause";
        }
        
        if (lowerName.contains("lz4") || lowerName.startsWith("org.lz4")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("jna") || lowerName.startsWith("net.java.dev.jna")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("hadoop") || lowerName.startsWith("org.apache.hadoop")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("spark") || lowerName.startsWith("org.apache.spark")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("flink") || lowerName.startsWith("org.apache.flink")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("elasticsearch") || lowerName.startsWith("org.elasticsearch")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("lucene") || lowerName.startsWith("org.apache.lucene")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("solr") || lowerName.startsWith("org.apache.solr")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("jedis") || lowerName.startsWith("redis.clients")) {
            return "MIT";
        }
        
        if (lowerName.contains("lettuce") || lowerName.startsWith("io.lettuce")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("mongo") || lowerName.startsWith("org.mongodb")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("postgresql") || lowerName.startsWith("org.postgresql")) {
            return "PostgreSQL";
        }
        
        if (lowerName.contains("mysql") || lowerName.startsWith("mysql") || 
            lowerName.startsWith("com.mysql")) {
            return "GPL-2.0";
        }
        
        if (lowerName.contains("mariadb") || lowerName.startsWith("org.mariadb.jdbc")) {
            return "LGPL-2.1";
        }
        
        if (lowerName.contains("h2") || lowerName.startsWith("com.h2database")) {
            return "MPL-2.0";
        }
        
        if (lowerName.contains("hsqldb") || lowerName.startsWith("org.hsqldb")) {
            return "BSD-3-Clause";
        }
        
        if (lowerName.contains("derby") || lowerName.startsWith("org.apache.derby")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("oshi") || lowerName.startsWith("com.github.oshi")) {
            return "MIT";
        }
        
        if (lowerName.contains("javassist") || lowerName.startsWith("org.javassist")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("cglib") || lowerName.startsWith("cglib")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("asm") || lowerName.startsWith("org.ow2.asm")) {
            return "BSD-3-Clause";
        }
        
        if (lowerName.contains("bytebuddy") || lowerName.contains("byte-buddy") ||
            lowerName.startsWith("net.bytebuddy")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("reflections") || lowerName.startsWith("org.reflections")) {
            return "WTFPL";
        }
        
        if (lowerName.contains("joda") || lowerName.startsWith("joda-time") ||
            lowerName.startsWith("org.joda")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("icu4j") || lowerName.startsWith("com.ibm.icu")) {
            return "ICU";
        }
        
        if (lowerName.contains("bouncycastle") || lowerName.contains("bcprov") ||
            lowerName.contains("bcpkix") || lowerName.startsWith("org.bouncycastle")) {
            return "MIT";
        }
        
        if (lowerName.contains("selenium") || lowerName.startsWith("org.seleniumhq.selenium")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("hutool") || lowerName.startsWith("cn.hutool")) {
            return "MulanPSL-2.0";
        }
        
        if (lowerName.contains("fastjson") || lowerName.startsWith("com.alibaba.fastjson")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("druid") || lowerName.startsWith("com.alibaba.druid")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("dubbo") || lowerName.startsWith("com.alibaba.dubbo") ||
            lowerName.startsWith("org.apache.dubbo")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("nacos") || lowerName.startsWith("com.alibaba.nacos") ||
            lowerName.startsWith("io.nacos")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("sentinel") || lowerName.startsWith("com.alibaba.csp")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("seata") || lowerName.startsWith("io.seata")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("xxl-job") || lowerName.startsWith("com.xuxueli")) {
            return "GPL-3.0";
        }
        
        if (lowerName.contains("knife4j") || lowerName.startsWith("com.github.xiaoymin")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("sa-token") || lowerName.startsWith("cn.dev33")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("easyexcel") || lowerName.startsWith("com.alibaba.easyexcel")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("transmittable-thread-local") || 
            lowerName.startsWith("com.alibaba.ttl")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("rxjava") || lowerName.startsWith("io.reactivex")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("reactor") || lowerName.startsWith("io.projectreactor") ||
            lowerName.startsWith("reactor-core") || lowerName.startsWith("reactor-netty")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("feign") || lowerName.startsWith("io.github.openfeign")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("ribbon") || lowerName.startsWith("com.netflix.ribbon")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("eureka") || lowerName.startsWith("com.netflix.eureka")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("hystrix") || lowerName.startsWith("com.netflix.hystrix")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("zuul") || lowerName.startsWith("com.netflix.zuul")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("archaius") || lowerName.startsWith("com.netflix.archaius")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("resilience4j") || lowerName.startsWith("io.github.resilience4j")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("caffeine") || lowerName.startsWith("com.github.benmanes.caffeine")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("disruptor") || lowerName.startsWith("com.lmax.disruptor")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("jool") || lowerName.startsWith("org.jooq.jool")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("joor") || lowerName.startsWith("org.jooq.joor")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("vavr") || lowerName.startsWith("io.vavr")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("assertj") || lowerName.startsWith("org.assertj")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("hamcrest") || lowerName.startsWith("org.hamcrest")) {
            return "BSD-3-Clause";
        }
        
        if (lowerName.contains("json-path") || lowerName.contains("jsonpath") ||
            lowerName.startsWith("com.jayway.jsonpath")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("jsonassert") || lowerName.startsWith("org.skyscreamer")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("xmlunit") || lowerName.startsWith("org.xmlunit")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("awaitility") || lowerName.startsWith("org.awaitility")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("testcontainers") || lowerName.startsWith("org.testcontainers")) {
            return "MIT";
        }
        
        if (lowerName.contains("wiremock") || lowerName.startsWith("com.github.tomakehurst") ||
            lowerName.startsWith("org.wiremock")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("mockserver") || lowerName.startsWith("org.mock-server")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("okhttptest") || lowerName.startsWith("com.squareup.okhttp3")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("classgraph") || lowerName.startsWith("io.github.classgraph")) {
            return "MIT";
        }
        
        if (lowerName.contains("jcommander") || lowerName.startsWith("com.beust.jcommander")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("picocli") || lowerName.startsWith("info.picocli")) {
            return "Apache-2.0";
        }
        
        if (lowerName.contains("commons-cli") || lowerName.startsWith("commons-cli")) {
            return "Apache-2.0";
        }
        
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
