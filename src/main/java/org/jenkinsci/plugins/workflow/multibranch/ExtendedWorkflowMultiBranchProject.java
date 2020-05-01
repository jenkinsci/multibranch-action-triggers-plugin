package org.jenkinsci.plugins.workflow.multibranch;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;

public class ExtendedWorkflowMultiBranchProject extends WorkflowBranchProjectFactory {

    @Extension
    public static class ItemListenerImpl extends ItemListener {
        @Override
        public void onCreated(Item item) {
            super.onCreated(item);
            PipelineTriggerProperty.triggerPipelineTriggerPropertyFromParentForOnCreate(item);
        }

        @Override
        public void onDeleted(Item item) {
            super.onDeleted(item);
            PipelineTriggerProperty.triggerPipelineTriggerPropertyFromParentForOnDelete(item);
        }

    }

    @Extension
    public static class RunListenerImpl extends RunListener<Run>
    {
        @Override
        public void onDeleted(Run run) {
            super.onDeleted(run);
            PipelineTriggerProperty.triggerPipelineTriggerPropertyFromParentForOnRunDelete(run);
        }
    }
}
