<!--
  This is a demo page for AuthMe website integration.
  See AuthMeController.php and the extending classes for the PHP code you need.
-->
<!DOCTYPE html>
<html lang="en">
 <head>
   <title>AuthMe Integration Sample</title>
   <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 </head>
 <body>
<?php
error_reporting(E_ALL);

require 'AuthMeController.php';

// Change this to the file of the hash encryption you need, e.g. Bcrypt.php or Sha256.php
require 'Sha256.php';
// The class name must correspond to the file you have in require above! e.g. require 'Sha256.php'; and new Sha256();
$authme_controller = new Sha256();

$action = get_from_post_or_empty('action');
$user = get_from_post_or_empty('username');
$pass = get_from_post_or_empty('password');
$email = get_from_post_or_empty('email');

$was_successful = false;
if ($action && $user && $pass) {
    if ($action === 'Log in') {
        $was_successful = process_login($user, $pass, $authme_controller);
    } else if ($action === 'Register') {
        $was_successful = process_register($user, $pass, $email, $authme_controller);
    }
}

if (!$was_successful) {
    echo '<h1>Login sample</h1>
This is a demo form for AuthMe website integration. Enter your AuthMe login details
into the following form to test it.
<form method="post">
 <table>
   <tr><td>Name</td><td><input type="text" value="' . htmlspecialchars($user) . '" name="username" /></td></tr>
   <tr><td>Email</td><td><input type="text" value="' . htmlspecialchars($email) . '" name="email" /></td></tr>
   <tr><td>Pass</td><td><input type="password" value="' . htmlspecialchars($pass) . '" name="password" /></td></tr>
   <tr>
     <td><input type="submit" name="action" value="Log in" /></td>
     <td><input type="submit" name="action" value="Register" /></td>
   </tr>
 </table>
</form>';
}

function get_from_post_or_empty($index_name) {
    return trim(
        filter_input(INPUT_POST, $index_name, FILTER_UNSAFE_RAW, FILTER_REQUIRE_SCALAR | FILTER_FLAG_STRIP_LOW)
            ?: '');
}


// Login logic
function process_login($user, $pass, AuthMeController $controller) {
    if ($controller->checkPassword($user, $pass)) {
        printf('<h1>Hello, %s!</h1>', htmlspecialchars($user));
        echo 'Successful login. Nice to have you back!'
            . '<br /><a href="index.php">Back to form</a>';
        return true;
    } else {
        echo '<h1>Error</h1> Invalid username or password.';
    }
    return false;
}

// Register logic
function process_register($user, $pass, $email, AuthMeController $controller) {
    if ($controller->isUserRegistered($user)) {
        echo '<h1>Error</h1> This user already exists.';
    } else if (!is_email_valid($email)) {
        echo '<h1>Error</h1> The supplied email is invalid.';
    } else {
        // Note that we don't validate the password or username at all in this demo...
        $register_success = $controller->register($user, $pass, $email);
        if ($register_success) {
            printf('<h1>Welcome, %s!</h1>Thanks for registering', htmlspecialchars($user));
            echo '<br /><a href="index.php">Back to form</a>';
            return true;
        } else {
            echo '<h1>Error</h1>Unfortunately, there was an error during the registration.';
        }
    }
    return false;
}

function is_email_valid($email) {
    return trim($email) === ''
        ? true // accept no email
        : filter_var($email, FILTER_VALIDATE_EMAIL);
}

?>

 </body>
</html>
