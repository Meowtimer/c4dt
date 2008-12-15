<?php
/*
 * Created on 07.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
  $mysql_tables = array(
		'cats' => 
			  "`id` INT( 15 ) NOT NULL AUTO_INCREMENT ,"
			. "`name` VARCHAR( 200 ) NOT NULL ,"
			. "`description` LONGTEXT NOT NULL ,"
			. "`icon` VARCHAR( 255 ) NOT NULL ,"
			. "`parent_id` INT( 5 ) DEFAULT '0' NOT NULL ,"
			. "PRIMARY KEY ( `id` )",
  		'contents' => 
			  "`content_id` INT( 15 ) NOT NULL AUTO_INCREMENT ,"
			. "`name` VARCHAR( 200 ) NOT NULL ,"
			. "`content` LONGTEXT NOT NULL ,"
			. "`icon` VARCHAR( 255 ) NOT NULL ,"
			. "`parent_id` INT( 5 ) DEFAULT '0' NOT NULL ,"
			. "PRIMARY KEY ( `content_id` )"
		);
		
$overlay_tables = array(
	'cats' => array(
		'id' => ARCOSQL_NUMBER,
		'name' => ARCOSQL_TEXT,
		'description' => ARCOSQL_TEXT,
		'icon' => ARCOSQL_URL,
		'parent_id' => ARCOSQL_NUMBER),
	'contents' => array(
		'content_id' => ARCOSQL_NUMBER,
		'name' => ARCOSQL_TEXT,
		'content' => ARCOSQL_TEXT,
		'icon' => ARCOSQL_URL,
		'parent_id' => ARCOSQL_NUMBER
	));
?>
