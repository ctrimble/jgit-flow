package com.atlassian.maven.plugins.jgitflow.mojo;

import com.atlassian.maven.jgitflow.api.MavenJGitFlowExtension;
import com.atlassian.maven.jgitflow.api.MavenReleaseFinishExtension;
import com.atlassian.maven.plugins.jgitflow.ReleaseContext;
import com.atlassian.maven.plugins.jgitflow.exception.MavenJGitFlowException;
import com.atlassian.maven.plugins.jgitflow.manager.FlowReleaseManager;

import com.google.common.base.Strings;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * @since version
 */
@Mojo(name = "release-finish", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST)
public class ReleaseFinishMojo extends AbstractJGitFlowMojo
{
    /**
     * Whether to automatically assign submodules the parent version. If set to false, the user will be prompted for the
     * version of each submodules.
     *
     */
    @Parameter( defaultValue = "false", property = "autoVersionSubmodules" )
    private boolean autoVersionSubmodules = false;
    
    @Parameter( defaultValue = "false", property = "pushReleases" )
    private boolean pushReleases = false;

    @Parameter( defaultValue = "false", property = "noDeploy" )
    private boolean noDeploy = false;

    @Parameter( defaultValue = "false", property = "keepBranch" )
    private boolean keepBranch = false;

    @Parameter( defaultValue = "false", property = "squash" )
    private boolean squash = false;

    @Parameter( defaultValue = "false", property = "noTag" )
    private boolean noTag = false;

    @Parameter( defaultValue = "false", property = "noReleaseBuild" )
    private boolean noReleaseBuild = false;

    @Parameter( defaultValue = "false", property = "noReleaseMerge" )
    private boolean noReleaseMerge = false;

    @Parameter( defaultValue = "true", property = "useReleaseProfile" )
    private boolean useReleaseProfile = true;
    
    @Parameter( defaultValue = "true", property = "updateDependencies" )
    private boolean updateDependencies = true;

    @Parameter( property = "tagMessage", defaultValue = "")
    private String tagMessage = "";

    @Parameter( property = "releaseBranchVersionSuffix", defaultValue = "")
    private String releaseBranchVersionSuffix = "";

    @Parameter(defaultValue = "")
    private String releaseFinishExtension = "";
    
    @Component(hint = "release")
    FlowReleaseManager releaseManager;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClassloader(getClasspath()));
        
        MavenReleaseFinishExtension extensionObject = (MavenReleaseFinishExtension) getExtensionInstance(releaseFinishExtension);
        
        ReleaseContext ctx = new ReleaseContext(getBasedir());
        ctx.setInteractive(getSettings().isInteractiveMode())
                .setAutoVersionSubmodules(autoVersionSubmodules)
                .setReleaseBranchVersionSuffix(releaseBranchVersionSuffix)
                .setPushReleases(pushReleases)
                .setKeepBranch(keepBranch)
                .setSquash(squash)
                .setNoTag(noTag)
                .setNoBuild(noReleaseBuild)
                .setNoDeploy(noDeploy)
                .setUseReleaseProfile(useReleaseProfile)
                .setTagMessage(tagMessage)
                .setUpdateDependencies(updateDependencies)
                .setAllowSnapshots(allowSnapshots)
                .setEnableSshAgent(enableSshAgent)
                .setAllowUntracked(allowUntracked)
                .setNoReleaseMerge(noReleaseMerge)
                .setAllowRemote(isRemoteAllowed())
                .setAlwaysUpdateOrigin(alwaysUpdateOrigin)
                .setDefaultOriginUrl(defaultOriginUrl)
                .setScmCommentPrefix(scmCommentPrefix)
                .setScmCommentSuffix(scmCommentSuffix)
                .setUsername(username)
                .setPassword(password)
                .setPullMaster(pullMaster)
                .setPullDevelop(pullDevelop)
                .setReleaseFinishExtension(extensionObject)
                .setFlowInitContext(getFlowInitContext().getJGitFlowContext());

        try
        {
            releaseManager.finish(ctx, getReactorProjects(),session);
        }
        catch (MavenJGitFlowException e)
        {
            throw new MojoExecutionException("Error finishing release: " + e.getMessage(),e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }
}
