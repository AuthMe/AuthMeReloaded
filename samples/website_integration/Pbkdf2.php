<?php

/***********************************************************
 * AuthMe website integration logic for PBKDF2             *
 * ------------------------------------------------------- *
 * See AuthMeController for details.                       *
 *                                                         *
 * Source: https://github.com/AuthMe/AuthMeReloaded/       *
 ***********************************************************/
class Pbkdf2 extends AuthMeController {

    /** @var string[] range of characters for salt generation */
    private $CHARS;

    const SALT_LENGTH = 16;
    const NUMBER_OF_ITERATIONS = 10000;

    public function __construct() {
        $this->CHARS = self::initCharRange();
    }

    protected function isValidPassword($password, $hash) {
        // hash := pbkdf2_sha256$iterations$salt$hash
        $parts = explode('$', $hash);
        return count($parts) === 4 && $hash === $this->computeHash($parts[1], $parts[2], $password);
    }

    protected function hash($password) {
        $salt = $this->generateSalt();
		return $this->computeHash(self::NUMBER_OF_ITERATIONS, $salt, $password);
    }
	
	private function computeHash($iterations, $salt, $password) {
	    return 'pbkdf2_sha256$' . self::NUMBER_OF_ITERATIONS . '$' . $salt 
			. '$' . hash_pbkdf2('sha256', $password, $salt, self::NUMBER_OF_ITERATIONS, 64, false);
	}

    /**
     * @return string randomly generated salt
     */
    private function generateSalt() {
        $maxCharIndex = count($this->CHARS) - 1;
        $salt = '';
        for ($i = 0; $i < self::SALT_LENGTH; ++$i) {
            $salt .= $this->CHARS[mt_rand(0, $maxCharIndex)];
        }
        return $salt;
    }

    private static function initCharRange() {
        return array_merge(range('0', '9'), range('a', 'f'));
    }
}
