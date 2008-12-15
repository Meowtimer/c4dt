<?php

/*
 * Created on 30.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */

class func extends library {

	public static function GetMethods() {
		return array (
			"str_enclose",
			"convert_to_html",
			"make_safe",
			"to_direct_url",
			"to_html_url",
			"call_user_func_array_named",
			"detect_mime",
			"prepare_for_textarea",
			"old_ie_png"
		);
	}
	
	public static function GetClasses() {
		return array ();
	}

	public static function GetSuperObjects() {
		return array ();
	}

	public function to_direct_url($url) {
		$search = array (
			'&amp;'
		);
		$replace = array (
			'&'
		);
		return str_replace($search, $replace, $url);
	}

	public function to_html_url($url) {
		if (strstr($url, "&amp;"))
			return $url;
		$search = array (
			'&'
		);
		$replace = array (
			'&amp;'
		);
		return str_replace($search, $replace, $url);
	}

	public function make_safe($string) {
		if (get_magic_quotes_gpc()) {
			return $string;
		} else
			return addslashes($string);
	}

	public function str_enclose($string, $single = false) {
		$quote = $single ? "'" : '"';
		return $quote . $string . $quote;
	}

	public function convert_to_html($string) {
		if (is_array($string)) {
			foreach ($string as $key => $value) {
				$string[$key] = nl2br(htmlspecialchars($value));
			}
		} else
			$string = nl2br(htmlspecialchars($string));
		return $string;
	}
	
	/**
	 * You dont know Jack 2
	 */
	public function prepare_for_textarea($string) {
		return str_replace(array("{","}"),array('&lebre;', '&ribre;'), htmlspecialchars($string)); // da schummelt einer
	}

	public function old_ie_png($string) {
		$string = preg_replace_callback('#<img(.+?)/>#',array($this,"old_ie_png_callback"),$string);
		return $string;
	}
	
	/**
	 * @access private but public due to callback
	 * @internal regex callback function
	 */
	public function old_ie_png_callback($match) {
		$span = "<span ";
		$attributes = $match[1];
		$src = preg_replace('/src="(?write=[^"])"/','$1',$attributes);
		$style = preg_replace('/style="([^"]+)"/','$1',$attributes);
//		$alt = preg_replace('/(alt="[^"]+")/','$1',$attributes);
//		$title = preg_replace('/(title="[^"]+")/','$1',$attributes);
		$span .= 'style="';
		$span .= 'filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src=\''.$src.'\', sizingMethod=\'image\',Enabled=true);';
		$span .= $style.'"';
		$span .= "/>";
		return $span;
	}
	

	public function call_user_func_array_named($function, $params) {
		if (is_array($function)) {
			if (!method_exists($function[0],$function[1])) {
				throw new ArcoException("Unknown method '".$function[1]."' in instance of '".get_class($function[0])."'.");
			}
		}
		else {
			if (!function_exists($function)) {
				throw new ArcoException("Unknown function '$function'");
			}
		}
		if (is_array($function)) {
			$reflect = new ReflectionMethod(is_object($function[0]) ? get_class($function[0]) : $function[0],$function[1]);
		}
		else {
			$reflect = new ReflectionFunction($function);
		}
		$real_params = array ();
		foreach ($reflect->getParameters() as $i => $param) {
			$pname = $param->getName();
			if ($param->isPassedByReference()) {
				logger::log("Function/Method requires refernce pass but call_user_func_array_named does not support references.");
			}
			if (array_key_exists($pname, $params)) {
				$real_params[] = $params[$pname];
			} else
				if ($param->isDefaultValueAvailable()) {
					$real_params[] = $param->getDefaultValue();
				} else {
					// --------------------------------
					// missing required parameter
					// --------------------------------
					throw new ArcoException("Too few parameters for function/method '".$function[1] ? $function[1] : $function."'.");
					return NULL;
				}
		}
		return call_user_func_array($function, $real_params);
	}
	
