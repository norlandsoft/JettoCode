package com.jettech.code.parser;

import com.jettech.code.entity.Dependency;
import java.io.File;
import java.util.List;

public interface DependencyParser {
    
    boolean supports(File projectDir);
    
    List<Dependency> parse(Long serviceId, File projectDir) throws Exception;
    
    String getEcosystem();
}
