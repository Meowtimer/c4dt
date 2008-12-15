<?php
/*
 * Created on 07.06.2007
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 $mysql_tables = array(
 	'items' => 
		'`id` INT( 14 ) NOT NULL AUTO_INCREMENT PRIMARY KEY ,
		`order` INT( 14 ) NOT NULL ,
		`picture` INT( 14 ) NOT NULL '
 );

 $global_tables = array(
	'config' =>
		"`id` MEDIUMINT NOT NULL AUTO_INCREMENT PRIMARY KEY ,
		`instance` VARCHAR( 255 ) NOT NULL ,
		`size_x` SMALLINT NOT NULL DEFAULT '300',
		`size_y` SMALLINT NOT NULL DEFAULT '300'"
 );
?>
