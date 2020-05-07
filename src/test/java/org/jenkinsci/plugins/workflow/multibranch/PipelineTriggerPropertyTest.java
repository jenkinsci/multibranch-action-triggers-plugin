package org.jenkinsci.plugins.workflow.multibranch;

import hudson.model.*;
import hudson.util.RunList;
import jenkins.branch.BranchSource;
import jenkins.branch.OrganizationFolder;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import org.apache.xpath.operations.Or;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PipelineTriggerPropertyTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    @Rule
    public GitSampleRepoRule gitRepo = new GitSampleRepoRule();
    @Rule
    public GitSampleRepoRule organizationRepo1 = new GitSampleRepoRule();
    @Rule
    public GitSampleRepoRule organizationRepo2 = new GitSampleRepoRule();
    @Rule
    public GitSampleRepoRule organizationRepo3 = new GitSampleRepoRule();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File repoFile;
    private static final String jenkinsFile = "Jenkinsfile";
    private String pipelineScript = "//No Content Necessary";
    private String pipelineFile = "Jenkinsfile";
    private String createTriggerJobName = "CreateTriggerJob";
    private String deleteTriggerJobName = "DeleteTriggerJob";
    private String deleteRunTriggerJobName = "DeleteRunTriggerJob";
    private String triggerFolderName = "TriggerFolder";
    private List<String> branchNames = Arrays.asList("master","feature", "bugfix");
    private int expectedPipelineCount = this.branchNames.size();
    private String branchIncludeFilter;
    private String branchExcludeFilter;


    private FreeStyleProject createTriggerJob;
    private FreeStyleProject deleteTriggerJob;
    protected FreeStyleProject deleteRunTriggerJob;

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"*", ""},
                {"master", ""},
                {"*", "master"},
                {"*", "feature bugfix"},
                {"feature bugfix", ""},
                {"feat*", ""},
                {"", ""}
        });
    }


    public PipelineTriggerPropertyTest(String branchIncludeFilter, String branchExcludeFilter) {
        this.branchIncludeFilter = branchIncludeFilter;
        this.branchExcludeFilter = branchExcludeFilter;
    }

    @Before
    public void setup() throws Exception {
        this.repoFile = this.initSourceCodeRepo();
    }

    @Test
    public void testPipelineTriggerPropertyWithFreeStyleJobs() throws Exception {

        List additionalParameters = new ArrayList();
        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(false);
        //Organization Folder Test
        WorkflowMultiBranchProject workflowMultiBranchProject = this.createOrganizationFolder(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);


        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(false);
        //WorkflowMultiBranch Test
        workflowMultiBranchProject = this.createWorkflowMultiBranchJobWithTriggers(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter, this.branchExcludeFilter, additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);

    }

    @Test
    public void testPipelineTriggerPropertyWithFreeStyleJobsWithAdditionalParameters() throws Exception {

        List additionalParameters = this.getAdditionalParametersForTest();
        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(false);
        //Organization Folder Test
        WorkflowMultiBranchProject workflowMultiBranchProject = this.createOrganizationFolder(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);


        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(false);
        //WorkflowMultiBranch Test
        workflowMultiBranchProject = this.createWorkflowMultiBranchJobWithTriggers(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter, this.branchExcludeFilter, additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);

    }

    @Test
    public void testPipelineTriggerPropertyWithFreeStyleJobsInFolder() throws Exception {

        List additionalParameters = new ArrayList();
        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(true);
        //Organization Folder Test
        WorkflowMultiBranchProject workflowMultiBranchProject = this.createOrganizationFolder(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);


        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(true);
        //WorkflowMultiBranch Test
        workflowMultiBranchProject = this.createWorkflowMultiBranchJobWithTriggers(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter, this.branchExcludeFilter, additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);

    }

    @Test
    public void testPipelineTriggerPropertyWithFreeStyleJobsInFolderWithAdditionalParameters() throws Exception {

        List additionalParameters = this.getAdditionalParametersForTest();
        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(true);
        //Organization Folder Test
        WorkflowMultiBranchProject workflowMultiBranchProject = this.createOrganizationFolder(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);


        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(true);
        //WorkflowMultiBranch Test
        workflowMultiBranchProject = this.createWorkflowMultiBranchJobWithTriggers(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter, this.branchExcludeFilter, additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);

    }


    private WorkflowMultiBranchProject createOrganizationFolder(
            Job createTriggerJob,
            Job deleteTriggerJob,
            Job deleteRunTriggerJob,
            String branchIncludeFilter,
            String branchExcludeFilter, List<AdditionalParameter> additionalParameters)
            throws Exception {
        OrganizationFolder organizationFolder = this.jenkins.createProject(OrganizationFolder.class, UUID.randomUUID().toString());
        organizationFolder.getNavigators().add(new GitDirectorySCMNavigator(this.repoFile.getAbsolutePath()));
        organizationFolder.getProperties().add(new PipelineTriggerProperty(
                createTriggerJob.getFullName(),
                deleteTriggerJob.getFullName(),
                deleteRunTriggerJob.getFullName(),
                branchIncludeFilter,
                branchExcludeFilter,additionalParameters));
        organizationFolder.scheduleBuild2(0);
        this.jenkins.waitUntilNoActivity();
        assertEquals(1, organizationFolder.getItems().size());
        WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) organizationFolder.getItem("repo-one");
        this.indexMultiBranchPipeline(workflowMultiBranchProject, this.expectedPipelineCount);
        this.jenkins.waitUntilNoActivity();
        OrganizationFolder reloadedOrganizationFolder = (OrganizationFolder) this.jenkins.jenkins.getItem(organizationFolder.getName());
        PipelineTriggerProperty reloadedPipelinePropertyTrigger = reloadedOrganizationFolder.getProperties().get(PipelineTriggerProperty.class);
        Assert.assertNotNull(reloadedPipelinePropertyTrigger);
        Assert.assertEquals(1, reloadedPipelinePropertyTrigger.getCreateActionJobs().size());
        Assert.assertEquals(1, reloadedPipelinePropertyTrigger.getDeleteActionJobs().size());
        Assert.assertEquals(1, reloadedPipelinePropertyTrigger.getActionJobsOnRunDelete().size());
        return workflowMultiBranchProject;

    }

    private WorkflowMultiBranchProject createWorkflowMultiBranchJobWithTriggers(
                Job createTriggerJob,
                Job deleteTriggerJob,
                Job deleteRunTriggerJob,
                String branchIncludeFilter,
                String branchExcludeFilter, List<AdditionalParameter> additionalParameters)
            throws Exception {

        //Create Multi Branch Pipeline Job with Git Repo
        WorkflowMultiBranchProject workflowMultiBranchProject = this.jenkins.createProject(WorkflowMultiBranchProject.class, UUID.randomUUID().toString());
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(new GitSCMSource(null, this.gitRepo.toString(), "", "*", "", false)));
        workflowMultiBranchProject.getProperties().add(new PipelineTriggerProperty(
                createTriggerJob.getFullName(),
                deleteTriggerJob.getFullName(),
                deleteRunTriggerJob.getFullName(),
                branchIncludeFilter,
                branchExcludeFilter, additionalParameters));
        this.indexMultiBranchPipeline(workflowMultiBranchProject, this.expectedPipelineCount);
        this.jenkins.waitUntilNoActivity();
        WorkflowMultiBranchProject reloadedWorkflowMultiBranchProject = (WorkflowMultiBranchProject) this.jenkins.jenkins.getItem(workflowMultiBranchProject.getName());
        PipelineTriggerProperty reloadedPipelineTriggerProperty = reloadedWorkflowMultiBranchProject.getProperties().get(PipelineTriggerProperty.class);
        Assert.assertNotNull(reloadedPipelineTriggerProperty);
        Assert.assertEquals(1, reloadedPipelineTriggerProperty.getCreateActionJobs().size());
        Assert.assertEquals(1, reloadedPipelineTriggerProperty.getDeleteActionJobs().size());
        Assert.assertEquals(1, reloadedPipelineTriggerProperty.getActionJobsOnRunDelete().size());
        return workflowMultiBranchProject;
    }
    public void checkResults(
            Job createTriggerJob,
            Job deleteTriggerJob,
            Job deleteRunTriggerJob,
            String branchIncludeFilter,
            String branchExcludeFilter, List<AdditionalParameter> additionalParameters,WorkflowMultiBranchProject workflowMultiBranchProject) throws Exception {

        //Test Pre Trigger Jobs
        this.checkTriggeredJobs(createTriggerJob, branchIncludeFilter, branchExcludeFilter, 1, null, null, workflowMultiBranchProject, additionalParameters);

        // Run jobs to create Runs
        List<WorkflowJob> workflowJobs = workflowMultiBranchProject.getAllItems(WorkflowJob.class);
        for (WorkflowJob workflowJob : workflowJobs)
        {
            assertEquals(1, workflowJob.getBuilds().size());
            workflowJob.scheduleBuild2(0);
            this.jenkins.waitUntilNoActivity();
            assertEquals(2, workflowJob.getBuilds().size());
            workflowJob.scheduleBuild2(0);
            this.jenkins.waitUntilNoActivity();
            assertEquals(3, workflowJob.getBuilds().size());
        }

        // Delete Run
        for (WorkflowJob workflowJob : workflowJobs)
        {
            workflowJob.getBuilds().getLastBuild().delete();
            this.jenkins.waitUntilNoActivity();
            System.out.println(workflowJob.getBuilds());
            assertEquals(workflowJob.getBuilds().size(), 2);
        }
        this.checkTriggeredJobs(deleteRunTriggerJob, branchIncludeFilter, branchExcludeFilter, 1, Collections.singletonList(3), Collections.singletonList("#3"), workflowMultiBranchProject,additionalParameters);

        //Change Branch Source and set Include field to None to test Pipeline Delete by Branch Indexing
        workflowMultiBranchProject.getSourcesList().clear();
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(new GitSCMSource(null, this.gitRepo.toString(), "", "none", "", false)));
        this.indexMultiBranchPipeline(workflowMultiBranchProject, 0);
        this.jenkins.waitUntilNoActivity();

        //Test Post Trigger Jobs
        this.checkTriggeredJobs(deleteTriggerJob, branchIncludeFilter, branchExcludeFilter, 1, null, null, workflowMultiBranchProject, additionalParameters);
        //Test whether a branch delete also triggers the deleteRun Jobs
        Set<Integer> expectedRunNumbers = new HashSet<>();
        expectedRunNumbers.add(1);
        expectedRunNumbers.add(2);
        expectedRunNumbers.add(3); // from previous delete run
        Set<String> expectedRunDisplayNames = new HashSet<>();
        expectedRunDisplayNames.add("#1");
        expectedRunDisplayNames.add("#2");
        expectedRunDisplayNames.add("#3"); // from previous delete run
        this.checkTriggeredJobs(deleteRunTriggerJob, branchIncludeFilter, branchExcludeFilter, 3, expectedRunNumbers, expectedRunDisplayNames, workflowMultiBranchProject,additionalParameters);
    }

    private void indexMultiBranchPipeline(WorkflowMultiBranchProject workflowMultiBranchProject, int expectedPipelineCount) throws Exception {
        workflowMultiBranchProject.scheduleBuild2(0);
        this.jenkins.waitUntilNoActivity();
        assertEquals(expectedPipelineCount, workflowMultiBranchProject.getItems().size());
    }

    private File initSourceCodeRepo() throws Exception {
        File localFolder = temporaryFolder.newFolder();
        this.gitRepo.init();
        this.gitRepo.write(this.pipelineFile, this.pipelineScript);
        this.gitRepo.git("add", this.pipelineFile);
        this.gitRepo.git("commit", "--all", "--message=InitRepoWithFile");
        for(int i = 1; i < this.branchNames.size(); i++)
            this.gitRepo.git("checkout", "-b", this.branchNames.get(i));
        this.gitRepo.git("clone", ".", new File(localFolder, "repo-one").getAbsolutePath());
        return localFolder;
    }

    private void checkTriggeredJobs(
                Job triggeredJob,
                String branchIncludeFilter,
                String branchExcludeFilter,
                int jobCountFactor,
                Collection<Integer> expectedRunNumbers,
                Collection<String> expectedRunNames,
                WorkflowMultiBranchProject callerJob, List<AdditionalParameter> additionalParameters)
            throws Exception {
        RunList<FreeStyleBuild> builds = triggeredJob.getBuilds();
        ArrayList filteredBranches = this.getFilteredBranchNames(branchIncludeFilter, branchExcludeFilter);

        assertEquals(filteredBranches.size() * jobCountFactor, builds.size());
        Iterator<FreeStyleBuild> iterator = builds.iterator();
        while (iterator.hasNext()) {
            FreeStyleBuild build = iterator.next();
            Map<String, String> buildVariables = build.getBuildVariables();
            if (buildVariables.containsKey(PipelineTriggerProperty.projectNameParameterKey) && buildVariables.containsKey(PipelineTriggerProperty.projectFullNameParameterKey)) {
                String projectName = buildVariables.get(PipelineTriggerProperty.projectNameParameterKey);
                String projectFullName = buildVariables.get(PipelineTriggerProperty.projectFullNameParameterKey);
                if ( !(filteredBranches.contains(projectName))) {
                    throw new Exception("Trigger Pipeline name not found in the Job Parameters");
                }
                if( !projectFullName.equals(callerJob.getFullName() + "/" + projectName) ) {
                    throw new Exception("Trigger Pipeline Full Name not found in the Job Parameter");
                }
            } else {
                throw new Exception(PipelineTriggerProperty.projectNameParameterKey + " key not found in Build Variables");
            }
            for(AdditionalParameter additionalParameter : additionalParameters) {
                if(buildVariables.containsKey(additionalParameter.getName())){
                    String value = buildVariables.get(additionalParameter.getName());
                    if(!value.equals(additionalParameter.getValue())) {
                        throw new Exception("Trigger Additional Parameter value is not equal to defined value");
                    }
                }
                else {
                    throw new Exception("Trigger Additional Parameter Name not found in the Job Parameters");
                }
            }
            if (expectedRunNumbers != null) {
                if (buildVariables.containsKey(PipelineTriggerProperty.runNumberParameterKey)) {
                    String runNumber = buildVariables.get(PipelineTriggerProperty.runNumberParameterKey);
                    if ( !(expectedRunNumbers.contains(Integer.parseInt(runNumber)))) {
                        throw new Exception("Run number " + runNumber + " not found in the expected triggered Jobs " + expectedRunNumbers);
                    }
                } else {
                    throw new Exception(PipelineTriggerProperty.runNumberParameterKey + " key not found in Build Variables");
                }
            }
            if (expectedRunNames != null) {
                if (buildVariables.containsKey(PipelineTriggerProperty.runDisplayNameParameterKey)) {
                    String runDisplayName = buildVariables.get(PipelineTriggerProperty.runDisplayNameParameterKey);
                    if ( !(expectedRunNames.contains(runDisplayName))) {
                        throw new Exception("Run display name " + runDisplayName + " not found in the expected triggered Jobs " + expectedRunNames);
                    }
                } else {
                    throw new Exception(PipelineTriggerProperty.runNumberParameterKey + " key not found in Build Variables");
                }
            }
        }
    }

    private ArrayList getFilteredBranchNames(String branchIncludeFilter, String branchExcludeFilter) {
        ArrayList filterBranchNames = new ArrayList();
        String includeFilterPattern = PipelineTriggerProperty.convertToPattern(branchIncludeFilter);
        String excludeFilterPattern = PipelineTriggerProperty.convertToPattern(branchExcludeFilter);
        for(String branchName : this.branchNames) {
            if( !Pattern.matches(excludeFilterPattern, branchName) && Pattern.matches(includeFilterPattern, branchName) ) {
                filterBranchNames.add(branchName);
            }
        }
        filterBranchNames = (ArrayList) filterBranchNames.stream().distinct().collect(Collectors.toList());
        return filterBranchNames;
    }

    private List<AdditionalParameter> getAdditionalParametersForTest(){
        return new ArrayList<>(Arrays.asList(
                new AdditionalParameter("name1", "value1"),
                new AdditionalParameter("name2", "value2")
        ));
    }

    private void initFreeStyleJobs(Boolean inFolder) throws IOException {
        MockFolder triggerFolder = jenkins.createFolder(UUID.randomUUID().toString());
        if( inFolder) {
            this.createTriggerJob = triggerFolder.createProject(FreeStyleProject.class, UUID.randomUUID().toString());
            this.deleteTriggerJob = triggerFolder.createProject(FreeStyleProject.class, UUID.randomUUID().toString());
            this.deleteRunTriggerJob = triggerFolder.createProject(FreeStyleProject.class, UUID.randomUUID().toString());

        }
        else
        {
            this.createTriggerJob = jenkins.createFreeStyleProject(UUID.randomUUID().toString());
            this.deleteTriggerJob = jenkins.createFreeStyleProject(UUID.randomUUID().toString());
            this.deleteRunTriggerJob = jenkins.createFreeStyleProject(UUID.randomUUID().toString());
        }
    }
}
