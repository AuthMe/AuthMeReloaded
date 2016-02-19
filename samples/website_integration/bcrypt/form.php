<!--
  This is a demo page for AuthMe website integration with BCrypt.
  See integration.php for the PHP code you need.
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

$action = get_from_post_or_empty('action');
$user = get_from_post_or_empty('username');
$pass = get_from_post_or_empty('password');

$was_successful = false;
if ($action && $user && $pass) {
    require_once('integration.php');
    if ($action === 'Log in') {
        $was_successful = process_login($user, $pass);
    } else if ($action === 'Register') {
        $was_successful = process_register($user, $pass);
    }
}

if (!$was_successful) {
    echo '<h1>Login sample</h1>
This is a demo form for AuthMe website integration. Enter your AuthMe login details
into the following form to test it.
<form method="post">
 <table>
   <tr><td>Name</td><td><input type="text" value="' . htmlspecialchars($user) . '" name="username" /></td></tr>
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
function process_login($user, $pass) {
    if (authme_check_password($user, $pass)) {
        printf('<h1>Hello, %s!</h1>', htmlspecialchars($user));
        echo 'Successful login. Nice to have you back!'
            . '<br /><a href="form.php">Back to form</a>';
        return true;
    } else {
        echo '<h1>Error</h1> Invalid username or password.';
    }
    return false;
}

// Register logic
function process_register($user, $pass) {
    if (authme_has_user($user)) {
        echo '<h1>Error</h1> This user already exists.';
    } else {
        // Note that we don't validate the password or username at all in this demo...
        $register_success = authme_register($user, $pass);
        if ($register_success) {
            printf('<h1>Welcome, %s!</h1>Thanks for registering', htmlspecialchars($user));
            echo '<br /><a href="form.php">Back to form</a>';
            return true;
        } else {
            echo '<h1>Error</h1>Unfortunately, there was an error during the registration.';
        }
    }
    return false;
}

?>

 </body>
</html>
