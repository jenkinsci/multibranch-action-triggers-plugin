package org.jenkinsci.plugins.workflow.multibranch;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ExtendedWorkflowMultiBranchProjectFactory extends WorkflowMultiBranchProjectFactory {

    private PipelineTriggerProperty pipelineTriggerProperty;

    public PipelineTriggerProperty getPipelineTriggerProperty() {
        return pipelineTriggerProperty;
    }

    @DataBoundConstructor
    public ExtendedWorkflowMultiBranchProjectFactory(PipelineTriggerProperty pipelineTriggerProperty) {
        this.pipelineTriggerProperty = pipelineTriggerProperty;
    }

    @DataBoundSetter
    public void setPipelineTriggerProperty(PipelineTriggerProperty pipelineTriggerProperty) {
        this.pipelineTriggerProperty = pipelineTriggerProperty;
    }

    @Override
    protected void customize(WorkflowMultiBranchProject project)  {
        project.getProperties().add(this.getPipelineTriggerProperty());
    }

}
