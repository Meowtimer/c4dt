<?php
/*
 * Created on 30.12.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 $mysql_tables = array(
 	'data' => 
 		" `id` INT( 14 ) NOT NULL AUTO_INCREMENT PRIMARY KEY ," .
 		"`data_id` VARCHAR( 255 ) NOT NULL ," .
 		"`data_content` BIGINT ( 20 ) NOT NULL ," .
 		"`tpl` LONGTEXT NOT NULL"
 	);

 $overlay_tables = array(
 	'data' =>
 		array(
 			'id' => ARCOSQL_NUMBER,
 			'data_id' => ARCOSQL_TEXT,
 			'data_content' => ARCOSQL_LANGTEXT,
 			'tpl' => ARCOSQL_HTML));
 


?>
