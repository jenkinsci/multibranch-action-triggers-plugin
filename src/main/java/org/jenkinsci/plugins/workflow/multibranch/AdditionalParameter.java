package org.jenkinsci.plugins.workflow.multibranch;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

public class AdditionalParameter extends AbstractDescribableImpl<AdditionalParameter> {

    private String name;
    private String value;

    @DataBoundConstructor
    public AdditionalParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    @DataBoundSetter
    public void setValue(String value) {
        this.value = value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AdditionalParameter>{

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Additional Parameter";
        }
    }
}
