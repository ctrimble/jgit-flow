package com.atlassian.maven.plugins.jgitflow.manager.tasks;

import java.util.List;

import com.atlassian.jgitflow.core.JGitFlow;
import com.atlassian.maven.plugins.jgitflow.BranchType;
import com.atlassian.maven.plugins.jgitflow.ReleaseContext;
import com.atlassian.maven.plugins.jgitflow.VersionState;
import com.atlassian.maven.plugins.jgitflow.exception.MavenJGitFlowException;
import com.atlassian.maven.plugins.jgitflow.helper.MavenExecutionHelper;
import com.atlassian.maven.plugins.jgitflow.helper.SessionAndProjects;
import com.atlassian.maven.plugins.jgitflow.provider.*;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import static com.google.common.base.Preconditions.checkNotNull;

@Component(role = CheckoutAndGetProjects.class)
public class CheckoutAndGetProjects
{
    @Requirement
    private MavenExecutionHelper mavenExecutionHelper;

    @Requirement
    private JGitFlowProvider jGitFlowProvider;

    @Requirement
    private MavenSessionProvider sessionProvider;

    @Requirement
    private ContextProvider contextProvider;

    @Requirement
    private ReactorProjectsProvider reactorProjectsProvider;
    
    public SessionAndProjects run(String branchName) throws MavenJGitFlowException
    {
        try
        {
            JGitFlow flow = jGitFlowProvider.gitFlow();
            ReleaseContext ctx = contextProvider.getContext();

            flow.git().checkout().setName(branchName).call();
            
            //reload the reactor projects for develop
            MavenSession branchSession = mavenExecutionHelper.getSessionForBranch(branchName, ReleaseUtil.getRootProject(reactorProjectsProvider.getReactorProjects()), sessionProvider.getSession());
            
            return new SessionAndProjects(branchSession,branchSession.getSortedProjects());
        }
        catch (Exception e)
        {
            throw new MavenJGitFlowException("Error checking out branch and loading projects", e);
        }
    }
}