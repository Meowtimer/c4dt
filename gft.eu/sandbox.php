<?php
/*
 * Created a long time ago
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */

define("IN_ARCO",TRUE);

include("kernel/engine.inc.php");
session_start();

set_magic_quotes_runtime(0);

try {
	// --------------------------------
	// Boot up Arco Virtual Machine
	// --------------------------------
	$engine = new engine();
	$engine->boot();

	// --------------------------------
	// Set environment
	// --------------------------------
	if (!$engine->input['page']) $engine->currentPage = $engine->config['default_page'];
	else $engine->currentPage = $engine->input['page'];
//	$engine->currentSite = "http://".$_SERVER['HTTP_HOST'].$_SERVER['SCRIPT_NAME']."?page=".$engine->currentPage;
	$engine->currentSite = INDEXFILE."?page=".$engine->currentPage;
	
	$user->sessionVars->lang = arco::$engine->currentLang;
	$user->sessionVars->skin = arco::$engine->currentSkin;
	
	if ($engine->input['do'] == "export") { 
		$langArray = array();
		$idArray = array();
		
		$qry = $db->select("lang","*","WHERE language = 'en' ORDER BY id");
		while($qry->next) {
			print "-----------------------------------------------------\r\n";
			print "ID: ".$qry->id.":\r\n";
			if (($key = array_search($qry->text,$langArray)) !== FALSE) {
				print "->ID[".$idArray[$key]."]\r\n";
			}
			else {
				$langArray[] = $qry->text;
				$idArray[] = $qry->id;
				print $qry->text;
				if ($qry->text{strlen($qry->text)-1} != "\n") print "\r\n";
			}
			print "-----------------------------------------------------\r\n";
		}
	}
	elseif ($engine->input['do'] == "import") {
		$newLang = $engine->input['newlang'];
		$data = array();
		$file = file_get_contents("translate.".$newLang.".txt");
		if (!strlen($file)) die("File not found.");
		$segments = explode("-----------------------------------------------------",$file);
		print "segements-count: ".count($segments)."<br>\n";
		foreach($segments as $segment) {
			if (!strlen(trim($segment))) continue;
			$segment = trim($segment);
//			print $segment;
			if (!preg_match('/^ID: (?P<id>\d+):\r?\n(?P<text>[^~]+)/',$segment,$match)) {
				continue;
			}
			else {
				if (preg_match('/^->ID\[(?P<refid>\d+)\]$/',$match['text'],$refmatch)) {
					print "$match[id] is a reference to $refmatch[refid]<br>";
					$match['text'] = $data[$refmatch['refid']];
				}
//				print_r($match);
				$data[$match['id']] = $match['text'];
				$db->insert("lang",array("id" => $match['id'],"text" => $db->make_safe($match['text']),"language" => $newLang));
			}
		}
		
//		print_r($data);
	}
	elseif ($engine->input['do'] == "copyentoru") {
		$ids = array(104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 125, 128, 129, 131);
		foreach($ids as $id) {
			$en = $db->select("lang","text","WHERE id = $id AND language = 'en'")->next['text'];
			if (!strlen($en)) die("invalid query");
			header("Content-Type: text/plain");
			print $id.":::::::";
			print $en;
			print "\n\n\n";
			$db->update("lang",array('text' => $en),"WHERE id = $id AND language = 'ru'");
		}
		
	}
	
//	print_r($idArray);
	
	// --------------------------------
	// Initiate page
	// --------------------------------
	//$page = $view->get_page(arco::$engine->currentPage);
	
	// --------------------------------
	// Build and show page
	// --------------------------------
	//print $page->build_page();
	arco::$user->sessionVars->lastPage = $engine->currentRequest;
	
	// --------------------------------
	// Thats it, isnt it great?
	// --------------------------------
}
catch(ArcoException $e) {
	print "<center>";
	print "<b>Arco Exception</b><br />".$e->GetMessage();
	print "<br />";
	print "<b>Trace:</b>:<br />";
	print nl2br($e->GetTraceAsString());
	print "</center>";	
}
 
?>