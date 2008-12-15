<?php
/*
 * Created on 04.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 class permission {
 	public $perm;
 	public $plugin;
 	public $instance;
 	public $value;
 	public $main_perm;
 	
 	public function __construct($perm,$value,$plugin = false, $instance = false) {
 		$this->perm = $perm;
 		$this->plugin = $plugin;
 		$this->instance = $instance;
 		$this->main_perm = $plugin === false ? true : false;
 	}
 }
 
?>
