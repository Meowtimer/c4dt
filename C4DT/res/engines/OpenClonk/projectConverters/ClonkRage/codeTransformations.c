// set object properties
ObjectSetAction($obj$, $action$, $params...$) => $obj$->SetAction($action$, $params...$);
$setter:Call,/Set(XDir|YDir)/$($dir$, $target$, $precision$) => $target$->$setter>name$($dir$, $precision$);

// object finding
FindObject2($params...$) => FindObject($params...$);
FindObject($id:ID$, $rest...$) => FindObject(Find_ID($id$), $rest$);

// locals
LocalN($name:String$, $obj$) => $obj$.$name>literal$;
LocalN($name:String$) => this.$name>literal$;

// messages
Message($msg$, $obj$, $params...$) => $obj$->Message($msg$, $params...$);

// effects
EffectVar($index$, $target$, $effect$) => $effect$.$index!"var"+eval(value[0])$;

// misc
() => nil;
this() => this;