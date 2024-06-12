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

package io.ballerina.scan.internal;

import io.ballerina.cli.utils.OsUtils;
import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.scan.BaseTest;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleKind;
import io.ballerina.scan.Source;
import io.ballerina.scan.utils.ScanTomlFile;
import io.ballerina.scan.utils.ScanUtils;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.scan.TestConstants.LINUX_LINE_SEPARATOR;
import static io.ballerina.scan.TestConstants.WINDOWS_LINE_SEPARATOR;

public class ProjectAnalyzerTest extends BaseTest {
    private ProjectAnalyzer projectAnalyzer;
    private Project project;
    private final String userDir = System.getProperty("user.dir");

    @BeforeTest
    void initialize() {
        Path ballerinaProject = testResources.resolve("test-resources")
                .resolve("bal-project-with-config-file");
        System.setProperty("user.dir", ballerinaProject.toString());
        project = BuildProject.load(ballerinaProject);
    }

    @BeforeMethod
    void initializeMethod() throws RuntimeException {
        Optional<ScanTomlFile> scanTomlFile = ScanUtils.loadScanTomlConfigurations(project, printStream);
        if (scanTomlFile.isEmpty()) {
            throw new RuntimeException("Failed to load scan toml file!");
        }
        projectAnalyzer = new ProjectAnalyzer(project, scanTomlFile.get());
    }

    @AfterTest
    void cleanup() {
        System.setProperty("user.dir", userDir);
    }

    @Test(description = "Test analyzing project with core analyzer")
    void testAnalyzingProjectWithCoreAnalyzer() {
        List<Issue> issues = projectAnalyzer.analyze(List.of(CoreRule.AVOID_CHECKPANIC.rule()));
        Assert.assertEquals(issues.size(), 1);
        Issue issue = issues.get(0);
        Assert.assertEquals(issue.source(), Source.BUILT_IN);
        LineRange location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), "main.bal");
        Assert.assertEquals(location.startLine().line(), 20);
        Assert.assertEquals(location.startLine().offset(), 17);
        Assert.assertEquals(location.endLine().line(), 20);
        Assert.assertEquals(location.endLine().offset(), 39);
        Rule rule = issue.rule();
        Assert.assertEquals(rule.id(), "ballerina:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.description(), "Avoid checkpanic");
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
    }

    @Test(description = "Test analyzing project with external analyzers")
    void testAnalyzingProjectWithExternalAnalyzers() {
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers(printStream)
                .orElse(null);
        Assert.assertNotNull(externalAnalyzers);
        List<Issue> issues = projectAnalyzer.runExternalAnalyzers(externalAnalyzers);
        Assert.assertEquals(issues.size(), 3);
        Issue issue = issues.get(0);
        LineRange location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), "main.bal");
        Assert.assertEquals(location.startLine().line(), 16);
        Assert.assertEquals(location.startLine().offset(), 0);
        Assert.assertEquals(location.endLine().line(), 21);
        Assert.assertEquals(location.endLine().offset(), 1);
        Rule rule = issue.rule();
        Assert.assertEquals(rule.id(), "ballerina/example_module_static_code_analyzer:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.description(), "rule 1");
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
        issue = issues.get(1);
        location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), "main.bal");
        Assert.assertEquals(location.startLine().line(), 16);
        Assert.assertEquals(location.startLine().offset(), 0);
        Assert.assertEquals(location.endLine().line(), 21);
        Assert.assertEquals(location.endLine().offset(), 1);
        rule = issue.rule();
        Assert.assertEquals(rule.id(), "exampleOrg/example_module_static_code_analyzer:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.description(), "rule 1");
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
        issue = issues.get(2);
        location = issue.location().lineRange();
        Assert.assertEquals(location.fileName(), "main.bal");
        Assert.assertEquals(location.startLine().line(), 16);
        Assert.assertEquals(location.startLine().offset(), 0);
        Assert.assertEquals(location.endLine().line(), 21);
        Assert.assertEquals(location.endLine().offset(), 1);
        rule = issue.rule();
        Assert.assertEquals(rule.id(), "ballerinax/example_module_static_code_analyzer:1");
        Assert.assertEquals(rule.numericId(), 1);
        Assert.assertEquals(rule.description(), "rule 1");
        Assert.assertEquals(rule.kind(), RuleKind.CODE_SMELL);
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = null;
        for (DocumentId documentId : defaultModule.documentIds()) {
            document = defaultModule.document(documentId);
            if (!document.name().equals("main.bal")) {
                break;
            }
        }
        Assert.assertNotNull(document);
        String result = document.textDocument().toString().replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        Assert.assertTrue(result.contains("import exampleOrg/example_module_static_code_analyzer as _;"));
        Assert.assertTrue(result.contains("import ballerina/example_module_static_code_analyzer as _;"));
        Assert.assertTrue(result.contains("import ballerinax/example_module_static_code_analyzer as _;"));
        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
        if (ballerinaToml != null) {
            result = ballerinaToml.tomlDocument().textDocument().toString()
                    .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        }
        Assert.assertTrue(result.contains("""
                [[dependency]]
                org = 'ballerina'
                name = 'example_module_static_code_analyzer'
                version = '0.1.0'
                """));
        Assert.assertTrue(result.contains("""
                [[dependency]]
                org = 'ballerinax'
                name = 'example_module_static_code_analyzer'
                version = '0.1.0'
                repository = 'local'
                """));
    }

    @Test(description = "test method for printing static code analysis rules to the console")
    void testPrintRulesToConsole() throws IOException {
        Map<String, List<Rule>> externalAnalyzers = projectAnalyzer.getExternalAnalyzers(printStream)
                .orElse(null);
        Assert.assertNotNull(externalAnalyzers);
        List<Rule> rules = CoreRule.rules();
        externalAnalyzers.values().forEach(rules::addAll);
        ScanUtils.printRulesToConsole(rules, printStream);
        Path validationResultsFilePath;
        if (OsUtils.isWindows()) {
            validationResultsFilePath = testResources.resolve("command-outputs")
                    .resolve("print-rules-to-console.txt");
        } else {
            validationResultsFilePath = testResources.resolve("command-outputs")
                    .resolve("ubuntu").resolve("print-rules-to-console.txt");
        }
        String expected = Files.readString(validationResultsFilePath, StandardCharsets.UTF_8)
                .replace(WINDOWS_LINE_SEPARATOR, LINUX_LINE_SEPARATOR);
        String result = readOutput(true).trim();
        Assert.assertEquals(result, expected);
    }
}
