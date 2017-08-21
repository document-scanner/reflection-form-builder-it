/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.reflection.form.builder.jpa.sequence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.jhbuild.java.wrapper.ActionOnMissingBinary;
import richtercloud.jhbuild.java.wrapper.ArchitectureNotRecognizedException;
import richtercloud.jhbuild.java.wrapper.BuildFailureException;
import richtercloud.jhbuild.java.wrapper.ExtractionException;
import richtercloud.jhbuild.java.wrapper.JHBuildJavaWrapper;
import richtercloud.jhbuild.java.wrapper.MissingSystemBinary;
import richtercloud.jhbuild.java.wrapper.ModuleBuildFailureException;
import richtercloud.jhbuild.java.wrapper.OSNotRecognizedException;
import richtercloud.jhbuild.java.wrapper.download.AutoDownloader;
import richtercloud.message.handler.IssueHandler;
import richtercloud.message.handler.LoggerIssueHandler;
import richtercloud.reflection.form.builder.jpa.JPACachedFieldRetriever;
import richtercloud.reflection.form.builder.jpa.ReflectionFormBuilderITTools;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.PostgresqlAutoPersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.PostgresqlAutoPersistenceStorageConf;
import richtercloud.test.tools.ParallelITExecutor;
import richtercloud.validation.tools.FieldRetriever;

/**
 *
 * @author richter
 */
public class PostgresqlSequenceManagerIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(PostgresqlSequenceManagerIT.class);

    /**
     * Test of createSequence method, of class PostgresqlSequenceManager.
     */
    @Test
    public void testCreateSequence() throws InterruptedException,
            ExecutionException,
            OSNotRecognizedException,
            ArchitectureNotRecognizedException,
            IOException,
            ExtractionException,
            MissingSystemBinary,
            BuildFailureException,
            ModuleBuildFailureException {
        int parallelism = 10;
            //parallelism allows to better reproduce and detect deadlocks under
            //heavy load
        String sequenceName = "with-minus";
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class));
        String username = "reflection-form-builder";
        String password = username;
        String databaseName = "reflection-form-builder";
        IssueHandler issueHandler = new LoggerIssueHandler(LOGGER);
        File postgresqlInstallationPrefixDir = Files.createTempDirectory(PostgresqlSequenceManagerIT.class.getSimpleName()).toFile();
        LOGGER.debug(String.format("using '%s' as PostgreSQL installation prefix",
                postgresqlInstallationPrefixDir.getAbsolutePath()));
        File downloadDir = Files.createTempDirectory(PostgresqlSequenceManagerIT.class.getSimpleName()).toFile();
            //SystemUtils.getUserHome() causes trouble
            //($HOME/jhbuild/checkout might be jhbuilds default extraction
            //directory)
        LOGGER.debug(String.format("using '%s' as JHBuild Java wrapper download directory",
                downloadDir));
        JHBuildJavaWrapper jHBuildJavaWrapper = new JHBuildJavaWrapper(postgresqlInstallationPrefixDir, //installationPrefixDir
                downloadDir, //downloadDir
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                new AutoDownloader(), //downloader
                false,
                true, //silenceStdout
                true, //silenceStderr
                issueHandler);
        String moduleName = "postgresql-9.6.3";
        LOGGER.info(String.format("building module %s from JHBuild Java wrapper's default moduleset",
                moduleName));
        jHBuildJavaWrapper.installModuleset(moduleName);
            //moduleset shipped with jhbuild-java-wrapper
        String initdb = new File(postgresqlInstallationPrefixDir,
                String.join(File.separator, "bin", "initdb")).getAbsolutePath();
        String postgres = new File(postgresqlInstallationPrefixDir,
                String.join(File.separator, "bin", "postgres")).getAbsolutePath();
        String createdb = new File(postgresqlInstallationPrefixDir,
                String.join(File.separator, "bin", "createdb")).getAbsolutePath();
        String pgCtl = new File(postgresqlInstallationPrefixDir,
                String.join(File.separator, "bin", "pg_ctl")).getAbsolutePath();
        String persistenceUnitName = "reflection-form-builder-it";
        Lock findPortLock = new ReentrantLock(true //fair
                );
        FieldRetriever fieldRetriever = new JPACachedFieldRetriever();
        ParallelITExecutor parallelITExecutor = new ParallelITExecutor();
        parallelITExecutor.executeLambdaInParallel(parallelism, () -> {
            PersistenceStorage<Long> storage = null;
            try {
                File databaseDir = Files.createTempDirectory(PostgresqlSequenceManagerIT.class.getSimpleName()).toFile();
                LOGGER.debug(String.format("using '%s' as database directory", databaseDir));
                FileUtils.forceDelete(databaseDir);
                File schemeChecksumFile = File.createTempFile(PostgresqlSequenceManagerIT.class.getSimpleName(), "checksum");
                findPortLock.lock();
                try {
                    int databasePort = ReflectionFormBuilderITTools.findFreePort(PostgresqlAutoPersistenceStorageConf.PORT_DEFAULT);
                    LOGGER.info(String.format("using random next free port %d",
                            databasePort));
                    PostgresqlAutoPersistenceStorageConf storageConf = new PostgresqlAutoPersistenceStorageConf(entityClasses,
                            "localhost",
                            username,
                            password,
                            databaseName,
                            schemeChecksumFile,
                            databaseDir.getAbsolutePath(), //databaseDir
                            initdb, //initdbBinaryPath
                            postgres, //postgresBinaryPath
                            createdb, //createdbBinaryPath
                            pgCtl, //pgCtlBinaryPath
                            databasePort
                    );
                    storage = new PostgresqlAutoPersistenceStorage(storageConf,
                            persistenceUnitName,
                            10, //parallelQueryCount
                            fieldRetriever,
                            issueHandler);
                    storage.start();
                }finally {
                    findPortLock.unlock();
                }
                PostgresqlSequenceManager instance = new PostgresqlSequenceManager(storage);
                instance.createSequence(sequenceName);
                long nextSequenceValue = instance.getNextSequenceValue(sequenceName);
                assertEquals(1L, nextSequenceValue);
            } finally {
                if(storage != null) {
                    storage.shutdown();
                }
            }
            return null;
        });
    }
}
