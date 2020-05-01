package org.jenkinsci.plugins.workflow.multibranch;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;
import jenkins.branch.MultiBranchProjectFactory;
import jenkins.branch.MultiBranchProjectFactoryDescriptor;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import java.io.IOException;

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
