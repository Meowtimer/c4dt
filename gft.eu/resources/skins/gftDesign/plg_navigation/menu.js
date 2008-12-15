// ---------------------------
// Sugar
// ---------------------------
document.createPNGImage = function(src) {
	if (/\.jpe?g/i.test(src)) {
		var dummy = document.createElement("img");
		dummy.src = src;
		dummy.alt = "";
		return dummy;
	}
	//var dummy = document.createElement("img");
	//dummy.src = src;
	//dummy.alt = "";
	
	var img = document.createElement("span");
	img.style.backgroundImage = "none";
//	img.src = "resources/skins/gftDesign/images/design/spacer.gif";
	//img.title = "Arco Systems";
//	img.style.width = dummy.width + "px";
//	img.style.height = dummy.height + "px";
	//img.runtimeStyle.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(enabled=true, src='" + src + "',sizingMethod='image',Enabled=true)";
	img.style.display = "inline-block";
	img.style.width = "500px";
	img.style.height = "500px";
	img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "',sizingMethod='image',Enabled=true)";
	return img;
}

document.openMenus = new Array(); // of ArcoMenu

// ---------------------------
// Item
// ---------------------------
function ArcoItem(id, src, link, parentMenu) {
	this.itemId	 = id;
	this.picSrc = src;
	this.linkUrl = link;
	this.parentMenu = parentMenu;
	if (!this.parentMenu) alert("ERROR! itemId: " + this.itemId);
}

ArcoItem.prototype.subMenu = null;
ArcoItem.prototype.parentMenu = null;
ArcoItem.prototype.itemDiv = null;  // access this
ArcoItem.prototype.subMenuDiv = null;
ArcoItem.prototype.hideTimer = null;

ArcoItem.prototype.generateObject = function() { // only called by menu once
	var arVersion = navigator.appVersion.split("MSIE");
	var version = parseFloat(arVersion[1]);
	this.itemDiv = document.createElement("div");
	var dummyDiv = document.createElement("div");
	var dummyLink = document.createElement("a");
	dummyLink.href = this.linkUrl;
	var dummyPic;
	if ((version >= 5.5) && (version < 7.0)) {
		dummyPic = document.createPNGImage(this.picSrc);
	}
	else {
		dummyPic = document.createElement("img");
		dummyPic.src = this.picSrc;
		dummyPic.alt = "";
	}
	//dummyPic.style.position = "relative";
	//dummyPic.style.zIndex = "11";
	dummyPic.onmouseover = this.over;
	dummyPic.onmouseout = this.out;
	dummyPic.arco = this;
	dummyLink.appendChild(dummyPic);
	dummyDiv.appendChild(dummyLink);
	this.itemDiv.appendChild(dummyDiv);
	return this.itemDiv;
}

ArcoItem.prototype.stopHiding = function() {
	window.clearTimeout(this.hideTimer);
	this.hideTimer = null;
}

ArcoItem.prototype.isHiding = function() {
	if (this.hideTimer) return true;
	else return false;
}

ArcoItem.prototype.isOpen = function() {
	if (this.subMenuDiv) return true;
	else return false;
}

ArcoItem.prototype.show = function() {
	this.stopHiding();
	if (this.isOpen()) return true;
	// save our menuObj in openMenus
	document.openMenus[this.subMenu.menuId] = this.subMenu;
	// create and show submenu
	this.subMenuDiv = this.subMenu.generateObject(this.subMenu.depth);
	this.itemDiv.appendChild(this.subMenuDiv);
	//this.subMenuDiv.firstChild.firstChild.style.width = "200px";
	//this.subMenuDiv.firstChild.firstChild.style.height = "100px";
	this.subMenuDiv.firstChild.firstChild.style.display = "inline-block";
	//this.subMenuDiv.firstChild.firstChild.style.border = "1px solid black";
	// save me in the parent menu
	this.parentMenu.openChildMenus[this.itemId] = this;
	return true;
}

ArcoItem.prototype.hide = function() {
	if (!this.subMenu) {
//		alert("subMenu lost!"); // does not occur anymore
		return false;
	}
	this.stopHiding();
	// first close all children
	if (this.subMenu.openChildMenus.length > 0) {
		for(var i in this.subMenu.openChildMenus) {
			if (!this.subMenu.openChildMenus[i]) continue; // already killed? multi threading crash?
			this.subMenu.openChildMenus[i].hide();
		}
	}
	document.openMenus[this.subMenu.menuId] = null;
	this.itemDiv.removeChild(this.subMenuDiv);
	this.subMenuDiv = null;
	// remove me from parent open children
	this.parentMenu.openChildMenus[this.itemId] = null;
}

ArcoItem.prototype.over = function() {
	if (this.arco.parentMenu.menuId == 0) {
		this.src = this.src.replace(/.jpg/,"_rollover.jpg");
	}

	if (this.arco.subMenu) {
		this.arco.parentMenu.hideAll();
		this.arco.show();
	}	
//	this.arco.parentMenu.stopHiding();
}

ArcoItem.prototype.out = function() {
	if (this.arco.parentMenu.menuId == 0) {
		this.src = this.src.replace(/_rollover.jpg/,".jpg");
	}
	if (this.arco.subMenu) {
		this.arco.delayHide();
	}
}

ArcoItem.prototype.delayHide = function() {
	this.stopHiding();
	this.hideTimer = window.setTimeout("document.openMenus[" + this.subMenu.menuId + "].parentItem.hide()",800);
}

