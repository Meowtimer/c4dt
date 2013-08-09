// set object properties
ObjectSetAction($obj$, $action$, $params...$) => $obj$->SetAction($action$, $params...$);
$setter:Call,/Set(XDir|YDir)/$($dir$, $:Integer,/0/$ | $:Whitespace$, $rest...$) => $setter!Call(value.name, placeholder.subElements)$($dir$, $rest$);
$setter:Call,/Set(XDir|YDir)/$($dir$, $target$, $rest...$) => $target$->$setter>name$($dir$, $rest$);

// convert all indirect calls with string parameter for the function name into direct calls
$:Call,/DefinitionCall|ObjectCall|PrivateCall|DefinitionCall/$($target$, $func:String$, $params...$) => $target$->$func!Call(value.literal, placeholder.subElements)$($params$);
// convert indirect calls with complex parameter for the function name into Call calls
$:Call,/DefinitionCall|ObjectCall|PrivateCall|DefinitionCall/$($target, $func:String~$, $params...$) => $target$->Call($func$, $params$);

AddCommand($target$, $params...$) => $target$->AddCommand($params$);

// object finding
FindObject2($params...$) => FindObject($params...$);
Chain(
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
LocalN($name:String$, $obj$) => $obj$.$name>literal$;
LocalN($name:String$) => this.$name>literal$;
Local($number:Integer$) => this.$number>literal!"local"+value$;
Local($number:Integer$, $obj$) => $obj$.$number>literal!"local"+value$;

Var() => $!var("var0")$;
Var($number:Integer$) => $number>literal!var("var"+value)$;

// messages
Message($msg$, 0, $params...$) => Message($msg$, $params$);
Message($msg$, $obj$, $params...$) => $obj$->Message($msg$, $params...$);

// effects
EffectVar($index:Integer$, $target$, $effect$) => $effect$.$index>literal!"var"+value$;
EffectVar($complex$, $target$, $effect$) => $effect$[Format("var%d", $complex$)];

// misc
() => nil;
this() => this;
$m:MemberOperator,/->[A-Z_0-9]{4}::/$ => $m!"->"$;

// ids
$id:ID,/[0-9].*/$ => $id>literal!"_"+value$;

$x:Directive,/strict/$ => $x!"// Everything is superstrict"$;