package org.jenkinsci.plugins.workflow.multibranch;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleProject;
import jenkins.branch.OrganizationFolder;
import jenkins.scm.impl.trait.WildcardSCMHeadFilterTrait;
import jenkins.scm.impl.trait.WildcardSCMSourceFilterTrait;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.*;

/**
 * Custom Test Class for testing with Online GitHub Repo. Skipped on pipeline build. For Local tests only.
 */
@RunWith(JUnit4.class)
public class GithubFolderPipelineTriggerPropertyTest extends Assert {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private final String username = "aytuncbeken";
    private final String repository = "multibranch-action-triggers-test";
    private final String branch = "master";

    @Test
    public void createGitHubOrganizationFolder() throws Exception {
        if( System.getProperty("github.password") == null) {
            System.out.println("Github Password not set, skipping test");
            return;
        }
        String password = System.getProperty("github.password");
        BaseStandardCredentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, UUID.randomUUID().toString(),"",this.username,password);
        SystemCredentialsProvider.getInstance().setDomainCredentialsMap(Collections.singletonMap(Domain.global(), Arrays.asList(credentials)));

        GitHubPluginConfig gitHubPluginConfig = (GitHubPluginConfig) this.jenkins.getInstance().getDescriptor(GitHubPluginConfig.class);
        gitHubPluginConfig.setConfigs(Arrays.asList(new GitHubServerConfig(credentials.getId())));
        gitHubPluginConfig.save();
        this.jenkins.getInstance().save();

        FreeStyleProject createTriggerJob = this.jenkins.createFreeStyleProject(UUID.randomUUID().toString());
        FreeStyleProject deleteTriggerJob = this.jenkins.createFreeStyleProject(UUID.randomUUID().toString());
        FreeStyleProject deleteRunTriggerJob = this.jenkins.createFreeStyleProject(UUID.randomUUID().toString());
       GitHubSCMNavigator gitHubSCMNavigator = new GitHubSCMNavigator(this.username);
        gitHubSCMNavigator.setTraits(Arrays.asList(
                new WildcardSCMHeadFilterTrait(this.branch, ""),
                new WildcardSCMSourceFilterTrait(this.repository,""),
                new BranchDiscoveryTrait(true, false)
        ));


        gitHubSCMNavigator.setCredentialsId(credentials.getId());
        OrganizationFolder organizationFolder = this.jenkins.createProject(OrganizationFolder.class, UUID.randomUUID().toString());
        organizationFolder.getNavigators().add(gitHubSCMNavigator);
        organizationFolder.getProperties().add(new PipelineTriggerProperty(
                createTriggerJob.getFullName(),
                deleteTriggerJob.getFullName(),
                deleteRunTriggerJob.getFullName(),
                "*",
                "",new ArrayList<>()));
        organizationFolder.scheduleBuild2(0);
        organizationFolder.scheduleBuild2(0);
        WorkflowMultiBranchProject workflowMultiBranchProject = this.waitForIndex(organizationFolder,this.repository);
        workflowMultiBranchProject.scheduleBuild2(0);
        WorkflowJob workflowJob = this.waitForIndex(workflowMultiBranchProject, this.branch);
        WorkflowRun run = workflowJob.getLastBuild();
        while (run.getResult() == null) {
            System.out.println("Waiting for Workflow Job to finish:" + run.getResult());
            Thread.sleep(500);
        }
        while (createTriggerJob.getLastBuild().getResult() == null) {
            System.out.println("Waiting for Create Trigger Job to finish:" + createTriggerJob.getLastBuild().getResult());
            Thread.sleep(500);
        }
        Assert.assertEquals(1, createTriggerJob.getLastBuild().number);
        Assert.assertEquals("SUCCESS", createTriggerJob.getLastBuild().getResult().toString());
        workflowJob.getLastBuild().delete();
        while (workflowJob.getBuilds().size() > 0 ) {
            System.out.println("Waiting for build to be deleted");
            Thread.sleep(500);
        }
        Thread.sleep(1000);
        while (deleteRunTriggerJob.isBuilding() == true) {
            System.out.println("Waiting for the DeleteRunTrigger Build to Finish");
            Thread.sleep(500);
        }
        while (deleteRunTriggerJob.getLastBuild().getResult() == null) {
            System.out.println("Waiting for Delete Run Job to finish:" + deleteRunTriggerJob.getLastBuild().getResult());
            Thread.sleep(500);
        }
        Assert.assertEquals(1, deleteRunTriggerJob.getLastBuild().number);
        Assert.assertEquals("SUCCESS", deleteRunTriggerJob.getLastBuild().getResult().toString());
        workflowJob.delete();


