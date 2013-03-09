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

package uk.org.whoami.authme.cache.auth;

import java.util.HashMap;

public class PlayerCache {

    private static PlayerCache singleton = null;
    private HashMap<String, PlayerAuth> cache;

    private PlayerCache() {
        cache = new HashMap<String, PlayerAuth>();
    }

    public void addPlayer(PlayerAuth auth) {
        cache.put(auth.getNickname(), auth);
    }

    public void updatePlayer(PlayerAuth auth) {
        cache.remove(auth.getNickname());
        cache.put(auth.getNickname(), auth);
    }

    public void removePlayer(String user) {
        cache.remove(user);
    }

    public boolean isAuthenticated(String user) {
        return cache.containsKey(user);
    }

    public PlayerAuth getAuth(String user) {
        return cache.get(user);
    }

    public static PlayerCache getInstance() {
        if (singleton == null) {
            singleton = new PlayerCache();
        }
        return singleton;
    }
}
