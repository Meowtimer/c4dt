// set object properties
ObjectSetAction($obj$, $action$, $params...$) => $obj$->SetAction($action$, $params...$);
$setter:Call,/Set(XDir|YDir)/$($dir$, $:Integer,/0/$ | $:Whitespace$, $rest...$) => $setter!Call(value.name, placeholder.subElements)$($dir$, $rest$);
$setter:Call,/Set(XDir|YDir)/$($dir$, $target$, $rest...$) => $target$->$setter>name$($dir$, $rest$);
GetXDir($target$, $rest...$) => $target$->GetXDir($rest$);
GetYDir($target$, $rest...$) => $target$->GetYDir($rest$);
GetXDir(0, $rest...$) => GetXDir($rest$);
GetYDir(0, $rest...$) => GetYDir($rest$);

// convert all indirect calls with string parameter for the function name into direct calls
$:Call,/DefinitionCall|ObjectCall|PrivateCall|ProtectedCall|DefinitionCall/$($target$, $func:String$, $params...$)
	=> $target$->$func!Call(value.literal, placeholder.subElements)$($params$);
$:Call,/DefinitionCall|ObjectCall|PrivateCall|ProtectedCall||DefinitionCall/$($target$, $func:~String$, $params...$)
	=> $target$->Call($func$, $params$);

AddCommand($target$, $params...$) => $target$->AddCommand($params$);

// object finding
FindObject2($params...$) => FindObject($params...$);
Chain(
	FindObjectOwner($id$, $owner$, $rest...$) => FindObject($id$, $rest$, Find_Owner($owner$)),
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1$, $container:~Whitespace$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1$, Find_Container($container$), $parms...$),
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1:~Whitespace$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, Find_ActionTarget($actiontarget1$), $parms...$),
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, $actiontarget1:~Whitespace$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action$, Find_ActionTarget($actiontarget1$), $parms...$),
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, $action:~Whitespace$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, $ocf$, Find_Action($action$), $parms...$),
	FindObject($id$, $x$, $y$, $w$, $h$, $ocf:~Whitespace$, $parms...$)
		=> FindObject($id$, $x$, $y$, $w$, $h$, Find_OCF($ocf$), $parms...$),
	FindObject($id$, $x:~Whitespace$, $y:~Whitespace$, $w:~Whitespace$, $h:~Whitespace$, $parms...$)
		=> FindObject($id$, Find_InRect($x$, $y$, $w$, $h$), $parms...$),
	FindObject($id:~Whitespace$, $parms...$)
		=> FindObject(Find_ID($id$), $parms...$)
);

NoContainer() => Find_NoContainer();

// locals
LocalN($name:String$, $obj$)               => $obj$.$name!Var(value.literal)$;
LocalN($name:String$)                      => this.$name!Var(value.literal)$;
Local($number:Integer$)                    => this.$number!Var("local"+value.literal)$;
Local($number:Integer$, $obj$)             => $obj$.$number!Var("local"+value.literal)$;
SetLocal($number:Integer$, $value$)        => this.$number!Var("local"+value.literal)$ = $value$;
SetLocal($number:Integer$, $value$, $obj$) => $obj$.$number!Var("local"+value.literal)$ = $value$;
Local($complex:~Integer$)                  => this[Format("local%d", $complex$)];
Local($complex:~Integer$, $obj$)           => $obj$[Format("local%d", $complex$)];
Local()                                    => this.local0;

Var() => $!EnforceLocal("var0")$;
Var($number:Integer$) => $number!EnforceLocal("var"+value.literal)$;

// messages
Message($msg$, 0, $params...$) => Message($msg$, $params$);
Message($msg$, $obj$, $params...$) => $obj$->Message($msg$, $params...$);

// effects
EffectVar($index:Integer$, $target$, $effect$) => $effect$.$index!Var("var"+value.literal$;
EffectVar($complex$, $target$, $effect$) => $effect$[Format("var%d", $complex$)];

GetVisibility($target$) => $target$.Visibility;
SetVisibility($vis$, $target$) => $target$.Visibility = $vis$;
$target$->SetVisibility($vis$) => $target$.Visibility = $vis$;
$target$->GetVisibility() => $target$.Visibility;

IsNewgfx() => true;

GetColorDw() => GetColor();
GetColorDw($target$) => $target$->GetColor();
SetColorDw($color$) => SetColor($color$);
SetColorDw($color$, $target$) => $target$->SetColor($color$);
CastC4ID($data$) => $data$;

DoMagicEnergy($...$) => Log("Guenther killed the magics :c");
GetMagicEnergy($...$) => 0;
EnergyCheck($...$) => true;

GetDesc($obj$) => $obj$.Description;
GetDesc($$, $def$) => $def$.Description;
GetDesc() => this.Description;

// misc
() => nil;
this() => this;
$m:MemberOperator,/->[A-Z_0-9]{4}::/$ => $m!MemberOperator(false, false, nil)$;

$x:Directive,/strict/$ => $x!"// Everything is superstrict"$;