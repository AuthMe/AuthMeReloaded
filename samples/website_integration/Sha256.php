<?php

/***********************************************************
 * AuthMe website integration logic for SHA256             *
 * ------------------------------------------------------- *
 * See AuthMeController for details.                       *
 *                                                         *
 * Source: https://github.com/AuthMe/AuthMeReloaded/       *
 ***********************************************************/
class Sha256 extends AuthMeController {

    /** @var string[] range of characters for salt generation */
    private $CHARS;

    const SALT_LENGTH = 16;

    public function __construct() {
        $this->CHARS = self::initCharRange();
    }

    protected function isValidPassword($password, $hash) {
        // $SHA$salt$hash, where hash := sha256(sha256(password) . salt)
        $parts = explode('$', $hash);
        return count($parts) === 4 && $parts[3] === hash('sha256', hash('sha256', $password) . $parts[2]);
    }

    protected function hash($password) {
        $salt = $this->generateSalt();
        return '$SHA$' . $salt . '$' . hash('sha256', hash('sha256', $password) . $salt);
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
