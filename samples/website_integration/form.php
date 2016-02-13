<!--
  This is a demo page for AuthMe website integration.
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

$user = get_from_post_or_empty('username');
$pass = get_from_post_or_empty('password');

$was_successful = false;
if ($user && $pass) {
	require_once('integration.php');
	if (authme_check_password($user, $pass)) {
		printf('<h1>Hello, %s!</h1>', htmlspecialchars($user));
		echo 'Successful login. Nice to have you back!'
			. '<br /><a href="form.php">Back to form</a>';
		$was_successful = true;
	} else {
		echo '<h1>Error</h1> Invalid username or password.';
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
   <tr><td colspan="2"><input type="submit" value=" Log in " />
 </table>
</form>';
}

function get_from_post_or_empty($index_name) {
	return trim(
		filter_input(INPUT_POST, $index_name, FILTER_UNSAFE_RAW, FILTER_REQUIRE_SCALAR | FILTER_FLAG_STRIP_LOW)
		    ?: '');
}
?>

 </body>
</html>
