<?php
/*
 * Created on 22.03.2007
 *
 */
 
 class htmlformer extends library {
 	public static function GetMethods() {
		return array ();
	}

	public static function GetClasses() {
		return array ("former");
	}

	public static function GetSuperObjects() {
		return array ();
	}
 }
 
 class former implements ArrayAccess {
	
	public $raw;
	public $formated;
	
	private $depth = 0;
	private $c = 0;
	
	public function init() {
		$this->formated = "";
	}
	
	public function formXHTML($raw) {
		$this->raw = $raw;
		$this->formated = "";
//		$this->deletePrecedingSpaces();
//		$this->raw = str_replace(array("\t","\r","\n"),array(""),$this->raw);
		$this->c = 0;
		while($this->walk()); 
		return $this->formated;
	}
	
	private function deletePrecedingSpaces() {
		$this->raw = preg_replace('/\n\s+/',"",$this->raw);
	}
	
	private function walk() { // catches "<img" and ">" | unclosable | "<\w" <subs> ">" enclosing | starts comment && cdata && tag
		$match = array();
		switch($this[0]) {
			case "<":
				if ($this->get(1,3) == "!--") { // comment
					$this[] = $this->getIndent()."<!--\n";
					$this->move(4);
					$this->depth++;
					$this->walk_comment();
					$this->depth--;
				}
				elseif($this->get(1) == "!") { // doctype
					$this[] = "<!";
					$this->move(2);
					if ($this->match('/>/',$match, PREG_OFFSET_CAPTURE)) {
						$this[] = $this->getmove(0,$match[0][1]);
						if ($this[0] != ">") die("there is an offset error" . $this->get(-5,10));
						$this[] = $this->getmove(0)."\n";
					}

				}
				elseif ($this->match('/^((<script)|(<style))/', $match)) {
					$this[] = $this->getIndent().$match[1];
					$this->move(strlen($match[1]));
					$this->walk_tag();
					if ($this[0] != ">") die("there is an offset error" . $this->get(-5,10));
					$this[] = $this->getmove(0,1);
					$this->depth++;
					$num = $this->walk_cdata();
					$this->depth--;
					if ($this->match('/^((<\/script>)|(<\/style>))/', $match)) {
						$this[] = ($num > 0 ? $this->getIndent() : "").$match[1]."\n";
						$this->move(strlen($match[1]));
					}
					else throw new ArcoException("Invalid regex scriptstyle");
				}
				elseif($this->match('/^<(\w+)/',$match,0)) {
					$tagname = $match[1];
					$this[] = $this->getIndent().$match[0];
					$this->move(strlen($match[0]));
					$this->walk_tag();
					if ($this[0] == ">") { // <tag x=x > ... </tag>
//						print "jo".$match[1];
						$this[] = ">\n";
						$this->move(1);
						$this->depth++;
						while($this->walk());
//						fputs(fopen("php://stdout","w"),".");
						$this->depth--;
						if (!$this->match('#^</'.$tagname.'>#',$match,0)) {
							$surround = $this->get(-10, 10);
							$surround .= "%";
							$surround .= $this->get(0, 10);
							throw new ArcoException("can't find closing tag for '".$tagname."'. (".htmlentities($surround).")");
						}
						$this[] = $this->getIndent().$match[0]."\n";
						$this->move(strlen($match[0]));
					}
					elseif ($this[0] == "/" && $this[1] == ">") { // <tag />
//						print "no".$match[1];
						$this[] = "/>\n";
						$this->move(2);
					}
					else {
						throw new ArcoException("i am currently dying");
					}
				}
				else {
					return false;
					throw new ArcoException("unknown situation: '" . $this->get(-4,8)."'");
				}
			break;
			default:
				if (!preg_match('/[^<]+/',$this->raw,$match,0,$this->c)) return false;
				if (strlen(trim($match[0])))
					$this[] = $this->getIndent()."".$match[0].""."\n";
				$this->move(strlen($match[0]));
				return true;
			break;
		}
		return true;
	}
	
	private function walk_tag() { // catches " src=""" | closed by ">" | subs enclosed by """"
		$match = array();
		if ($this->match('#^(([\s\w\d_-]+="[^"]*")*\s*\w*)/?>#',$match)) {
			$this[] = $match[1];
			$this->move(strlen($match[1]));
		}
		else throw new ArcoException("Tag error: '".htmlentities($this->get(-10,20))."'");
	}
	
	private function walk_comment() { // catches <!-- "..." --> | closed by "-->" | no subs
		if (preg_match('/-->/', $this->raw, $match, PREG_OFFSET_CAPTURE, $this->c)) {
			$this[] = $this->getIndent().$this->getmove(0,$match[0][1] - $this->c)."\n"; // whole comment
			return;
		}
		else throw new ArcoException("HTML Syntax Error: unclosed comment at pos ".$this->c);
	}
	
	private function walk_cdata() { // catches scripts and styles | closed by "</script>" || "</style>" | no subs
		$match = array();
		if ($this->match('#((</script>)|(</style>))#', $match, PREG_OFFSET_CAPTURE, $this->c)) {
			$num = strlen($this->get(0,$match[0][1]));
			if ($num) $this[] = "\n".$this->getIndent().$this->getmove(0,$match[0][1])."\n"; // whole script / style
			return $num;
		}
		else throw new ArcoException("HTML Syntax Error: unclosed cdata at pos ".$this->c);
	}
	
	private function getIndent() {
		if ($this->depth < 0) throw new ArcoException("depth error");
		return str_repeat("   ",$this->depth);
	}
	
	private function match($regex,&$match,$flags = 0,$pos = false) {
		if ($pos === FALSE) $pos = $this->c;
		return preg_match($regex,substr($this->raw,$pos),$match,$flags);
	}
	
	private function move($int) {
		$this->c += $int;
		return $this->c;
	}
	
	public function get($offset,$length = 1) {
		return substr($this->raw,$this->c + $offset,$length);
	}
	
	public function getmove($offset,$length = 1) {
		$str = substr($this->raw,$this->c + $offset,$length);
		$this->move($offset + $length);
		return $str;
	}
	
	public function offsetExists($offset) {
		if ($this->c + $offset > strlen($this->raw) || $this->c + $offset < 0) return false;
		else return true;
	}
	
	public function offsetGet($offset) {
		return substr($this->raw, $this->c + $offset, 1);
	}
	
	public function offsetSet($offset, $value) {
		if (is_null($offset)) {
			$this->formated .= $value;
		}
		else throw new ArcoException("blub");
	}
	
	public function offsetUnset($offset) {
		throw new ArcoException("flub");
	}
	
 }
 
// $f = new former;
// try {
//	$f->formXHTML(file_get_contents("../arco_index.htm"));
// }
// catch(Exception $e) {
// 	print $e->getMessage();
// 	print "\n\n";
// }
// print $f->formated;
 
?>