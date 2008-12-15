<?php

function recognizeRu($chars) {
	$ok = 0;
	$rusbegin = false;
	for($x = 0;$x < strlen($chars);$x++) {
		if ($ok > 3) return true;
		if ($rusbegin) {
			if (ord($chars{$x}) > 0x80) {
				$ok++;
			}
			$rusbegin = false;
		}
		else if (ord($chars{$x}) == 0xD0 || ord($chars{$x}) == 0xD1) {
			$rusbegin = true;
		}
	}
	return false;
}

list($text,) = explode("---",$_SERVER['QUERY_STRING']);

header("Content-Type: image/png");

include("image.inc.php");

$im = new image;

$im->createtruecolor(136,30);
//$im->createfrompng("subnavi_fade2.png");

$im->alphablending(false);
$im->savealpha(true);

$im->fill(0,0,$im->colorallocatealpha(255,255,255,127));

$txt = new im_ttf_text(urldecode($text));

$txt->size = 14;
$txt->angle = 0;
$txt->startPoint = new im_point(0,0);
$txt->color = $im->colorallocate(0x66,0x66,0x66);
$txt->fontFile = "AGENCYREXT.ttf";

if (recognizeRu($txt->text)) {
	$txt->size = floor($txt->size * 0.8);
	$txt->fontFile = "tahoma.ttf";
	//print "tahoma!";
}

$txt->boxSize = new im_point(137,30);
$txt->align = IM_CENTER;
$txt->valign = IM_CENTER;

try {
	$im->execute($txt);
}
catch(Exception $e) {
	$txt->size = $txt->size / 0.8 * 0.7;
	try {
		$im->execute($txt);
	}
	catch(Exception $a) {
		header("Content-Type: text/plain");
		print "Exception: ";
		print $a->getMessage();
		exit;
	}
}

//$im->ttftext(18,0,10,34,$im->colorallocate(0x99,0x99,0x99),"AGENCYR.TTF","FireXTrainer");

$im->png();


?>