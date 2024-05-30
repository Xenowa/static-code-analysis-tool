/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan.utils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code ScanTomlFile} contains an in-memory representation of the scan.toml file.
 *
 * @since 0.1.0
 * */
public class ScanTomlFile {
    private final Set<Platform> platforms;
    private final Set<Analyzer> analyzers;
    private final Set<RuleToFilter> rulesToInclude;
    private final Set<RuleToFilter> rulesToExclude;

    ScanTomlFile() {
        this.platforms = new LinkedHashSet<>();
        this.analyzers = new LinkedHashSet<>();
        this.rulesToInclude = new LinkedHashSet<>();
        this.rulesToExclude = new LinkedHashSet<>();
    }

    void setPlatform(Platform platform) {
        platforms.add(platform);
    }

    void setAnalyzer(Analyzer analyzer) {
        analyzers.add(analyzer);
    }

    void setRuleToInclude(RuleToFilter rule) {
        rulesToInclude.add(rule);
    }

    void setRuleToExclude(RuleToFilter rule) {
        rulesToExclude.add(rule);
    }

    /**
     * Returns an unmodifiable {@link Set} of platforms.
     *
     * @return an unmodifiable set of platforms
     */
    public Set<Platform> getPlatforms() {
        return Collections.unmodifiableSet(platforms);
    }

    /**
     * Returns an unmodifiable {@link Set} of analyzers.
     *
     * @return an unmodifiable set of analyzers
     */
    public Set<Analyzer> getAnalyzers() {
        return Collections.unmodifiableSet(analyzers);
    }

    /**
     * Returns an unmodifiable {@link Set} of rules to include.
     *
     * @return an unmodifiable set of rules to include
     */
    public Set<RuleToFilter> getRulesToInclude() {
        return Collections.unmodifiableSet(rulesToInclude);
    }

    /**
     * Returns an unmodifiable {@link Set} of rules to exclude.
     *
     * @return an unmodifiable set of rules to exclude
     */
    public Set<RuleToFilter> getRulesToExclude() {
        return Collections.unmodifiableSet(rulesToExclude);
    }

    public record Platform(String name, String path, Map<String, Object> arguments) {
        /**
         * @param name      in-memory representation of platform name
         * @param path      in-memory representation of platform JAR path
         * @param arguments in-memory representation of platform arguments
         */
        public Platform(String name, String path, Map<String, Object> arguments) {
            this.name = name;
            this.path = path;
            this.arguments = Collections.unmodifiableMap(arguments);
        }
    }

    public record Analyzer(String org, String name, String version, String repository) { }

    public record RuleToFilter(String id) { }
}
