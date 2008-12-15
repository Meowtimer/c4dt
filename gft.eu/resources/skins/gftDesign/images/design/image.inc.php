<?php // image

define("IM_CENTER",1);
define("IM_LEFT",0);
define("IM_RIGHT",2);
define("IM_BOTTOM",0);
define("IM_TOP",2);


interface im_function {
	
	public function GetParamsArray();
	public function GetFuncName();
	
}

class im_point {
	public $x;
	public $y;
	
	public function __construct($x,$y) {
		$this->x = $x;
		$this->y = $y;
	}
}

class im_bbox {
	
	public $array = array();
	
	public $leftBottom;
	public $rightBottom;
	public $rightTop;
	public $leftTop;
	
	public function __construct($inputArray) {
		$this->leftBottom = new im_point($inputArray[0],$inputArray[1]);
		$this->rightBottom = new im_point($inputArray[2],$inputArray[3]);
		$this->rightTop = new im_point($inputArray[4],$inputArray[5]);
		$this->leftTop = new im_point($inputArray[6],$inputArray[7]);
	}
	
}

class im_ttf_text implements im_function {
	// $im->ttftext(18,0,10,34,$im->colorallocate(0x99,0x99,0x99),"AGENCYR.TTF","FireXTrainer");
	public $size;
	public $angle;
	public $startPoint; // bottom left
	public $boxSize;
	public $color;
	public $fontFile;
	public $text;
	public $align = IM_LEFT;
	public $valign = IM_BOTTOM;
	
	public function __construct($text) {
		$this->text = $text;
	}
	
	public function GetParamsArray() {
		$x = $this->startPoint->x;
		$y = $this->startPoint->y;
		if (IM_LEFT < $this->align) {
			$bbox = new im_bbox(imagettfbbox($this->size,$this->angle,$this->fontFile,$this->text));
			if ($bbox->rightBottom->x > $this->boxSize->x) throw new Exception("Oversize.x!" . $bbox->rightBottom->x ." ". $this->boxSize->x);
			if ($bbox->rightTop->y > $this->boxSize->y) throw new Exception("Oversize.y!");
			if ($this->align == IM_CENTER) {
				$x += floor(($this->boxSize->x - $bbox->rightBottom->x) / 2);
			}
			if ($this->align == IM_RIGHT) {
				$x += ($this->boxSize->x - $bbox->rightBottom->x);
			}
		}
		if (IM_BOTTOM < $this->valign) {
			$bbox = new im_bbox(imagettfbbox($this->size,$this->angle,$this->fontFile,$this->text));
			if ($bbox->rightBottom->x > $this->boxSize->x) throw new Exception("Oversize.x!" . $bbox->rightBottom->x ." ". $this->boxSize->x);
			if ($bbox->rightTop->y > $this->boxSize->y) throw new Exception("Oversize.y!");
			if ($this->valign == IM_CENTER) {
				$y += floor(($this->boxSize->y - $bbox->rightTop->y) / 2);
			}
			if ($this->valign == IM_TOP) {
				$y += ($this->boxSize->y - $bbox->rightTop->y);
			}
		}
		
		
		$array[0] = $this->size;
		$array[1] = $this->angle;
		$array[2] = $x;
		$array[3] = $y;
		$array[4] = $this->color;
		$array[5] = $this->fontFile;
		$array[6] = $this->text;
		return $array;
	}
	
	public function GetFuncName() {
		return "ttftext";
	}
	
}




class image {

	public $im;
	
	public function createfrompng($fileName) {
		$this->im = imagecreatefrompng($fileName);
	}
	
	public function createtruecolor($x,$y) {
		$this->im = imagecreatetruecolor($x,$y);
	}
	
	public function ttfbbox($size,$angle,$fontFile,$text) {
		return new im_bbox(imagettfbbox($size,$angle,$fontFile,$text));
	}
	
	public function execute(im_function $obj) {
		return $this->__call($obj->GetFuncName(),$obj->getParamsArray());
	}
	
	public function __call($func,$params) {
		if (!function_exists("image".$func)) throw new Exception("function 'image".$func."' does not exist!");
		array_unshift($params,$this->im);
		return call_user_func_array("image".$func,$params);
	}

}

?>