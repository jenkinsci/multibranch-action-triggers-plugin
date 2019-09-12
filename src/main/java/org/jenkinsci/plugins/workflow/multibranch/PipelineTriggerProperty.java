package org.jenkinsci.plugins.workflow.multibranch;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import jenkins.branch.MultiBranchProject;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Job property to enable setting jobs to trigger when a pipeline is created or deleted.
 * In details by this, multi branch pipeline will trigger other job/jobs depending on the configuration.
 * Jobs defined in Pipeline Pre Create Jobs Trigger Field, will be triggered when a new pipeline created by branch indexing.
 * Jobs defined in Pipeline Post Create Jobs Trigger Field, will be triggered when a pipeline is deleted by branch indexing.
 */
public class PipelineTriggerProperty extends AbstractFolderProperty<MultiBranchProject<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(PipelineTriggerProperty.class.getName());

    private String createActionJobsToTrigger;
    private String deleteActionJobsToTrigger;
    private transient List<Job> createActionJobs;
    private transient List<Job> deleteActionJobs;
    private final int quitePeriod = 0;
    static final String projectNameParameterKey = "SOURCE_PROJECT_NAME";

    /**
     * @param createActionJobsToTrigger  Full names of the jobs in comma separated format which are defined in the field
     * @param deleteActionJobsToTrigger Full names of the jobs in comma separated format which are defined in the field
     * @see DataBoundConstructor
     */
    @DataBoundConstructor
    public PipelineTriggerProperty(String createActionJobsToTrigger, String deleteActionJobsToTrigger) {
        this.setCreateActionJobsToTrigger(createActionJobsToTrigger);
        this.setDeleteActionJobsToTrigger(deleteActionJobsToTrigger);
    }

    /**
     * Getter method for @createActionJobsToTrigger
     *
     * @return Full names of the jobs in comma separated format
     */
    public String getCreateActionJobsToTrigger() {
        return createActionJobsToTrigger;
    }

    /**
     * Setter method for @createActionJobsToTrigger
     * Additionally. this methods parses job names from @createActionJobsToTrigger, convert to List of Job and store in @createActionJobs for later use.
     *
     * @param createActionJobsToTrigger Full names of the jobs in comma separated format which are defined in the field
     */
    @DataBoundSetter
    public void setCreateActionJobsToTrigger(String createActionJobsToTrigger) {
        this.setCreateActionJobs(this.validateJobs(createActionJobsToTrigger, true));
        this.createActionJobsToTrigger = this.convertJobsToCommaSeparatedString(this.getCreateActionJobs());
    }

    /**
     * Getter method for @deleteActionJobsToTrigger
     *
     * @return Full names of the jobs in comma-separated format
     */
    public String getDeleteActionJobsToTrigger() {
        return deleteActionJobsToTrigger;
    }

    /**
     * Setter method for @deleteActionJobsToTrigger
     * Additionally. this methods parses job names from @deleteActionJobsToTrigger, convert to List of Job and store in @deleteActionJobs for later use.
     *
     * @param deleteActionJobsToTrigger Full names of the jobs in comma-separated format which are defined in the field
     */
    @DataBoundSetter
    public void setDeleteActionJobsToTrigger(String deleteActionJobsToTrigger) {
        this.setDeleteActionJobs(this.validateJobs(deleteActionJobsToTrigger, true));
        this.deleteActionJobsToTrigger = this.convertJobsToCommaSeparatedString(this.getDeleteActionJobs());
    }

    /**
     * Getter method for @createActionJobs
     *
     * @return List of Job for Pre Action
     */
    public List<Job> getCreateActionJobs() {
        return createActionJobs;
    }

    /**
     * Setter method for @createActionJobs
     *
     * @param createActionJobs List of Job for Pre Action
     */
    public void setCreateActionJobs(List<Job> createActionJobs) {
        this.createActionJobs = createActionJobs;
    }

    /**
     * Getter method for @deleteActionJobs
     *
     * @return List of Job for Post Action
     */
    public List<Job> getDeleteActionJobs() {
        return deleteActionJobs;
    }

    /**
     * Setter method for @createActionJobs
     *
     * @param deleteActionJobs List of Job for Post Action
     */
    public void setDeleteActionJobs(List<Job> deleteActionJobs) {
        this.deleteActionJobs = deleteActionJobs;
    }

    /**
     * @see AbstractFolderPropertyDescriptor
     */
    @Extension
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        /**
         * @return Property Name
         * @see AbstractFolderPropertyDescriptor
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Pipeline Trigger";
        }

        /**
         * Return true if calling class is MultiBranchProject
         *
         * @param containerType See AbstractFolder
         * @return boolean
         * @see AbstractFolderPropertyDescriptor
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractFolder> containerType) {
            return WorkflowMultiBranchProject.class.isAssignableFrom(containerType);
        }

        /**
         * Auto complete methods @createActionJobsToTrigger field.
         *
         * @param value Value to search in Job Full Names
         * @return AutoCompletionCandidates
         */
        public AutoCompletionCandidates doAutoCompleteCreateActionJobsToTrigger(@QueryParameter String value) {
            return this.autoCompleteCandidates(value);
        }

        /**
         * Auto complete methods @deleteActionJobsToTrigger field.
         *
         * @param value Value to search in Job Full Namesif
         * @return AutoCompletionCandidates
         */
        public AutoCompletionCandidates doAutoCompleteDeleteActionJobsToTrigger(@QueryParameter String value) {
            return this.autoCompleteCandidates(value);
        }

        /**
         * Get all Job items in Jenkins. Filter them if they contain @value in Job Full names.
         * Also filter Jobs which have @Item.BUILD and @Item.READ permissions.
         *
         * @param value Value to search in Job Full Names
         * @return AutoCompletionCandidates
         */
        private AutoCompletionCandidates autoCompleteCandidates(String value) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
            for (Job job : jobs) {
                String jobName = job.getFullName();
                if (jobName.contains(value.trim()) && job.hasPermission(Item.BUILD) && job.hasPermission(Item.READ))
                    candidates.add(jobName);
            }
            return candidates;
        }
    }

    /**
     * Find and check Jobs which are defined in @actionJobsToTrigger in comma-separated format.
     * Additionally, create StringParameterDefinition in Jobs to pass @projectNameParameterKey as build value.
     *
     * @param actionJobsToTrigger                 Full names of the Jobs in comma-separated format which are defined in the field
     * @param addSourceProjectNameStringParameter If set True, create StringParameterDefinition in Job
     * @return List of Job
     */
    private List<Job> validateJobs(String actionJobsToTrigger, boolean addSourceProjectNameStringParameter) {
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
        List<Job> validatedJobs = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(Util.fixNull(Util.fixEmptyAndTrim(actionJobsToTrigger)), ",");
        while (tokenizer.hasMoreTokens()) {
            String tokenJobName = tokenizer.nextToken();
            for (Job job : jobs) {
                if (job.getFullName().trim().equals(tokenJobName.trim())) {
                    if (addSourceProjectNameStringParameter) {
                        //Try to add job property. If fails do not stop just log warning.
                        try {
                            job.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition(PipelineTriggerProperty.projectNameParameterKey, "", "Added by Multibranch Pipeline Plugin")));
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "Could not set String Parameter Definition. This may affect jobs which are triggered from Multibranch Pipeline Plugin.", ex);
                        }
                    }
                    validatedJobs.add(job);
                }
            }
        }
        return validatedJobs;
    }

    /**
     * Get full names Jobs and return in comma separated format.
     *
     * @param jobs List of Job
     * @return Full names of the jobs in comma separated format
     */
    private String convertJobsToCommaSeparatedString(List<Job> jobs) {
        List<String> jobFullNames = jobs.stream().map(AbstractItem::getFullName).collect(Collectors.toList());
        return String.join(",", jobFullNames);
    }

    /**
     * Build Jobs which are defined in the @createActionJobsToTrigger field.
     *
     * @param projectName Name of the project. This will be branch name which is found in branch indexing.
     *                    Also this value will be passed as StringParameterDefinition
     */
    public void buildCreateActionJobs(String projectName) {
        this.buildJobs(projectName, this.validateJobs(this.getCreateActionJobsToTrigger(), false));
    }

    /**
     * Build Jobs which are defined in the @deleteActionJobsToTrigger field.
     *
     * @param projectName Name of the project. This will be branch name which is found in branch indexing.
     *                    Also this value will be passed as StringParameterDefinition
     */
    public void buildDeleteActionJobs(String projectName) {
        this.buildJobs(projectName, this.validateJobs(this.getDeleteActionJobsToTrigger(), false));
    }


    /**
     * Build Jobs and pass parameter to Build
     *
     * @param projectName Name of the project. This value will be passed as StringParameterDefinition
     * @param jobsToBuild List of Jobs to build
     */
    private void buildJobs(String projectName, List<Job> jobsToBuild) {
        List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(new StringParameterValue(PipelineTriggerProperty.projectNameParameterKey, projectName, "Set by Multibranch Pipeline Plugin"));
        ParametersAction parametersAction = new ParametersAction(parameterValues);
        for (Job job : jobsToBuild) {

            if (job instanceof AbstractProject) {
                AbstractProject abstractProject = (AbstractProject) job;
                abstractProject.scheduleBuild2(this.quitePeriod, parametersAction);
            } else if (job instanceof WorkflowJob) {
                WorkflowJob workflowJob = (WorkflowJob) job;
                workflowJob.scheduleBuild2(this.quitePeriod, parametersAction);
            }
        }
    }

    private static void triggerActionJobs(WorkflowJob workflowJob, PipelineTriggerBuildAction action) {
        if( !( workflowJob.getParent()  instanceof WorkflowMultiBranchProject)  ) {
            LOGGER.log(Level.WARNING,"Caller Job is not child of WorkflowMultiBranchProject. Skipping.");
            return;
        }
        WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) workflowJob.getParent();
        PipelineTriggerProperty pipelineTriggerProperty = workflowMultiBranchProject.getProperties().get(PipelineTriggerProperty.class);
        if( pipelineTriggerProperty != null)
            if( action.equals(PipelineTriggerBuildAction.createPipelineAction))
                pipelineTriggerProperty.buildCreateActionJobs(workflowJob.getName());
            else if (action.equals(PipelineTriggerBuildAction.deletePipelineAction))
                pipelineTriggerProperty.buildDeleteActionJobs(workflowJob.getName());
    }

    public static void triggerDeleteActionJobs(WorkflowJob workflowJob) {
        triggerActionJobs(workflowJob, PipelineTriggerBuildAction.deletePipelineAction);
    }

    public static void triggerCreateActionJobs(WorkflowJob workflowJob) {
        triggerActionJobs(workflowJob, PipelineTriggerBuildAction.createPipelineAction);
    }

    private enum PipelineTriggerBuildAction {
        createPipelineAction, deletePipelineAction
    }

}
