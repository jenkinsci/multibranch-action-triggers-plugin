package org.jenkinsci.plugins.workflow.multibranch;

import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;

import hudson.util.DescribableList;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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
