<?php

// by ZokRadonh [arctics.net]

class imaging extends library {
 	public static function GetMethods() {
		return array ();
	}

	public static function GetClasses() {
		return array ();
	}

	public static function GetSuperObjects() {
		return array ();
	}
}

/**
 * Simple image class providing basic image editing features.
 * @version beta 0.4
 */
class Image implements Iterator {
	
	private $im;
	private $debug;
	
	public $type;
	
	public function __construct($image) {
		$this->im = $image;
//		$this->debug = imagecreatetruecolor($this->width, $this->height);
		//imagecopy($this->debug,$this->im,0,0,0,0,$this->width, $this->height);
	}
	
	public function GetPixel($x,$y) {
		if ($x < 0 || $x >= $this->width) throw new ImageSizeExceededException($x);
		if ($y < 0 || $y >= $this->height) throw new ImageSizeExceededException($y);
		$pxl = new Pixel(imagecolorsforindex($this->im,imagecolorat($this->im,$x,$y)));
		$pxl->im = $this;
		$pxl->x = $x;
		$pxl->y = $y;
		return $pxl;
	}
	
	public function SetDebugPixel($pixel) {
		if ($pixel->x < 0 || $pixel->x >= $this->width) throw new ImageSizeExceededException($pixel->x);
		if ($pixel->y < 0 || $pixel->y >= $this->height) throw new ImageSizeExceededException($pixel->y);
		if ($pixel->alpha !== FALSE) {
			$color = imagecolorallocatealpha($this->debug,$pixel->r,$pixel->g,$pixel->b,$pixel->alpha);
		}
		else $color = imagecolorallocate($this->debug, $pixel->r, $pixel->g, $pixel->b);
		imagesetpixel($this->debug,$pixel->x,$pixel->y,$color);
		return true;
	}
	
	public function SetPixel($pixel) {
		if ($pixel->x < 0 || $pixel->x >= $this->width) throw new ImageSizeExceededException($pixel->x);
		if ($pixel->y < 0 || $pixel->y >= $this->height) throw new ImageSizeExceededException($pixel->y);
		if ($pixel->alpha !== FALSE) {
			$color = imagecolorallocatealpha($this->im,$pixel->r,$pixel->g,$pixel->b,$pixel->alpha);
//			put("+");
		}
		else $color = imagecolorallocate($this->im, $pixel->r, $pixel->g, $pixel->b);
		imagesetpixel($this->im,$pixel->x,$pixel->y,$color);
//		put("set".$pixel->x);
		return true;
	}
	
	public function GetBrightness($x,$y) {
		$pxl = imagecolorsforindex($this->im,imagecolorat($this->im,$x,$y));
		unset($pxl['alpha']);
		return array_sum($pxl) / 3;
	}
	
	public function __call($func,$params) {
		if (substr($func,0,5) == "image") $func = substr($func,5);
		$imageFunc = "image" . $func;
		if (!function_exists($imageFunc)) throw new ArcoException("Unknown image-function '$imageFunc'.");
		array_unshift($params,$this->im);
		return call_user_func_array($imageFunc,$params);
	}
	
	public function __get($var) {
		if ($var == "width") return $this->Width();
		elseif ($var == "height") return $this->Height();
		else return null;
	}
	
	public function Width() {
		return imagesx($this->im);
	}
	
	public function Height() {
		return imagesy($this->im);
	}
	
	public function CutPart($x,$y,$width,$height) {
		$im = imagecreatetruecolor($width,$height);
		imagecopyresized($im,$this->im,0,0,$x,$y,$width,$height,$width,$height);
		return $im;
	}
	
	public function CutPartImage($x,$y,$width,$height) {
		$im = imagecreatetruecolor($width,$height);
		imagecopyresized($im,$this->im,0,0,$x,$y,$width,$height,$width,$height);
		return new Image($im);
	}
	
	public function ResizeProportional($maxWidth, $maxHeight) {
		if (!$maxHeight) return false; 
		$maxScale = $maxWidth / $maxHeight;
		$scale = $this->width / $this->height;
		if ($scale > $maxScale) {
			$width = $maxWidth;
			$height = $this->height * ($maxWidth / $this->width);
		}
		else {
			$height = $maxHeight;
			$width = $this->width * ($maxHeight / $this->height);
		}
		
		$im = imagecreatetruecolor($width,$height);
		imagecopyresized($im,$this->im,0,0,0,0,$width,$height,$this->width,$this->height);
		$this->im = $im;
		return true;
	}
	
