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

function openRegForm(){
	$.post(root_url+"registerForm.php", {},
			function(data){
				$("#blurLayer").attr("style", "display:block");
				$("#registerForm").html(data);
				var left = $(window).width()/2 - $("#registerForm").width()/2;
				var top = $(window).height()/2 - $("#registerForm").height()/2;	
				$("#registerForm").attr("style", "display:block;left:"+left+"px;top:"+top+"px;");				
			});
}

function register(company, title, url, content, cost){
	$.post(root_url + "controllers/controller.php", 
			{request:"register", company:company, title:title, url:url, content:content, cost:cost},
			function(data){
				clearAll();
				if(data == "success")
					alert("Registered Your Advertisement.\nThank You!");
				else
					alert("Register is failded.\nPlease try again.");				
			});
}

/*
function getSearchResult(query){
	$.post("localhost:25804/search", 
			{query:query, ranker:"favorite", format:"html"},
			function(data){
				alert(data);				
			});
}

function getAdsResult(query){
	alert("ads : " + query);
}
*/