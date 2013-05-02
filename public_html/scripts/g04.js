var root_url = "http://cs.nyu.edu/~jj1233/";

$(document).ready(function(){
	$("#blurLayer").click(function(e){
		clearAll();		
	});
})

function clearAll(){
	$("#blurLayer").fadeOut();
	$("#registerForm").fadeOut();
}

function getInstance()
{	
	if(window.XMLHttpRequest) // For using IE7, Firefox, Chrome, Opera, Safari
	{
		xmlhttp = new XMLHttpRequest();
	} 
	else if (window.ActiveXObject) // For using IE6, IE5
	{
		xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
	} 
	else	// This browser cannot use Ajax
		alert("I'm so sorry. Your browser cannot support the XMLHTTP");
	return xmlhttp;
}

function openRegForm(){
	xmlhttp = getInstance();
	xmlhttp.onreadystatechange=function(){
		if(xmlhttp.readyState == 4)
		{
			$("#blurLayer").attr("style", "display:block");
			$("#registerForm").html(xmlhttp.responseText);
			var left = $(window).width()/2 - $("#registerForm").width()/2;
			var top = $(window).height()/2 - $("#registerForm").height()/2;	
			$("#registerForm").attr("style", "display:block;left:"+left+"px;top:"+top+"px;");
		}
	}
	var url = root_url + "registerForm.php";
	xmlhttp.open("GET", url, true);
	xmlhttp.send(null);
}

function register(company, title, url, content, cost){
	xmlhttp = getInstance();
	xmlhttp.onreadystatechange=function(){
		if(xmlhttp.readyState == 4)
		{
			clearAll();
			if(xmlhttp.responseText == "success")
				alert("Registered Your Advertisement.\nThank You!");
			else
				alert("Register is failded.\nPlease try again.");
		}
	}
	var condition = "?request=register";
	condition += "&company=" + company;
	condition += "&title=" + title;
	condition += "&url=" + url;
	condition += "&content=" + content;
	condition += "&cost=" + cost;
	var path = root_url + "controllers/controller.php" + condition;
	xmlhttp.open("GET", path, true);
	xmlhttp.send(null);	
}

function getSearchResult(query){
	xmlhttp = getInstance();
	xmlhttp.onreadystatechange=function(){
		if(xmlhttp.readyState == 4)
		{
			alert(xmlhttp.responseText);
		}
	}

	var condition = "/search?";
	condition += "query='" + query + "'";
	condition += "&ranker=favorite";
	condition += "&format=html";
	var path = "localhost:25804" + condition;
	xmlhttp.open("GET", path, true);
	xmlhttp.send(null);	
}

function getAdsResult(query){
	alert("ads : " + query);
}