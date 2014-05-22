package com.atlassian.maven.plugins.jgitflow.extension.command;

import java.util.List;

import com.atlassian.jgitflow.core.GitFlowConfiguration;
import com.atlassian.jgitflow.core.JGitFlow;
import com.atlassian.jgitflow.core.JGitFlowReporter;
import com.atlassian.jgitflow.core.command.JGitFlowCommand;
import com.atlassian.jgitflow.core.exception.JGitFlowExtensionException;
import com.atlassian.jgitflow.core.extension.ExtensionCommand;
import com.atlassian.jgitflow.core.extension.ExtensionFailStrategy;
import com.atlassian.maven.plugins.jgitflow.BranchType;
import com.atlassian.maven.plugins.jgitflow.ReleaseContext;
import com.atlassian.maven.plugins.jgitflow.VersionType;
import com.atlassian.maven.plugins.jgitflow.helper.CurrentBranchHelper;
import com.atlassian.maven.plugins.jgitflow.helper.PomUpdater;
import com.atlassian.maven.plugins.jgitflow.helper.ProjectHelper;
import com.atlassian.maven.plugins.jgitflow.provider.ContextProvider;
import com.atlassian.maven.plugins.jgitflow.provider.JGitFlowProvider;
import com.atlassian.maven.plugins.jgitflow.provider.ProjectCacheKey;
import com.atlassian.maven.plugins.jgitflow.provider.ReactorProjectsProvider;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.jgit.api.Git;

import static com.google.common.base.Preconditions.checkNotNull;

@Component(role = UpdatePomsWithNonSnapshotCommand.class)
public class UpdatePomsWithNonSnapshotCommand extends AbstractLogEnabled implements ExtensionCommand
{
    @Requirement
    private ContextProvider contextProvider;

    @Requirement
    private JGitFlowProvider jGitFlowProvider;

    @Requirement
    private PomUpdater pomUpdater;

    @Requirement
    private ProjectHelper projectHelper;

    @Requirement
    private ReactorProjectsProvider projectsProvider;

    @Requirement
    private CurrentBranchHelper currentBranchHelper;

    @Override
    public void execute(GitFlowConfiguration configuration, Git git, JGitFlowCommand gitFlowCommand, JGitFlowReporter reporter) throws JGitFlowExtensionException
    {
        ProjectCacheKey cacheKey = null;
        VersionType versionType = null;
        String versionSuffix = "";

        BranchType branchType = BranchType.UNKNOWN;

        try
        {
            branchType = currentBranchHelper.getBranchType();

            ReleaseContext ctx = contextProvider.getContext();
            JGitFlow flow = jGitFlowProvider.gitFlow();

            switch (branchType)
            {
                case RELEASE:
                    cacheKey = ProjectCacheKey.RELEASE_START_LABEL;
                    versionSuffix = ctx.getReleaseBranchVersionSuffix();
                    break;

                case HOTFIX:
                    cacheKey = ProjectCacheKey.HOTFIX_LABEL;
                    versionSuffix = "";
                    break;

                default:
                    throw new JGitFlowExtensionException("Unsupported branch type '" + branchType.name() + "' while running " + this.getClass().getSimpleName() + " command");
            }

            checkNotNull(cacheKey);

            String unprefixedBranchName = currentBranchHelper.getUnprefixedBranchName();
            String fullBranchName = currentBranchHelper.getBranchName();

            getLogger().info("(" + fullBranchName + ") Updating poms for " + branchType.name());

            //reload the reactor projects for release
            List<MavenProject> branchProjects = currentBranchHelper.getProjectsForCurrentBranch();

            pomUpdater.removeSnapshotFromPomVersions(cacheKey, unprefixedBranchName, versionSuffix, branchProjects);

            projectHelper.commitAllPoms(flow.git(), branchProjects, ctx.getScmCommentPrefix() + "updating poms for branch'" + unprefixedBranchName + "' with non-snapshot versions" + ctx.getScmCommentSuffix());
        }
        catch (Exception e)
        {
            throw new JGitFlowExtensionException("Error updating poms with non-snapshot versions for branch '" + branchType.name() + "'");
        }
    }

    @Override
    public ExtensionFailStrategy failStrategy()
    {
        return ExtensionFailStrategy.ERROR;
    }
}