<?php
/*****************************************************************************
 * AuthMe website integration logic for SHA256                               *
 * --------------------------------                                          *
 * Check with authme_check_password() whether the received username and      *
 * password match the AuthMe MySQL database. Don't forget to adjust the      *
 * database info in authme_get_hash().                                       *
 *                                                                           *
 * Source: https://github.com/AuthMe-Team/AuthMeReloaded/                    *
 *****************************************************************************/

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
            return authme_check_hash($password, $hash);
        }
    }
    return false;
}

/**
 * Retrieves the hash associated with the given user from the database.
 *
 * @param string $username the username whose hash should be retrieved
 * @return string|null the hash, or null if unavailable (e.g. username doesn't exist)
 */
function authme_get_hash($username) {
    // Add here your database host, username, password and database name
    $mysqli = new mysqli('HOST', 'USER', 'PWD', 'DB');
    $authme_table = 'authme';

    if (mysqli_connect_error()) {
        printf('Could not connect to AuthMe database. Errno: %d, error: "%s"',
            mysqli_connect_errno(), mysqli_connect_error());
    } else {
        $stmt = $mysqli->prepare("SELECT password FROM $authme_table WHERE username = ?");
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
 * Checks the given clear-text password against the hash.
 *
 * @param string $password the clear-text password to check
 * @param string $hash the hash to check the password against
 * @return bool true iff the password matches the hash, false otherwise
 */
function authme_check_hash($password, $hash) {
    // $SHA$salt$hash, where hash := sha256(sha256(password) . salt)
    $parts = explode('$', $hash);
    return count($parts) === 4
        && $parts[3] === hash('sha256', hash('sha256', $password) . $parts[2]);
}