	public static function CreateFromJpeg($fileName) {
		$im = new Image(imagecreatefromjpeg($fileName));
		$im->type = "jpeg";
		return $im;
	}
	
	public static function CreateFromPng($fileName) {
		$im = new Image(imagecreatefrompng($fileName));
		$im->type = "png";
		return $im;
	}
	
	public static function CreateFromGif($fileName) {
		$im = new Image(imagecreatefromgif($fileName));
		$im->type = "gif";
		return $im;
	}
	
	public static function Create($width,$height) {
		return new Image(imagecreate($width,$height));
	}
	
	public static function CreateTrueColor($width,$height) {
		return new Image(imagecreatetruecolor($width,$height));
	}

	public static function CreateByBlob($blob, $mime = false) {
		if ($mime === false) {
			if (substr($blob,0,4) == chr(0xFF).chr(0xD8).chr(0xFF).chr(0xE0)) {
				$mime = "image/jpeg";
			}
			elseif(substr($blob,0,3) == "GIF") {
				$mime = "image/gif";
			}
			elseif(substr($blob,1,3) == "PNG") {
				$mime = "image/png";
			} 
			else {
				throw new ImageException("Unable to create image from blob. Unknown blob data type.");
			}
		}
		$tmp = TMP.md5(microtime(true));
		$written = file_put_contents($tmp,$blob);
		if ($written != strlen($blob)) throw new IOException("Unable to write to '$tmp'.");
		switch($mime) {
			case "image/jpeg":
				$im = image::CreateFromJpeg($tmp);
				break;
			case "image/png":
				$im = image::CreateFromPng($tmp);
				break;
			case "image/gif":
				$im = image::CreateFromGif($tmp);
				break;
			default:
				throw new ImageException("Unknown image mime type '$mime'. Unable to read blob data.");
		}
		if (!unlink($tmp)) throw new AVMRuntimeException("Unable to delete '$tmp'.");
		if (file_exists($tmp)) throw new AVMRuntimeException("Unable to delete '$tmp'.");
		return $im;
	}
	
	public function GetBlob($type = false) {
		$tmp = TMP.md5(microtime(true));
		$this->Export($tmp, $type);
		if (!file_exists($tmp)) throw new ImageException("Unable to get blob of image. File was not created.");
		$blob = file_get_contents($tmp);
		if (!unlink($tmp)) throw new AVMRuntimeException("Unable to delete '$tmp'.");
		if (file_exists($tmp)) throw new AVMRuntimeException("Unable to delete '$tmp'.");
		return $blob;
	}
	
	public function Export($filename, $type = false, $quality = 100) {
		if ($type !== false) $fileType = $type;
		else $fileType = $this->type;
		if (!$fileType) throw new ImageException("Unable to export image. You have to specify an image type.");
		if ($quality == 100 && $fileType == "png") $quality = 9;
		$this->$fileType($filename, $quality);
	}

	private $cx = 0;
	private $cy = 0;
	private $iterationOrder;
	public function SetIterationOrder($downright) {
		$this->iterationOrder = $downright;
	}
	
	public function current() {
		return $this->GetPixel($this->cx,$this->cy);
	}
	
	public function key() {
		return array($this->cx,$this->cy);
	}
	
	public function SkipXPixel($int) {
		$this->cx += $int;
	}
	
	public function next() {
		if (!$this->iterationOrder) {
			$this->cx++;
			if ($this->cx >= $this->width) {
				$this->cx = 0;
				$this->cy++;
			}
		}
		else {
			$this->cy++;
			if ($this->cy >= $this->height) {
				$this->cy = 0;
				$this->cx++;
			}
		}
	}
	
	public function valid() {
		if (!$this->iterationOrder && $this->cy >= $this->height) return false;
		elseif ($this->iterationOrder && $this->cx >= $this->width) return false;
		else return true;
	}
	
	public function reset($x, $y) {
		if ($x !== false) $this->cx = $x;
		if ($y !== false) $this->cy = $y;
	}
	
	public function rewind() {
		$this->cx = 0;
		$this->cy = 0;
	}
	
	public function seek($strange) {
		$high = $strange >> 16;
		$low = $strange & 0xFFFF;
		$this->cx = $high;
		$this->cy = $low;
	}
	
