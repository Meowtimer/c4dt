<?php
/*
 * Created on 31.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 if (!defined("IN_ARCO")) die("suchst du was?");
 
 /**
  * Language system
  * @author ZokRadonh
  * @package ArcoVM
  * @version alpha 0.1
  */
 class language {
 	public $loaded_packs;
 	
 	public function __get($getname) {
 		// --------------------------------
		// Main language package
		// --------------------------------
 		if ($getname == "main") {
 			if (!is_array($this->loaded_packs["main"])) throw new ArcoException("Main language package is not loaded.");
 			return new language_sub($this->loaded_packs["main"]);
 		}
 		// --------------------------------
		// Plugin language packages
		// --------------------------------
 		if (!array_key_exists($getname,arco::$engine->plugins)) throw new ArcoException("Unknown plugin '$getname'.\nBest Regards\nYour lang system");
 		if (!array_key_exists($getname,$this->loaded_packs)) throw new ArcoException("Unknown language pack '$getname'. Is the pack already loaded?");
 		$sub = new language_sub($this->loaded_packs[$getname]);
 		$sub->main = $this;
 		$sub->pluginName = $getname;
 		return $sub;
 	}
 	
 	public function get_lang_for($object) {
 		return $this->{$object->getName()};
 	}
 	
 	public function load_lang($file,$name) {
 	 	$language = array();
 	 	if (!file_exists(LANG.(arco::$engine->currentLang)."/$file")) throw new ArcoException("Cannot load langfile '$file' with name '$name'. File does not exist.(".LANG.(arco::$engine->currentLang)."/$file".")");
 		include(LANG.(arco::$engine->currentLang)."/$file");
 		$this->loaded_packs[$name] = $language;
 	}
 	
 	/**
 	 * Loads the language data for the specified plugin
 	 * @param plugin plugin
 	 * @return void
 	 */
 	public function load_lang_for(plugin $plugin) { // yeah, i love those _for functions =)
 		$langfile = LANG.(arco::$engine->currentLang)."/lang_".$plugin->getName().".php";
 		if (!file_exists($langfile)) throw new FileNotFoundException($langfile);
 		$language = array();
 		include($langfile);
 		foreach($language as $langName => $langValue) {
 			if (array_key_exists($langName,arco::$engine->plugins)) throw new ArcoException("Illegal language item name '$langName' with value '$langValue' in plugin '".$plugin->getName()."'. A plugin with the same name exists.\nChose another name for your lang item to prevent conflicts.");
 		}
 		$this->loaded_packs[$plugin->getName()] = $language;
 	}
 	
 	 /**
 	 * Fetches all available languages
 	 * @return array of languages
 	 */
 	public function get_languages() {
 		$languages = array();
 		$dir = opendir(LANG);
 		while($file = readdir($dir)) {
 			if ($file == "." || $file == ".." || !is_dir($file)) continue;
 			$languages[] = $file;
 		}
 		return $languages;
 	}
 	
 	public function create_lang_item($language,$text) {
 		$res = arco::$db->insert("lang",array('language' => $language, 'text' => $text));
 		return $res->insertId;
 	}
 	
 	/**
 	 * @internal internal mYsql function
 	 */
 	public function edit_lang_item($id,$language,$text) {
 		if ($id == 0) return $this->create_lang_item($language,$text);
 		$r = arco::$db->select("lang","*","WHERE id = $id AND language = '$language'");
 		if ($r->num > 0) {
 			arco::$db->update("lang",array('text' => $text),"WHERE id = $id AND language = '$language'");
 		}
 		else {
 			arco::$db->insert("lang",array('id' => $id, 'language' => $language, 'text' => $text));
 		}
 		return $id;
 	}
 	
 	public function delete_lang_item($id) {
 		if ($id == 0) return false;
 		arco::$db->delete("lang","WHERE id = $id");
 	}

 	public function get_lang_item($id, $language) {
 		if ($id == 0) return null;
 		$r = arco::$db->select("lang","text","WHERE id = $id AND language = '$language'");
 		return $r->next["text"];
 	}
 	
 	public function filter($id) {
 		return $this->get_lang_item($id, arco::$engine->currentLang);
 	}
 	
 	/**
 	 * @deprecated 0.5 - 30.12.2006
 	 */
 	public function filter_old($data) {
// 		preg_match('/ \{ '.strtoupper(arco::$engine->currentLang).' \} (.+?) (?:(?: \{[A-Z]{2}\} ) | (?:$)) /sx',$data,$match);
// 		return $match[1];
		for($x = 0;$x < strlen($data);$x++) {
			$lang = substr($data,$x,2);
			$x += 2;
			$len = hexdec(substr($data,$x,4));
			$x += 4;
			if ($lang == arco::$engine->currentLang) {
				if ($len == 0) return "";
				return substr($data,$x,$len);
			}
			else $x += $len;
		} //en0013Hello this is fritzde000CDu bist doof
		return null;
 	}
 	
 	public function create_lang_string($text) {
 		arco::$engine->currentLang.str_pad(dechex(strlen($text)),4,"0",STR_PAD_LEFT).$text;
 	}
 	
 }
 
 class language_sub {
 	public $pluginData;
 	public $pluginName;
 	public $main;
 	
 	/**
 	 * Initiates a new language package instance
 	 * @param array pluginData ($language array)
 	 */
 	public function __construct($pluginData) {
 		$this->pluginData = $pluginData;
 	}
 	
 	/**
 	 * all language messages are nl2br'd and htmlentitie'd
 	 */
 	public function __get($getname) {
 		if (!array_key_exists($getname,$this->pluginData)) throw new ArcoException("Unknown message '$getname' in plugin '".$this->pluginName."'. (Language: ".arco::$engine->currentLang.")");
 		$text = $this->pluginData[$getname];
 		return nl2br(htmlentities($text,ENT_COMPAT,"UTF-8"));
// 		if (arco::$engine->charset != "ISO-8859-1") {
// 			$text = iconv(arco::$engine->charset,"UTF-8",$text);
// 			if ($text === FALSE) {
//	 			$text = $this->pluginData[$getname];
//	 			logger::log("Unable to convert from ".arco::$engine->charset." to UTF-8");
// 			}
// 			$unicodeText = nl2br(htmlentities($text,ENT_COMPAT,"UTF-8"));
// 			// --------------------------------
// 			// Revert to original charset
// 			// --------------------------------
// 			return iconv("UTF-8",arco::$engine->charset,$unicodeText);
// 		}
// 		return nl2br(htmlentities($text));
 	}
 	
 	/**
 	 * filter current language out of a language blob
 	 */
 	public function filter($data) {
 		return arco::$lang->filter($data);
 	}
 	
 }
 
?>
