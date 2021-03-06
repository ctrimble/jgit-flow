package com.atlassian.maven.plugins.jgitflow.it;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import com.atlassian.maven.plugins.jgitflow.manager.AbstractFlowReleaseManager;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import static org.junit.Assert.assertEquals;

/**
 * @since version
 */
public class FinishScriptHelper
{
    
    /**
     * The absolute path to the base directory of the test project.
     */
    protected File baseDirectory;

    /**
     * The absolute path to the local repository used for the Maven invocation on the test project.
     */
    protected File localRepositoryPath;

    /**
     * The storage of key-value pairs used to pass data from the pre-build hook script to the post-build hook script.
     */
    protected Map context;

    public FinishScriptHelper(File baseDirectory, File localRepositoryPath, Map context)
    {
        this.baseDirectory = baseDirectory;
        this.localRepositoryPath = localRepositoryPath;
        this.context = context;
    }

    public Gits createAndCloneRepo(String masterVersion, String developVersion, String releaseVersion, String branchPrefix) throws GitAPIException, IOException
    {
        File remotesBaseDir = new File(baseDirectory.getParentFile().getParentFile(), "remotes");
        remotesBaseDir.mkdirs();

        File remoteProjectDir = new File(remotesBaseDir, baseDirectory.getName());
        remoteProjectDir.mkdirs();

        FileUtils.copyDirectory(baseDirectory, remoteProjectDir);

        //update any @project.version@ vars with our plugin version
        URL url = Resources.getResource("VERSION");
        String flowVersion = Resources.toString(url, Charsets.UTF_8);
        
        Collection<File> masterXmls = FileUtils.listFiles(remoteProjectDir, new WildcardFileFilter("*.xml"), FileFilterUtils.directoryFileFilter());
        
        for (File masterXml : masterXmls)
        {
            String xmlString = FileUtils.readFileToString(masterXml);
            String updatedMasterXml = StringUtils.replace(xmlString, "<version>@project.version@</version>", "<version>" + flowVersion + "</version>");
            FileUtils.writeStringToFile(masterXml, updatedMasterXml);
        }

        Git remoteGit = Git.init().setDirectory(remoteProjectDir).call();
        remoteGit.add().addFilepattern(".").call();
        remoteGit.commit().setMessage("initial commit").call();
        remoteGit.branchCreate().setName("develop").call();
        remoteGit.commit().setMessage("added develop branch").call();

        //update Develop Version
        remoteGit.checkout().setName("develop").call();
        Collection<File> developPoms = FileUtils.listFiles(remoteProjectDir, FileFilterUtils.nameFileFilter("pom.xml"), FileFilterUtils.directoryFileFilter());

        for (File developPom : developPoms)
        {
            System.out.println("updating pom for develop: " + developPom.getAbsolutePath());
            String pomString = FileUtils.readFileToString(developPom);
            String updatedDevelopPom = StringUtils.replace(pomString, "<version>" + masterVersion + "</version>", "<version>" + developVersion + "</version>");
            FileUtils.writeStringToFile(developPom, updatedDevelopPom);
        }

        remoteGit.add().addFilepattern(".").call();
        remoteGit.commit().setMessage("updating develop versions").call();

        String branchVersion = releaseVersion;

        if("feature/".equals(branchPrefix))
        {
            branchVersion = StringUtils.replace(releaseVersion, "-", "_");
            branchVersion = StringUtils.substringBeforeLast(developVersion, "-SNAPSHOT") + "-" + branchVersion + "-SNAPSHOT";
        }
        
        remoteGit.branchCreate().setName(branchPrefix + releaseVersion).call();
        remoteGit.commit().setMessage("added branch " + branchPrefix + releaseVersion).call();
        remoteGit.checkout().setName(branchPrefix + releaseVersion).call();

        //update branch Version
        Collection<File> releasePoms = FileUtils.listFiles(remoteProjectDir, FileFilterUtils.nameFileFilter("pom.xml"), FileFilterUtils.directoryFileFilter());

        //if the prefix is feature, we need special handling of the version/branch
        
        
        for (File releasePom : releasePoms)
        {
            System.out.println("updating pom for branch: " + releasePom.getAbsolutePath());
            String pomString = FileUtils.readFileToString(releasePom);
            String updatedReleasePom = StringUtils.replace(pomString, "<version>" + developVersion + "</version>", "<version>" + branchVersion + "</version>");
            FileUtils.writeStringToFile(releasePom, updatedReleasePom);
        }

        remoteGit.add().addFilepattern(".").call();
        remoteGit.commit().setMessage("updating release versions").call();

        FileUtils.deleteQuietly(baseDirectory);
        baseDirectory.mkdirs();

        Git localGit = Git.cloneRepository().setDirectory(baseDirectory).setURI("file://" + remoteGit.getRepository().getWorkTree().getPath()).call();

        Gits gits = new Gits(remoteGit,localGit);
        
        remoteGit.checkout().setName("master").call();
        
        return gits; 
    }

    public Git remoteGit() throws IOException
    {
        File remotesBaseDir = new File(baseDirectory.getParentFile().getParentFile(), "remotes");
        File remoteProjectDir = new File(remotesBaseDir, baseDirectory.getName());

        return Git.open(remoteProjectDir);
        
    }
    
    public void comparePomFiles(String expectedPath, String actualPath) throws IOException
    {
        String expectedPom = ReleaseUtil.readXmlFile(new File(baseDirectory, expectedPath));
        String actualPom = ReleaseUtil.readXmlFile(new File(baseDirectory, actualPath));

        assertEquals("Pom files don't match!!!: \nexpected:\n" + expectedPom + "actual:\n" + actualPom, expectedPom, actualPom);
    }
    
    public void clearOrigin(Git git) throws IOException
    {
        StoredConfig config = git.getRepository().getConfig();
        config.unsetSection("remote","origin");
        config.save();
    }

    public class Gits 
    {
        public final Git remote;
        public final Git local;

        public Gits(Git remote, Git local)
        {
            this.remote = remote;
            this.local = local;
        }
    }
}
