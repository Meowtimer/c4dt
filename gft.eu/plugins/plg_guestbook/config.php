<?php
/*
 * Created on 30.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 $mysql_tables = array('posts' => 
					  ' `id` INT(5) NOT NULL AUTO_INCREMENT, '
			        . ' `author` VARCHAR(200) NOT NULL, '
			        . ' `email` VARCHAR(255) NOT NULL, '
			        . ' `text` LONGTEXT NOT NULL, '
			        . ' `time` VARCHAR(20) NOT NULL, '
			        . ' `ip` VARCHAR(15) NOT NULL,'
			        . ' PRIMARY KEY (`id`)'
 					);
 
 /*
  * $sql = 'CREATE TABLE `adce_guestbook_posts_0` ('
        . ' `id` INT(5) NOT NULL AUTO_INCREMENT, '
        . ' `author` VARCHAR(200) NOT NULL, '
        . ' `email` VARCHAR(255) NOT NULL, '
        . ' `text` LONGTEXT NOT NULL, '
        . ' `time` VARCHAR(20) NOT NULL, '
        . ' `ip` VARCHAR(15) NOT NULL,'
        . ' PRIMARY KEY (`id`)'
        . ' )'
        . ' TYPE = myisam';
  */
 
?>
