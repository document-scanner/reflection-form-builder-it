/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.richtercloud.reflection.form.builder.jpa.sequence;

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.message.handler.LoggerIssueHandler;
import de.richtercloud.reflection.form.builder.jpa.ReflectionFormBuilderITUtils;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityA;
import de.richtercloud.reflection.form.builder.jpa.retriever.JPAOrderedCachedFieldRetriever;
import de.richtercloud.reflection.form.builder.jpa.storage.MySQLAutoPersistenceStorage;
import de.richtercloud.reflection.form.builder.jpa.storage.MySQLAutoPersistenceStorageConf;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.retriever.FieldOrderValidationException;
import de.richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import de.richtercloud.reflection.form.builder.storage.StorageCreationException;
import de.richtercloud.validation.tools.FieldRetriever;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 */
public class MySQLSequenceManagerIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(MySQLSequenceManagerIT.class);

    @Test
    public void testCreateSequence() throws IOException,
            FieldOrderValidationException,
            StorageConfValidationException,
            StorageCreationException,
            SequenceManagementException {
        PersistenceStorage<Long> storage = null;
        try {
            //assert that $HOME/mysql-5.7.24 is present and request user to download
            //if not (there might be a more elegant way to do this)
            String homeDirPath = System.getProperty("user.home");
            assert homeDirPath != null;
            File homeDir = new File(homeDirPath);
            assert homeDir.exists();
            File mySQLDir = new File(homeDir, "mysql-5.7.24");
            if(!mySQLDir.exists()) {
                throw new IllegalArgumentException(String.format("There's no MySQL installation at '%s'. Download and extract manually and restart. Can't proceed.", mySQLDir.getAbsolutePath()));
            }else if(!mySQLDir.isDirectory()){
                throw new IllegalArgumentException(String.format("MySQL directory '%s' exists, but is not a directory", mySQLDir.getAbsolutePath()));
            }

            String sequenceName = "with-minus";
            Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class));
            File databaseDir = Files.createTempDirectory(MySQLSequenceManagerIT.class.getSimpleName()).toFile();
            FileUtils.forceDelete(databaseDir);
            String databaseName = "reflection-form-builder-it";
            File schemeChecksumFile = File.createTempFile(MySQLSequenceManagerIT.class.getSimpleName(), "checksum");
            String username = "reflection-form-builder";
            String password = username;
            File myCnfFile = File.createTempFile(MySQLSequenceManagerIT.class.getSimpleName(), "mycnf");
            myCnfFile.delete();
            int databasePort = ReflectionFormBuilderITUtils.findFreePort(MySQLAutoPersistenceStorageConf.PORT_DEFAULT);
            LOGGER.info(String.format("using random next free port %d",
                    databasePort));
            MySQLAutoPersistenceStorageConf storageConf = new MySQLAutoPersistenceStorageConf(databaseDir.getAbsolutePath(),
                    mySQLDir.getAbsolutePath(),
                    "localhost",
                    databasePort,
                    entityClasses,
                    username,
                    password,
                    databaseName,
                    schemeChecksumFile);
            storageConf.setPassword(password);
            storageConf.setBaseDir(mySQLDir.getAbsolutePath());
            storageConf.setMyCnfFilePath(myCnfFile.getAbsolutePath());
            String persistenceUnitName = "reflection-form-builder-it";
            FieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
            IssueHandler issueHandler = new LoggerIssueHandler(LOGGER);
            storage = new MySQLAutoPersistenceStorage(storageConf,
                    persistenceUnitName,
                    10, //parallelQueryCount
                    issueHandler,
                    fieldRetriever);
            storage.start();
            MySQLSequenceManager instance = new MySQLSequenceManager(storage);
            instance.createSequence(sequenceName);
            long nextSequenceValue = instance.getNextSequenceValue(sequenceName);
            assertEquals(1L, nextSequenceValue);
        }finally {
            if(storage != null) {
                storage.shutdown();
            }
        }
    }
}