        Thread.sleep(1000);
        while (deleteTriggerJob.isBuilding() == true) {
            System.out.println("Waiting for the Delete Trigger Build to Finish");
            Thread.sleep(500);
        }
        while (deleteTriggerJob.getLastBuild().getResult() == null) {
            System.out.println("Waiting for Delete Trigger Job to finish:" + deleteTriggerJob.getLastBuild().getResult());
            Thread.sleep(500);
        }
        Assert.assertEquals(1, deleteTriggerJob.getLastBuild().number);
        Assert.assertEquals("SUCCESS", deleteTriggerJob.getLastBuild().getResult().toString());

        PipelineTriggerProperty organizationPipelineTriggerProperty = organizationFolder.getProperties().get(PipelineTriggerProperty.class);
        organizationPipelineTriggerProperty.setCreateActionJobsToTrigger("");
        organizationPipelineTriggerProperty.setDeleteActionJobsToTrigger("");
        organizationPipelineTriggerProperty.setActionJobsToTriggerOnRunDelete("");
        organizationFolder.scheduleBuild2(0);
        organizationFolder.scheduleBuild2(0);
        workflowMultiBranchProject = this.waitForIndex(organizationFolder,this.repository);
        PipelineTriggerProperty workflowPipelineTriggerProperty = workflowMultiBranchProject.getProperties().get(PipelineTriggerProperty.class);
        assertEquals(0, workflowPipelineTriggerProperty.getCreateActionJobs().size());
        assertEquals(0, workflowPipelineTriggerProperty.getDeleteActionJobs().size());
        assertEquals(0, workflowPipelineTriggerProperty.getActionJobsOnRunDelete().size());



    }

    private WorkflowJob waitForIndex(WorkflowMultiBranchProject workflowMultiBranchProject, String branchName) throws Exception {
        int waitCounter=0;
        int maxWait=20;
        WorkflowJob workflowJob = null;
        do{
            Thread.sleep(1000);
            workflowJob = workflowMultiBranchProject.getItem(branchName);
            waitCounter++;
            System.out.println("Waiting for index completed - WorkflowMultiBranchProject:" + workflowMultiBranchProject.getName());
        }while (workflowJob == null && waitCounter < maxWait);
        if(workflowJob== null)
            throw new Exception("WorkflowJob indexing is failed");
        return workflowJob;
    }

    private WorkflowMultiBranchProject waitForIndex(OrganizationFolder organizationFolder, String itemName) throws Exception {
        int waitCounter=0;
        int maxWait=20;
        WorkflowMultiBranchProject workflowMultiBranchProject = null;
        do{
            Thread.sleep(1000);
            workflowMultiBranchProject = (WorkflowMultiBranchProject) organizationFolder.getItem(itemName);
            waitCounter++;
            System.out.println("Waiting for index completed - OrganizationFolder:" + organizationFolder.getName());
        }while (workflowMultiBranchProject == null && waitCounter < maxWait);
        if(workflowMultiBranchProject== null)
            throw new Exception("WorkflowMultiBranchProject indexing is failed");
        return workflowMultiBranchProject;

    }

}
