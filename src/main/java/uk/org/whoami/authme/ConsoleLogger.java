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

package uk.org.whoami.authme;

import java.util.logging.Logger;

public class ConsoleLogger {

    private static final Logger log = Logger.getLogger("Minecraft");

    public static void info(String message) {
        log.info("[AuthMe] " + message);
    }

    public static void showError(String message) {
        log.severe("[AuthMe] ERROR: " + message);
    }
}
