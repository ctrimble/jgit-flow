package com.atlassian.maven.plugins.jgitflow.mojo;

import com.atlassian.maven.jgitflow.api.MavenHotfixStartExtension;
import com.atlassian.maven.plugins.jgitflow.ReleaseContext;
import com.atlassian.maven.plugins.jgitflow.exception.MavenJGitFlowException;
import com.atlassian.maven.plugins.jgitflow.manager.FlowReleaseManager;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * @since version
 */
@Mojo(name = "hotfix-start", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST)
public class HotfixStartMojo extends AbstractJGitFlowMojo
{
    /**
     * Whether to automatically assign submodules the parent version. If set to false, the user will be prompted for the
     * version of each submodules.
     *
     */
    @Parameter( defaultValue = "false", property = "autoVersionSubmodules" )
    private boolean autoVersionSubmodules = false;

    /**
     * Default version to use when preparing a release
     *
     */
    @Parameter( property = "releaseVersion", defaultValue = "")
    private String releaseVersion = "";

    @Parameter( defaultValue = "true", property = "updateDependencies" )
    private boolean updateDependencies = true;

    @Parameter( defaultValue = "false", property = "pushHotfixes" )
    private boolean pushHotfixes = false;

    @Parameter( property = "startCommit", defaultValue = "")
    private String startCommit = "";

    @Component(hint = "hotfix")
    FlowReleaseManager releaseManager;

    @Parameter(defaultValue = "")
    private String hotfixStartExtension = "";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClassloader(getClasspath()));
        
        MavenHotfixStartExtension extensionObject = (MavenHotfixStartExtension) getExtensionInstance(hotfixStartExtension);
        
        ReleaseContext ctx = new ReleaseContext(getBasedir());
        ctx.setAutoVersionSubmodules(autoVersionSubmodules)
           .setInteractive(getSettings().isInteractiveMode())
           .setDefaultReleaseVersion(releaseVersion)
           .setAllowSnapshots(allowSnapshots)
           .setUpdateDependencies(updateDependencies)
           .setEnableSshAgent(enableSshAgent)
           .setAllowUntracked(allowUntracked)
           .setPushHotfixes(pushHotfixes)
           .setStartCommit(startCommit)
           .setAllowRemote(isRemoteAllowed())
           .setAlwaysUpdateOrigin(alwaysUpdateOrigin)
           .setDefaultOriginUrl(defaultOriginUrl)
           .setPullMaster(pullMaster)
           .setPullDevelop(pullDevelop)
           .setScmCommentPrefix(scmCommentPrefix)
           .setScmCommentSuffix(scmCommentSuffix)
           .setUsername(username)
           .setPassword(password)
           .setHotfixStartExtension(extensionObject)
           .setFlowInitContext(getFlowInitContext().getJGitFlowContext());

        try
        {
            releaseManager.start(ctx, getReactorProjects(),session);
        }
        catch (MavenJGitFlowException e)
        {
            throw new MojoExecutionException("Error starting hotfix: " + e.getMessage(),e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }
}