	private $cachedPixels = array();
	private function isPixelCached($needle) {
		foreach($this->cachedPixels as $pixel) {
			if ($needle->Equals($pixel)) return true;
		}
		return false;
	}
	
	public function MagicDoubleCallback(Pixel $start, $condition, $positive, $negative) { // Zok's Triple Magic: Pixel, Callback, Callback, Callback
		$this->cachedPixels = array();
		$this->subMagic($start, $condition, $positive, $negative);
	}
	
	private function subMagic(Pixel $start, $condition, $positive, $negative) {
		for($x = 0;$x < 8;$x += 1) {
			$c = $start->Near($x);
			if (!$c) continue;
			if (!$this->isPixelCached($c)) $this->cachedPixels[] = $c;
			else {
//				put(".");
				continue;
			}
			if (call_user_func($condition, $start, $c)) {
				call_user_func($positive, $c);
				$this->subMagic($c, $condition, $positive, $negative);
			}
			else {
				call_user_func($negative, $c);
			}
		}
	}
	
	public function ExportDebug($fileName) {
		return imagepng($this->debug,$fileName);
	}
	
	public function ttfbbox($size, $angle, $fontFile, $text) {
		return imagettfbbox($size, $angle, $fontFile, $text);
	}
	
	public function PaintWaterProof($ttf,$text = "GFT") {
		$ttfFile = SKINS.arco::$engine->currentSkin."/$ttf";
		if (!file_exists($ttfFile)) throw new FileNotFoundException($ttfFile, "File needed to generate TTF-Text in WaterProof.");
		$size = min($this->height,$this->width) / 4;
		if (strlen($text) > 3) $size = 200 / strlen($text);
		$box = $this->ttfbbox($size, 0, $ttfFile, $text);
		$startPosY = $this->height / 2 + ($box[1] - $box[5]) / 3;
		$startPosX = ($this->width - $box[2]) / 2;
		$this->ttftext($size, 0, $startPosX, $startPosY, $this->colorallocatealpha(256,256,256,90), $ttfFile, $text);
		return 2;
	}
	
	public function Cache() {
		
	}
	
}

class Pixel {
	
	public $im;
	public $x;
	public $y;
	public $r;
	public $g;
	public $b;
	public $alpha = FALSE;
	public function __construct() {
		if (func_num_args() == 1) {
			$array = func_get_arg(0);
			$this->r = $array['red'];
			$this->g = $array['green'];
			$this->b = $array['blue'];
			if (isset($array['alpha']))
				$this->alpha = $array['alpha'];			
		}
		elseif (func_num_args() > 1) {
			$this->r = func_get_arg(0);
			$this->g = func_get_arg(1);
			$this->b = func_get_arg(2);
			if (func_num_args() > 3)
				$this->alpha = func_get_arg(3);
		}
	}
	
	public function GetBrightness() {
		return ($this->r + $this->g + $this->b) / 3;
	}
	
	public function Equals($pixel) {
		if (($this->x == $pixel->x) && ($this->y == $pixel->y)) return true;
		else return false;
	}
	
	public function Flush() { // edit parent image
		if (!$this->im) throw new ImageException("Missing parent image object");
		$this->im->SetDebugPixel($this);
		return true;
	}
	
	public function Near($dir) {
		if (!$this->im) throw new ImageException("Missing parent image object");
		try {
			switch($dir) {
				case 0: return $this->im->GetPixel($this->x + 0,$this->y - 1);
				case 1: return $this->im->GetPixel($this->x + 1,$this->y - 1);
				case 2: return $this->im->GetPixel($this->x + 1,$this->y - 0);
				case 3: return $this->im->GetPixel($this->x + 1,$this->y + 1);
				case 4: return $this->im->GetPixel($this->x + 0,$this->y + 1);
				case 5: return $this->im->GetPixel($this->x - 1,$this->y + 1);
				case 6: return $this->im->GetPixel($this->x - 1,$this->y - 0);
				case 7: return $this->im->GetPixel($this->x - 1,$this->y - 1);
			}
		}
		catch (ImageSizeExceededException $e) {
			return FALSE;
		}
	}
	
}

class ImageException extends ArcoException {
	public function __construct($msg) {
		parent::__construct($msg,0);
	}
}

class ImageSizeExceededException extends ImageException {
	public function __construct($msg) {
		parent::__construct($msg,0);
	}
}



?>