<?php
/*
 * Created a long time ago
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */

//die("Es werden gerade Wartungsarbeiten durchgef�hrt. Homepage wird in sp�testens zwei Stunden(14 Uhr) wieder erreichbar sein.");

define("IN_ARCO",TRUE);

define("LOCAL",TRUE);

//if (!function_exists("date_default_timezone_set")) die("PHP-Version too low. PHP >= 5.1 is required. You have ".phpversion().".");

include("kernel/engine.inc.php");
if (!$_GET['resource']) session_start();

set_magic_quotes_runtime(0);

try {
	// --------------------------------
	// Boot up Arco Virtual Machine
	// --------------------------------
	$stime = microtime(true);
	$engine = new engine();
	$engine->boot();

	// --------------------------------
	// Set environment
	// --------------------------------
	$user->sessionVars->lang = arco::$engine->currentLang;
	$user->sessionVars->skin = arco::$engine->currentSkin;
	
	// --------------------------------
	// Font Request Handler
	// --------------------------------
	if (arco::$engine->input['write']) {
		session_write_close();
		
		$font = SKINS.arco::$engine->currentSkin."/AGENCYREXT.ttf";
		$size = arco::$engine->input['size'];
		$text = (arco::$engine->input['write']);
		$color = arco::$engine->input['color'];
		if (arco::$engine->currentLang == "ru") {
			$font = SKINS.arco::$engine->currentSkin."/tahoma.ttf";
			$size = floor(arco::$engine->input['size'] * 0.8);
		}
		$optionHash = md5($font.$text.$size.$color);
		$mtime = microtime(true);
		$mspan = $mtime - $stime;
		$r = arco::$db->select('resources','cache',"WHERE cache_hash = '$optionHash'");
		if ($r->num) {
			if ($_SERVER['HTTP_IF_MODIFIED_SINCE'] && strtotime($_SERVER['HTTP_IF_MODIFIED_SINCE']) + 100000 > $r->last_modified) {
				header("HTTP/1.1 304 Not Modified");
				header("Cache-Control: ");
				header("Pragma: ");
				header("Expires: ");
				exit;
			}
			$etime = microtime(true);
			$espan = $etime - $stime;
			$data = $r->next;
			header("Content-Type: image/png");
			header("Cache-Control: ");
			header("Last-Modified: ".date("r",$r->last_modified));
			header("Pragma: ");
			header("Expires: ");
			header("Content-Length: ".strlen($data['cache']));
			echo $data['cache'];

			//logger::log("pic by cache in [$mspan:$espan]");
			exit;
		}
		
		$sizes = imagettfbbox($size, 0, $font,$text);
//		print nl2br(print_r($sizes,true));
		$width = $sizes[2];
		$height = - $sizes[5];
		$oversize = $sizes[1];
		//print $width ."_". $height;
		$im = image::CreateTrueColor($width + 5,$height + $oversize + 1);
		$im->alphablending(false);
		$im->savealpha(true);
		
		if ($color) {
			$rgb = array_map("hexdec",str_split($color,2));
		}
		else $rgb = array(166,18,3);
		
		$im->fill(0, 0, $im->colorallocatealpha(0,0,0,127));
		$im->ttftext($size, 0, 0, $height, $im->colorallocate($rgb[0],$rgb[1],$rgb[2]), $font, $text );
		header("Content-Type: image/png");
		header("Cache-Control: ");
		header("Pragma: ");
		header("Last-Modified: ".date("r"));
		header("Expires: ");
		arco::$db->insert('resources',array('mime' => 'image/png', 'cache' => arco::$db->make_safe($im->GetBlob("png")), 'cache_hash' => $optionHash, 'last_modified' => time()));
		$im->png(null,9);
		exit;
	}
	
	// --------------------------------
	// Resource Request Handler
	// --------------------------------
	if (arco::$engine->input['resource']) {
		session_write_close();
		// --------------------------------
		// Initialize resource handler
		// --------------------------------
		$resource = resource::load(arco::$engine->input['resource']);
		
		if ($_SERVER['HTTP_IF_MODIFIED_SINCE'] && strtotime($_SERVER['HTTP_IF_MODIFIED_SINCE']) + 100000 > $resource->cacheTime) {
			header("HTTP/1.1 304 Not Modified");
			header("Cache-Control: ");
			header("Pragma: ");
			header("Expires: ");
			exit;
		}
		
		// --------------------------------
		// Send header
		// --------------------------------
		header("Content-Type: $resource->mime");
		header("Cache-Control: ");
		header("Last-Modified: ".date("r",$resource->cacheTime));
		header("Pragma: ");
		header("Expires: ");
		
		// --------------------------------
		// Send content
		// --------------------------------
		if (strlen($resource->cachedBlob) > 0) {
			header("Content-Length: ".strlen($resource->cachedBlob));
			echo $resource->cachedBlob;
		}
		else {
			header("Content-Length: ".strlen($resource->blob));
			echo $resource->blob;
		}
		
		logger::log("Generated resource.");
		exit;
	}

	// --------------------------------
	// Page Request Handler
	// --------------------------------	
	if (!$engine->input['page']) $engine->currentPage = $engine->config['default_page'];
	else $engine->currentPage = $engine->input['page'];
	$engine->currentSite = "?page=".$engine->currentPage;
	
	// --------------------------------
	// Initiate page
	// --------------------------------	
	$page = $view->get_page(arco::$engine->currentPage);
	if ($page->locked) throw new ViewException("Page '".arco::$engine->currentPage."' is not published yet.");
	// --------------------------------
	// Build and show page
	// --------------------------------
	$html = $page->build_page();
	if ($engine->config["use_former"]) $html = $libs->classes["former"]->formXHTML($html);
	print $html;
	if (!$engine->was_error() && $engine->last != $engine->currentRequest) arco::$user->sessionVars->lastPage = $engine->currentRequest;
	// --------------------------------
	// Thats it, isnt it great?
	// --------------------------------
}
catch(ArcoException $e) {
	print "<center>";
	print "<b>Arco Exception</b><br />".$e->GetMessage();
	print "<br />";
	print "<b>Trace:</b><br />";
	print nl2br($e->GetTraceAsString());
	print "</center>";	
}
 
?>