package org.jenkinsci.plugins.workflow.multibranch;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSource;
import com.cloudbees.jenkins.plugins.bitbucket.OriginPullRequestDiscoveryTrait;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.PersistentDescriptor;
import hudson.util.RunList;
import io.jenkins.plugins.gitlabbranchsource.BranchDiscoveryTrait;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSource;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceBuilder;
import io.jenkins.plugins.gitlabbranchsource.OriginMergeRequestDiscoveryTrait;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import jenkins.branch.BranchSource;
import jenkins.model.GlobalConfiguration;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.scm.impl.trait.WildcardSCMHeadFilterTrait;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Assert;
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
public class GitlabPipelineTriggerPropertyPRTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private final List<String> branchNames = Arrays.asList("master");
    private final List<String> prNames = Arrays.asList("MR-1-merge");
    private final int expectedPipelineCount = this.branchNames.size() + this.prNames.size();
    private String branchIncludeFilter;
    private String branchExcludeFilter;
    private GitLabServer gitLabServer;
    private final String owner = "aytuncbeken";
    private final String repository = "aytuncbeken/multibranch-action-triggers-test";
    private FreeStyleProject createTriggerJob;
    private FreeStyleProject deleteTriggerJob;
    private FreeStyleProject deleteRunTriggerJob;
    private BaseStandardCredentials credentials;

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"*", ""}
        });
    }

    public GitlabPipelineTriggerPropertyPRTest(String branchIncludeFilter, String branchExcludeFilter) {
        this.branchIncludeFilter = branchIncludeFilter;
        this.branchExcludeFilter = branchExcludeFilter;
    }

    @Test
    public void testPR() throws Exception {
        if( System.getProperty("gitlab.password") == null) {
            System.out.println("BitBucket Password not set, skipping test");
            return;
        }
        String password = System.getProperty("gitlab.password");
        credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, UUID.randomUUID().toString(),"",this.owner, password);
        SystemCredentialsProvider.getInstance().setDomainCredentialsMap(Collections.singletonMap(Domain.global(), Collections.singletonList(credentials)));

        List additionalParameters = new ArrayList();
        //Create Free Style Jobs for Testing Trigger
        this.initFreeStyleJobs(false);
        WorkflowMultiBranchProject workflowMultiBranchProject = this.createWorkflowMultiBranchJobWithTriggers(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter,  additionalParameters);
        this.checkResults(createTriggerJob, deleteTriggerJob, deleteRunTriggerJob, this.branchIncludeFilter,this.branchExcludeFilter, additionalParameters,workflowMultiBranchProject);
    }

    private WorkflowMultiBranchProject createWorkflowMultiBranchJobWithTriggers(
                Job createTriggerJob,
                Job deleteTriggerJob,
                Job deleteRunTriggerJob,
                String branchIncludeFilter,
                String branchExcludeFilter, List<AdditionalParameter> additionalParameters)
            throws Exception {

        gitLabServer = new GitLabServer("https://gitlab.com","gitlab",credentials.getId());
        GitLabServers gitlabServers = (GitLabServers) jenkins.getInstance().getDescriptor(GitLabServers.class);
        gitlabServers.addServer(gitLabServer);
        gitlabServers.save();
        this.jenkins.getInstance().save();


        //Create Multi Branch Pipeline Job with Git Repo
        WorkflowMultiBranchProject workflowMultiBranchProject = this.jenkins.createProject(WorkflowMultiBranchProject.class, UUID.randomUUID().toString());
        GitLabSCMSource gitLabSCMSource = new GitLabSCMSource(gitLabServer.getName(),this.owner,this.repository);
        gitLabSCMSource.setTraits(Arrays.asList(new BranchDiscoveryTrait(true,false), new WildcardSCMHeadFilterTrait("*",""), new OriginMergeRequestDiscoveryTrait(1)));
        workflowMultiBranchProject.setSourcesList(Arrays.asList(new BranchSource(gitLabSCMSource)));
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
        GitLabSCMSource gitLabSCMSource = new GitLabSCMSource(gitLabServer.getName(),this.owner,this.repository);
        gitLabSCMSource.setTraits(Arrays.asList());
        workflowMultiBranchProject.setSourcesList(Arrays.asList(new BranchSource(gitLabSCMSource)));
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
                String sourceBranchNameVar = buildVariables.get(PipelineTriggerProperty.sourceBranchName);
                String targetBranchNameVar = buildVariables.get(PipelineTriggerProperty.targetBranchName);
                if ( !(filteredBranches.contains(projectName))) {
                    throw new Exception("Trigger Pipeline name not found in the Job Parameters");
                }
                if( !projectFullName.equals(callerJob.getFullName() + "/" + projectName) ) {
                    throw new Exception("Trigger Pipeline Full Name not found in the Job Parameter");
                }
                if(!projectName.equals(this.branchNames.get(0)) ) {
                    String sourceBranchName = "test-branch";
                    if (!sourceBranchNameVar.equals(sourceBranchName)) {
                        throw new Exception("Trigger Pipeline Source Branch Name not found in Job Parameter");
                    }
                    if (!targetBranchNameVar.equals(this.branchNames.get(0))) {
                        throw new Exception("Trigger Pipeline Target Branch Name not found in Job Parameter");
                    }
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
        for(String prName : this.prNames) {
            if( !Pattern.matches(excludeFilterPattern, prName) && Pattern.matches(includeFilterPattern, prName) ) {
                filterBranchNames.add(prName);
            }
        }
        filterBranchNames = (ArrayList) filterBranchNames.stream().distinct().collect(Collectors.toList());
        return filterBranchNames;
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
