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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.retriever.JPAOrderedCachedFieldRetriever;
import richtercloud.reflection.form.builder.jpa.storage.DerbyEmbeddedPersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.DerbyEmbeddedPersistenceStorageConf;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.validation.tools.FieldRetriever;

/**
 *
 * @author richter
 */
public class DerbySequenceManagerIT {

    /**
     * Test of createSequence method, of class DerbySequenceManager.
     */
    @Test
    public void testCreateSequence() throws Exception {
        PersistenceStorage<Long> storage = null;
        try {
            String sequenceName = "with-minus";
            Set<Class<?>> entityClasses = new HashSet<Class<?>>(Arrays.asList(EntityA.class));
            File databaseDir = Files.createTempDirectory(DerbySequenceManagerIT.class.getSimpleName()).toFile();
            FileUtils.forceDelete(databaseDir);
            File schemeChecksumFile = File.createTempFile(DerbySequenceManagerIT.class.getSimpleName(), "checksum");
            DerbyEmbeddedPersistenceStorageConf storageConf = new DerbyEmbeddedPersistenceStorageConf(entityClasses,
                    databaseDir.getAbsolutePath(), //databaseName
                    schemeChecksumFile);
            String persistenceUnitName = "reflection-form-builder-it";
            FieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
            storage = new DerbyEmbeddedPersistenceStorage(storageConf,
                    persistenceUnitName,
                    10, //parallelQueryCount
                    fieldRetriever);
            storage.start();
            DerbySequenceManager instance = new DerbySequenceManager(storage);
            instance.createSequence(sequenceName);
            long nextSequenceValue = instance.getNextSequenceValue(sequenceName);
            assertEquals(0L, nextSequenceValue);
        }finally {
            if(storage != null) {
                storage.shutdown();
            }
        }
    }
}
