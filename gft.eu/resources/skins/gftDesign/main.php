<?php
   /*������������������������������������������*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_main extends template implements main_tpl {

public function tpl_head($title,$headaddition) {
$HTML = <<<ADCE
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
 <title>$title</title>
 <meta http-equiv="Content-Type" content="text/html; charset={arco.outputCharset}" />
 <meta name="keywords" content="FireExTrainer  GFT  GeneralFireTech  Feuer  Brand  Simulation  Fire  FireWheel  FireMan  FireTray FireTrolly  Gas  Feuerschutz  Feuerlöschtrainer  Firextinguisher  Firextinguishetrainer  Feuerloescher FireTruck Brandhausmodule CFFT" />
 <meta name="description" content="GFT General FireTech GmbH" />
 <meta name="robots" content="index,follow" />
 <style type="text/css" media="all" />
	@import url({arco.const.skins}{arco.currentSkin}/style.css);
	/*@import url(resources/skins/gftDesign/style.highlvl.css) all; /* min-height workaround: exclude ie5 + ie6 */
 </style>
 <!--[if lt IE 7]>
   <style type="text/css">
     @import url(resources/skins/gftDesign/style.lowlvl.css);
   </style>
 <![endif]-->
 <script type="text/javascript" src="{arco.const.skins}{arco.currentSkin}/misc.js"></script>
 <script type="text/javascript" src="{arco.const.skins}{arco.currentSkin}/tooltip.js"></script>
   <!-- compliance patch for microsoft browsers -->
   

 $headaddition
</head>
<body onload="initArco();">
ADCE;
return $HTML;
}

public function tpl_foot($lang,$footaddition) {
$HTML = <<<ADCE
$footaddition
	<div id="language">
	<a href="{arco.currentRequest}&amp;lang=en"><img src="{arco.const.skins}{arco.currentSkin}/images/en/short_lang_switch.jpg" style="position: relative; left: 411px; margin-bottom: 5px;" alt="" /></a><br /><a href="{arco.currentRequest}&amp;lang=de"><img src="{arco.const.skins}{arco.currentSkin}/images/de/short_lang_switch.jpg" style="position: relative; left: 411px; margin-bottom: 5px;" alt="" /></a><br /><a href="{arco.currentRequest}&amp;lang=pl"><img src="{arco.const.skins}{arco.currentSkin}/images/pl/short_lang_switch.jpg" style="position: relative; left: 411px; margin-bottom: 5px;" alt="" /></a><br /><a href="{arco.currentRequest}&amp;lang=ru"><img src="{arco.const.skins}{arco.currentSkin}/images/ru/short_lang_switch.jpg" style="position: relative; left: 411px;" alt="" /></a></div>
<div id="copyright">
	&copy; 2007 - General FireTech GmbH - <img src="{arco.const.skins}{arco.currentSkin}/images/design/logo_grey_small.jpg" alt="" style="vertical-align: middle;" /> - All Rights Reserved
</div>
</body>
</html>
ADCE;
return $HTML;
}

public function tpl_image($path,$attributes = false) {
$HTML = <<<ADCE
<img src="$path"{?attributes} $attributes{/attributes}/>
ADCE;
return $HTML;
}

public function tpl_wmvVideo($resource) {
$HTML = <<<ADCE
<OBJECT id="mediaPlayer" width="320" height="285" 
      classid="CLSID:22d6f312-b0f6-11d0-94ab-0080c74c7e95" 
      codebase="http://activex.microsoft.com/activex/controls/mplayer/en/nsmp2inf.cab#Version=5,1,52,701"
      standby="Loading Microsoft Windows Media Player components..." type="application/x-oleobject">
      
      <param name="fileName" value="$resource">
      <param name="animationatStart" value="true">
      <param name="transparentatStart" value="true">
      <param name="autoStart" value="true">
      <param name="showControls" value="true">
      <param name="loop" value="false">
      <EMBED type="application/x-mplayer2"
        pluginspage="http://microsoft.com/windows/mediaplayer/en/download/"
        id="mediaPlayer" name="mediaPlayer" displaysize="4" autosize="-1" 
        bgcolor="darkblue" showcontrols="true" showtracker="-1" 
        showdisplay="0" showstatusbar="-1" videoborder3d="-1" width="320" height="285"
        src="$resource" autostart="true" designtimesp="5311" loop="false">
      </EMBED>
</OBJECT>
ADCE;
return $HTML;
}

public function tpl_flvVideo($resource) {
$HTML = <<<ADCE
<object type="application/x-shockwave-flash" data="{arco.const.skins}{arco.currentSkin}/FlowPlayerBlack.swf" width="400" height="330" id="FlowPlayer">
	<param name="allowScriptAccess" value="always" />
	<param name="movie" value="FlowPlayerBlack.swf" />
	<param name="quality" value="high" />
	<param name="scaleMode" value="showAll" />
	<param name="allowfullscreen" value="true" />
	<param name="wmode" value="transparent" />
	<param name="allowNetworking" value="all" />
	<param name="flashvars" value="config={ 
		autoPlay: true, 
		loop: false, 
		initialScale: 'scale',
		showLoopButton: false,
		showPlayListButtons: false,
		playList: [
			{ url: '$resource' }
		]
		}" />
</object>
ADCE;
return $HTML;
}

public function tpl_errorBox($msg) {
$HTML = <<<ADCE
<div class="errorBox">
  $msg
</div>
ADCE;
return $HTML;
}

public function tpl_hyperlink($link,$text,$tooltip = false) {
$HTML = <<<ADCE
<a href="$link"{?tooltip} tooltip="$tooltip"{/tooltip}>$text</a>
ADCE;
return $HTML;
}

public function tpl_hover_info($infotext) {
$HTML = <<<ADCE

ADCE;
return $HTML;	
}
}
?>