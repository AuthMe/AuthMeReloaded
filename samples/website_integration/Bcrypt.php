<?php

/***********************************************************
 * AuthMe website integration logic for BCrypt             *
 * ------------------------------------------------------- *
 * See AuthMeController for details.                       *
 *                                                         *
 * Source: https://github.com/AuthMe/AuthMeReloaded/       *
 ***********************************************************/
class Bcrypt extends AuthMeController {

    protected function hash($password) {
        return password_hash($password, PASSWORD_BCRYPT);
    }

    protected function isValidPassword($password, $hash) {
        return password_verify($password, $hash);
    }

}
