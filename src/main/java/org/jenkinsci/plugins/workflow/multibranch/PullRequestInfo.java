package org.jenkinsci.plugins.workflow.multibranch;
/**
 * Class for storing Pull Request source and target branch names
 */
public class PullRequestInfo {
    private String sourceBranchName;
    private String targetBranchName;

    public PullRequestInfo(String sourceBranchName, String targetBranchName) {
        this.sourceBranchName = sourceBranchName;
        this.targetBranchName = targetBranchName;
    }

    public String getSourceBranchName() {
        return sourceBranchName;
    }

    public String getTargetBranchName() {
        return targetBranchName;
    }
}
