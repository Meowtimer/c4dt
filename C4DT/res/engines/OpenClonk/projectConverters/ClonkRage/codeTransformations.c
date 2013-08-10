// replace ObjectSetAction with direct SetAction call
ObjectSetAction($obj$, $action$, $params...$) => $obj$->SetAction($action$, $params...$);

// convert (Set|Get)(X|Y)Dir calls to direct calls

SetXDir($dir$, $:Integer,/0/$ | $:Whitespace$, $rest...$) => SetXDir($dir$, $rest$);
SetYDir($dir$, $:Integer,/0/$ | $:Whitespace$, $rest...$) => SetXDir($dir$, $rest$);
SetXDir($dir$, $target$, $rest...$) => $target$->SetXDir($dir$, $rest$);
SetYDir($dir$, $target$, $rest...$) => $target$->SetXDir($dir$, $rest$);

GetXDir($:Integer,/0/$ | $:Whitespace$, $rest...$) => GetXDir($rest$);
GetYDir($:Integer,/0/$ | $:Whitespace$, $rest...$) => GetYDir($rest$);
GetXDir($target$, $rest+$) => $target$->GetXDir($rest$);
GetYDir($target$, $rest+$) => $target$->GetYDir($rest$);

// convert all indirect calls with string parameter for the function name into direct calls
$:Call,/DefinitionCall|ObjectCall|PrivateCall|ProtectedCall|DefinitionCall/$($target$, $func:String$, $params...$)
	=> $target$->$func!Call(value.literal, placeholder.subElements)$($params$);
// convert all indirect calls where function parameter is complex expression into Call() calls
$:Call,/DefinitionCall|ObjectCall|PrivateCall|ProtectedCall||DefinitionCall/$($target$, $func:~String$, $params...$)
	=> $target$->Call($func$, $params$);

// AddCommand direct call
AddCommand($target$, $params...$) => $target$->AddCommand($params$);

// FindObject is the new FindObject2
FindObject2($params...$) => FindObject($params...$);

// chained transformations to digest the old CR FindObject into the modular Find_* calls
Chain(
	
	// first, normalize FindObjectOwner to FindObject call and then continue looking where it takes us
	FindObjectOwner($id$, $owner$, $rest...$)
		=> FindObject($id$, $rest$, Find_Owner($owner$)),

	// digest parameters right-to-left

	// NoContainer() gets turned into Find_NoContainer()
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1$, NoContainer(), $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1$, Find_NoContainer(), $parms...$),
	
	// other owner parameter gets turned into Find_Container($container$)
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1$, $container:~Whitespace$ & $container:~Call,/Find_NoContainer/$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1$, Find_Container($container$), $parms...$),
	
	// action target
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1:~Whitespace$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, Find_ActionTarget($actiontarget1$), $parms...$),
	
	// action
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action:~Whitespace$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, Find_Action($action$), $parms...$),
	
	// ocf
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf:~Whitespace$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, Find_OCF($ocf$), $parms...$),
	
	// rect
	FindObject($id$, $x:~Whitespace$, $y:~Whitespace$, $w:~Whitespace$, $h:~Whitespace$, $parms...$)
		=> FindObject($id$, Find_InRect($x$, $y$, $w$, $h$), $parms...$),
	
	// passed 0 or whitespace for id? remove
	FindObject($?value.is(Whitespace)||(value.is(Integer)&&value.literal==0)$, $parms...$) => FindObject($parms$),
	// id
	FindObject($id$, $parms...$)
		=> FindObject(Find_ID($id$), $parms...$),

	// remove useless calls caused by converting 0 passings
	FindObject(Find_ID(0), $rest...$) => FindObject($rest$),
	FindObject($left...$, Find_Action(0), $right...$) => FindObject($left$, $right$),
	FindObject($left...$, Find_ActionTarget(0), $right...$) => FindObject($left$, $right$)
);

// Local*() calls get converted into local<number> local accesses, but that is quite unoptimal
LocalN($name:String$, $obj$)               => $obj$.$name!Var(value.literal)$;
LocalN($name:String$)                      => this.$name!Var(value.literal)$;
Local($number:Integer$)                    => this.$number!Var("local"+value.literal)$;
Local($number:Integer$, $obj$)             => $obj$.$number!Var("local"+value.literal)$;
SetLocal($number:Integer$, $value$)        => this.$number!Var("local"+value.literal)$ = $value$;
SetLocal($number:Integer$, $value$, $obj$) => $obj$.$number!Var("local"+value.literal)$ = $value$;
Local($complex:~Integer$)                  => this[Format("local%d", $complex$)];
Local($complex:~Integer$, $obj$)           => $obj$[Format("local%d", $complex$)];
Local()                                    => this.local0;

// for Var() calls declare var<number> vars
Var() => $!EnforceLocal("var0")$;
Var($number:Integer$) => $number!EnforceLocal("var"+value.literal)$;

// Message direct call
Message($msg$, $:Integer,/0/$ | $:Whitespace$, $params...$) => Message($msg$, $params$);
Message($msg$, $obj$, $params...$) => $obj$->Message($msg$, $params...$);

// EffectVar() calls turned into <effect>.var<number> accesses
EffectVar($index:Integer$, $target$, $effect$) => $effect$.$index!Var("var"+value.literal$;
EffectVar($complex$, $target$, $effect$) => $effect$[Format("var%d", $complex$)];

// Visibility is property, yes.
GetVisibility($target$) => $target$.Visibility;
SetVisibility($vis$, $target$) => $target$.Visibility = $vis$;
$target$->SetVisibility($vis$) => $target$.Visibility = $vis$;
$target$->GetVisibility() => $target$.Visibility;

// it's always newgfx-time
IsNewgfx() => true;

// ColorDw replaced by Color, I guess
GetColorDw() => GetColor();
GetColorDw($target$) => $target$->GetColor();
SetColorDw($color$) => SetColor($color$);
SetColorDw($color$, $target$) => $target$->SetColor($color$);

// remove all the casts
$:Call,/Cast(Int|C4ID|C4Object|Bool|Any)/$($data$) => $data$;

// no magics in this world
DoMagicEnergy($...$) => Log("Guenther killed the magics :c");
GetMagicEnergy($...$) => 0;

// but we get free energy, yay
EnergyCheck($...$) => true;

// GetDesc() is Description property, I guess
GetDesc($obj$) => $obj$.Description;
GetDesc($$, $def$) => $def$.Description;
GetDesc() => this.Description;

// OCF_Prey-using code crashes because of the reasons
OCF_Prey => FatalError("No OCF_Prey");

// misc
() => nil;
this() => this;
$m:MemberOperator,/->[A-Z_0-9]{4}::/$ => $m!MemberOperator(false, false, nil)$;

// replace #strict with comments, whatevs
$x:Directive,/strict/$ => $x!"// Everything is superstrict"$;