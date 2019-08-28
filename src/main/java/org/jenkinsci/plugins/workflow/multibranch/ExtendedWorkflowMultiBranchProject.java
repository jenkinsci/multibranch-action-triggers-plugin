package org.jenkinsci.plugins.workflow.multibranch;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
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
            if( item instanceof WorkflowJob && item.getParent() instanceof WorkflowMultiBranchProject)
                PipelineTriggerProperty.triggerCreateActionJobs((WorkflowJob) item);
        }

        @Override
        public void onDeleted(Item item) {
            super.onDeleted(item);
            if( item instanceof WorkflowJob && item.getParent() instanceof WorkflowMultiBranchProject)
                PipelineTriggerProperty.triggerDeleteActionJobs((WorkflowJob) item);
        }
    }
}