	public function detect_mime($filename) {
		$filetype = strtolower(strrchr($filename, "."));
	
		switch ($filetype) {
			case ".zip" :
				$mime = "application/zip";
				break;
			case ".ez" :
				$mime = "application/andrew-inset";
				break;
			case ".hqx" :
				$mime = "application/mac-binhex40";
				break;
			case ".cpt" :
				$mime = "application/mac-compactpro";
				break;
			case ".doc" :
				$mime = "application/msword";
				break;
			case ".bin" :
				$mime = "application/octet-stream";
				break;
			case ".dms" :
				$mime = "application/octet-stream";
				break;
			case ".lha" :
				$mime = "application/octet-stream";
				break;
			case ".lzh" :
				$mime = "application/octet-stream";
				break;
			case ".exe" :
				$mime = "application/octet-stream";
				break;
			case ".class" :
				$mime = "application/octet-stream";
				break;
			case ".so" :
				$mime = "application/octet-stream";
				break;
			case ".dll" :
				$mime = "application/octet-stream";
				break;
			case ".oda" :
				$mime = "application/oda";
				break;
			case ".pdf" :
				$mime = "application/pdf";
				break;
			case ".ai" :
				$mime = "application/postscript";
				break;
			case ".eps" :
				$mime = "application/postscript";
				break;
			case ".ps" :
				$mime = "application/postscript";
				break;
			case ".smi" :
				$mime = "application/smil";
				break;
			case ".smil" :
				$mime = "application/smil";
				break;
			case ".xls" :
				$mime = "application/vnd.ms-excel";
				break;
			case ".ppt" :
				$mime = "application/vnd.ms-powerpoint";
				break;
			case ".wbxml" :
				$mime = "application/vnd.wap.wbxml";
				break;
			case ".wmlc" :
				$mime = "application/vnd.wap.wmlc";
				break;
			case ".wmlsc" :
				$mime = "application/vnd.wap.wmlscriptc";
				break;
			case ".bcpio" :
				$mime = "application/x-bcpio";
				break;
			case ".vcd" :
				$mime = "application/x-cdlink";
				break;
			case ".pgn" :
				$mime = "application/x-chess-pgn";
				break;
			case ".cpio" :
				$mime = "application/x-cpio";
				break;
			case ".csh" :
				$mime = "application/x-csh";
				break;
			case ".dcr" :
				$mime = "application/x-director";
				break;
			case ".dir" :
				$mime = "application/x-director";
				break;
			case ".dxr" :
				$mime = "application/x-director";
				break;
			case ".dvi" :
				$mime = "application/x-dvi";
				break;
			case ".spl" :
				$mime = "application/x-futuresplash";
				break;
			case ".gtar" :
				$mime = "application/x-gtar";
				break;
			case ".hdf" :
				$mime = "application/x-hdf";
				break;
			case ".js" :
				$mime = "application/x-javascript";
				break;
			case ".skp" :
				$mime = "application/x-koan";
				break;
			case ".skd" :
				$mime = "application/x-koan";
				break;
			case ".skt" :
				$mime = "application/x-koan";
				break;
			case ".skm" :
				$mime = "application/x-koan";
				break;
			case ".latex" :
				$mime = "application/x-latex";
				break;
			case ".nc" :
				$mime = "application/x-netcdf";
				break;
			case ".cdf" :
				$mime = "application/x-netcdf";
				break;
			case ".sh" :
				$mime = "application/x-sh";
				break;
			case ".shar" :
				$mime = "application/x-shar";
				break;
			case ".swf" :
				$mime = "application/x-shockwave-flash";
				break;
			case ".sit" :
				$mime = "application/x-stuffit";
				break;
			case ".sv4cpio" :
				$mime = "application/x-sv4cpio";
				break;
			case ".sv4crc" :
				$mime = "application/x-sv4crc";
				break;
			case ".tar" :
				$mime = "application/x-tar";
				break;
			case ".tcl" :
				$mime = "application/x-tcl";
				break;
			case ".tex" :
				$mime = "application/x-tex";
				break;
			case ".texinfo" :
				$mime = "application/x-texinfo";
				break;
			case ".texi" :
				$mime = "application/x-texinfo";
				break;
			case ".t" :
				$mime = "application/x-troff";
				break;
			case ".tr" :
				$mime = "application/x-troff";
				break;
			case ".roff" :
				$mime = "application/x-troff";
				break;
			case ".man" :
				$mime = "application/x-troff-man";
				break;
			case ".me" :
				$mime = "application/x-troff-me";
				break;
			case ".ms" :
				$mime = "application/x-troff-ms";
				break;
			case ".ustar" :
				$mime = "application/x-ustar";
				break;
			case ".src" :
				$mime = "application/x-wais-source";
				break;
			case ".xhtml" :
				$mime = "application/xhtml+xml";
				break;
			case ".xht" :
				$mime = "application/xhtml+xml";
				break;
			case ".zip" :
				$mime = "application/zip";
				break;
			case ".au" :
				$mime = "audio/basic";
				break;
			case ".snd" :
				$mime = "audio/basic";
				break;
			case ".mid" :
				$mime = "audio/midi";
				break;
			case ".midi" :
				$mime = "audio/midi";
				break;
			case ".kar" :
				$mime = "audio/midi";
				break;
			case ".mpga" :
				$mime = "audio/mpeg";
				break;
			case ".mp2" :
				$mime = "audio/mpeg";
				break;
			case ".mp3" :
				$mime = "audio/mpeg";
				break;
			case ".aif" :
				$mime = "audio/x-aiff";
				break;
			case ".aiff" :
				$mime = "audio/x-aiff";
				break;
			case ".aifc" :
				$mime = "audio/x-aiff";
				break;
			case ".m3u" :
				$mime = "audio/x-mpegurl";
				break;
			case ".ram" :
				$mime = "audio/x-pn-realaudio";
				break;
			case ".rm" :
				$mime = "audio/x-pn-realaudio";
				break;
			case ".rpm" :
				$mime = "audio/x-pn-realaudio-plugin";
				break;
			case ".ra" :
				$mime = "audio/x-realaudio";
				break;
			case ".wav" :
				$mime = "audio/x-wav";
				break;
			case ".pdb" :
				$mime = "chemical/x-pdb";
				break;
			case ".xyz" :
				$mime = "chemical/x-xyz";
				break;
			case ".bmp" :
				$mime = "image/bmp";
				break;
			case ".gif" :
				$mime = "image/gif";
				break;
			case ".ief" :
				$mime = "image/ief";
				break;
			case ".jpeg" :
				$mime = "image/jpeg";
				break;
			case ".jpg" :
				$mime = "image/jpeg";
				break;
			case ".jpe" :
				$mime = "image/jpeg";
				break;
			case ".png" :
				$mime = "image/png";
				break;
			case ".tiff" :
				$mime = "image/tiff";
				break;
			case ".tif" :
				$mime = "image/tiff";
				break;
			case ".djvu" :
				$mime = "image/vnd.djvu";
				break;
			case ".djv" :
				$mime = "image/vnd.djvu";
				break;
			case ".wbmp" :
				$mime = "image/vnd.wap.wbmp";
				break;
			case ".ras" :
				$mime = "image/x-cmu-raster";
				break;
			case ".pnm" :
				$mime = "image/x-portable-anymap";
				break;
			case ".pbm" :
				$mime = "image/x-portable-bitmap";
				break;
			case ".pgm" :
				$mime = "image/x-portable-graymap";
				break;
			case ".ppm" :
				$mime = "image/x-portable-pixmap";
				break;
			case ".rgb" :
				$mime = "image/x-rgb";
				break;
			case ".xbm" :
				$mime = "image/x-xbitmap";
				break;
			case ".xpm" :
				$mime = "image/x-xpixmap";
				break;
			case ".xwd" :
				$mime = "image/x-xwindowdump";
				break;
			case ".igs" :
				$mime = "model/iges";
				break;
			case ".iges" :
				$mime = "model/iges";
				break;
			case ".msh" :
				$mime = "model/mesh";
				break;
			case ".mesh" :
				$mime = "model/mesh";
				break;
			case ".silo" :
				$mime = "model/mesh";
				break;
			case ".wrl" :
				$mime = "model/vrml";
				break;
			case ".vrml" :
				$mime = "model/vrml";
				break;
			case ".css" :
				$mime = "text/css";
				break;
			case ".html" :
				$mime = "text/html";
				break;
			case ".htm" :
				$mime = "text/html";
				break;
			case ".asc" :
				$mime = "text/plain";
				break;
			case ".txt" :
				$mime = "text/plain";
				break;
			case ".rtx" :
				$mime = "text/richtext";
				break;
			case ".rtf" :
				$mime = "text/rtf";
				break;
			case ".sgml" :
				$mime = "text/sgml";
				break;
			case ".sgm" :
				$mime = "text/sgml";
				break;
			case ".tsv" :
				$mime = "text/tab-separated-values";
				break;
			case ".wml" :
				$mime = "text/vnd.wap.wml";
				break;
			case ".wmls" :
				$mime = "text/vnd.wap.wmlscript";
				break;
			case ".etx" :
				$mime = "text/x-setext";
				break;
			case ".xml" :
				$mime = "text/xml";
				break;
			case ".xsl" :
				$mime = "text/xml";
				break;
			case ".mpeg" :
				$mime = "video/mpeg";
				break;
			case ".mpg" :
				$mime = "video/mpeg";
				break;
			case ".mpe" :
				$mime = "video/mpeg";
				break;
			case ".qt" :
				$mime = "video/quicktime";
				break;
			case ".mov" :
				$mime = "video/quicktime";
				break;
			case ".mxu" :
				$mime = "video/vnd.mpegurl";
				break;
			case ".avi" :
				$mime = "video/x-msvideo";
				break;
			case ".movie" :
				$mime = "video/x-sgi-movie";
				break;
			case ".flv" :
				$mime = "video/x-flv";
				break;
			case ".asf" :
				$mime = "video/x-ms-asf";
				break;
			case ".asx" :
				$mime = "video/x-ms-asf";
				break;
			case ".wm" :
				$mime = "video/x-ms-wm";
				break;
			case ".wmv" :
				$mime = "video/x-ms-wmv";
				break;
			case ".wvx" :
				$mime = "video/x-ms-wvx";
				break;
			case ".ice" :
				$mime = "x-conference/x-cooltalk";
				break;
		}
	
		return $mime;
	}
}
?>
