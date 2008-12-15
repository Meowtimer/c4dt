<?php
/*
 * Created on 08.09.2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - PHPeclipse - PHP - Code Templates
 */
 
 $mysql_tables = array(
	'items' => 
		"`id` INT( 14 ) NOT NULL AUTO_INCREMENT PRIMARY KEY ,
		`text` BIGINT NOT NULL ,
		`short_name` VARCHAR( 100 ) NOT NULL ,
		`link` VARCHAR( 200 ) NOT NULL ,
		`order` INT( 5 ) NOT NULL ,
		`displayinlang` VARCHAR( 250 ) NOT NULL ,
		`template` LONGTEXT NOT NULL ,
		`parent` INT( 5 ) NOT NULL DEFAULT '0'"
	);
		
 $global_tables = array(
 	'config' => 
	 	"`instance` VARCHAR( 200 ) NOT NULL ," .
	 	"`template` LONGTEXT NOT NULL ," .
	 	"PRIMARY KEY ( `instance` )"
 		
 	);
 		
 $overlay_tables = array(
	'items' => array(
		'id' => ARCOSQL_NUMBER,
		'text' => ARCOSQL_LANG,
		'short_name' => ARCOSQL_TEXT,
		'link' => ARCOSQL_URL,
		'order' => ARCOSQL_NUMBER,
 		'displayinlang' => ARCOSQL_TEXT,
		'template' => ARCOSQL_HTML,
		'parent' => ARCOSQL_NUMBER),
	'config' => array(
		'instance' => ARCOSQL_TEXT,
		'template' => ARCOSQL_HTML
	));
 
?>
