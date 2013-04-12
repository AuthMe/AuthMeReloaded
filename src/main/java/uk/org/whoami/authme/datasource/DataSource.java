/*
 * Copyright 2011 Sebastian Köhler <sebkoehler@whoami.org.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.whoami.authme.datasource;

import java.util.List;

import uk.org.whoami.authme.cache.auth.PlayerAuth;

public interface DataSource {

    public enum DataSourceType {

        MYSQL, FILE, SQLITE
    }

    boolean isAuthAvailable(String user);

    PlayerAuth getAuth(String user);

    boolean saveAuth(PlayerAuth auth);

    boolean updateSession(PlayerAuth auth);

    boolean updatePassword(PlayerAuth auth);

    int purgeDatabase(long until);

    boolean removeAuth(String user);

    boolean updateQuitLoc(PlayerAuth auth);

    int getIps(String ip);

    List<String> getAllAuthsByName(PlayerAuth auth);

    List<String> getAllAuthsByIp(String ip);

    List<String> getAllAuthsByEmail(String email);

    boolean updateEmail(PlayerAuth auth);

    boolean updateSalt(PlayerAuth auth);

    void close();

    void reload();

    void purgeBanned(List<String> banned);

}
