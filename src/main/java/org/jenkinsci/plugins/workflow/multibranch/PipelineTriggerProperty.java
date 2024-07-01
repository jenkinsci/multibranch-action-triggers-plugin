package org.jenkinsci.plugins.workflow.multibranch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
import hudson.util.DescribableList;
import jenkins.branch.Branch;
import jenkins.branch.MultiBranchProject;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Job property to enable setting jobs to trigger when a pipeline is created or deleted.
 * In details by this, multi branch pipeline will trigger other job/jobs depending on the configuration.
 * Jobs defined in Pipeline Pre Create Jobs Trigger Field, will be triggered when a new pipeline created by branch indexing.
 * Jobs defined in Pipeline Post Create Jobs Trigger Field, will be triggered when a pipeline is deleted by branch indexing.
 * Jobs defined in the Pipeline Run Delete Jobs Trigger Field will be triggered when a Pipeline run is deleted
 * (either by explicitly deleting the run or the branch in the run.
 */
public class PipelineTriggerProperty extends AbstractFolderProperty<MultiBranchProject<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(PipelineTriggerProperty.class.getName());

    private String createActionJobsToTrigger = "";
    private String deleteActionJobsToTrigger = "";
    private String actionJobsToTriggerOnRunDelete = "";
    private transient List<Job> createActionJobs = new ArrayList<>();
    private transient List<Job> deleteActionJobs = new ArrayList<>();
    private transient List<Job> actionJobsOnRunDelete = new ArrayList<>();
    @SuppressFBWarnings(value = "SS_SHOULD_BE_STATIC", justification = "TODO needs triage")
    private final int quitePeriod = 0;
    static final String projectNameParameterKey = "SOURCE_PROJECT_NAME";
    static final String projectFullNameParameterKey = "SOURCE_PROJECT_FULL_NAME";
    static final String runNumberParameterKey = "SOURCE_RUN_NUMBER";
    static final String runDisplayNameParameterKey = "SOURCE_RUN_DISPLAY_NAME";
    static final String sourceBranchName = "SOURCE_BRANCH_NAME";
    static final String targetBranchName = "TARGET_BRANCH_NAME";
    private String branchIncludeFilter = "*";
    private String branchExcludeFilter = "";
    private List<AdditionalParameter> additionalParameters = new ArrayList<>();

    /**
     * @param createActionJobsToTrigger      Full names of the jobs in comma separated format which are defined in the field
     * @param deleteActionJobsToTrigger      Full names of the jobs in comma separated format which are defined in the field
     * @param actionJobsToTriggerOnRunDelete Full names of the jobs in comma separated format which are defined in the field
     * @see DataBoundConstructor
     */
    @DataBoundConstructor
    public PipelineTriggerProperty(
            String createActionJobsToTrigger,
            String deleteActionJobsToTrigger,
            String actionJobsToTriggerOnRunDelete,
            String branchIncludeFilter, String
                    branchExcludeFilter, List<AdditionalParameter> additionalParameters) {
        this.setCreateActionJobsToTrigger(createActionJobsToTrigger);
        this.setDeleteActionJobsToTrigger(deleteActionJobsToTrigger);
        this.setActionJobsToTriggerOnRunDelete(actionJobsToTriggerOnRunDelete);
        this.setBranchIncludeFilter(branchIncludeFilter);
        this.setBranchExcludeFilter(branchExcludeFilter);
        this.setAdditionalParameters(additionalParameters);
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
        this.createActionJobsToTrigger = createActionJobsToTrigger;
        this.setCreateActionJobs(this.validateJobString(this.getCreateActionJobsToTrigger()));
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
        this.deleteActionJobsToTrigger = deleteActionJobsToTrigger;
        this.setDeleteActionJobs(this.validateJobString(this.getDeleteActionJobsToTrigger()));
    }

    /**
     * Getter method for @actionJobsToTriggerOnRunDelete
     *
     * @return Full names of the jobs in comma-separated format
     */
    public String getActionJobsToTriggerOnRunDelete() {
        return actionJobsToTriggerOnRunDelete;
    }

    /**
     * Setter method for @actionJobsToTriggerOnRunDelete
     * Additionally. this methods parses job names from @actionJobsToTriggerOnRunDelete,
     * convert to List of Job and store in @actionJobsOnRunDelete for later use.
     *
     * @param actionJobsToTriggerOnRunDelete Full names of the jobs in comma-separated format which are defined in the field
     */
    @DataBoundSetter
    public void setActionJobsToTriggerOnRunDelete(String actionJobsToTriggerOnRunDelete) {
        this.actionJobsToTriggerOnRunDelete = actionJobsToTriggerOnRunDelete;
        this.setActionJobsOnRunDelete(this.validateJobString(this.getActionJobsToTriggerOnRunDelete()));
    }

    /**
     * Getter method for @createActionJobs
     *
     * @return List of Job for Pre Action
     */
    public List<Job> getCreateActionJobs() {
        return this.validateJobString(this.getCreateActionJobsToTrigger());
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
        return this.validateJobString(this.getDeleteActionJobsToTrigger());
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
     * Getter method for @actionJobsOnRunDelete
     *
     * @return List of Job for Run Delete Action
     */
    public List<Job> getActionJobsOnRunDelete() {
        return this.validateJobString(this.getActionJobsToTriggerOnRunDelete());
    }

    /**
     * Setter method for @actionJobsOnRunDelete
     *
     * @param actionJobsOnRunDelete List of Job for Run Delete Action
     */
    public void setActionJobsOnRunDelete(List<Job> actionJobsOnRunDelete) {
        this.actionJobsOnRunDelete = actionJobsOnRunDelete;
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
            if( WorkflowMultiBranchProject.class.isAssignableFrom(containerType))
                return true;
            else if(OrganizationFolder.class.isAssignableFrom(containerType))
                return true;
            else
                return false;
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
         * Auto complete methods @deleteActionJobsToTrigger field.
         *
         * @param value Value to search in Job Full Namesif
         * @return AutoCompletionCandidates
         */
        public AutoCompletionCandidates doAutoCompleteActionJobsToTriggerOnRunDelete(@QueryParameter String value) {
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

    public void setTriggerJobParameters(){
        this.setJobParametersForCreateActionTriggers();
        this.setJobParameterForDeleteActionTriggers();
        this.setJobParameterForJobsOnRunDeleteTriggers();
    }

    private void setJobParametersForCreateActionTriggers() {
        this.setJobParameters(this.getCreateActionJobs(), false, this.getAdditionalParameters());
    }

    private void setJobParameterForDeleteActionTriggers() {
        this.setJobParameters(this.getDeleteActionJobs(), false, this.getAdditionalParameters());
    }

    private void setJobParameterForJobsOnRunDeleteTriggers() {
        this.setJobParameters(this.getActionJobsOnRunDelete(), true, this.getAdditionalParameters());
    }

    private List<Job> validateJobString(String actionTriggersJobString) {
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
        List<Job> validatedJobs = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(Util.fixNull(Util.fixEmptyAndTrim(actionTriggersJobString)), ",");
        while (tokenizer.hasMoreTokens()) {
            String tokenJobName = tokenizer.nextToken();
            for (Job job : jobs) {
                if (job.getFullName().trim().equals(tokenJobName.trim())) {
                    validatedJobs.add(job);
                }
            }
        }
        return validatedJobs;
    }

    /**
     * Find and check Jobs which are defined in @actionJobsToTrigger in comma-separated format.
     * Additionally, create StringParameterDefinition(s) in Jobs to pass @projectNameParameterKey
     * and possibly @runNumberParameterKey and @runDisplayNameParameterKey as build value.
     *
     * @param actionJobsToTrigger Full names of the Jobs in comma-separated format which are defined in the field
     * @param addRunParameters    If the parameters for Run number and Run display names should also be created on the job
     * @return List of Job
     */
    private void setJobParameters(List<Job> actionJobsToTrigger, boolean addRunParameters, List<AdditionalParameter> additionalParameters) {
        for (Job job : actionJobsToTrigger) {
            List<ParameterDefinition> parameters = new ArrayList<>();
            //Try to add job properties. If fails do not stop just log warning.
                parameters.add(new StringParameterDefinition(
                        PipelineTriggerProperty.projectNameParameterKey,
                        "This will be set by MultiBranch Pipeline Plugin",
                        "Added by MultiBranch Pipeline Plugin"));
                parameters.add(new StringParameterDefinition(
                        PipelineTriggerProperty.projectFullNameParameterKey,
                        "This will be set by MultiBranch Pipeline Plugin",
                        "Added by MultiBranch Pipeline Plugin"));
                parameters.add(new StringParameterDefinition(
                        PipelineTriggerProperty.sourceBranchName,
                        "This will be set by MultiBranch Pipeline Plugin",
                        "Added by MultiBranch Pipeline Plugin"));
                parameters.add(new StringParameterDefinition(
                        PipelineTriggerProperty.targetBranchName,
                        "This will be set by MultiBranch Pipeline Plugin",
                        "Added by MultiBranch Pipeline Plugin"));
            if (addRunParameters) {
                parameters.add(new StringParameterDefinition(
                        PipelineTriggerProperty.runNumberParameterKey,
                        "This will be set by MultiBranch Pipeline Plugin",
                        "Added by MultiBranch Pipeline Plugin"));
            }
            if (addRunParameters ) {
                parameters.add(new StringParameterDefinition(
                        PipelineTriggerProperty.runDisplayNameParameterKey,
                        "This will be set by MultiBranch Pipeline Plugin",
                        "Added by MultiBranch Pipeline Plugin"));
            }
            if (additionalParameters != null) {
                for (AdditionalParameter additionalParameter : additionalParameters) {
                        parameters.add(new StringParameterDefinition(
                                additionalParameter.getName(),
                                additionalParameter.getValue(),
                                "Added by MultiBranch Pipeline Plugin"
                        ));
                }
            }

            if (!parameters.isEmpty()) {
                try {
                    ParameterDefinition[] propertiesToAdd = parameters.toArray(new ParameterDefinition[]{});
                    job.addProperty(new ParametersDefinitionProperty(propertiesToAdd));
                    job.save();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "[MultiBranch Action Triggers Plugin] Could not set String Parameter Definitions." +
                                    " This may affect jobs which are triggered from MultiBranch Pipeline Plugin.",
                            ex);
                }
            }
        }
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
     * @param projectName     Name of the project. This will be branch name which is found in branch indexing.
     *                        Also this value will be passed as StringParameterDefinition
     * @param projectFullName Full name of the project.
     *                        Also this value will be passed as StringParameterDefinition
     * @param sourceBranchName  Source Branch Name. Also this value will be passed as StringParameterDefinition
     * @param targetBranchName  Target Branch Name. Also this value will be passed as StringParameterDefinition.
     *                          Applicable only for PR jobs
     */
    private void buildCreateActionJobs(String projectName, String projectFullName, String sourceBranchName, String targetBranchName) {
        this.setJobParametersForCreateActionTriggers();
        this.buildJobs(projectName, projectFullName, null, null, this.getCreateActionJobs(), sourceBranchName, targetBranchName);
    }

    /**
     * Build Jobs which are defined in the @deleteActionJobsToTrigger field.
     *
     * @param projectName     Name of the project. This will be branch name which is found in branch indexing.
     *                        Also this value will be passed as StringParameterDefinition
     * @param projectFullName Full name of the project.
     *                        Also this value will be passed as StringParameterDefinition
     * @param sourceBranchName  Source Branch Name. Also this value will be passed as StringParameterDefinition
     * @param targetBranchName  Target Branch Name. Also this value will be passed as StringParameterDefinition
     *                          Applicable only for PR jobs
     */
    private void buildDeleteActionJobs(String projectName, String projectFullName, String sourceBranchName, String targetBranchName) {
        this.setJobParameterForDeleteActionTriggers();
        this.buildJobs(projectName, projectFullName, null, null, this.getDeleteActionJobs(), sourceBranchName, targetBranchName);
    }

    /**
     * Build Jobs which are defined in the @actionJobsToTriggerOnRunDelete field.
     *
     * @param projectName     Name of the project. This will be branch name which is found in branch indexing.
     *                        Also this value will be passed as StringParameterDefinition
     * @param projectFullName Full name of the project.
     *                        Also this value will be passed as StringParameterDefinition
     * @param runNumber       Number of the upstream build.
     *                        This value will be passed as StringParameterDefinition.
     * @param runDisplayName  Display Name of the upstream build.
     *                        This value will be passed as StringParameterDefinition.
     * @param sourceBranchName  Source Branch Name. Also this value will be passed as StringParameterDefinition
     * @param targetBranchName  Target Branch Name. Also this value will be passed as StringParameterDefinition
     *                          Applicable only for PR jobs
     */
    private void buildActionJobsOnRunDelete(String projectName, String projectFullName, Integer runNumber, String runDisplayName, String sourceBranchName, String targetBranchName) {
        this.setJobParameterForJobsOnRunDeleteTriggers();
        this.buildJobs(projectName, projectFullName, runNumber, runDisplayName, this.getActionJobsOnRunDelete(), sourceBranchName, targetBranchName);
    }


    /**
     * Build Jobs and pass parameter to Build
     *
     * @param projectName     Name of the project. This value will be passed as StringParameterDefinition
     * @param projectFullName Full name of the project.
     *                        Also this value will be passed as StringParameterDefinition
     * @param runNumber       Number of the upstream build, or null if the upstream trigger is not a Run.
     *                        This value will be passed as StringParameterDefinition.
     * @param runDisplayName  Display Name of the upstream build, or null if the upstream trigger is not a Run.
     *                        This value will be passed as StringParameterDefinition.
     * @param jobsToBuild     List of Jobs to build
     * @param sourceBranchName  Source Branch Name. Also this value will be passed as StringParameterDefinition
     * @param targetBranchName  Target Branch Name. Also this value will be passed as StringParameterDefinition
     *                          Applicable only for PR jobs
     */
    private void buildJobs(
            String projectName,
            String projectFullName,
            Integer runNumber,
            String runDisplayName,
            List<Job> jobsToBuild, String sourceBranchName, String targetBranchName) {
        List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(new StringParameterValue(PipelineTriggerProperty.projectNameParameterKey, projectName, "Set by MultiBranch Pipeline Plugin"));
        parameterValues.add(new StringParameterValue(PipelineTriggerProperty.projectFullNameParameterKey, projectFullName, "Set by MultiBranch Pipeline Plugin"));
        parameterValues.add(new StringParameterValue(PipelineTriggerProperty.sourceBranchName, sourceBranchName, "Set by MultiBranch Pipeline Plugin"));
        parameterValues.add(new StringParameterValue(PipelineTriggerProperty.targetBranchName, targetBranchName, "Set by MultiBranch Pipeline Plugin"));
        if (runNumber != null) {
            parameterValues.add(new StringParameterValue(PipelineTriggerProperty.runNumberParameterKey, runNumber.toString(), "Set by MultiBranch Pipeline Plugin"));
        }
        if (runDisplayName != null) {
            parameterValues.add(new StringParameterValue(PipelineTriggerProperty.runDisplayNameParameterKey, runDisplayName, "Set by MultiBranch Pipeline Plugin"));
        }
        for(AdditionalParameter additionalParameter : this.getAdditionalParameters()) {
            parameterValues.add(new StringParameterValue(additionalParameter.getName(), additionalParameter.getValue(),"Set by MultiBranch Pipeline Plugin"));
        }
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

    private void triggerActionJobs(WorkflowJob workflowJob, Run<?, ?> run, PipelineTriggerBuildAction action) {
        if (!(workflowJob.getParent() instanceof WorkflowMultiBranchProject)) {
            LOGGER.log(Level.FINE, "[MultiBranch Action Triggers Plugin] Caller Job is not child of WorkflowMultiBranchProject. Skipping.");
            return;
        }
        WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) workflowJob.getParent();
        PipelineTriggerProperty pipelineTriggerProperty = workflowMultiBranchProject.getProperties().get(PipelineTriggerProperty.class);
        PullRequestInfo pullRequestInfo = this.getPullRequestInfo(workflowJob);
        if (pipelineTriggerProperty != null) {
            if (checkExcludeFilter(workflowJob.getName(), pipelineTriggerProperty)) {
                LOGGER.log(Level.INFO, "[MultiBranch Action Triggers Plugin] {0} excluded by the Exclude Filter", workflowJob.getName());
            } else if (checkIncludeFilter(workflowJob.getName(), pipelineTriggerProperty)) {
                if (action.equals(PipelineTriggerBuildAction.createPipelineAction))
                    pipelineTriggerProperty.buildCreateActionJobs(workflowJob.getName(), workflowJob.getFullName(), pullRequestInfo.getSourceBranchName(), pullRequestInfo.getTargetBranchName());
                else if (action.equals(PipelineTriggerBuildAction.deletePipelineAction))
                    pipelineTriggerProperty.buildDeleteActionJobs(workflowJob.getName(), workflowJob.getFullName(), pullRequestInfo.getSourceBranchName(), pullRequestInfo.getTargetBranchName());
                else if (action.equals(PipelineTriggerBuildAction.deleteRunPipelineAction))
                    pipelineTriggerProperty.buildActionJobsOnRunDelete(workflowJob.getName(), workflowJob.getFullName(), run.getNumber(), run.getDisplayName(), pullRequestInfo.getSourceBranchName(), pullRequestInfo.getTargetBranchName());
            } else {
                LOGGER.log(Level.INFO, "[MultiBranch Action Triggers Plugin] {0} not included by the Include Filter", workflowJob.getName());
            }
        }
    }

    public void triggerDeleteActionJobs(WorkflowJob workflowJob) {
        this.triggerActionJobs(workflowJob, null, PipelineTriggerBuildAction.deletePipelineAction);
    }

    public void triggerCreateActionJobs(WorkflowJob workflowJob) {
        this.triggerActionJobs(workflowJob, null, PipelineTriggerBuildAction.createPipelineAction);
    }

    public void triggerActionJobsOnRunDelete(WorkflowJob workflowJob, Run<?, ?> run) {
        this.triggerActionJobs(workflowJob, run, PipelineTriggerBuildAction.deleteRunPipelineAction);
    }

    private enum PipelineTriggerBuildAction {
        createPipelineAction, deletePipelineAction, deleteRunPipelineAction
    }

    public String getBranchIncludeFilter() {
        return branchIncludeFilter;
    }

    @DataBoundSetter
    public void setBranchIncludeFilter(String branchIncludeFilter) {
        this.branchIncludeFilter = branchIncludeFilter;
    }

    public String getBranchExcludeFilter() {
        return branchExcludeFilter;
    }

    @DataBoundSetter
    public void setBranchExcludeFilter(String branchExcludeFilter) {
        this.branchExcludeFilter = branchExcludeFilter;
    }

    private boolean checkIncludeFilter(String projectName, PipelineTriggerProperty pipelineTriggerProperty) {
        String wildcardDefinitions = pipelineTriggerProperty.getBranchIncludeFilter();
        return Pattern.matches(convertToPattern(wildcardDefinitions), projectName);
    }

    private boolean checkExcludeFilter(String projectName, PipelineTriggerProperty pipelineTriggerProperty) {
        String wildcardDefinitions = pipelineTriggerProperty.getBranchExcludeFilter();
        return Pattern.matches(convertToPattern(wildcardDefinitions), projectName);
    }

    public static String convertToPattern(String wildcardDefinitions) {
        StringBuilder quotedBranches = new StringBuilder();
        for (String wildcard : wildcardDefinitions.split(" ")) {
            StringBuilder quotedBranch = new StringBuilder();
            for (String branch : wildcard.split("(?=[*])|(?<=[*])")) {
                if (branch.equals("*")) {
                    quotedBranch.append(".*");
                } else if (!branch.isEmpty()) {
                    quotedBranch.append(Pattern.quote(branch));
                }
            }
            if (quotedBranches.length() > 0) {
                quotedBranches.append("|");
            }
            quotedBranches.append(quotedBranch);
        }
        return quotedBranches.toString();
    }

    public List<AdditionalParameter> getAdditionalParameters() {
        return additionalParameters;
    }

    @DataBoundSetter
    public void setAdditionalParameters(List<AdditionalParameter> additionalParameters) {
        if( additionalParameters == null)
            this.additionalParameters = new ArrayList<>();
        else
            this.additionalParameters = additionalParameters;
    }

    @Extension
    public static class PipelineTriggerPropertyListener extends ItemListener{
        @Override
        public void onUpdated(Item item) {
            super.onCreated(item);
            if( item instanceof OrganizationFolder) {
                OrganizationFolder organizationFolder = (OrganizationFolder) item;
                PipelineTriggerProperty pipelineTriggerProperty = organizationFolder.getProperties().get(PipelineTriggerProperty.class);
                if(pipelineTriggerProperty != null) {
                    for (MultiBranchProject multiBranchProject : organizationFolder.getItems()) {
                        if (!(multiBranchProject instanceof WorkflowMultiBranchProject))
                            continue;
                            PipelineTriggerProperty jobPipelineTriggerProperty = (PipelineTriggerProperty) multiBranchProject.getProperties().get(PipelineTriggerProperty.class);
                            multiBranchProject.getProperties().remove(jobPipelineTriggerProperty);
                            multiBranchProject.getProperties().add(pipelineTriggerProperty);
                    }
                }
            }
        }

        @Override
        public void onCreated(Item item) {
            super.onUpdated(item);
            if( item instanceof WorkflowMultiBranchProject) {
                WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) item;
                DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> properties = workflowMultiBranchProject.getProperties();
                PipelineTriggerProperty pipelineTriggerProperty = properties.get(PipelineTriggerProperty.class);
                if( pipelineTriggerProperty == null && workflowMultiBranchProject.getParent() instanceof OrganizationFolder) {
                    //Check Parent Organization Folder
                    OrganizationFolder organizationFolder = (OrganizationFolder) workflowMultiBranchProject.getParent();
                    DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> folderProperties = organizationFolder.getProperties();
                    PipelineTriggerProperty folderPipelineTriggerProperty = folderProperties.get(PipelineTriggerProperty.class);
                    if( folderPipelineTriggerProperty != null) {
                        workflowMultiBranchProject.getProperties().add(folderPipelineTriggerProperty);
                        folderPipelineTriggerProperty.setTriggerJobParameters();
                    }
                }
                if (pipelineTriggerProperty != null)
                    pipelineTriggerProperty.setTriggerJobParameters();
            }
        }
    }

    public static PipelineTriggerProperty getPipelineTriggerPropertyFromItem(Item item) {
        WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) item.getParent();
        DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> properties = workflowMultiBranchProject.getProperties();
        return properties.get(PipelineTriggerProperty.class);
    }

    public static PipelineTriggerProperty getPipelineTriggerPropertyFromItem(Run run) {
        WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) run.getParent().getParent();
        DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> properties = workflowMultiBranchProject.getProperties();
        return properties.get(PipelineTriggerProperty.class);
    }

    public static void triggerPipelineTriggerPropertyFromParentForOnCreate(Item item){
        if (item instanceof WorkflowJob && item.getParent() instanceof WorkflowMultiBranchProject) {
            PipelineTriggerProperty pipelineTriggerProperty = getPipelineTriggerPropertyFromItem(item);
            if(pipelineTriggerProperty != null)
                pipelineTriggerProperty.triggerCreateActionJobs((WorkflowJob) item);
            else
                LOGGER.fine(String.format("PipelineTriggerProperty is null in Item:%s", item.getFullName()));
        }
        else {
            LOGGER.fine(String.format("Item:%s is not instance of WorkflowJob", item.getFullName()));
        }
    }

    public static void triggerPipelineTriggerPropertyFromParentForOnDelete(Item item){
        if (item instanceof WorkflowJob && item.getParent() instanceof WorkflowMultiBranchProject) {
            PipelineTriggerProperty pipelineTriggerProperty = getPipelineTriggerPropertyFromItem(item);
            if(pipelineTriggerProperty != null){
                pipelineTriggerProperty.triggerDeleteActionJobs((WorkflowJob) item);
                for (Run run : ((WorkflowJob) item).getBuilds()) {
                    pipelineTriggerProperty.triggerActionJobsOnRunDelete((WorkflowJob) item, run);
                }
            }
            else
                LOGGER.fine(String.format("PipelineTriggerProperty is null in Item:%s", item.getFullName()));
        }
        else {
            LOGGER.fine(String.format("Item:%s is not instance of WorkflowJob", item.getFullName()));
        }
    }

    public static void triggerPipelineTriggerPropertyFromParentForOnRunDelete(Run run){
        if (run.getParent() instanceof WorkflowJob && run.getParent().getParent() instanceof WorkflowMultiBranchProject) {
            PipelineTriggerProperty pipelineTriggerProperty = getPipelineTriggerPropertyFromItem(run);
            if(pipelineTriggerProperty != null)
                pipelineTriggerProperty.triggerActionJobsOnRunDelete((WorkflowJob) run.getParent(), run);
            else
                LOGGER.fine(String.format("PipelineTriggerProperty is null in Item:%s", run.getParent().getFullName()));
        }
        else {
            LOGGER.fine(String.format("Item:%s is not instance of WorkflowJob", run.getParent().getFullName()));
        }
    }

    /**
     * Get Pull Request related information from workflow job.
     * @param workflowJob Indexed job
     * @return PullRequestInfo
     */
    public PullRequestInfo getPullRequestInfo(WorkflowJob workflowJob) {
        BranchJobProperty branchJobProperty = workflowJob.getProperty(BranchJobProperty.class);
        if(branchJobProperty == null) {
            LOGGER.fine("BranchJobProperty not found. Returning empty PullRequestInfo");
            return new PullRequestInfo("","");
        }
        Branch branch = branchJobProperty.getBranch();
        SCMHead scmHead = branch.getHead();
        if( scmHead instanceof ChangeRequestSCMHead2) {
            ChangeRequestSCMHead2 changeRequestSCMHead2 = (ChangeRequestSCMHead2) branch.getHead();
            String sourceBranchName = changeRequestSCMHead2.getOriginName();
            String targetBranchName = changeRequestSCMHead2.getTarget().getName();
            return new PullRequestInfo(sourceBranchName, targetBranchName);
        }
        else
        {
            return new PullRequestInfo(scmHead.getName(),"");
        }
    }

}
