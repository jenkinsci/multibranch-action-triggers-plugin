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

    private String createActionJobsToTrigger;
    private String deleteActionJobsToTrigger;
    private String actionJobsToTriggerOnRunDelete;
    private transient List<Job> createActionJobs;
    private transient List<Job> deleteActionJobs;
    private transient List<Job> actionJobsOnRunDelete;
    private final int quitePeriod = 0;
    static final String projectNameParameterKey = "SOURCE_PROJECT_NAME";
    static final String projectFullNameParameterKey = "SOURCE_PROJECT_FULL_NAME";
    static final String runNumberParameterKey = "SOURCE_RUN_NUMBER";
    static final String runDisplayNameParameterKey = "SOURCE_RUN_DISPLAY_NAME";
    private String branchIncludeFilter;
    private String branchExcludeFilter;

    /**
     * @param createActionJobsToTrigger Full names of the jobs in comma separated format which are defined in the field
     * @param deleteActionJobsToTrigger Full names of the jobs in comma separated format which are defined in the field
     * @param actionJobsToTriggerOnRunDelete Full names of the jobs in comma separated format which are defined in the field
     * @see DataBoundConstructor
     */
    @DataBoundConstructor
    public PipelineTriggerProperty(
            String createActionJobsToTrigger,
            String deleteActionJobsToTrigger,
            String actionJobsToTriggerOnRunDelete,
            String branchIncludeFilter, String
            branchExcludeFilter) {
        this.setCreateActionJobsToTrigger(createActionJobsToTrigger);
        this.setDeleteActionJobsToTrigger(deleteActionJobsToTrigger);
        this.setActionJobsToTriggerOnRunDelete(actionJobsToTriggerOnRunDelete);
        this.setBranchIncludeFilter(branchIncludeFilter);
        this.setBranchExcludeFilter(branchExcludeFilter);
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
        this.setCreateActionJobs(this.validateJobs(createActionJobsToTrigger, false));
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
        this.setDeleteActionJobs(this.validateJobs(deleteActionJobsToTrigger, false));
        this.deleteActionJobsToTrigger = this.convertJobsToCommaSeparatedString(this.getDeleteActionJobs());
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
        this.setActionJobsOnRunDelete(this.validateJobs(actionJobsToTriggerOnRunDelete, true));
        this.actionJobsToTriggerOnRunDelete = this.convertJobsToCommaSeparatedString(this.getActionJobsOnRunDelete());
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
     * Getter method for @actionJobsOnRunDelete
     *
     * @return List of Job for Run Delete Action
     */
    public List<Job> getActionJobsOnRunDelete() {
        return actionJobsOnRunDelete;
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
     * Additionally, create StringParameterDefinition(s) in Jobs to pass @projectNameParameterKey
     * and possibly @runNumberParameterKey and @runDisplayNameParameterKey as build value.
     *
     * @param actionJobsToTrigger Full names of the Jobs in comma-separated format which are defined in the field
     * @param addRunParameters    If the parameters for Run number and Run display names should also be created on the job
     * @return List of Job
     */
    private List<Job> validateJobs(String actionJobsToTrigger, boolean addRunParameters) {
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
        List<Job> validatedJobs = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(Util.fixNull(Util.fixEmptyAndTrim(actionJobsToTrigger)), ",");
        while (tokenizer.hasMoreTokens()) {
            String tokenJobName = tokenizer.nextToken();
            for (Job job : jobs) {
                if (job.getFullName().trim().equals(tokenJobName.trim())) {
                    List<ParameterDefinition> parameters = new ArrayList<>();
                    //Try to add job properties. If fails do not stop just log warning.
                    if (!checkJobHasSetProjectNameParameter(job))
                    {
                        parameters.add(new StringParameterDefinition(
                            PipelineTriggerProperty.projectNameParameterKey,
                            "",
                            "Added by Multibranch Pipeline Plugin"));
                    }
                    if (!checkJobHasSetProjectFullNameParameter(job))
                    {
                        parameters.add(new StringParameterDefinition(
                            PipelineTriggerProperty.projectFullNameParameterKey,
                            "",
                            "Added by Multibranch Pipeline Plugin"));
                    }
                    if (addRunParameters && !checkJobHasSetRunNumberParameter(job)) {
                        parameters.add(new StringParameterDefinition(
                            PipelineTriggerProperty.runNumberParameterKey,
                            "",
                            "Added by Multibranch Pipeline Plugin"));
                    }
                    if (addRunParameters && !checkJobHasSetRunDisplayNameParameter(job)) {
                        parameters.add(new StringParameterDefinition(
                            PipelineTriggerProperty.runDisplayNameParameterKey,
                            "",
                            "Added by Multibranch Pipeline Plugin"));
                    }
                    if (!parameters.isEmpty())
                    {
                        try {
                            ParameterDefinition[] propertiesToAdd = parameters.toArray(new ParameterDefinition[]{});
                            job.addProperty(new ParametersDefinitionProperty(propertiesToAdd));
                            job.save();
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "[MultiBranch Action Triggers Plugin] Could not set String Parameter Definitions." +
                                " This may affect jobs which are triggered from Multibranch Pipeline Plugin.",
                                ex);
                        }
                    }
                    validatedJobs.add(job);
                }
            }
        }
        return validatedJobs;
    }

    /**
     * Check if the job has set the parameter for @projectNameParameterKey
     * @param job The Job to check
     * @return true if the parameter is present, false otherwise
     */
    private static boolean checkJobHasSetProjectNameParameter(Job job) {
        List<JobProperty> allProperties = job.getAllProperties();
        for (JobProperty property : allProperties) {
            if (property instanceof ParametersDefinitionProperty &&
                    ((ParametersDefinitionProperty) property).
                            getParameterDefinition(PipelineTriggerProperty.projectNameParameterKey) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the job has set the parameter for @projectFullNameParameterKey
     * @param job The Job to check
     * @return true if the parameter is present, false otherwise
     */
    private static boolean checkJobHasSetProjectFullNameParameter(Job job) {
        List<JobProperty> allProperties = job.getAllProperties();
        for (JobProperty property : allProperties) {
            if (property instanceof ParametersDefinitionProperty &&
                ((ParametersDefinitionProperty) property).
                    getParameterDefinition(PipelineTriggerProperty.projectFullNameParameterKey) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the job has set the parameter for @runNumberParameterKey
     * @param job The Job to check
     * @return true if the parameter is present, false otherwise
     */
    private static boolean checkJobHasSetRunNumberParameter(Job job) {
        List<JobProperty> allProperties = job.getAllProperties();
        for (JobProperty property: allProperties){
            if (property instanceof ParametersDefinitionProperty &&
                ((ParametersDefinitionProperty) property).
                    getParameterDefinition(PipelineTriggerProperty.runNumberParameterKey) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the job has set the parameter for @runDisplayNameParameterKey
     * @param job The Job to check
     * @return true if the parameter is present, false otherwise
     */
    private static boolean checkJobHasSetRunDisplayNameParameter(Job job) {
        List<JobProperty> allProperties = job.getAllProperties();
        for (JobProperty property: allProperties){
            if (property instanceof ParametersDefinitionProperty &&
                ((ParametersDefinitionProperty) property).
                    getParameterDefinition(PipelineTriggerProperty.runDisplayNameParameterKey) != null) {
                return true;
            }
        }
        return false;
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
     * @param projectFullName Full name of the project.
     *                    Also this value will be passed as StringParameterDefinition
     */
    public void buildCreateActionJobs(String projectName, String projectFullName) {
        this.buildJobs(projectName, projectFullName, null, null, this.validateJobs(this.getCreateActionJobsToTrigger(), false));
    }

    /**
     * Build Jobs which are defined in the @deleteActionJobsToTrigger field.
     *
     * @param projectName Name of the project. This will be branch name which is found in branch indexing.
     *                    Also this value will be passed as StringParameterDefinition
     * @param projectFullName Full name of the project.
     *                    Also this value will be passed as StringParameterDefinition
     */
    public void buildDeleteActionJobs(String projectName, String projectFullName) {
        this.buildJobs(projectName, projectFullName, null, null, this.validateJobs(this.getDeleteActionJobsToTrigger(), false));
    }

    /**
     * Build Jobs which are defined in the @actionJobsToTriggerOnRunDelete field.
     *
     * @param projectName Name of the project. This will be branch name which is found in branch indexing.
     *                    Also this value will be passed as StringParameterDefinition
     * @param projectFullName Full name of the project.
     *                    Also this value will be passed as StringParameterDefinition
     * @param runNumber   Number of the upstream build.
     *                    This value will be passed as StringParameterDefinition.
     * @param runDisplayName Display Name of the upstream build.
     *                    This value will be passed as StringParameterDefinition.
     */
    public void buildActionJobsOnRunDelete(String projectName, String projectFullName, Integer runNumber, String runDisplayName) {
        this.buildJobs(projectName, projectFullName, runNumber, runDisplayName, this.validateJobs(this.getActionJobsToTriggerOnRunDelete(), true));
    }


    /**
     * Build Jobs and pass parameter to Build
     *
     * @param projectName Name of the project. This value will be passed as StringParameterDefinition
     * @param projectFullName Full name of the project.
     *                    Also this value will be passed as StringParameterDefinition
     * @param runNumber   Number of the upstream build, or null if the upstream trigger is not a Run.
     *                    This value will be passed as StringParameterDefinition.
     * @param runDisplayName Display Name of the upstream build, or null if the upstream trigger is not a Run.
     *                    This value will be passed as StringParameterDefinition.
     * @param jobsToBuild List of Jobs to build
     */
    private void buildJobs(
            String projectName,
            String projectFullName,
            Integer runNumber,
            String runDisplayName,
            List<Job> jobsToBuild) {
        List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(new StringParameterValue(PipelineTriggerProperty.projectNameParameterKey, projectName, "Set by Multibranch Pipeline Plugin"));
        parameterValues.add(new StringParameterValue(PipelineTriggerProperty.projectFullNameParameterKey, projectFullName, "Set by Multibranch Pipeline Plugin"));
        if (runNumber != null)
        {
            parameterValues.add(new StringParameterValue(PipelineTriggerProperty.runNumberParameterKey, runNumber.toString(), "Set by Multibranch Pipeline Plugin"));
        }
        if (runDisplayName != null)
        {
            parameterValues.add(new StringParameterValue(PipelineTriggerProperty.runDisplayNameParameterKey, runDisplayName, "Set by Multibranch Pipeline Plugin"));
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

    private static void triggerActionJobs(WorkflowJob workflowJob, Run<?, ?> run, PipelineTriggerBuildAction action) {
        if (!(workflowJob.getParent() instanceof WorkflowMultiBranchProject)  ) {
            LOGGER.log(Level.WARNING, "[MultiBranch Action Triggers Plugin] Caller Job is not child of WorkflowMultiBranchProject. Skipping.");
            return;
        }
        WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) workflowJob.getParent();
        PipelineTriggerProperty pipelineTriggerProperty = workflowMultiBranchProject.getProperties().get(PipelineTriggerProperty.class);
        if (pipelineTriggerProperty != null) {
            if (checkExcludeFilter(workflowJob.getName(), pipelineTriggerProperty)) {
                LOGGER.log(Level.WARNING, "[MultiBranch Action Triggers Plugin] {0} excluded by the Exclude Filter", workflowJob.getName());
            } else if (checkIncludeFilter(workflowJob.getName(), pipelineTriggerProperty)) {
                if (action.equals(PipelineTriggerBuildAction.createPipelineAction))
                    pipelineTriggerProperty.buildCreateActionJobs(workflowJob.getName(), workflowJob.getFullName());
                else if (action.equals(PipelineTriggerBuildAction.deletePipelineAction))
                    pipelineTriggerProperty.buildDeleteActionJobs(workflowJob.getName(), workflowJob.getFullName());
                else if (action.equals(PipelineTriggerBuildAction.deleteRunPipelineAction))
                    pipelineTriggerProperty.buildActionJobsOnRunDelete(workflowJob.getName(), workflowJob.getFullName(), run.getNumber(), run.getDisplayName());
            } else {
                LOGGER.log(Level.WARNING, "[MultiBranch Action Triggers Plugin] {0} not included by the Include Filter", workflowJob.getName());
            }
        }
    }

    public static void triggerDeleteActionJobs(WorkflowJob workflowJob) {
        triggerActionJobs(workflowJob, null, PipelineTriggerBuildAction.deletePipelineAction);
    }

    public static void triggerCreateActionJobs(WorkflowJob workflowJob) {
        triggerActionJobs(workflowJob, null, PipelineTriggerBuildAction.createPipelineAction);
    }

    public static void triggerActionJobsOnRunDelete(WorkflowJob workflowJob, Run<?, ?> run) {
        triggerActionJobs(workflowJob, run, PipelineTriggerBuildAction.deleteRunPipelineAction);
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

    private static boolean checkIncludeFilter(String projectName, PipelineTriggerProperty pipelineTriggerProperty) {
        String wildcardDefinitions = pipelineTriggerProperty.getBranchIncludeFilter();
        return Pattern.matches(convertToPattern(wildcardDefinitions), projectName);
    }

    private static boolean checkExcludeFilter(String projectName, PipelineTriggerProperty pipelineTriggerProperty) {
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
}
