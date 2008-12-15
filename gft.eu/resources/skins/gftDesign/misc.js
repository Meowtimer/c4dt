  // --------------------------------
  // Cross Browser
  // --------------------------------
  var dom = document.getElementById ? 1 : 0;
  var iex = document.createEventObject ? 1 : 0;
  var ns4 = document.layers ? 1 : 0;
  var ie6 = (document.compatMode && iex) ? 1 : 0;
  var ff = navigator.userAgent.search(/Firefox/) > -1 ? 1 : 0;
  
  // --------------------------------
  // Cookie Function
  // --------------------------------
  function getCookie(Keksname)
  {
	cookieArr=document.cookie.split(";");
	for(var i=0;i<cookieArr.length;i++)
	{
		if(cookieArr[i].split("=")[0] == Keksname)
		{
				return unescape(cookieArr[i].split("=")[1]);
		}
	}
	return null;
  }
  
  // --------------------------------
  // Mouse Position Functions
  // --------------------------------
  function getMousePosY(e){
	if (iex) {
		var ev = document.createEventObject(); // natz den ie | frame independent workaround
		e = ev;
	}
	return iex ? e.clientY : e.pageY;
  }
  
  function getMousePosX(e){
	if (iex) {
		var ev = document.createEventObject(); // natz den ie | frame independent workaround
		e = ev;
	}
	return iex ? e.clientX : e.pageX;
  }
  
  
  function correctPNG() // fix ie5.5 & ie6 png transparency incompatibility
  {

   var arVersion = navigator.appVersion.split("MSIE");
   var version = parseFloat(arVersion[1]);
   if ((version >= 5.5) && (version < 7.0) && (document.body.filters)) 
   {
      for(var i=0; i<document.images.length; i++)
      {
         var img = document.images[i];
         var imgName = img.src.toUpperCase();
         
         if (imgName.substring(imgName.length-3, imgName.length) == "PNG" || img.getAttribute("png") == "true" || imgName.search(/WRITE=/) != -1)
         {
         	//alert("correct for " + imgName);
         	
         	var parent = img.parentNode;
         	var span = document.createElement("span");
         	
			/*for(var o in img.style) {
				if (o == "length" || o.substr(0,3) == "get" || o.substr(0,3) == "set" || o == "removeProperty" || o == "item" || o == "parentRule") continue;
				try {
				span.style[o] = img.style[o];
				}
				catch(Exception) {
				
				}
				
			}*/
			
			span.style.cssText = img.style.cssText;
			

			
			
			//span.filters("DXImageTransform.Microsoft.AlphaImageLoader").src = img.src;
		//	alert("puh");
	//		span.filters.item("DXImageTransform.Microsoft.AlphaImageLoader").sizingMethod = "scale";
//			span.filters.item("DXImageTransform.Microsoft.AlphaImageLoader").Enabled = true;
			span.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src=\'" + img.src + "\',sizingMethod=\'image\',Enabled=true)";
			
			//alert("puh");
	//		if (!span) alert("parent is tod");
//         	alert("done?");



			span.style.display = "inline-block";
			span.style.width = "200px";
			span.style.height = "80px";

         	parent.replaceChild(span,img);
         	
            /*var imgID = (img.id) ? "id='" + img.id + "' " : "";
            var imgClass = (img.className) ? "class='" + img.className + "' " : "";
            var imgTitle = (img.title) ? "title='" + img.title + "' " : "title='" + img.alt + "' ";
            var imgStyle = "display:inline-block;" + img.style.cssText;
            if (img.align == "left") imgStyle = "float:left;" + imgStyle;
            if (img.align == "right") imgStyle = "float:right;" + imgStyle;
            if (img.parentElement.href) imgStyle = "cursor:hand;" + imgStyle;
            var strNewHTML = "<span " + imgID + imgClass + imgTitle
            + " style=\"" + "width:" + img.width + "px; height:" + img.height + "px;" + imgStyle + ";"
            + "filter:progid:DXImageTransform.Microsoft.AlphaImageLoader"
            + "(src=\'" + img.src + "\', sizingMethod='scale');\"></span>" ;
            img.outerHTML = strNewHTML;*/
            i--;

         }
      }
      correctPNGBG();
   }
   
  }
  
  function correctPNGBG() // fix ie5.5 & ie6 png transparency incompatibility
  {

   var arVersion = navigator.appVersion.split("MSIE");
   var version = parseFloat(arVersion[1]);
   if ((version >= 5.5) && (version < 7.0) && (document.body.filters)) 
   {
      var els = new Array();
      var b = document.getElementsByTagName("div")
      for(var i in b ) {
         //alert(i + "=" + b[i]);
        if (i.search(/^\d+$/) != -1) els.push(b[i]);
      }
      b = document.getElementsByTagName("td");
      for(var i in b) {
        if (i.search(/^\d+$/) != -1) els.push(b[i]);
      }
      //alert(els.length);
      for(var i in els)
      {
         var img = els[i];
         if (!img.style.backgroundImage) continue;
         var imgName = img.style.backgroundImage.replace(/url\(/,"").replace(/\)/,"");
         //alert(imgName);
         if (imgName.toUpperCase().substring(imgName.length-3, imgName.length) == "PNG" || img.getAttribute("png") == "true" || imgName.toUpperCase().search(/WRITE=/) != -1)
         {
			//alert("doit");
			img.style.backgroundImage = "none";
			img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src=\'" + imgName + "\',sizingMethod=\'scale\',Enabled=true)";
         }
      }
   }    
  }
  
  function correctPNGFor(img) {
  	var imgName = img.src.toUpperCase();
         if (imgName.substring(imgName.length-3, imgName.length) == "PNG" || img.getAttribute("png") == "true")
         {
         	alert("correct for " + imgName);
            var imgID = (img.id) ? "id='" + img.id + "' " : "";
            var imgClass = (img.className) ? "class='" + img.className + "' " : "";
            var imgTitle = (img.title) ? "title='" + img.title + "' " : "title='" + img.alt + "' ";
            var imgStyle = "display:inline-block;" + img.style.cssText;
            if (img.align == "left") imgStyle = "float:left;" + imgStyle;
            if (img.align == "right") imgStyle = "float:right;" + imgStyle;
            if (img.parentElement.href) imgStyle = "cursor:hand;" + imgStyle;
            var strNewHTML = "<span " + imgID + imgClass + imgTitle
            + " style=\"" + "width:" + img.width + "px; height:" + img.height + "px;" + imgStyle + ";"
            + "filter:progid:DXImageTransform.Microsoft.AlphaImageLoader"
            + "(src=\'" + img.src + "\', sizingMethod='scale');\"></span>" ;
            img.outerHTML = strNewHTML;
         }
  }
  
  var lastE;
  
  function showArea(obj) {
    if (lastE) {
    	lastE.style.border = "1px none";
    	//obj.style.removeAttribute("backgroundImage",false);
    }
  	var right = document.getElementById("serviceRightFloat");
    right.innerHTML = obj.lastChild.innerHTML;
    obj.style.color = "#DDDDDD";
    obj.style.borderBottom = "1px dashed black";
    //obj.style.backgroundImage = "url(resources/skins/gftDesign/images/design/table_head_background.png)";
    
//	obj.lastChild.style.display = "inline";
//	obj.lastChild.style.backgroundColor = "#CCCCCC";
  }
  
  function hideArea(obj) {
  	obj.style.color = "#000000";
  	lastE = obj;
    //var right = document.getElementById("serviceRightFloat");
	//right.innerHTML = "&nbsp;";
//	obj.lastChild.style.display = "none";
  }



  if (window.attachEvent) window.attachEvent("onload", correctPNG);