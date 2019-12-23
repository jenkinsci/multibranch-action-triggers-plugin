package org.jenkinsci.plugins.workflow.multibranch;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;

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
            if (item instanceof WorkflowJob && item.getParent() instanceof WorkflowMultiBranchProject)
                PipelineTriggerProperty.triggerCreateActionJobs((WorkflowJob) item);
        }

        @Override
        public void onDeleted(Item item) {
            super.onDeleted(item);
            if (item instanceof WorkflowJob && item.getParent() instanceof WorkflowMultiBranchProject) {
                PipelineTriggerProperty.triggerDeleteActionJobs((WorkflowJob) item);
                for (Run run : ((WorkflowJob) item).getBuilds()) {
                    PipelineTriggerProperty.triggerActionJobsOnRunDelete((WorkflowJob) item, run);
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
            System.out.println(run.getParent().getClass());
            if (run.getParent() instanceof WorkflowJob) {
                PipelineTriggerProperty.triggerActionJobsOnRunDelete((WorkflowJob) run.getParent(), run);
            }
        }
    }
}