// ---------------------------
// Menu
// ---------------------------
function ArcoMenu(id, parent, depth) {
	this.menuId = id;
	this.childItems = new Array();
	this.parentItem = parent;
	this.depth = depth;
	this.openChildMenus = new Array();
}

ArcoMenu.prototype.stopHiding = function() {
	if (!this.parentItem) return true;
	window.clearTimeout(this.parentItem.hideTimer);
	this.parentItem.hideTimer = null;
	this.parentItem.parentMenu.stopHiding();
}

ArcoMenu.prototype.hideAll = function() {
	for(var i in this.openChildMenus) {
		if (!this.openChildMenus[i]) continue; // blub
		this.openChildMenus[i].hide();
	}
}

ArcoMenu.prototype.over = function() {
	if (this.arco.menuId == 0) return false; // mainMenu
	this.arco.parentItem.show();
	this.arco.parentItem.parentMenu.stopHiding();
}

ArcoMenu.prototype.out = function() {
	if (this.arco.menuId == 0) return false; // mainMenu
	this.arco.parentItem.delayHide();
}

ArcoMenu.prototype.menuDiv = null;
ArcoMenu.prototype.parentItem = null;
ArcoMenu.prototype.childItems;
ArcoMenu.prototype.openChildMenus;

ArcoMenu.prototype.generateObject = function(depth) {
	if (depth < 0) alert("Invalid depth value (" + depth + ")");
	this.depth = depth;
	var arVersion = navigator.appVersion.split("MSIE");
	var version = parseFloat(arVersion[1]);
	this.menuDiv = document.createElement("div");
	switch(depth) {
		case 0:
			this.menuDiv.style.position = "absolute";
			this.menuDiv.arco = this;
			this.menuDiv.onmouseover = this.over;
			this.menuDiv.onmouseout = this.out;
			for(var i in this.childItems) { // i am bad i know - not made for arrays
				
				this.menuDiv.appendChild(this.childItems[i].generateObject());
			}
		break;
		case 1:
			this.menuDiv.style.position = "absolute";
			this.menuDiv.style.left = "93px";
			this.menuDiv.style.marginTop = "-31px";
			var dummy = document.createElement("div");
			dummy.style.marginLeft = "0px";
			if ((version >= 5.5) && (version < 7.0)) {
				dummy.appendChild(document.createPNGImage("resources/skins/gftDesign/images/design/subnavi_fade1.png"));
			}
			else {
				dummy.appendChild(document.createElement("img"));
				dummy.firstChild.src = "resources/skins/gftDesign/images/design/subnavi_fade1.png";
				dummy.firstChild.alt = "";
			}
			this.menuDiv.appendChild(dummy);
			var rel = document.createElement("div");
			rel.style.position = "relative";
			rel.style.left = "39px";
			rel.style.top = "-47px";
			dummy = document.createElement("div");
			if ((version >= 5.5) && (version < 7.0)) {
				dummy.appendChild(document.createPNGImage("resources/skins/gftDesign/images/design/subnavi_fade2.png"));
			}
			else {
				dummy.appendChild(document.createElement("img"));
				dummy.firstChild.src = "resources/skins/gftDesign/images/design/subnavi_fade2.png";
				dummy.firstChild.alt = "";
			}
			rel.appendChild(dummy); // before stretch
			var stretch = document.createElement("div");
			stretch.style.position = "absolute";
			stretch.style.top = "8px";
			stretch.style.left = "0px";
			stretch.style.paddingTop = "0px"; // -8
//			if ((version >= 5.5) && (version < 7.0)) {
//				stretch.style.display = "inline-block";
//				stretch.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='resources/skins/gftDesign/images/design/subnavi_fade_stretch.png',sizingMethod='scale',Enabled=true)"
////				
//				
//				stretch.style.background = "none";
//			}
//			else {
				stretch.style.backgroundImage = "url(resources/skins/gftDesign/images/design/subnavi_fade_stretch.png)";
				stretch.style.backgroundRepeat = "repeat-y";
//			}
			stretch.style.width = "136px";
			var blub1 = document.createElement("div");
			blub1.style.marginLeft = "22px";
			blub1.style.backgroundColor = "#DEDEDE";
			//blub1.style.width = "114px";
			var blub2 = document.createElement("div");
//			blub2.style.position = "absolute";
//			blub2.style.top = "0px";
//			blub2.style.left = "0px";
//			blub2.style.zIndex = "11";
			blub2.style.marginLeft = "-22px";
			blub2.style.marginTop = "-8px"; // margin top -55px
			
			blub2.onmouseover = this.over;
			blub2.onmouseout = this.out;
			blub2.arco = this;
			//blub2.style.position = "relative";
			//blub2.style.zIndex = "10";
			for(var i in this.childItems) {
				this.childItems[i].parentMenu = this;
				blub2.appendChild(this.childItems[i].generateObject());
			}
			blub1.appendChild(blub2);
			stretch.appendChild(blub1);
			rel.appendChild(stretch);
			this.menuDiv.appendChild(rel);
		break;
		default:
			this.menuDiv.style.position = "absolute";
			this.menuDiv.style.left = "0px";
			this.menuDiv.style.width = "100%"; // opera fix
			var dummy = document.createElement("div");
			dummy.style.position = "relative";
			dummy.style.left = "136px";
			dummy.style.top = "-30px";
			dummy.style.backgroundColor = "#DEDEDE";
			dummy.onmouseover = this.over;
			dummy.onmouseout = this.out;
			dummy.arco = this;
			for(var i in this.childItems) {
				dummy.appendChild(this.childItems[i].generateObject());
			}
			this.menuDiv.appendChild(dummy);
		break;
	}
	return this.menuDiv;
}