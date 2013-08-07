#!/usr/bin/perl

use strict;
use warnings;
use DBI;

##############################
# EDIT THESE CONFIG SETTINGS #
##############################

my $host = "localhost";
my $database = "authme";
my $username = "authme";
my $password = "password";
my $auth_file = "/opt/craftbukkit/plugins/auths.db";

###############################
# DO NOT EDIT BELOW THIS LINE #
###############################

open FILE, "$auth_file" or die $!;
my $dbh = DBI->connect("DBI:mysql:$database;host=$host", "$username", "$password") or die "Could not connect to database: $DBI::errstr";

$dbh->do('CREATE TABLE `authme` (
         `id` INTEGER AUTO_INCREMENT,
         `username` VARCHAR(255) NOT NULL,
         `password` VARCHAR(255) NOT NULL,
         `ip` VARCHAR(40) NOT NULL,
         `lastlogin` BIGINT,
         CONSTRAINT `table_const_prim` PRIMARY KEY (`id`));');

my $st = 'INSERT INTO `authme` (`username`, `password`, `ip`, `lastlogin`) VALUES ';
my $i = 0;

while(<FILE>) {
    if($i == 1000) {
        $i = 0;
        $dbh->do($st);
        $st = 'INSERT INTO `authme` (`username`, `password`, `ip`, `lastlogin`) VALUES ';
    }
    my @auth = split(':');
    
    if($i != 0) {
        $st .= ", ";
    }
    
    $st .= "(\"$auth[0]\", \"$auth[1]\", ";
    $st .= "\"" . ($auth[2] || '198.18.0.1') . "\", ";
    $st .= ($auth[3] || '0') . ")";
    $i++;
}

if($i > 0) {
    $dbh->do($st);
}

$dbh->disconnect();
close FILE;

