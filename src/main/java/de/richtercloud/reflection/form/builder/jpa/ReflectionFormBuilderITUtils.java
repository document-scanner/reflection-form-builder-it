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
package de.richtercloud.reflection.form.builder.jpa;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author richter
 */
public final class ReflectionFormBuilderITUtils {
    private final static int PORT_MAX = 65535;

    public static int findFreePort(int startingFrom) throws IOException {
        int retValue = findFreePort("localhost",
                startingFrom);
        return retValue;
    }

    /*
    internal implementation notes:
    - providing a lock here doesn't make sense because it would be locked at any
    invocation of findFreePort and then the caller would be responsible for
    releasing it - it's more error proof and intuitive to leave all locks on the
    caller's side
    */
    /**
     * This is method is not thread-safe because it doesn't make sense to find
     * the next free port in a concurrent environment until the service for
     * which the port is found has been started. Therefore callers need to work
     * with locks in order to achieve that.
     *
     * @param host find for host
     * @param startingFrom the port to start from
     * @return the found free port
     * @throws IllegalStateException if no free port between
     *     {@code startingFrom} and {@link #PORT_MAX} could be found
     * @throws IOException if an unexpected I/O exception occurs
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static int findFreePort(String host,
            int startingFrom) throws IOException {
        synchronized (ReflectionFormBuilderITUtils.class) {
            for (int i = startingFrom; i < PORT_MAX; i++) {
                try {
                    Socket testSocket = new Socket(host,
                            i);
                    testSocket.close();
                } catch (IOException ex) {
                    //expected if port is free
                    return i;
                }
            }
            throw new IllegalStateException(String.format("no free port between %d and %d is avaiable",
                    startingFrom,
                    PORT_MAX));
        }
    }

    private ReflectionFormBuilderITUtils() {
    }
}
