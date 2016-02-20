<?php
/*****************************************************************************
 * AuthMe website integration logic for BCrypt                               *
 * --------------------------------                                          *
 * Check with authme_check_password() whether the received username and      *
 * password match the AuthMe MySQL database. Don't forget to adjust the      *
 * database info in authme_get_hash().                                       *
 *                                                                           *
 * Source: https://github.com/AuthMe-Team/AuthMeReloaded/                    *
 *****************************************************************************/

/** The name of the authme MySQL table. */
define('AUTHME_TABLE', 'authme');


/**
 * Entry point function to check supplied credentials against the AuthMe database.
 *
 * @param string $username the username
 * @param string $password the password
 * @return bool true iff the data is correct, false otherwise
 */
function authme_check_password($username, $password) {
    if (is_scalar($username) && is_scalar($password)) {
        $hash = authme_get_hash($username);
        if ($hash) {
            return password_verify($password, $hash);
        }
    }
    return false;
}

/**
 * Returns a connection to the database.
 *
 * @return mysqli|null the mysqli object or null upon error
 */
function authme_get_mysqli() {
    $mysqli = new mysqli('localhost', 'root', '', 'authme');
    if (mysqli_connect_error()) {
        printf('Could not connect to AuthMe database. Errno: %d, error: "%s"',
            mysqli_connect_errno(), mysqli_connect_error());
        return null;
    }
    return $mysqli;
}

/**
 * Retrieves the hash associated with the given user from the database.
 *
 * @param string $username the username whose hash should be retrieved
 * @return string|null the hash, or null if unavailable (e.g. username doesn't exist)
 */
function authme_get_hash($username) {
    // Add here your database host, username, password and database name
    $mysqli = authme_get_mysqli();
    if ($mysqli !== null) {
        $stmt = $mysqli->prepare('SELECT password FROM ' . AUTHME_TABLE . ' WHERE username = ?');
        $stmt->bind_param('s', $username);
        $stmt->execute();
        $stmt->bind_result($password);
        if ($stmt->fetch()) {
            return $password;
        }
    }
    return null;
}

/**
 * Returns whether the user exists in the database or not.
 *
 * @param string $username the username to check
 * @return bool true if the user exists; false otherwise
 */
function authme_has_user($username) {
    $mysqli = authme_get_mysqli();
    if ($mysqli !== null) {
        $stmt = $mysqli->prepare('SELECT 1 FROM ' . AUTHME_TABLE . ' WHERE username = ?');
        $stmt->bind_param('s', $username);
        $stmt->execute();
        return $stmt->fetch();
    }

    // Defensive default to true; we actually don't know
    return true;
}

/**
 * Registers a player with the given username.
 *
 * @param string $username the username to register
 * @param string $password the password to associate to the user
 * @return bool whether or not the registration was successful
 */
function authme_register($username, $password) {
    $mysqli = authme_get_mysqli();
    if ($mysqli !== null) {
        $hash = password_hash($password, PASSWORD_BCRYPT);
        $stmt = $mysqli->prepare('INSERT INTO ' . AUTHME_TABLE . ' (username, realname, password, ip) '
            . 'VALUES (?, ?, ?, ?)');
        $username_low = strtolower($username);
        $stmt->bind_param('ssss', $username, $username_low, $hash, $_SERVER['REMOTE_ADDR']);
        return $stmt->execute();
    }
    return false;
}

