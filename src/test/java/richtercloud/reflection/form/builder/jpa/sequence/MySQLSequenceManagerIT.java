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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.LoggerMessageHandler;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.jpa.JPACachedFieldRetriever;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.storage.MySQLAutoPersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.MySQLAutoPersistenceStorageConf;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 *
 * @author richter
 */
public class MySQLSequenceManagerIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(MySQLSequenceManagerIT.class);

    /**
     * Test of createSequence method, of class MySQLSequenceManager.
     */
    @Test
    public void testCreateSequence() throws Exception {
        //assert that $HOME/mysql-5.7.17 is present and request user to download
        //if not (there might be a more elegant way to do this)
        String homeDirPath = System.getProperty("user.home");
        assert homeDirPath != null;
        File homeDir = new File(homeDirPath);
        assert homeDir.exists();
        File mySQLDir = new File(homeDir, "mysql-5.7.17");
        if(!mySQLDir.exists()) {
            throw new IllegalArgumentException(String.format("There's no MySQL installation at '%s'. Download and extract manually and restart. Can't proceed.", mySQLDir.getAbsolutePath()));
        }else if(!mySQLDir.isDirectory()){
            throw new IllegalArgumentException(String.format("MySQL directory '%s' exists, but is not a directory", mySQLDir.getAbsolutePath()));
        }

        String sequenceName = "with-minus";
        Set<Class<?>> entityClasses = new HashSet<Class<?>>(Arrays.asList(EntityA.class));
        File databaseDir = File.createTempFile(MySQLSequenceManagerIT.class.getSimpleName(), "database");
        FileUtils.forceDelete(databaseDir);
        File schemeChecksumFile = File.createTempFile(MySQLSequenceManagerIT.class.getSimpleName(), "checksum");
        String username = "reflection-form-builder";
        String password = username;
        File myCnfFile = File.createTempFile(MySQLSequenceManagerIT.class.getSimpleName(), "mycnf");
        myCnfFile.delete();
        MySQLAutoPersistenceStorageConf storageConf = new MySQLAutoPersistenceStorageConf(entityClasses,
                username,
                databaseDir.getAbsolutePath(),
                schemeChecksumFile);
        storageConf.setPassword(password);
        storageConf.setBaseDir(mySQLDir.getAbsolutePath());
        storageConf.setMyCnfFilePath(myCnfFile.getAbsolutePath());
        String persistenceUnitName = "reflection-form-builder-it";
        FieldRetriever fieldRetriever = new JPACachedFieldRetriever();
        MessageHandler messageHandler = new LoggerMessageHandler(LOGGER);
        PersistenceStorage<Long> storage = new MySQLAutoPersistenceStorage(storageConf,
                persistenceUnitName,
                10, //parallelQueryCount
                messageHandler,
                fieldRetriever);
        storage.start();
        MySQLSequenceManager instance = new MySQLSequenceManager(storage);
        try {
            instance.createSequence(sequenceName);
        }finally {
            storage.shutdown();
        }
    }
}
