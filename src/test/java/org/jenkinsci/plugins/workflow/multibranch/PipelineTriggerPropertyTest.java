package org.jenkinsci.plugins.workflow.multibranch;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.util.RunList;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class PipelineTriggerPropertyTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    @Rule
    public GitSampleRepoRule gitRepo = new GitSampleRepoRule();

    private String pipelineScript = "//No Content Necessary";
    private String pipelineFile = "Jenkinsfile";
    private String createTriggerJobName = "CreateTriggerJob";
    private String deleteTriggerJobName = "DeleteTriggerJob";
    private String additionalBranchName = "feature";
    private String triggerFolderName = "TriggerFolder";

    private int expectedBuildNumPerJob = 2;
    private int expectedPipelineCount = 2;


    @Before
    public void setup() throws Exception {
        this.initSourceCodeRepo();
    }

    @Test
    public void testPipelineTriggerPropertyWithFreeStyleJobs() throws Exception {

        //Create Free Style Jobs for Testing Trigger
        FreeStyleProject createTriggerJob = jenkins.createFreeStyleProject(this.createTriggerJobName);
        FreeStyleProject deleteTriggerJob = jenkins.createFreeStyleProject(this.deleteTriggerJobName);

        //Create WorkflowMultiBranch Job and Test
        this.createWorkflowMultiBranchJobWithTriggers(createTriggerJob, deleteTriggerJob);
    }

    @Test
    public void testPipelineTriggerPropertyWithFreeStyleJobsInFolder() throws Exception {

        MockFolder triggerFolder = jenkins.createFolder(this.triggerFolderName);
        FreeStyleProject createTriggerJob = triggerFolder.createProject(FreeStyleProject.class, this.createTriggerJobName);
        FreeStyleProject deleteTriggerJob = triggerFolder.createProject(FreeStyleProject.class, this.deleteTriggerJobName);

        this.createWorkflowMultiBranchJobWithTriggers(createTriggerJob,deleteTriggerJob);


    }

    private void createWorkflowMultiBranchJobWithTriggers(Job createTriggerJob, Job deleteTriggerJob) throws Exception {

        //Create Multi Branch Pipeline Job with Git Repo
        WorkflowMultiBranchProject workflowMultiBranchProject = this.jenkins.createProject(WorkflowMultiBranchProject.class, UUID.randomUUID().toString());
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(new GitSCMSource(null, this.gitRepo.toString(), "", "*", "", false)));
        workflowMultiBranchProject.getProperties().add(new PipelineTriggerProperty(createTriggerJob.getFullName(), deleteTriggerJob.getFullName()));
        this.indexMultiBranchPipeline(workflowMultiBranchProject, this.expectedPipelineCount);
        this.jenkins.waitUntilNoActivity();

        //Test Pre Trigger Jobs
        this.checkTriggeredJobs(createTriggerJob);

        //Change Branch Source and set Include field to None to test Pipeline Delete by Branch Indexing
        workflowMultiBranchProject.getSourcesList().clear();
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(new GitSCMSource(null, this.gitRepo.toString(), "", "none", "", false)));
        this.indexMultiBranchPipeline(workflowMultiBranchProject, 0);
        this.jenkins.waitUntilNoActivity();

        //Test Post Trigger Jobs
        this.checkTriggeredJobs(deleteTriggerJob);
    }

    private void indexMultiBranchPipeline(WorkflowMultiBranchProject workflowMultiBranchProject, int expectedPipelineCount) throws Exception {
        workflowMultiBranchProject.scheduleBuild2(0);
        this.jenkins.waitUntilNoActivity();
        assertEquals(expectedPipelineCount, workflowMultiBranchProject.getItems().size());
    }

    private void initSourceCodeRepo() throws Exception {
        this.gitRepo.init();
        this.gitRepo.write(this.pipelineFile, this.pipelineScript);
        this.gitRepo.git("add", this.pipelineFile);
        this.gitRepo.git("commit", "--all", "--message=InitRepoWithFile");
        this.gitRepo.git("checkout", "-b", this.additionalBranchName);
    }

    private void checkTriggeredJobs(Job triggeredJob) throws Exception {
        RunList<FreeStyleBuild> builds = triggeredJob.getBuilds();
        assertEquals(this.expectedBuildNumPerJob, builds.size());
        Iterator<FreeStyleBuild> iterator = builds.iterator();
        while (iterator.hasNext()) {
            FreeStyleBuild build = iterator.next();
            Map<String, String> buildVariables = build.getBuildVariables();
            if( buildVariables.containsKey(PipelineTriggerProperty.projectNameParameterKey) ) {
                String value = buildVariables.get(PipelineTriggerProperty.projectNameParameterKey);
                if (!(value.contains("master") || value.contains(this.additionalBranchName))) {
                    throw new Exception("Trigger Pipeline name not found in the Job Logs");
                }
            }
            else {
                throw new Exception(PipelineTriggerProperty.projectNameParameterKey + " key not found in Build Variables");
            }
        }
    }
}
