package org.jenkinsci.plugins.workflow.multibranch;

import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;

import hudson.util.DescribableList;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

public class ExtendedWorkflowMultiBranchProject extends WorkflowMultiBranchProject {

    public ExtendedWorkflowMultiBranchProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Extension
    public static class ItemListenerImpl extends ItemListener {
        @Override
        public void onCreated(Item item) {
            super.onCreated(item);
            if (item instanceof WorkflowJob && item.getParent() instanceof WorkflowMultiBranchProject){
                WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) item.getParent();
                DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> properties = workflowMultiBranchProject.getProperties();
                PipelineTriggerProperty pipelineTriggerProperty = properties.get(PipelineTriggerProperty.class);
                if(pipelineTriggerProperty != null)
                    pipelineTriggerProperty.triggerCreateActionJobs((WorkflowJob) item);
            }
        }

        @Override
        public void onDeleted(Item item) {
            super.onDeleted(item);
            if (item instanceof WorkflowJob && item.getParent() instanceof WorkflowMultiBranchProject) {
                WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) item.getParent();
                DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> properties = workflowMultiBranchProject.getProperties();
                PipelineTriggerProperty pipelineTriggerProperty = properties.get(PipelineTriggerProperty.class);
                if(pipelineTriggerProperty != null) {
                    pipelineTriggerProperty.triggerDeleteActionJobs((WorkflowJob) item);
                    for (Run run : ((WorkflowJob) item).getBuilds()) {
                        pipelineTriggerProperty.triggerActionJobsOnRunDelete((WorkflowJob) item, run);
                    }
                }
            }
        }

    }

    @Extension
    public static class RunListenerImpl extends RunListener<Run>
    {
        @Override
        public void onDeleted(Run run) {
            super.onDeleted(run);
            if (run.getParent() instanceof WorkflowJob && run.getParent().getParent() instanceof WorkflowMultiBranchProject) {
                WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) run.getParent().getParent();
                DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> properties = workflowMultiBranchProject.getProperties();
                PipelineTriggerProperty pipelineTriggerProperty = properties.get(PipelineTriggerProperty.class);
                if(pipelineTriggerProperty != null)
                    pipelineTriggerProperty.triggerActionJobsOnRunDelete((WorkflowJob) run.getParent(), run);
            }
        }
    }
}
