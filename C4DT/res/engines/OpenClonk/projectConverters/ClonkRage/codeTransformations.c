ObjectSetAction($obj$, $action$, $params...$) => $obj$->SetAction($action$, $params...$);
FindObject2($params...$) => FindObject($params...$);
LocalN($name$, $obj$) => $obj$->$name.stringValue$;
$id:IDLiteral$ => $id.definitionName$;
() => nil;
Message($msg$, $obj$, $params...$) => $obj$->Message($msg$, $params...$);